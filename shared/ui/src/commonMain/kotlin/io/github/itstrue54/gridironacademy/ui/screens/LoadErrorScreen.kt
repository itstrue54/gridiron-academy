package io.github.itstrue54.gridironacademy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.itstrue54.gridironacademy.ui.components.PrimaryButton
import io.github.itstrue54.gridironacademy.ui.resources.Res
import io.github.itstrue54.gridironacademy.ui.resources.load_error_body
import io.github.itstrue54.gridironacademy.ui.resources.load_error_title
import io.github.itstrue54.gridironacademy.ui.resources.try_again
import io.github.itstrue54.gridironacademy.ui.theme.Gridiron
import io.github.itstrue54.gridironacademy.ui.theme.GridironTheme
import io.github.itstrue54.gridironacademy.ui.theme.Skin
import org.jetbrains.compose.resources.stringResource

/**
 * Lessons failed to load (SPEC 3.3, DESIGN 9.11): a kind message and Try again, never a
 * crash. The shell logs the cause; nothing technical is shown to a child.
 */
@Composable
fun LoadErrorScreen(dark: Boolean, onRetry: () -> Unit) {
    GridironTheme(skin = Skin.Adult, dark = dark) {
        val c = Gridiron.colors
        Column(
            Modifier.fillMaxSize().background(c.surface).safeDrawingPadding().padding(Gridiron.skin.gutter),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(stringResource(Res.string.load_error_title), style = Gridiron.type.headline, color = c.ink, modifier = Modifier.semantics { heading() })
            Text(stringResource(Res.string.load_error_body), style = Gridiron.type.body, color = c.inkDim)
            PrimaryButton(stringResource(Res.string.try_again), onRetry)
        }
    }
}
