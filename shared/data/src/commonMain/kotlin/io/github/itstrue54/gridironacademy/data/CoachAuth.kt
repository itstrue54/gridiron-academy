package io.github.itstrue54.gridironacademy.data

import io.github.itstrue54.gridironacademy.progress.AgeBand
import kotlinx.coroutines.flow.StateFlow

sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class SignedIn(val email: String?) : Session
}

sealed interface AuthResult {
    data object Ok : AuthResult
    /** Signed up; the account works once the emailed link is opened. */
    data object CheckEmail : AuthResult
    data class Failed(val message: String) : AuthResult
}

/** Sign-in for a real account (SPEC 7.2 step 1). The demo has none. */
interface CoachAuth {
    val session: StateFlow<Session>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String, displayName: String): AuthResult
    suspend fun signOut()
}

/**
 * The first steps of a real account (SPEC 7.2): add your child, create a team. Each
 * returns null on success or a message to show.
 */
interface CoachSetup {
    suspend fun addChild(nickname: String, ageBand: AgeBand): String?
    suspend fun createTeam(name: String, variant: String, ageGroup: String, season: String): String?
}
