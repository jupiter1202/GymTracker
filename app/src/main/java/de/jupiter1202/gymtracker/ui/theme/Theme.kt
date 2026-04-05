package de.jupiter1202.gymtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EmeraldPulseColorScheme = darkColorScheme(
    primary                = EmeraldPrimary,
    onPrimary              = EmeraldOnPrimary,
    primaryContainer       = EmeraldPrimaryContainer,
    onPrimaryContainer     = EmeraldOnPrimaryContainer,
    secondary              = EmeraldSecondary,
    onSecondary            = EmeraldOnSecondary,
    secondaryContainer     = EmeraldSecondaryContainer,
    onSecondaryContainer   = EmeraldOnSecondaryContainer,
    tertiary               = EmeraldTertiary,
    onTertiary             = EmeraldOnTertiary,
    tertiaryContainer      = EmeraldTertiaryContainer,
    onTertiaryContainer    = EmeraldOnTertiaryContainer,
    error                  = EmeraldError,
    onError                = EmeraldOnError,
    errorContainer         = EmeraldErrorContainer,
    onErrorContainer       = EmeraldOnErrorContainer,
    background             = EmeraldBackground,
    onBackground           = EmeraldOnBackground,
    surface                = EmeraldSurface,
    onSurface              = EmeraldOnSurface,
    onSurfaceVariant       = EmeraldOnSurfaceVariant,
    surfaceVariant         = EmeraldSurfaceContainerHighest,
    surfaceTint            = EmeraldSurfaceTint,
    inverseSurface         = EmeraldInverseSurface,
    inverseOnSurface       = EmeraldInverseOnSurface,
    inversePrimary         = EmeraldInversePrimary,
    outline                = EmeraldOutline,
    outlineVariant         = EmeraldOutlineVariant,
    scrim                  = Color.Black,
    surfaceBright          = EmeraldSurfaceBright,
    surfaceContainer       = EmeraldSurfaceContainer,
    surfaceContainerHigh   = EmeraldSurfaceContainerHigh,
    surfaceContainerHighest = EmeraldSurfaceContainerHighest,
    surfaceContainerLow    = EmeraldSurfaceContainerLow,
    surfaceContainerLowest = EmeraldSurfaceContainerLowest,
    surfaceDim             = EmeraldSurfaceDim,
)

@Composable
fun GymTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmeraldPulseColorScheme,
        typography  = EmeraldTypography,
        content     = content
    )
}
