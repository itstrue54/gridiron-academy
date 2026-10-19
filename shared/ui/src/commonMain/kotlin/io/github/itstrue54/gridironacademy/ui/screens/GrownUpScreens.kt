package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.engine.Rng
import io.github.itstrue54.gridironacademy.progress.AgeBand
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.app.LearnerPlatform
import io.github.itstrue54.gridironacademy.ui.app.RestoreResult
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.components.SecondaryButton
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import org.jetbrains.compose.resources.stringResource

/**
 * The grown-up gate (DESIGN 9.9): a multiplication a young child can't guess (both
 * factors 6 to 9, typed, not picked), then a two-second hold. Screen-reader users get a
 * tap in place of the hold; the question is the real lock.
 */
@Composable
fun GrownUpGateScreen(state: LearnerAppState, seed: Long) {
    val c = Gridiron.colors
    var round by remember { mutableStateOf(0) }
    val (a, b) = remember(round) { Rng(seed + round).let { r -> (MIN_FACTOR + r.nextInt(FACTOR_SPAN)) to (MIN_FACTOR + r.nextInt(FACTOR_SPAN)) } }
    var typed by remember(round) { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val right = typed.toIntOrNull() == a * b
    fun attempt() {
        if (right) state.enterGrownUpZone() else { wrong = true; round++ }
    }
    Column(
        Modifier.fillMaxSize().screenBackground().safeDrawingPadding().padding(Gridiron.skin.gutter).imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.grown_ups_only), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
        }
        Text(stringResource(Res.string.gate_body), style = Gridiron.type.body, color = c.inkDim)
        Text("$a × $b = ?", style = Gridiron.type.displayLarge, color = c.ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        OutlinedTextField(
            value = typed, onValueChange = { v -> typed = v.filter { it.isDigit() }.take(3); wrong = false },
            label = { Text(stringResource(Res.string.gate_answer)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = Gridiron.type.title.copy(color = c.ink), modifier = Modifier.fillMaxWidth(),
        )
        if (wrong) Text(stringResource(Res.string.gate_wrong), style = Gridiron.type.label, color = c.retry)
        HoldButton(stringResource(Res.string.gate_hold), enabled = typed.isNotEmpty(), onHeld = ::attempt)
    }
}

/** Press and hold for [HOLD_MS] to fire; the ring fills while held and empties on release. */
@Composable
private fun HoldButton(label: String, enabled: Boolean, onHeld: () -> Unit) {
    val c = Gridiron.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
            onHeld()
            progress.snapTo(0f)
        } else progress.animateTo(0f, tween(RELEASE_MS))
    }
    Surface(
        onClick = {},
        enabled = enabled,
        interactionSource = source,
        shape = Gridiron.skin.buttonShape,
        color = if (enabled) c.gold else c.line,
        contentColor = if (enabled) c.onGold else c.inkDim,
        modifier = Modifier.fillMaxWidth().heightIn(min = Gridiron.skin.touchTarget)
            .semantics { onClick(label) { if (enabled) onHeld(); enabled } },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(22.dp)) {
                drawArc(c.onGold.copy(alpha = 0.3f), 0f, 360f, false, style = Stroke(3.dp.toPx()))
                drawArc(c.onGold, -90f, 360f * progress.value, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = Gridiron.type.cta)
        }
    }
}

/** The grown-up zone (DESIGN 9.9): what this build can do without the backend. */
@Composable
fun GrownUpZoneScreen(state: LearnerAppState, platform: LearnerPlatform) {
    val c = Gridiron.colors
    val profile = state.profile ?: return
    // Follows the profile: after a restore or a save the box shows the name now in effect,
    // so "Save name" can't quietly undo a restore (found on the phone).
    var name by remember(profile.nickname) { mutableStateOf(profile.nickname) }
    var note by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    val saved = stringResource(Res.string.backup_saved)
    val notSaved = stringResource(Res.string.backup_not_saved)
    val restored = stringResource(Res.string.backup_restored)
    val tooNew = stringResource(Res.string.backup_too_new)
    val notBackup = stringResource(Res.string.backup_not_backup)
    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.grown_up_zone), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gridiron.skin.gutter).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            note?.let { Card(edge = c.link) { Text(it, style = Gridiron.type.body, color = c.ink) } }

            Section(stringResource(Res.string.zone_profile))
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(20) }, singleLine = true,
                label = { Text(stringResource(Res.string.nickname_label)) }, modifier = Modifier.fillMaxWidth(),
                textStyle = Gridiron.type.body.copy(color = c.ink),
            )
            if (name.trim() != profile.nickname && name.isNotBlank()) {
                SecondaryButton(stringResource(Res.string.save_name), onClick = { state.renameProfile(name) })
            }
            Text(stringResource(Res.string.zone_age), style = Gridiron.type.label, color = c.inkDim)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBand.entries.forEach { band ->
                    FilterChip(
                        selected = profile.ageBand == band, onClick = { state.changeAgeBand(band) },
                        label = { Text(band.label, style = Gridiron.type.body) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink),
                    )
                }
            }
            Text(stringResource(Res.string.zone_age_note), style = Gridiron.type.label, color = c.inkDim)

            Section(stringResource(Res.string.zone_report))
            Text(stringResource(Res.string.report_minutes, state.minutesThisWeek()), style = Gridiron.type.body, color = c.ink)
            Text(stringResource(Res.string.report_mastery_note), style = Gridiron.type.label, color = c.inkDim)
            state.progressReport().forEach { r ->
                Card(edge = if (r.passed) c.correct else null) {
                    Column {
                        Text(stringResource(Res.string.level_named_title, r.level.number, r.level.title), style = Gridiron.type.answer, color = c.ink)
                        Text(stringResource(Res.string.report_line, r.drillsPassed, r.drills, r.conceptsMastered, r.concepts),
                            style = Gridiron.type.label, color = c.inkDim)
                    }
                }
            }

            Section(stringResource(Res.string.zone_backup))
            Text(stringResource(Res.string.backup_body), style = Gridiron.type.label, color = c.inkDim)
            SecondaryButton(stringResource(Res.string.backup_export), onClick = {
                platform.exportBackup(state.backupText(), "gridiron-academy-${profile.nickname.filter { it.isLetterOrDigit() }.ifEmpty { "player" }}.json") { ok ->
                    note = if (ok) saved else notSaved
                }
            })
            SecondaryButton(stringResource(Res.string.backup_import), onClick = { confirmImport = true })

            Section(stringResource(Res.string.zone_privacy))
            Text(stringResource(Res.string.privacy_statement), style = Gridiron.type.body, color = c.ink)

            Section(stringResource(Res.string.zone_about))
            Text(stringResource(Res.string.about_line, platform.appVersion, state.content.curriculum.version),
                style = Gridiron.type.label, color = c.inkDim)
        }
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(Res.string.backup_import)) },
            text = { Text(stringResource(Res.string.backup_import_warning, profile.nickname)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    platform.pickBackup { text ->
                        note = when (text?.let { state.restoreBackup(it) }) {
                            null -> null
                            RestoreResult.Restored -> restored
                            RestoreResult.TooNew -> tooNew
                            RestoreResult.NotABackup -> notBackup
                        }
                    }
                }) { Text(stringResource(Res.string.backup_choose_file)) }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), style = Gridiron.type.eyebrow, color = Gridiron.colors.inkDim,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() })
}

private const val MIN_FACTOR = 6
private const val FACTOR_SPAN = 4
private const val HOLD_MS = 2_000
private const val RELEASE_MS = 200
