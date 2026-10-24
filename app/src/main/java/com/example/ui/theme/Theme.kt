package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BioSyncColorScheme = darkColorScheme(
  primary = PureWhite,
  onPrimary = ObsidianBlack,
  secondary = CyberCyan,
  onSecondary = ObsidianBlack,
  tertiary = ElectricBlue,
  background = ObsidianBlack,
  onBackground = Slate100,
  surface = ObsidianPanel,
  onSurface = PureWhite,
  surfaceVariant = ObsidianTrack,
  onSurfaceVariant = Neutral400,
  outline = ObsidianBorder,
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BioSyncColorScheme,
    typography = Typography,
    content = content,
  )
}
