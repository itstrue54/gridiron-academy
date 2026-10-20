package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.content.MapLayout
import io.github.itstrue54.gridironacademy.content.MapNode
import io.github.itstrue54.gridironacademy.content.PlaybookMap
import io.github.itstrue54.gridironacademy.model.GlossaryEntry
import io.github.itstrue54.gridironacademy.model.PackSide
import io.github.itstrue54.gridironacademy.progress.MasteryState
import io.github.itstrue54.gridironacademy.ui.app.LearnerAppState
import io.github.itstrue54.gridironacademy.ui.coach.MasteryGlyph
import io.github.itstrue54.gridironacademy.ui.coach.masteryWord
import io.github.itstrue54.gridironacademy.ui.components.Card
import io.github.itstrue54.gridironacademy.ui.components.Glyph
import io.github.itstrue54.gridironacademy.ui.components.GlyphIcon
import io.github.itstrue54.gridironacademy.ui.components.IconAction
import io.github.itstrue54.gridironacademy.ui.resources.*
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.screenBackground
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Playbook map (DESIGN 9.14): the concept graph as a vertical skill tree per side,
 * each concept a MasteryNode joined by chalk lines to what it builds on. Pinch to zoom,
 * drag to move; a search lights up matches; List shows the same tree as rows for
 * TalkBack. Tapping a concept opens its glossary card with Quiz me.
 */
@Composable
fun PlaybookMapScreen(state: LearnerAppState) {
    val c = Gridiron.colors
    val entries = state.content.glossary.entries
    val byId = remember(entries) { entries.associateBy { it.id } }
    var sideName by rememberSaveable { mutableStateOf(PackSide.OFFENSE.name) }
    val side = PackSide.valueOf(sideName)
    var listMode by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    val layout = remember(entries, side) { PlaybookMap.layout(entries, side) }
    val mastery = remember(state.save) { state.conceptMastery() }
    // A concept no step on this ladder teaches can't be mastered here: shown, not counted.
    val onLadder = remember(state.save.profile) { state.ladderConcepts() }
    fun stateOf(id: String) = mastery[id]?.state ?: MasteryState.NEW

    val q = query.trim().lowercase()
    val matches = remember(q, layout) {
        if (q.isEmpty()) emptySet()
        else layout.nodes.filter { q in it.entry.term.lowercase() }.map { it.entry.id }.toSet()
    }
    // A search with nothing on this tab moves to the first tab that has it.
    LaunchedEffect(q) {
        if (q.isNotEmpty() && matches.isEmpty()) {
            entries.firstOrNull { q in it.term.lowercase() }?.let { sideName = it.side.name }
        }
    }
    fun open(id: String) {
        byId[id]?.let { sideName = it.side.name }
        selected = id
    }

    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.playbook_map), style = Gridiron.type.headline, color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() })
            TextButton(onClick = { listMode = !listMode }) {
                Text(stringResource(if (listMode) Res.string.map_show_map else Res.string.map_show_list),
                    style = Gridiron.type.label, color = c.link)
            }
        }
        Column(Modifier.padding(horizontal = Gridiron.skin.gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.glossary_search), style = Gridiron.type.body) },
                singleLine = true, textStyle = Gridiron.type.body.copy(color = c.ink),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PackSide.entries.forEach { s ->
                    FilterChip(
                        selected = s == side, onClick = { sideName = s.name; selected = null },
                        label = { Text(stringResource(sideLabel(s)), style = Gridiron.type.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = c.gold, selectedLabelColor = c.onGold, labelColor = c.ink),
                    )
                }
            }
            val countable = layout.nodes.filter { it.entry.id in onLadder }
            val done = countable.count { stateOf(it.entry.id) == MasteryState.MASTERED }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.map_progress, done, countable.size), style = Gridiron.type.label, color = c.inkDim,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { showKey = !showKey }) {
                    Text(stringResource(if (showKey) Res.string.map_key_hide else Res.string.map_key), style = Gridiron.type.label, color = c.link)
                }
            }
            if (showKey) Legend()
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sheetMax = maxHeight * SHEET_SHARE
            if (listMode) MapList(layout, byId, matches, q.isNotEmpty(), onLadder, ::stateOf, ::open)
            else MapCanvas(layout, matches, q.isNotEmpty(), selected, onLadder, ::stateOf, onTap = { selected = it })
            selected?.let { byId[it] }?.let { e ->
                ConceptSheet(state, e, entries, layout, stateOf(e.id), e.id in onLadder, Modifier.align(Alignment.BottomCenter).heightIn(max = sheetMax),
                    onOpen = ::open, onClose = { selected = null })
            }
        }
    }
}

@Composable
private fun MapCanvas(
    layout: MapLayout,
    matches: Set<String>,
    searching: Boolean,
    selected: String?,
    onLadder: Set<String>,
    stateOf: (String) -> MasteryState,
    onTap: (String) -> Unit,
) {
    val c = Gridiron.colors
    val density = LocalDensity.current
    var view by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(layout.side) { mutableStateOf(1f) }
    var pan by remember(layout.side) { mutableStateOf(Offset.Zero) }
    val colW = with(density) { (view.width / PlaybookMap.MAX_COLS).toDp() }
    val rowH = ROW_HEIGHT
    val contentW = view.width.toFloat()
    val contentH = with(density) { (rowH * layout.rows).toPx() + TOP_PAD.toPx() * 2 }

    fun clamp(t: Offset, s: Float): Offset {
        fun axis(v: Float, size: Float, viewSize: Float, centre: Boolean): Float {
            val scaled = size * s
            return if (scaled <= viewSize) (if (centre) (viewSize - scaled) / 2 else 0f) else v.coerceIn(viewSize - scaled, 0f)
        }
        return Offset(axis(t.x, contentW, view.width.toFloat(), centre = true), axis(t.y, contentH, view.height.toFloat(), centre = false))
    }

    fun show(n: MapNode) {
        val y = with(density) { (TOP_PAD + rowH * n.row).toPx() } * scale
        pan = clamp(Offset(pan.x, -y + view.height / 8f), scale)
    }
    // Search: bring the first match into view. A tapped concept moves above its card.
    LaunchedEffect(matches, view) { layout.nodes.firstOrNull { it.entry.id in matches }?.let(::show) }
    LaunchedEffect(selected, view) { selected?.let { layout.byId[it] }?.let(::show) }

    Box(
        Modifier.fillMaxSize().clipToBounds().onSizeChanged { view = it }
            .pointerInput(layout.side, view) {
                detectTransformGestures { centroid, delta, zoom, _ ->
                    val s = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    val t = centroid - (centroid - pan) * (s / scale) + delta
                    scale = s
                    pan = clamp(t, s)
                }
            },
    ) {
        if (view == IntSize.Zero) return@Box
        Box(
            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                .requiredSize(with(density) { contentW.toDp() }, with(density) { contentH.toDp() })
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale; scaleY = scale
                    translationX = pan.x; translationY = pan.y
                },
        ) {
            val lineColor = c.line
            val hot = c.gold
            Canvas(Modifier.matchParentSize()) {
                val cw = size.width / PlaybookMap.MAX_COLS
                val rh = rowH.toPx()
                val top = TOP_PAD.toPx()
                val r = NODE.toPx() / 2
                fun centre(n: MapNode) = Offset(((n.x + 0.5) * cw).toFloat(), top + n.row * rh + r)
                // Every line faint; the tapped concept's lines drawn last, in gold.
                layout.edges.sortedBy { selected != null && (it.from == selected || it.to == selected) }.forEach { e ->
                    val a = centre(layout.byId.getValue(e.from))
                    val b = centre(layout.byId.getValue(e.to))
                    val lit = selected != null && (e.from == selected || e.to == selected)
                    val from = Offset(a.x, a.y + r)
                    val to = Offset(b.x, b.y - r)
                    val bend = (to.y - from.y) / 2
                    val path = Path().apply {
                        moveTo(from.x, from.y)
                        cubicTo(from.x, from.y + bend, to.x, to.y - bend, to.x, to.y)
                    }
                    drawPath(path, if (lit) hot else lineColor.copy(alpha = FAINT),
                        style = Stroke(width = (if (lit) 3.dp else 1.5.dp).toPx(), cap = StrokeCap.Round))
                }
            }
            layout.nodes.forEach { n ->
                val id = n.entry.id
                MasteryNode(
                    n.entry, stateOf(id),
                    highlighted = id in matches || id == selected,
                    dimmed = searching && id !in matches,
                    offLadder = id !in onLadder,
                    modifier = Modifier
                        .offset(x = colW * n.x.toFloat(), y = TOP_PAD + rowH * n.row)
                        .width(colW),
                    onClick = { onTap(id) },
                )
            }
        }
        Text(stringResource(Res.string.map_zoom_hint), style = Gridiron.type.label, color = c.inkDim,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .background(c.surface.copy(alpha = HINT_BACK), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                .clearAndSetSemantics { })
    }
}

/** DESIGN 8: the state glyph in a circle and the concept's name under it. */
@Composable
private fun MasteryNode(
    entry: GlossaryEntry,
    mastery: MasteryState,
    highlighted: Boolean,
    dimmed: Boolean,
    offLadder: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Gridiron.colors
    val word = stringResource(if (offLadder) Res.string.map_off_course else masteryWord(mastery))
    val desc = stringResource(Res.string.map_node, entry.term, word)
    Column(
        modifier.alpha(if (dimmed) DIMMED else 1f).clickable(onClick = onClick).padding(horizontal = 2.dp)
            .clearAndSetSemantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(NODE).background(c.surfaceRaised, CircleShape)
                .border(if (highlighted) 3.dp else 1.dp, if (highlighted) c.gold else c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (!offLadder) MasteryGlyph(mastery, size = GLYPH) else GlyphIcon(Glyph.DASH, c.inkDim, size = GLYPH) }
        // A long word shrinks rather than breaking mid-word ("Quarterbac / k").
        val style = Gridiron.type.label
        val long = entry.term.split(' ', '-').maxOf { it.length } > LONG_WORD
        Text(entry.term, style = if (long) style.copy(fontSize = style.fontSize * SHRINK) else style, color = c.ink, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The same tree as rows, basics first: what TalkBack users get (DESIGN 9.14). */
@Composable
private fun MapList(
    layout: MapLayout,
    byId: Map<String, GlossaryEntry>,
    matches: Set<String>,
    searching: Boolean,
    onLadder: Set<String>,
    stateOf: (String) -> MasteryState,
    onOpen: (String) -> Unit,
) {
    val c = Gridiron.colors
    val shown = layout.nodes.filter { !searching || it.entry.id in matches }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(shown, key = { it.entry.id }) { n ->
            val e = n.entry
            val m = stateOf(e.id)
            Card(onClick = { onOpen(e.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val on = e.id in onLadder
                    if (on) MasteryGlyph(m, size = 28.dp) else GlyphIcon(Glyph.DASH, c.inkDim, size = 28.dp)
                    Column(Modifier.weight(1f)) {
                        Text(e.term, style = Gridiron.type.answer, color = c.ink)
                        Text(stringResource(if (on) masteryWord(m) else Res.string.map_off_course), style = Gridiron.type.label, color = c.inkDim)
                        val needs = e.requires.mapNotNull { byId[it]?.term }
                        if (needs.isNotEmpty()) {
                            Text(stringResource(Res.string.map_needs, needs.joinToString()), style = Gridiron.type.label, color = c.inkDim)
                        }
                    }
                }
            }
        }
    }
}

/** A concept's glossary card plus where it sits: what to learn first and what it leads to. */
@Composable
private fun ConceptSheet(
    state: LearnerAppState,
    e: GlossaryEntry,
    entries: List<GlossaryEntry>,
    layout: MapLayout,
    mastery: MasteryState,
    onLadder: Boolean,
    modifier: Modifier,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
) {
    val c = Gridiron.colors
    val byId = remember(entries) { entries.associateBy { it.id } }
    val same = e.requires.filter { it in layout.byId }.mapNotNull { byId[it] }
    val other = PlaybookMap.otherSide(e, entries)
    val leads = layout.edges.filter { it.from == e.id }.mapNotNull { byId[it.to] }
    Column(
        modifier.fillMaxWidth().background(c.surface).border(1.dp, c.line)
            .verticalScroll(rememberScrollState()).padding(Gridiron.skin.gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onLadder) MasteryGlyph(mastery, size = 24.dp) else GlyphIcon(Glyph.DASH, c.inkDim, size = 24.dp)
            Text(stringResource(if (onLadder) masteryWord(mastery) else Res.string.map_off_course), style = Gridiron.type.label, color = c.inkDim, modifier = Modifier.weight(1f))
            IconAction(Glyph.CLOSE, stringResource(Res.string.map_close), onClick = onClose)
        }
        TermCard(state, e)
        Links(stringResource(Res.string.map_learn_first), same, onOpen)
        other.groupBy { it.side }.forEach { (s, es) ->
            Links(stringResource(Res.string.map_from_side, stringResource(sideLabel(s))), es, onOpen)
        }
        Links(stringResource(Res.string.map_leads_to), leads, onOpen)
    }
}

@Composable
private fun Links(title: String, terms: List<GlossaryEntry>, onOpen: (String) -> Unit) {
    if (terms.isEmpty()) return
    val c = Gridiron.colors
    Column {
        Text(title, style = Gridiron.type.eyebrow, color = c.inkDim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            terms.forEach { t ->
                TextButton(onClick = { onOpen(t.id) }) { Text(t.term, style = Gridiron.type.label, color = c.link) }
            }
        }
    }
}

@Composable
private fun Legend() {
    val c = Gridiron.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MasteryState.entries.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MasteryGlyph(m, size = 16.dp)
                Text(stringResource(masteryWord(m)), style = Gridiron.type.label, color = c.inkDim)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GlyphIcon(Glyph.DASH, c.inkDim, size = 16.dp)
            Text(stringResource(Res.string.map_off_course), style = Gridiron.type.label, color = c.inkDim)
        }
    }
}

private fun sideLabel(s: PackSide): StringResource = when (s) {
    PackSide.OFFENSE -> Res.string.map_offense
    PackSide.DEFENSE -> Res.string.map_defense
    PackSide.SPECIAL -> Res.string.map_special
    PackSide.RULES -> Res.string.map_rules
}

private val NODE = 44.dp
private val GLYPH = 32.dp
private val ROW_HEIGHT = 104.dp
private val TOP_PAD = 16.dp
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 2.5f
private const val DIMMED = 0.35f
private const val FAINT = 0.5f
private const val HINT_BACK = 0.85f
private const val LONG_WORD = 9
private const val SHRINK = 0.82f
/** The sheet never covers more than this share of the map (DESIGN 8, FeedbackSheet). */
private const val SHEET_SHARE = 0.55f
