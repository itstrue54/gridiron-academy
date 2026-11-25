package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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
import io.github.itstrue54.gridironacademy.ui.resources.map_zoom_out
import io.github.itstrue54.gridironacademy.ui.resources.map_zoom_in
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.DisposableEffect

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

    // The system Back closes an open concept card before it leaves the map.
    DisposableEffect(selected) {
        state.backOverride = if (selected != null) ({ selected = null; true }) else null
        onDispose { state.backOverride = null }
    }

    Column(Modifier.fillMaxSize().screenBackground().safeDrawingPadding()) {
        // At large text the title keeps the whole row (a long word never breaks) and the
        // List/Map switch drops to its own line.
        val bigText = LocalDensity.current.fontScale >= LARGE_TEXT
        val modeSwitch = @Composable {
            TextButton(onClick = { listMode = !listMode }) {
                Text(stringResource(if (listMode) Res.string.map_show_map else Res.string.map_show_list),
                    style = Gridiron.type.label, color = c.link)
            }
        }
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconAction(Glyph.BACK, stringResource(Res.string.back), onClick = { state.back() })
            Text(stringResource(Res.string.playbook_map), style = Gridiron.type.headline, color = c.ink,
                modifier = Modifier.weight(1f).semantics { heading() })
            if (!bigText) modeSwitch()
        }
        if (bigText) Row(Modifier.padding(horizontal = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) { modeSwitch() }
        Column(Modifier.padding(horizontal = Gridiron.skin.gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(40) }, modifier = Modifier.fillMaxWidth(),
                // A label, not a placeholder: it stays for a screen reader once the learner types.
                label = { Text(stringResource(Res.string.glossary_search), style = Gridiron.type.label) },
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
    // Labels are measured at the column width: each shrinks until its longest word fits (no
    // mid-word break), and the row grows to the tallest label at the reader's text size.
    val measurer = rememberTextMeasurer()
    val labelStyle = Gridiron.type.label
    val labelW = (colW - LABEL_PAD * 2).coerceAtLeast(1.dp)
    val availPx = with(density) { labelW.roundToPx() }
    // Measured once per width, text scale and style (a font that loads late changes the style),
    // not on every recomposition (a pan or zoom).
    val labels = remember(layout, availPx, density.fontScale, labelStyle) {
        layout.nodes.associate { n -> n.entry.id to fitLabel(measurer, n.entry.term, labelStyle, availPx, MIN_LABEL_SHRINK / density.fontScale) }
    }
    val labelH = with(density) { (labels.values.maxOfOrNull { it.heightPx } ?: 0).toDp() }
    val rowH = maxOf(ROW_HEIGHT, NODE + LABEL_GAP + labelH + ROW_GAP)
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
                    label = labels.getValue(id),
                    modifier = Modifier
                        .offset(x = colW * n.x.toFloat(), y = TOP_PAD + rowH * n.row)
                        .width(colW),
                    onClick = { onTap(id) },
                )
            }
        }
        // Pinching always has buttons too (DESIGN 12): zoom about the middle of the view.
        // The hint shares the row with the buttons and wraps in what is left, so big text never puts it over them.
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            listOf(
                Triple("−", stringResource(Res.string.map_zoom_out), 1f / ZOOM_STEP),
                Triple("+", stringResource(Res.string.map_zoom_in), ZOOM_STEP),
            ).forEach { (glyph, label, factor) ->
                val enabled = if (factor < 1f) scale > MIN_SCALE else scale < MAX_SCALE
                Surface(
                    onClick = {
                        val s = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
                        val centre = Offset(view.width / 2f, view.height / 2f)
                        pan = clamp(centre - (centre - pan) * (s / scale), s)
                        scale = s
                    },
                    enabled = enabled,
                    shape = Gridiron.skin.buttonShape,
                    color = c.surfaceRaised.copy(alpha = ZOOM_BUTTON_BACK),
                    contentColor = if (enabled) c.ink else c.line,
                    border = BorderStroke(1.dp, c.line),
                    modifier = Modifier.size(Gridiron.skin.touchTarget).semantics { contentDescription = label },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(glyph, style = Gridiron.type.title, modifier = Modifier.clearAndSetSemantics {})
                    }
                }
            }
            // Decorative: one line at normal size, gone at large text where it would cover the nodes.
            if (LocalDensity.current.fontScale < LARGE_TEXT) Box(Modifier.weight(1f), contentAlignment = Alignment.BottomEnd) {
                Text(stringResource(Res.string.map_zoom_hint), style = Gridiron.type.label.copy(fontSize = Gridiron.type.label.fontSize * 0.85f), color = c.inkDim, textAlign = TextAlign.End,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.background(c.surface.copy(alpha = HINT_BACK), RoundedCornerShape(HINT_RADIUS)).padding(horizontal = 8.dp, vertical = 2.dp)
                        .clearAndSetSemantics { })
            }
        }
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
    label: MapLabel,
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
        // The style was shrunk until the longest word fits, so it wraps at spaces only; a word that
        // fits at no size is ellipsized on one line (the full name is in the node's description).
        Text(entry.term, style = label.style, color = c.ink, textAlign = TextAlign.Center,
            maxLines = if (label.ellipsize) 1 else LABEL_LINES, softWrap = !label.ellipsize,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = LABEL_GAP))
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

/** A map label: its style, the height it takes, and whether it had to be cut to one line. */
private class MapLabel(val style: TextStyle, val heightPx: Int, val ellipsize: Boolean)

/**
 * [style] shrunk until the longest word of [term] fits in [availPx]: down to [minFactor]
 * for legibility, then on to [LAST_RESORT] of that for a single overlong word. Hyphenation
 * is off, so a word is never split ("Quarterbac / k"); if it fits at no size the label is
 * ellipsized on one line instead. The height is what the wrapped label then takes.
 */
private fun fitLabel(measurer: TextMeasurer, term: String, style: TextStyle, availPx: Int, minFactor: Float): MapLabel {
    fun overlong(s: TextStyle) = term.split(' ', '-').any { measurer.measure(it, s, softWrap = false).size.width > availPx }
    var s = style
    var factor = 1f
    val floor = minFactor * LAST_RESORT
    while (factor > floor && overlong(s)) {
        factor *= LABEL_SHRINK_STEP
        s = style.copy(fontSize = style.fontSize * factor, lineHeight = style.lineHeight * factor)
    }
    val ellipsize = overlong(s)
    val h = if (ellipsize) measurer.measure(term, s, constraints = Constraints(maxWidth = availPx), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis).size.height
        else measurer.measure(term, s, constraints = Constraints(maxWidth = availPx), maxLines = LABEL_LINES).size.height
    return MapLabel(s, h, ellipsize)
}

/** A lone overlong word may go to this share of the usual floor before it is ellipsized. */
private const val LAST_RESORT = 0.75f
private const val LARGE_TEXT = 1.5f
private val LABEL_PAD = 2.dp
private val LABEL_GAP = 4.dp
private val ROW_GAP = 12.dp
private const val LABEL_LINES = 3
/** The smallest a label may get: 0.8 of its normal size, counting the reader's text scale (so 0.4 of the declared size at 200%). */
private const val MIN_LABEL_SHRINK = 0.8f
private const val LABEL_SHRINK_STEP = 0.9f
private val NODE = 44.dp
private val GLYPH = 32.dp
private val ROW_HEIGHT = 104.dp
private val TOP_PAD = 16.dp
private const val ZOOM_STEP = 1.5f
private const val ZOOM_BUTTON_BACK = 0.95f
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 2.5f
private const val DIMMED = 0.35f
private const val FAINT = 0.5f
private const val HINT_BACK = 0.85f
private val HINT_RADIUS = 16.dp
/** The sheet never covers more than this share of the map (DESIGN 8, FeedbackSheet). */
private const val SHEET_SHARE = 0.55f
