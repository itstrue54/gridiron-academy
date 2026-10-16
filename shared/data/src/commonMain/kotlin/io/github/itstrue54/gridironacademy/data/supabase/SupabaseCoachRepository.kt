package io.github.itstrue54.gridironacademy.data.supabase

import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.data.*
import io.github.itstrue54.gridironacademy.model.Track
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.progress.Assignment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/** Where the project lives. The anon key is public by design; RLS does the protecting. */
data class SupabaseConfig(val url: String, val anonKey: String)

/**
 * The Coach app on Supabase (ADR-004). Reads everything the signed-in adult may see (row-
 * level security does the filtering) and folds it with [SnapshotAssembler]; writes go
 * through the same policies, sends through `send_assignment` in one transaction.
 */
class SupabaseCoachRepository(
    private val client: SupabaseClient,
    private val content: Content,
    private val today: () -> Long,
    scope: CoroutineScope,
) : CoachRepository, CoachAuth, CoachSetup {

    private val state = MutableStateFlow(empty(""))
    override val snapshot: StateFlow<CoachSnapshot> = state.asStateFlow()

    override val session: StateFlow<Session> = client.auth.sessionStatus.map { s ->
        when (s) {
            is SessionStatus.Authenticated -> Session.SignedIn(s.session.user?.email)
            is SessionStatus.Initializing -> Session.Loading
            else -> Session.SignedOut
        }
    }.stateIn(scope, SharingStarted.Eagerly, Session.Loading)

    override suspend fun signIn(email: String, password: String): AuthResult = attempt {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        ensureAccount(null)
        refresh()
        AuthResult.Ok
    }

    override suspend fun signUp(email: String, password: String, displayName: String): AuthResult = attempt {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put(DISPLAY_NAME, displayName.trim()) }
        }
        // With email confirmation on (the default) there's no session until the link is opened.
        if (client.auth.currentSessionOrNull() == null) return@attempt AuthResult.CheckEmail
        ensureAccount(displayName.trim())
        refresh()
        AuthResult.Ok
    }

    override suspend fun signOut() {
        runCatching { client.auth.signOut() }
        state.value = empty("")
    }

    override suspend fun refresh() {
        val uid = client.auth.currentUserOrNull()?.id ?: return
        val me = client.from("account").select { filter { eq("id", uid) } }.decodeSingleOrNull<AccountRow>()
            ?: ensureAccount(null) ?: return
        val rows = ServerRows(
            me = me,
            learners = all("learner", "id"),
            teams = all("team", "id"),
            staff = all("staff", "team_id", "account_id"),
            members = all("team_member", "team_id", "learner_id"),
            events = all("event", "learner_id", "device_id", "seq"),
            assignments = all("assignment", "id"),
            targets = all("assignment_target", "assignment_id", "learner_id"),
        )
        state.value = SnapshotAssembler.assemble(rows, content, today())
    }

    override suspend fun send(draft: Assignment): SendResult = try {
        val id = client.postgrest.rpc(
            "send_assignment",
            buildJsonObject {
                put("team", draft.teamId?.let(::JsonPrimitive) ?: JsonNull)
                put("from_name", draft.fromName)
                putJsonArray("pack_ids") { draft.packIds.forEach { add(it) } }
                put("due_day", draft.dueDay?.let(::JsonPrimitive) ?: JsonNull)
                put("pass_pct", draft.passPct)
                put("note", draft.note)
                put("sent_day", today())
                putJsonArray("targets") {
                    draft.targets.forEach { t ->
                        addJsonObject { put("learner_id", t.learnerId); put("lessons_at_send", t.lessonsAtSend) }
                    }
                }
            },
        ).decodeAs<String>()
        refresh()
        SendResult.Sent(draft.copy(id = id, sentDay = today()))
    } catch (e: Exception) {
        SendResult.Rejected(friendly(e))
    }

    override suspend fun addChild(nickname: String, ageBand: AgeBand): String? = write {
        val uid = client.auth.currentUserOrNull()?.id ?: return@write
        client.from("learner").insert(
            LearnerRow(nickname = nickname.trim(), ageBand = ageBand.label, track = if (ageBand.track == Track.KID) "kid" else "adult", managedBy = uid),
        )
    }

    override suspend fun createTeam(name: String, variant: String, ageGroup: String, season: String): String? = write {
        val uid = client.auth.currentUserOrNull()?.id ?: return@write
        client.from("team").insert(TeamRow(ownerId = uid, name = name.trim(), variant = variant.trim(), ageGroup = ageGroup.trim(), season = season.trim()))
    }

    /** Creates the caller's account row the first time; returns it. */
    private suspend fun ensureAccount(name: String?): AccountRow? {
        val user = client.auth.currentUserOrNull() ?: return null
        val display = name?.takeIf { it.isNotBlank() }
            ?: user.userMetadata?.get(DISPLAY_NAME)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: "Coach"
        val row = AccountRow(user.id, display.take(MAX_NAME))
        client.from("account").upsert(row) {
            onConflict = "id"
            ignoreDuplicates = true
        }
        return client.from("account").select { filter { eq("id", user.id) } }.decodeSingleOrNull<AccountRow>()
    }

    /** Every row of a table, a page at a time (PostgREST caps each response). */
    private suspend inline fun <reified T : Any> all(table: String, vararg orderBy: String): List<T> {
        val out = mutableListOf<T>()
        var from = 0L
        while (true) {
            val page = client.from(table).select {
                orderBy.forEach { order(it, Order.ASCENDING) }
                range(from, from + PAGE - 1)
            }.decodeList<T>()
            out += page
            if (page.size < PAGE) return out
            from += PAGE
        }
    }

    private suspend fun write(block: suspend () -> Unit): String? = try {
        block(); refresh(); null
    } catch (e: Exception) {
        friendly(e)
    }

    private suspend fun attempt(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: Exception) {
        AuthResult.Failed(friendly(e))
    }

    private fun empty(id: String) = CoachSnapshot(
        CoachAccount(id, "", isParent = false, isCoach = false), emptyList(), emptyList(), emptyMap(), emptyList(), emptyList(), demo = false,
    )

    private companion object {
        const val PAGE = 1000L
        const val MAX_NAME = 60
        const val DISPLAY_NAME = "display_name"

        /** The server's own words where they're meant for people; a plain line otherwise. */
        fun friendly(e: Exception): String {
            val m = e.message.orEmpty()
            return when {
                "Invalid login credentials" in m -> "That email and password don't match."
                "already registered" in m -> "That email already has an account. Sign in instead."
                "Email not confirmed" in m -> "Open the link we emailed you, then sign in."
                "violates check" in m && "note" in m -> "The note can't contain links, emails or phone numbers."
                "row-level security" in m -> "You don't have access to that."
                "Unable to resolve host" in m || "timeout" in m.lowercase() || "connect" in m.lowercase() -> "No connection. Try again when you're online."
                else -> m.lineSequence().firstOrNull()?.take(160)?.ifBlank { null } ?: "Something went wrong. Try again."
            }
        }
    }
}

/** Builds the client the Coach app needs: auth (session kept on the device) and the database. */
fun supabaseCoach(config: SupabaseConfig, content: Content, today: () -> Long, scope: CoroutineScope): SupabaseCoachRepository {
    val client = io.github.jan.supabase.createSupabaseClient(config.url, config.anonKey) {
        install(io.github.jan.supabase.auth.Auth)
        install(io.github.jan.supabase.postgrest.Postgrest)
    }
    return SupabaseCoachRepository(client, content, today, scope)
}
