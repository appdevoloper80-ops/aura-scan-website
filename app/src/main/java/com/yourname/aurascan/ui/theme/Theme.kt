package com.yourname.aurascan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = OnTealPrimaryLight,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = SlateBlueSecondaryLight,
    onSecondary = OnSlateBlueSecondaryLight,
    secondaryContainer = SlateBlueContainerLight,
    onSecondaryContainer = OnSlateBlueContainerLight,
    tertiary = SeafoamTertiaryLight,
    onTertiary = OnSeafoamTertiaryLight,
    tertiaryContainer = SeafoamContainerLight,
    onTertiaryContainer = OnSeafoamContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = OnTealPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = SlateBlueSecondaryDark,
    onSecondary = OnSlateBlueSecondaryDark,
    secondaryContainer = SlateBlueContainerDark,
    onSecondaryContainer = OnSlateBlueContainerDark,
    tertiary = SeafoamTertiaryDark,
    onTertiary = OnSeafoamTertiaryDark,
    tertiaryContainer = SeafoamContainerDark,
    onTertiaryContainer = OnSeafoamContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

@Composable
fun AuraScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraScanTypography,
        content = content
    )
}
