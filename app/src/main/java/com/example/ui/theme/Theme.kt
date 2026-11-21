package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AppAccent

private fun lightColors(accent: AppAccent) = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = Color(accent.lightHex),
    onSecondary = TerracottaOnSecondary,
    secondaryContainer = Color(accent.lightHex).copy(alpha = 0.16f),
    onSecondaryContainer = EspressoOnSurface,
    tertiary = SageTertiary,
    onTertiary = SageOnTertiary,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = SageOnContainer,
    background = ParchmentBackground,
    onBackground = EspressoOnSurface,
    surface = ParchmentSurface,
    onSurface = EspressoOnSurface,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = EspressoMuted,
    outline = ParchmentOutline
)

private fun darkColors(accent: AppAccent) = darkColorScheme(
    primary = DarkForestPrimary,
    onPrimary = DarkForestOnPrimary,
    primaryContainer = DarkForestContainer,
    onPrimaryContainer = DarkForestOnContainer,
    secondary = Color(accent.darkHex),
    onSecondary = DarkTerracottaOnSecondary,
    secondaryContainer = Color(accent.darkHex).copy(alpha = 0.22f),
    onSecondaryContainer = CreamOnSurface,
    tertiary = DarkSageTertiary,
    onTertiary = DarkSageOnTertiary,
    background = CharcoalBackground,
    onBackground = CreamOnSurface,
    surface = CharcoalSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CreamMuted,
    outline = CharcoalOutline
)

val FolioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: AppAccent = AppAccent.TERRACOTTA,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(accent) else lightColors(accent),
        typography = FolioTypography,
        shapes = FolioShapes,
        content = content
    )
}
