package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AfricanGold,
    secondary = MpesaGreen,
    tertiary = CoralRed,
    background = PitchBlack,
    surface = DeepCharcoal,
    onBackground = TextPureWhite,
    onSurface = TextPureWhite,
    onPrimary = PitchBlack,
    onSecondary = PitchBlack,
    surfaceVariant = LightCharcoal,
    onSurfaceVariant = TextMutedGray,
    outline = BorderGray
)

private val LightColorScheme = DarkColorScheme // Enforce a beautiful dark theme throughout for a premium look

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to Dark mode always as requested
  dynamicColor: Boolean = false, // Use our handcrafted university brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
