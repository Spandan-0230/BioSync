package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFontFamily = FontFamily(
  Font(R.font.inter, FontWeight.Light),
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
  Font(R.font.inter, FontWeight.ExtraBold),
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.SemiBold),
  Font(R.font.jetbrains_mono, FontWeight.Bold),
  Font(R.font.jetbrains_mono, FontWeight.ExtraBold),
)

val Typography = Typography(
  displayLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 46.sp,
    lineHeight = 46.sp,
    letterSpacing = (-1.2).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.5).sp,
  ),
  titleMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.3).sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 19.sp,
    letterSpacing = 0.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = (-0.2).sp,
  ),
  labelSmall = TextStyle(
    fontFamily = JetBrainsMonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.1.sp,
  ),
)
