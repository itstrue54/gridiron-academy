package io.github.itstrue54.gridironacademy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalGridironColors = staticCompositionLocalOf { Daylight }
val LocalGridironType = staticCompositionLocalOf { AdultType }

/** System animator scale 0 or the in-app setting (DESIGN §10): motion becomes cross-fades. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Provides the skin, palette and type scale, and maps them onto Material 3 so stock
 * components (text fields, switches, sheets) match. The palette follows the phone unless
 * a profile overrides it (DESIGN §3).
 */
@Composable
fun GridironTheme(
    skin: Skin,
    dark: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) Floodlight else Daylight
    val type = if (skin.voice == Voice.KID) KidType else AdultType
    CompositionLocalProvider(
        LocalSkin provides skin,
        LocalGridironColors provides colors,
        LocalGridironType provides type,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = type.toMaterial(),
            shapes = Shapes(small = skin.chipShape, medium = skin.buttonShape, large = skin.cardShape),
            content = content,
        )
    }
}

/** Short access to the theme from any composable: `Gridiron.colors.gold`. */
object Gridiron {
    val colors: GridironColors
        @Composable @ReadOnlyComposable get() = LocalGridironColors.current
    val type: GridironType
        @Composable @ReadOnlyComposable get() = LocalGridironType.current
    val skin: Skin
        @Composable @ReadOnlyComposable get() = LocalSkin.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

// Error maps to the amber retry colour: there is no red in this app (DESIGN §1.3).
private fun GridironColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = gold, onPrimary = onGold,
        secondary = offense, tertiary = defense,
        background = surface, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = surfaceRaised, onSurfaceVariant = inkDim,
        surfaceContainer = surfaceRaised, surfaceContainerHigh = surfaceRaised,
        outline = line, outlineVariant = line,
        error = retry, onError = surface,
    )
} else {
    lightColorScheme(
        primary = gold, onPrimary = onGold,
        secondary = offense, tertiary = defense,
        background = surface, onBackground = ink,
        surface = surface, onSurface = ink,
        surfaceVariant = surfaceRaised, onSurfaceVariant = inkDim,
        surfaceContainer = surfaceRaised, surfaceContainerHigh = surfaceRaised,
        outline = line, outlineVariant = line,
        error = retry, onError = surface,
    )
}

private fun GridironType.toMaterial() = Typography(
    displayLarge = displayLarge,
    headlineMedium = headline,
    titleMedium = title,
    bodyLarge = body,
    bodyMedium = body,
    labelLarge = answer,
    labelMedium = label,
)
