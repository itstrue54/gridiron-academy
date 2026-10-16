package io.github.itstrue54.gridironacademy.ui.coach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.itstrue54.gridironacademy.content.Content
import io.github.itstrue54.gridironacademy.data.CoachDashboard
import io.github.itstrue54.gridironacademy.data.CoachRepository
import io.github.itstrue54.gridironacademy.data.SendResult
import io.github.itstrue54.gridironacademy.model.PackSide
import io.github.itstrue54.gridironacademy.progress.AssignmentRules
import io.github.itstrue54.gridironacademy.progress.NoteRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The four tabs (DESIGN 14.1). A parent with no team has no Playbook. */
enum class CoachTab { PLAYERS, ASSIGN, PLAYBOOK, MORE }

sealed interface CoachScreen {
    data object Players : CoachScreen
    data class Learner(val learnerId: String) : CoachScreen
    data class TeamView(val teamId: String) : CoachScreen
    data object Assign : CoachScreen
    data object Playbook : CoachScreen
    data object More : CoachScreen
}

enum class AssignStep { WHAT, WHO, OPTIONS, PREVIEW }

/** The four-step Assign sheet's choices so far (DESIGN 14.4). */
data class AssignDraft(
    val step: AssignStep = AssignStep.WHAT,
    val side: PackSide? = null,
    val packIds: List<String> = emptyList(),
    val learnerIds: List<String> = emptyList(),
    val teamId: String? = null,
    /** Days from today; null is no due date. */
    val dueInDays: Int? = DEFAULT_DUE_DAYS,
    val passPct: Int = AssignmentRules.DEFAULT_PASS_PCT,
    val note: String = "",
) {
    val noteOk: Boolean get() = NoteRules.problems(note).isEmpty()
    val canContinue: Boolean get() = when (step) {
        AssignStep.WHAT -> packIds.isNotEmpty()
        AssignStep.WHO -> learnerIds.isNotEmpty()
        AssignStep.OPTIONS -> noteOk
        AssignStep.PREVIEW -> true
    }

    companion object {
        const val DEFAULT_DUE_DAYS = 7
    }
}

/**
 * The Coach app's navigation and the assignment in progress. Every number on screen comes
 * from a [CoachDashboard] over the repository's snapshot; this class holds only choices.
 */
class CoachAppState(
    val content: Content,
    val repository: CoachRepository,
    val today: Long,
    private val scope: CoroutineScope,
) {
    var tab by mutableStateOf(CoachTab.PLAYERS)
        private set
    private val stack = mutableStateListOf<CoachScreen>()

    val screen: CoachScreen get() = stack.lastOrNull() ?: root(tab)

    var draft by mutableStateOf(AssignDraft())
        private set

    /** The last send, for the confirmation on Players; cleared when read. */
    var sent by mutableStateOf<SendResult?>(null)
        private set

    var sending by mutableStateOf(false)
        private set

    fun select(t: CoachTab) {
        tab = t
        stack.clear()
    }

    fun open(s: CoachScreen) {
        stack += s
    }

    /** True if handled; false lets the platform close the app. */
    fun back(): Boolean = when {
        tab == CoachTab.ASSIGN && stack.isEmpty() && draft.step != AssignStep.WHAT -> {
            draft = draft.copy(step = AssignStep.entries[draft.step.ordinal - 1]); true
        }
        stack.isNotEmpty() -> { stack.removeAt(stack.lastIndex); true }
        tab != CoachTab.PLAYERS -> { select(CoachTab.PLAYERS); true }
        else -> false
    }

    /** Open Assign with choices filled in ("Assign practice" from a learner card). */
    fun startAssign(packIds: List<String> = emptyList(), learnerIds: List<String> = emptyList()) {
        draft = AssignDraft(
            step = if (packIds.isNotEmpty() && learnerIds.isNotEmpty()) AssignStep.OPTIONS else AssignStep.WHAT,
            packIds = packIds,
            learnerIds = learnerIds,
        )
        select(CoachTab.ASSIGN)
    }

    fun edit(change: (AssignDraft) -> AssignDraft) {
        draft = change(draft)
    }

    fun next() {
        if (draft.canContinue && draft.step != AssignStep.PREVIEW) draft = draft.copy(step = AssignStep.entries[draft.step.ordinal + 1])
    }

    fun send(dashboard: CoachDashboard) {
        if (sending) return
        val d = draft
        val packs = d.packIds.mapNotNull { content.pack(it) }
        val a = dashboard.draft(packs, d.learnerIds, d.dueInDays?.let { today + it }, d.passPct, d.note, d.teamId)
        sending = true
        scope.launch {
            val result = repository.send(a)
            sending = false
            sent = result
            if (result is SendResult.Sent) {
                draft = AssignDraft()
                select(CoachTab.PLAYERS)
            }
        }
    }

    fun clearSent() {
        sent = null
    }

    private fun root(t: CoachTab): CoachScreen = when (t) {
        CoachTab.PLAYERS -> CoachScreen.Players
        CoachTab.ASSIGN -> CoachScreen.Assign
        CoachTab.PLAYBOOK -> CoachScreen.Playbook
        CoachTab.MORE -> CoachScreen.More
    }
}
