package io.github.itstrue54.gridironacademy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.ui.app.Cue
import io.github.itstrue54.gridironacademy.ui.app.LocalCues
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The pass moment (DESIGN 2, 10): the pass cue, then confetti for the Kid look. Drawn over
 * the result screen and gone after [CONFETTI_MS]; nothing moves under reduced motion. The
 * Adult look's gold sweep is [GoldSweep], placed under the headline by the screen.
 */
@Composable
fun Celebration(modifier: Modifier = Modifier) {
    val cues = LocalCues.current
    LaunchedEffect(Unit) { cues.play(Cue.PASS) }
    if (Gridiron.reducedMotion || Gridiron.skin.primeTime) return
    val c = Gridiron.colors
    val colors = listOf(c.gold, c.offense, c.defense, c.correct, c.xp, c.rank)
    // Same pieces every time: a fixed seed, so a screenshot or a render test is stable.
    val pieces = remember {
        val r = Random(PIECES_SEED)
        List(PIECES) { Piece(r.nextFloat(), r.nextFloat() * 0.35f, 0.7f + r.nextFloat() * 0.6f, r.nextFloat() * 360f, r.nextInt(6)) }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val p = t.value
        if (p >= 1f) return@Canvas
        val w = 10.dp.toPx()
        val h = 6.dp.toPx()
        for (piece in pieces) {
            val y = (-0.1f - piece.delay + p * piece.speed * 1.6f) * size.height
            if (y < -h || y > size.height) continue
            val x = (piece.x + 0.04f * sin((p * 6f + piece.x * 10f) * PI.toFloat())) * size.width
            rotate(piece.spin + p * 540f, Offset(x, y)) {
                drawRect(colors[piece.color % colors.size], Offset(x - w / 2, y - h / 2), Size(w, h))
            }
        }
    }
}

/** The Adult pass mark: a gold rule that sweeps out under the headline. */
@Composable
fun GoldSweep(modifier: Modifier = Modifier) {
    if (!Gridiron.skin.primeTime) return
    val gold = Gridiron.colors.gold
    val still = Gridiron.reducedMotion
    val t = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(SWEEP_MS)) }
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        val len = size.width * t.value
        drawRect(gold, Offset((size.width - len) / 2, 0f), Size(len, size.height))
    }
}

private class Piece(val x: Float, val delay: Float, val speed: Float, val spin: Float, val color: Int)

private const val PIECES = 70
private const val PIECES_SEED = 2026
private const val CONFETTI_MS = 2200
private const val SWEEP_MS = 600
