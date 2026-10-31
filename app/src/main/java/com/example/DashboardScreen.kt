package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaloriesEnd
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsEnd
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.FatsEnd
import com.example.ui.theme.FatsMid
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationEnd
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProteinEnd
import com.example.ui.theme.ProteinMid
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsEnd
import com.example.ui.theme.StepsMid
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Core BioSync Monochrome Palette (Identical across Steps 01–04)
private val Zinc100 = Color(0xFFF4F4F5)
private val Zinc200 = Color(0xFFE4E4E7)
private val Zinc300 = Color(0xFFD4D4D8)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc500 = Color(0xFF71717A)
private val Zinc600 = Color(0xFF52525B)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc800 = Color(0xFF27272A)
private val Zinc850 = Color(0xFF1F1F23)
private val Zinc900 = Color(0xFF18181B)
private val Zinc950 = Color(0xFF09090B)

// Sculpted 3D Floating Card Surfaces (Matches ChronometricDisplayCard & DailyTargetsCommandDeck)
private val FloatingCardTop = Color(0xFF18181E)
private val FloatingCardMid = Color(0xFF111116)
private val FloatingCardBottom = Color(0xFF09090C)

private val FloatingTileTop = Color(0xFF111115)
private val FloatingTileBottom = Color(0xFF08080B)

private val RecessedTrackFill = Color(0xFF07070A)
private val RecessedTrackBorder = Color(0xFF27272E)

private val WhiteVariantMulticolorGradient = listOf(
  Color(0xFF7A7F87), // Gunmetal Platinum
  Color(0xFFAEC0CE), // Cool Liquid Silver
  Color(0xFFE6E9EF), // Satin Ice White
  Color(0xFFF7EFE6), // Warm Pearl Champagne Highlight
  Color(0xFFFFFFFF), // Pure Brilliant White Crest
)

private val DashboardEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

internal fun safeDashboardBlur(radius: Float): Float = radius.coerceAtLeast(0.5f)

/**
 * Primary 3D Floating Command Deck Modifier matching `ChronometricDisplayCard` (Step 01),
 * `BiometricMassIndexCard` (Step 02), and `DailyTargetsCommandDeck` (Step 04):
 * - Deep multi-layered 3D drop shadow (`28.dp` blur offset downward + `14.dp` mid shadow)
 * - Ambient silver/platinum levitation halo (`20.dp` blur) + subtle top-edge accent aura
 * - Top-lit specular glass crown + `1.35.dp` liquid-chrome border + traveling shimmer wave
 * - Zero transparent colored surface coatings
 */
@Composable
internal fun Modifier.floating3DProtocolCard(
  cornerRadiusDp: Dp = 18.dp,
  topRimTint: Color = PureWhite,
  borderFlashProgress: Float = 0f,
): Modifier {
  val shape = RoundedCornerShape(cornerRadiusDp)
  return this
    .drawWithCache {
      val cornerPx = cornerRadiusDp.toPx()
      val cornerRadius = CornerRadius(cornerPx, cornerPx)

      val deepDropShadowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(230, 0, 0, 0)
          maskFilter = BlurMaskFilter(safeDashboardBlur(28.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      val midElevationShadowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(175, 0, 0, 0)
          maskFilter = BlurMaskFilter(safeDashboardBlur(13.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      val silverLevitationHaloPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(48, 226, 232, 245)
          maskFilter = BlurMaskFilter(safeDashboardBlur(20.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      val topAccentRimHaloPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(
            34,
            (topRimTint.red * 255).roundToInt(),
            (topRimTint.green * 255).roundToInt(),
            (topRimTint.blue * 255).roundToInt(),
          )
          maskFilter = BlurMaskFilter(safeDashboardBlur(16.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      val rearFlashHaloPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(0, 255, 255, 255)
          maskFilter = BlurMaskFilter(safeDashboardBlur(22.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }

      val baseBorderBrush = Brush.verticalGradient(
        0.0f to PureWhite.copy(alpha = 0.42f),
        0.18f to topRimTint.copy(alpha = 0.34f),
        0.48f to Zinc700.copy(alpha = 0.65f),
        0.82f to Zinc850,
        1.0f to Zinc950,
      )
      val topSpecularGlassBrush = Brush.verticalGradient(
        0.0f to PureWhite.copy(alpha = 0.065f),
        0.22f to PureWhite.copy(alpha = 0.018f),
        0.45f to Color.Transparent,
      )

      onDrawWithContent {
        val flashP = borderFlashProgress.coerceIn(0f, 1f)
        val flashEnvelope = if (flashP > 0.001f && flashP < 0.999f) {
          sin(flashP * PI.toFloat()).coerceIn(0f, 1f)
        } else {
          0f
        }

        // 1. Multi-layer 3D Floating Shadows & Ambient Levitation Halo
        drawIntoCanvas { canvas ->
          // Deep ground shadow pushed down for floating depth
          canvas.drawRoundRect(
            5.dp.toPx(),
            13.dp.toPx(),
            size.width - 5.dp.toPx(),
            size.height + 14.dp.toPx(),
            cornerPx,
            cornerPx,
            deepDropShadowPaint,
          )
          // Crisp mid-elevation shadow
          canvas.drawRoundRect(
            2.dp.toPx(),
            6.dp.toPx(),
            size.width - 2.dp.toPx(),
            size.height + 7.dp.toPx(),
            cornerPx,
            cornerPx,
            midElevationShadowPaint,
          )
          // All-around platinum levitation aura
          canvas.drawRoundRect(
            1.dp.toPx(),
            -2.dp.toPx(),
            size.width - 1.dp.toPx(),
            size.height + 5.dp.toPx(),
            cornerPx,
            cornerPx,
            silverLevitationHaloPaint,
          )
          // Subtle top crown backlight
          canvas.drawRoundRect(
            12.dp.toPx(),
            -3.dp.toPx(),
            size.width - 12.dp.toPx(),
            (size.height * 0.32f).coerceAtLeast(24.dp.toPx()),
            cornerPx,
            cornerPx,
            topAccentRimHaloPaint,
          )

          if (flashEnvelope > 0.01f) {
            rearFlashHaloPaint.asFrameworkPaint().alpha =
              (160f * flashEnvelope).roundToInt().coerceIn(0, 200)
            val waveCx = size.width * flashP
            val waveCy = size.height * flashP
            val spanX = size.width * 0.5f
            val spanY = size.height * 0.5f
            canvas.drawRoundRect(
              (waveCx - spanX).coerceAtLeast(-4.dp.toPx()),
              (waveCy - spanY).coerceAtLeast(-4.dp.toPx()),
              (waveCx + spanX).coerceAtMost(size.width + 4.dp.toPx()),
              (waveCy + spanY).coerceAtMost(size.height + 4.dp.toPx()),
              cornerPx,
              cornerPx,
              rearFlashHaloPaint,
            )
          }
        }

        // 2. Card Surface & Composable Content
        drawContent()

        // 3. Top-edge Specular Glass Highlight
        drawRoundRect(
          brush = topSpecularGlassBrush,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
        )

        // 4. 3D Specular Top-Lit Border
        drawRoundRect(
          brush = baseBorderBrush,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = 1.3.dp.toPx()),
        )

        // 5. Traveling Liquid-Silver Border Shimmer Wave (from ChronometricDisplayCard)
        if (flashEnvelope > 0.01f) {
          val diagW = size.width.coerceAtLeast(1f)
          val diagH = size.height.coerceAtLeast(1f)
          val bandSpanX = diagW * 0.42f
          val bandSpanY = diagH * 0.42f
          val centerX = -bandSpanX + (diagW + bandSpanX * 2f) * flashP
          val centerY = -bandSpanY + (diagH + bandSpanY * 2f) * flashP

          drawRoundRect(
            brush = Brush.linearGradient(
              colors = listOf(
                Color.Transparent,
                Zinc400.copy(alpha = 0.45f * flashEnvelope),
                PureWhite.copy(alpha = 0.95f * flashEnvelope),
                Color(0xFFF7EFE6).copy(alpha = 0.85f * flashEnvelope),
                Color.Transparent,
              ),
              start = Offset(centerX - bandSpanX * 0.85f, centerY - bandSpanY * 0.85f),
              end = Offset(centerX + bandSpanX * 0.85f, centerY + bandSpanY * 0.85f),
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = (1.9f + 0.6f * flashEnvelope).dp.toPx()),
          )
        }
      }
    }
    .clip(shape)
    .background(
      brush = Brush.verticalGradient(
        colors = listOf(FloatingCardTop, FloatingCardMid, FloatingCardBottom),
      ),
    )
}

/**
 * Floating 3D Inner Bento Tile Modifier matching `PipelineMetricTile` & `CompactMacroBentoTile`
 * in `ProtocolSummaryScreen.kt`:
 * - Gives every inner sub-card its own 3D floating elevation shadow + top-lit accent/platinum rim.
 */
@Composable
internal fun Modifier.floating3DInnerTile(
  cornerRadiusDp: Dp = 12.dp,
  accentColor: Color = Zinc700,
  isHighlighted: Boolean = false,
): Modifier {
  val shape = RoundedCornerShape(cornerRadiusDp)
  return this
    .drawWithCache {
      val cornerPx = cornerRadiusDp.toPx()
      val cornerRadius = CornerRadius(cornerPx, cornerPx)
      val tileShadowPaint = Paint().apply {
        isAntiAlias = true
        asFrameworkPaint().apply {
          color = android.graphics.Color.argb(190, 0, 0, 0)
          maskFilter = BlurMaskFilter(safeDashboardBlur(10.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
        }
      }
      val borderBrush = Brush.verticalGradient(
        0.0f to if (isHighlighted) accentColor.copy(alpha = 0.72f) else accentColor.copy(alpha = 0.38f),
        0.45f to Zinc800,
        1.0f to Zinc900,
      )
      val topSheenBrush = Brush.verticalGradient(
        0.0f to PureWhite.copy(alpha = 0.045f),
        0.35f to Color.Transparent,
      )

      onDrawWithContent {
        drawIntoCanvas { canvas ->
          canvas.drawRoundRect(
            2.dp.toPx(),
            5.dp.toPx(),
            size.width - 2.dp.toPx(),
            size.height + 5.dp.toPx(),
            cornerPx,
            cornerPx,
            tileShadowPaint,
          )
        }
        drawContent()
        drawRoundRect(
          brush = topSheenBrush,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
        )
        drawRoundRect(
          brush = borderBrush,
          topLeft = Offset.Zero,
          size = size,
          cornerRadius = cornerRadius,
          style = Stroke(width = if (isHighlighted) 1.3.dp.toPx() else 1.dp.toPx()),
        )
      }
    }
    .clip(shape)
    .background(
      brush = Brush.verticalGradient(
        colors = listOf(FloatingTileTop, FloatingTileBottom),
      ),
    )
}

/**
 * Used ONLY ONCE on the Dashboard (exclusively in the Streak Protocol Engine card header),
 * matching how `OPTIMAL RANGE` is used once in `BiometricMassIndexCard` on `PersonalDetailsScreen.kt`.
 */
@Composable
private fun StreakStatusOptimalBadge(
  text: String,
  startColor: Color,
  endColor: Color,
  modifier: Modifier = Modifier,
) {
  val badgeShape = RoundedCornerShape(6.dp)
  Box(
    modifier = modifier
      .clip(badgeShape)
      .background(
        brush = Brush.horizontalGradient(
          colors = listOf(startColor, endColor),
        ),
      )
      .border(
        width = 1.dp,
        color = startColor,
        shape = badgeShape,
      )
      .padding(horizontal = 9.5.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = ObsidianBlack,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 9.2.sp,
      letterSpacing = 0.7.sp,
      maxLines = 1,
    )
  }
}

/**
 * Luminous header dot matching `LuminousWhiteDot` / `PersonalSyncedRangeDot` across Steps 01–04.
 */
@Composable
internal fun DashboardSyncedHeaderDot(
  startColor: Color,
  endColor: Color,
  sizeDp: Dp = 8.dp,
) {
  Spacer(
    modifier = Modifier
      .size(sizeDp)
      .drawWithCache {
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              155,
              (startColor.red * 255).roundToInt(),
              (startColor.green * 255).roundToInt(),
              (startColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(
              safeDashboardBlur(this@drawWithCache.size.minDimension * 0.85f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }
        val dotBrush = Brush.linearGradient(colors = listOf(startColor, endColor))
        onDrawBehind {
          val r = (this.size.minDimension / 2f).coerceAtLeast(1f)
          drawIntoCanvas { canvas ->
            canvas.drawCircle(center, r * 1.3f, glowPaint)
          }
          drawCircle(brush = dotBrush, radius = r, center = center)
        }
      },
  )
}

@Immutable
private data class StreakPillarStatus(
  val code: String,
  val label: String,
  val achievedPercent: Int,
  val thresholdPercent: Int,
  val accentColor: Color,
  val secondaryColor: Color,
) {
  val isPassed: Boolean
    get() = achievedPercent >= thresholdPercent
}

@Composable
fun BioSyncDashboardScreen(
  subjectName: String,
  targetCaloriesKcal: Int,
  targetProteinGrams: Int,
  targetCarbsGrams: Int,
  targetFatsGrams: Int,
  targetHydrationMl: Int,
  targetStepsCount: Int,
  streakThreshold: StreakThresholdOption,
  mealFrequency: MealFrequencyOption,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  onOpenProtocolSummary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onOpenProtocolSummary)

  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val coroutineScope = rememberCoroutineScope()
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
  val headerGlowPhase = remember { Animatable(0f) }
  val cardShimmerWave = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      headerGlowPhase.snapTo(0f)
      headerGlowPhase.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 720, easing = DashboardEaseOut),
      )
    }
    launch {
      delay(140L)
      cardShimmerWave.snapTo(0f)
      cardShimmerWave.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1250, easing = DashboardEaseOut),
      )
    }
  }

  // Live Google Fit + Health Connect 1:1 Step & Distance Controller
  val stepSyncController = rememberBioSyncStepSyncController(
    decidedDailyStepsGoal = targetStepsCount,
    autoRequestPermissionsOnStart = false,
  )
  val liveSteps = stepSyncController.state.liveTodaySteps.coerceAtLeast(0)
  val liveDistanceKm = stepSyncController.state.liveDistanceKm.coerceAtLeast(0f)

  // Persisted Dashboard state
  val initialDashboardData = remember { DashboardLocalStore.load(context) }
  var todayDayKey by remember { mutableStateOf(initialDashboardData.dayKey) }
  var baseStreakDays by remember { mutableIntStateOf(initialDashboardData.baseStreakDays) }
  var lastCompletedStreakDayKey by remember { mutableStateOf(initialDashboardData.lastCompletedStreakDayKey) }
  var allCreatedSingleItems by remember { mutableStateOf(initialDashboardData.allCreatedSingleItems) }
  var savedQuickItems by remember { mutableStateOf(initialDashboardData.savedQuickItems) }
  var timelineEntries by remember { mutableStateOf(initialDashboardData.todayTimelineEntries) }

  var pendingDeletedItem by remember { mutableStateOf<PendingDeletedTimelineItem?>(null) }

  // Celebration particle burst animation state
  val celebrationBurstPhase = remember { Animatable(0f) }
  var hasCelebratedThisSession by remember { mutableStateOf(false) }

  // Derived daily totals from Today's Timeline (all default to 0 until user logs items/meals/water)
  val loggedCalories by remember(timelineEntries) {
    derivedStateOf { timelineEntries.sumOf { it.caloriesKcal }.coerceAtLeast(0) }
  }
  val loggedProtein by remember(timelineEntries) {
    derivedStateOf { timelineEntries.sumOf { it.proteinGrams }.coerceAtLeast(0) }
  }
  val loggedCarbs by remember(timelineEntries) {
    derivedStateOf { timelineEntries.sumOf { it.carbsGrams }.coerceAtLeast(0) }
  }
  val loggedFats by remember(timelineEntries) {
    derivedStateOf { timelineEntries.sumOf { it.fatsGrams }.coerceAtLeast(0) }
  }
  val loggedWaterMl by remember(timelineEntries) {
    derivedStateOf { timelineEntries.sumOf { it.hydrationMl }.coerceAtLeast(0) }
  }

  val requiredPercent = streakThreshold.percentage.coerceIn(50, 100)

  val stepsPct = remember(liveSteps, targetStepsCount) {
    ((liveSteps.toFloat() / targetStepsCount.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }
  val caloriesPct = remember(loggedCalories, targetCaloriesKcal) {
    ((loggedCalories.toFloat() / targetCaloriesKcal.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }
  val proteinPct = remember(loggedProtein, targetProteinGrams) {
    ((loggedProtein.toFloat() / targetProteinGrams.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }
  val carbsPct = remember(loggedCarbs, targetCarbsGrams) {
    ((loggedCarbs.toFloat() / targetCarbsGrams.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }
  val fatsPct = remember(loggedFats, targetFatsGrams) {
    ((loggedFats.toFloat() / targetFatsGrams.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }
  val waterPct = remember(loggedWaterMl, targetHydrationMl) {
    ((loggedWaterMl.toFloat() / targetHydrationMl.coerceAtLeast(1).toFloat()) * 100f).roundToInt()
  }

  val pillars = remember(
    stepsPct,
    caloriesPct,
    proteinPct,
    carbsPct,
    fatsPct,
    waterPct,
    requiredPercent,
  ) {
    listOf(
      StreakPillarStatus("STEPS", "Steps", stepsPct, requiredPercent, StepsStart, StepsMid),
      StreakPillarStatus("KCAL", "Calories", caloriesPct, requiredPercent, CaloriesStart, CaloriesMid),
      StreakPillarStatus("PRO", "Protein", proteinPct, requiredPercent, ProteinStart, ProteinMid),
      StreakPillarStatus("CARB", "Carbs", carbsPct, requiredPercent, CarbsStart, CarbsMid),
      StreakPillarStatus("FAT", "Fats", fatsPct, requiredPercent, FatsStart, FatsMid),
      StreakPillarStatus("H₂O", "Water", waterPct, requiredPercent, HydrationStart, HydrationMid),
    )
  }

  val passedPillarsCount = remember(pillars) { pillars.count { it.isPassed } }
  val isTodayStreakAchieved = passedPillarsCount == pillars.size

  val combinedStreakReadinessPct = remember(pillars, requiredPercent) {
    val sumNormalized = pillars.sumOf { pillar ->
      (pillar.achievedPercent.toDouble() / requiredPercent.toDouble()).coerceIn(0.0, 1.0)
    }
    ((sumNormalized / pillars.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
  }

  val displayedStreakDays = remember(baseStreakDays, lastCompletedStreakDayKey, todayDayKey, isTodayStreakAchieved) {
    if (isTodayStreakAchieved) {
      if (lastCompletedStreakDayKey == todayDayKey) baseStreakDays.coerceAtLeast(1) else baseStreakDays + 1
    } else {
      if (lastCompletedStreakDayKey == todayDayKey) (baseStreakDays - 1).coerceAtLeast(0) else baseStreakDays
    }
  }

  LaunchedEffect(isTodayStreakAchieved) {
    if (isTodayStreakAchieved) {
      if (lastCompletedStreakDayKey != todayDayKey) {
        baseStreakDays += 1
        lastCompletedStreakDayKey = todayDayKey
      }
      if (!hasCelebratedThisSession) {
        hasCelebratedThisSession = true
        haptics.confirm()
        celebrationBurstPhase.snapTo(0f)
        celebrationBurstPhase.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 1350, easing = DashboardEaseOut),
        )
      }
    } else {
      hasCelebratedThisSession = false
      if (lastCompletedStreakDayKey == todayDayKey) {
        baseStreakDays = (baseStreakDays - 1).coerceAtLeast(0)
        lastCompletedStreakDayKey = ""
      }
    }
  }

  LaunchedEffect(
    todayDayKey,
    baseStreakDays,
    lastCompletedStreakDayKey,
    allCreatedSingleItems,
    savedQuickItems,
    timelineEntries,
  ) {
    DashboardLocalStore.save(
      context = context,
      data = DashboardStoredData(
        dayKey = todayDayKey,
        baseStreakDays = baseStreakDays,
        lastCompletedStreakDayKey = lastCompletedStreakDayKey,
        allCreatedSingleItems = allCreatedSingleItems,
        savedQuickItems = savedQuickItems,
        todayTimelineEntries = timelineEntries,
      ),
    )
  }

  LaunchedEffect(pendingDeletedItem) {
    val currentPending = pendingDeletedItem ?: return@LaunchedEffect
    val waitMs = (currentPending.expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
    delay(waitMs + 50L)
    if (pendingDeletedItem?.entry?.id == currentPending.entry.id) {
      pendingDeletedItem = null
    }
  }

  val isWorkoutDayToday = remember(activityLevel, selectedWorkoutDays) {
    if (activityLevel == ActivityLevelOption.SEDENTARY) {
      false
    } else {
      val cal = java.util.Calendar.getInstance()
      val todayOpt = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> DayOfWeekOption.MONDAY
        java.util.Calendar.TUESDAY -> DayOfWeekOption.TUESDAY
        java.util.Calendar.WEDNESDAY -> DayOfWeekOption.WEDNESDAY
        java.util.Calendar.THURSDAY -> DayOfWeekOption.THURSDAY
        java.util.Calendar.FRIDAY -> DayOfWeekOption.FRIDAY
        java.util.Calendar.SATURDAY -> DayOfWeekOption.SATURDAY
        else -> DayOfWeekOption.SUNDAY
      }
      todayOpt in selectedWorkoutDays
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack),
    containerColor = ObsidianBlack,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(ObsidianBlack)
        .padding(innerPadding)
        .windowInsetsPadding(WindowInsets.systemBars),
      contentAlignment = Alignment.TopCenter,
    ) {
      val maxW = maxWidth
      val contentMaxWidth = if (maxW >= 600.dp) 580.dp else 480.dp
      val horizontalPad = (maxW.value * 0.05f).coerceIn(16f, 28f).dp
      val scrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .widthIn(max = contentMaxWidth)
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(scrollState)
          .padding(
            start = horizontalPad,
            end = horizontalPad,
            top = 6.dp,
            bottom = 32.dp,
          )
          .testTag("biosync_dashboard_screen"),
      ) {
        // 1. Header Bar identical in DNA to Steps 01–04 (BioSync + LIVE PROTOCOL + Shiny White Bar)
        DashboardHeaderBar(
          onBackClick = {
            haptics.selection()
            onOpenProtocolSummary()
          },
          phaseProvider = { headerGlowPhase.value },
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Eyebrow & Title Section (Matching Steps 01–04 clean typography without bulky badge blocks)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "PROTOCOL COMMAND • LIVE TELEMETRY",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(5.5.dp)
                .clip(CircleShape)
                .background(if (isWorkoutDayToday) StepsStart else Zinc400),
            )
            Text(
              text = if (isWorkoutDayToday) "TRAINING DAY" else "RECOVERY DAY",
              color = if (isWorkoutDayToday) StepsStart else Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = "Daily Protocol Dashboard",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 24.5.sp,
          letterSpacing = (-0.5).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "$subjectName • Reach ≥$requiredPercent% across all 6 categories to lock in today's combined streak.",
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 12.2.sp,
          lineHeight = 17.5.sp,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. FLOATING SECTION 1: COMBINED STREAK PROTOCOL ENGINE
        // (The ONLY card on the screen that uses the solid "OPTIMAL RANGE"-style status badge)
        DashboardStreakCommandCard(
          streakDays = displayedStreakDays,
          requiredThresholdPercent = requiredPercent,
          combinedReadinessPercent = combinedStreakReadinessPct,
          passedPillarsCount = passedPillarsCount,
          pillars = pillars,
          isStreakAchieved = isTodayStreakAchieved,
          borderFlashProgress = cardShimmerWave.value,
          celebrationPhaseProvider = { celebrationBurstPhase.value },
          onReplayCelebration = {
            haptics.selection()
            coroutineScope.launch {
              cardShimmerWave.snapTo(0f)
              cardShimmerWave.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1050, easing = DashboardEaseOut),
              )
            }
            if (isTodayStreakAchieved) {
              haptics.confirm()
              coroutineScope.launch {
                celebrationBurstPhase.snapTo(0f)
                celebrationBurstPhase.animateTo(
                  targetValue = 1f,
                  animationSpec = tween(durationMillis = 1350, easing = DashboardEaseOut),
                )
              }
            }
          },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. FLOATING SECTION 2 (PRIORITY #1): DAILY STEPS & DISTANCE (GOOGLE FIT 1:1 SYNCED)
        DashboardPriorityStepsCard(
          liveSteps = liveSteps,
          targetSteps = targetStepsCount,
          liveDistanceKm = liveDistanceKm,
          isActivelyWalking = stepSyncController.state.isActivelyWalking,
          streakThresholdPercent = requiredPercent,
          borderFlashProgress = cardShimmerWave.value,
          onRefreshSteps = {
            haptics.selection()
            stepSyncController.forceRefreshNow()
          },
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. FLOATING SECTION 3 (PRIORITY #2): DAILY ENERGY (CALORIES) & 3-COLUMN MACRO BENTO (PROTEIN | CARBS | FATS)
        DashboardCaloriesAndMacrosCard(
          loggedCalories = loggedCalories,
          targetCalories = targetCaloriesKcal,
          loggedProtein = loggedProtein,
          targetProtein = targetProteinGrams,
          loggedCarbs = loggedCarbs,
          targetCarbs = targetCarbsGrams,
          loggedFats = loggedFats,
          targetFats = targetFatsGrams,
          streakThresholdPercent = requiredPercent,
          borderFlashProgress = cardShimmerWave.value,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. FLOATING SECTION 4: DEDICATED HYDRATION (H2O) COMMAND DECK (Separate from Calories & Macros)
        DashboardHydrationCommandCard(
          loggedWaterMl = loggedWaterMl,
          targetWaterMl = targetHydrationMl,
          streakThresholdPercent = requiredPercent,
          borderFlashProgress = cardShimmerWave.value,
          onLogWaterMl = { addedMl ->
            val nowMs = System.currentTimeMillis()
            val newEntry = DashboardTimelineEntry(
              type = DashboardEntryType.HYDRATION,
              title = "Hydration +$addedMl ml",
              hydrationMl = addedMl,
              timestampEpochMs = nowMs,
              formattedTimeAmPm = DashboardTimeFormatter.formatAmPm(nowMs),
            )
            timelineEntries = listOf(newEntry) + timelineEntries
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 7. FLOATING SECTION 5: QUICK ACTIONS (+) BAR (Pop-up Composer on click + Saved Quick Items beside it)
        DashboardQuickActionsHubCard(
          allCreatedSingleItems = allCreatedSingleItems,
          savedQuickItems = savedQuickItems,
          targetCalories = targetCaloriesKcal,
          targetProtein = targetProteinGrams,
          targetCarbs = targetCarbsGrams,
          targetFats = targetFatsGrams,
          mealCount = mealFrequency.count,
          borderFlashProgress = cardShimmerWave.value,
          onLogNutritionItem = { item, shouldSaveToQuickActions ->
            val nowMs = System.currentTimeMillis()
            if (!item.isMeal) {
              val updatedSingles = listOf(item) + allCreatedSingleItems.filterNot {
                it.name.equals(item.name, ignoreCase = true)
              }
              allCreatedSingleItems = updatedSingles.take(40)
            }
            if (shouldSaveToQuickActions) {
              val savedCopy = item.copy(isSavedToQuickActions = true)
              val updatedSaved = listOf(savedCopy) + savedQuickItems.filterNot {
                it.name.equals(savedCopy.name, ignoreCase = true) && it.isMeal == savedCopy.isMeal
              }
              savedQuickItems = updatedSaved.take(30)
            }
            val newEntry = DashboardTimelineEntry(
              type = if (item.isMeal) DashboardEntryType.MEAL else DashboardEntryType.SINGLE_ITEM,
              title = item.name,
              subtitle = if (item.componentNames.isNotEmpty()) item.componentNames.joinToString(" + ") else "",
              caloriesKcal = item.caloriesKcal,
              proteinGrams = item.proteinGrams,
              carbsGrams = item.carbsGrams,
              fatsGrams = item.fatsGrams,
              timestampEpochMs = nowMs,
              formattedTimeAmPm = DashboardTimeFormatter.formatAmPm(nowMs),
            )
            timelineEntries = listOf(newEntry) + timelineEntries
          },
          onQuickTapSavedItem = { savedItem ->
            val nowMs = System.currentTimeMillis()
            val newEntry = DashboardTimelineEntry(
              type = if (savedItem.isMeal) DashboardEntryType.MEAL else DashboardEntryType.SINGLE_ITEM,
              title = savedItem.name,
              subtitle = if (savedItem.componentNames.isNotEmpty()) savedItem.componentNames.joinToString(" + ") else "Quick Action",
              caloriesKcal = savedItem.caloriesKcal,
              proteinGrams = savedItem.proteinGrams,
              carbsGrams = savedItem.carbsGrams,
              fatsGrams = savedItem.fatsGrams,
              timestampEpochMs = nowMs,
              formattedTimeAmPm = DashboardTimeFormatter.formatAmPm(nowMs),
            )
            timelineEntries = listOf(newEntry) + timelineEntries
          },
          onDeleteSavedQuickItem = { idToDelete ->
            savedQuickItems = savedQuickItems.filterNot { it.id == idToDelete }
            allCreatedSingleItems = allCreatedSingleItems.filterNot { it.id == idToDelete }
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 8. FLOATING SECTION 6: TODAY'S TIMELINE (At the very bottom: AM/PM timestamps, Delete & 3-second Undo)
        DashboardTodayTimelineSection(
          timelineEntries = timelineEntries,
          pendingDeletedItem = pendingDeletedItem,
          borderFlashProgress = cardShimmerWave.value,
          onDeleteEntry = { entryToDelete ->
            val idx = timelineEntries.indexOfFirst { it.id == entryToDelete.id }.coerceAtLeast(0)
            timelineEntries = timelineEntries.filterNot { it.id == entryToDelete.id }
            pendingDeletedItem = PendingDeletedTimelineItem(
              entry = entryToDelete,
              originalIndex = idx,
              expiresAtEpochMs = System.currentTimeMillis() + 3000L,
            )
          },
          onUndoDelete = {
            val pending = pendingDeletedItem
            if (pending != null) {
              val mutable = timelineEntries.toMutableList()
              val targetIdx = pending.originalIndex.coerceIn(0, mutable.size)
              mutable.add(targetIdx, pending.entry)
              timelineEntries = mutable
              pendingDeletedItem = null
            }
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )
      }
    }
  }
}

// ============================================================================
// HEADER BAR (IDENTICAL ECOSYSTEM DNA TO STEPS 01–04)
// ============================================================================

@Composable
private fun DashboardHeaderBar(
  onBackClick: () -> Unit,
  phaseProvider: () -> Float,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 46.dp)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .clickable(role = Role.Button, onClick = onBackClick)
        .testTag("dashboard_open_protocol_button"),
      contentAlignment = Alignment.CenterStart,
    ) {
      Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val chevron = Path().apply {
          moveTo(w * 0.68f, h * 0.14f)
          lineTo(w * 0.24f, h * 0.50f)
          lineTo(w * 0.68f, h * 0.86f)
        }
        drawPath(
          path = chevron,
          color = Zinc300,
          style = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "BioSync",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "LIVE PROTOCOL • ACTIVE",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.2.sp,
        letterSpacing = 1.0.sp,
      )
      Spacer(modifier = Modifier.height(4.5.dp))

      Spacer(
        modifier = Modifier
          .width(108.dp)
          .height(4.dp)
          .graphicsLayer()
          .drawWithCache {
            val barH = size.height.coerceAtLeast(1f)
            val radius = CornerRadius(barH / 2f, barH / 2f)
            val outerGlowPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(155, 255, 255, 255)
                maskFilter = BlurMaskFilter(safeDashboardBlur(barH * 1.9f), BlurMaskFilter.Blur.NORMAL)
              }
            }
            val shinyCoreBrush = Brush.horizontalGradient(
              colors = listOf(Color(0xFFC8CCD4), Color(0xFFF2F4F7), PureWhite),
            )
            onDrawBehind {
              if (size.width <= 1f || size.height <= 0.5f) return@onDrawBehind
              val p = phaseProvider().coerceIn(0f, 1f)
              val fillW = (size.width * p).coerceIn(barH, size.width)
              drawRoundRect(
                color = Zinc850,
                topLeft = Offset.Zero,
                size = Size(size.width, barH),
                cornerRadius = radius,
              )
              drawIntoCanvas { canvas ->
                canvas.drawRoundRect(0f, 0f, fillW, barH, barH / 2f, barH / 2f, outerGlowPaint)
              }
              drawRoundRect(
                brush = shinyCoreBrush,
                topLeft = Offset.Zero,
                size = Size(fillW, barH),
                cornerRadius = radius,
              )
            }
          },
      )
    }

    Box(
      modifier = Modifier.size(48.dp),
      contentAlignment = Alignment.CenterEnd,
    ) {
      DashboardSyncedHeaderDot(
        startColor = PureWhite,
        endColor = Zinc200,
        sizeDp = 8.dp,
      )
    }
  }
}

// ============================================================================
// FLOATING SECTION 1: STREAK PROTOCOL COMMAND CARD
// (Uses the single purposeful Optimal Status Badge, plus floating 3D pillar tiles)
// ============================================================================

@Composable
private fun DashboardStreakCommandCard(
  streakDays: Int,
  requiredThresholdPercent: Int,
  combinedReadinessPercent: Int,
  passedPillarsCount: Int,
  pillars: List<StreakPillarStatus>,
  isStreakAchieved: Boolean,
  borderFlashProgress: Float,
  celebrationPhaseProvider: () -> Float,
  onReplayCelebration: () -> Unit,
) {
  val animatedReadiness by animateFloatAsState(
    targetValue = (combinedReadinessPercent / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 480, easing = DashboardEaseOut),
    label = "streak_readiness_fill",
  )

  val badgeStart = if (isStreakAchieved) StepsStart else HydrationStart
  val badgeEnd = if (isStreakAchieved) CyberCyan else ProteinStart

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = if (isStreakAchieved) StepsStart else PureWhite,
        borderFlashProgress = borderFlashProgress,
      )
      .clickable(role = Role.Button, onClick = onReplayCelebration)
      .padding(16.dp)
      .testTag("dashboard_streak_card"),
  ) {
    // Celebratory Radial Shockwave & Particle Burst when Streak Goal is Achieved
    val burstPhase = celebrationPhaseProvider()
    if (burstPhase > 0.01f && burstPhase < 0.99f) {
      Canvas(modifier = Modifier.matchParentSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.45f
        val maxR = size.maxDimension * 0.62f
        val alpha = (1f - burstPhase).coerceIn(0f, 1f)

        drawCircle(
          color = PureWhite.copy(alpha = alpha * 0.4f),
          radius = maxR * burstPhase,
          center = Offset(cx, cy),
          style = Stroke(width = 2.dp.toPx()),
        )

        val sparkColors = listOf(PureWhite, StepsStart, CyberCyan, HydrationStart, CaloriesStart)
        for (i in 0 until 16) {
          val angle = (i * (2.0 * PI / 16.0)).toFloat()
          val dist = maxR * (0.16f + 0.84f * burstPhase)
          val px = cx + cos(angle) * dist
          val py = cy + sin(angle) * dist
          drawCircle(
            color = sparkColors[i % sparkColors.size].copy(alpha = alpha),
            radius = (3.8f * (1f - burstPhase * 0.45f)).dp.toPx(),
            center = Offset(px, py),
          )
        }
      }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Header Row: Luminous Dot + Title + The Single Relevant Optimal Status Badge (like Step 02 BMI)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          DashboardSyncedHeaderDot(
            startColor = if (isStreakAchieved) StepsStart else PureWhite,
            endColor = if (isStreakAchieved) CyberCyan else Zinc300,
          )
          Spacer(modifier = Modifier.width(9.dp))
          Text(
            text = "STREAK PROTOCOL ENGINE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.2.sp,
            letterSpacing = 1.0.sp,
          )
        }

        StreakStatusOptimalBadge(
          text = if (isStreakAchieved) "STREAK UNLOCKED" else "OPTIMAL GATE ≥ $requiredThresholdPercent%",
          startColor = badgeStart,
          endColor = badgeEnd,
        )
      }

      HorizontalDivider(
        thickness = 1.dp,
        color = Zinc800.copy(alpha = 0.85f),
      )

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Floating Inner Streak Readout Module (Matches DailyTargetsCommandDeck inner card on Step 04)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .floating3DInnerTile(
            cornerRadiusDp = 12.dp,
            accentColor = if (isStreakAchieved) StepsStart else Zinc600,
            isHighlighted = isStreakAchieved,
          )
          .padding(13.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .width(3.5.dp)
                  .height(12.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(
                    Brush.verticalGradient(
                      if (isStreakAchieved) listOf(StepsStart, CyberCyan) else listOf(PureWhite, Zinc400),
                    ),
                  ),
              )
              Text(
                text = "COMBINED DAILY STREAK",
                color = Zinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.2.sp,
                letterSpacing = 0.9.sp,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$streakDays",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.0).sp,
                modifier = Modifier.testTag("dashboard_streak_days_text"),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (streakDays == 1) "day active" else "days active",
                color = Zinc400,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(bottom = 3.dp),
              )
            }
          }

          Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(bottom = 2.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "READINESS ",
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.8.sp,
              )
              Text(
                text = "$combinedReadinessPercent%",
                color = if (isStreakAchieved) StepsStart else PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "$passedPillarsCount of 6 pillars ≥$requiredThresholdPercent%",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 10.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(11.dp))

        // Liquid-Chrome Multi-Tone White / Optimal Progress Bar with Luminous Reticle
        DashboardSpectrumGaugeBar(
          fillFraction = animatedReadiness,
          streakThresholdPercent = 100,
          gradientColors = if (isStreakAchieved) {
            listOf(StepsEnd, StepsMid, StepsStart, CyberCyan, PureWhite)
          } else {
            WhiteVariantMulticolorGradient
          },
          accentColor = if (isStreakAchieved) StepsStart else PureWhite,
          showThresholdMarker = false,
          barHeight = 9.5.dp,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. 6-Column Floating Pillar Matrix (Clean vertical accent bar + crisp numbers — NO colored block spam!)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        pillars.forEach { pillar ->
          val passed = pillar.isPassed
          val pillarFill = (pillar.achievedPercent.toFloat() / pillar.thresholdPercent.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)

          Column(
            modifier = Modifier
              .weight(1f)
              .floating3DInnerTile(
                cornerRadiusDp = 10.dp,
                accentColor = if (passed) pillar.accentColor else Zinc700,
                isHighlighted = passed,
              )
              .padding(horizontal = 6.dp, vertical = 7.5.dp),
            horizontalAlignment = Alignment.Start,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.5.dp),
              ) {
                Box(
                  modifier = Modifier
                    .width(2.5.dp)
                    .height(9.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(pillar.accentColor),
                )
                Text(
                  text = pillar.code,
                  color = if (passed) PureWhite else Zinc300,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.4.sp,
                  maxLines = 1,
                )
              }

              if (passed) {
                Text(
                  text = "✓",
                  color = pillar.accentColor,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.5.sp,
                )
              }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
              text = "${pillar.achievedPercent}%",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.2.sp,
              maxLines = 1,
            )

            Text(
              text = "/${pillar.thresholdPercent}%",
              color = if (passed) pillar.accentColor else Zinc500,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 8.2.sp,
              maxLines = 1,
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Mini telemetry bar matching PipelineMetricTile in ProtocolSummaryScreen
            Canvas(
              modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp),
            ) {
              val r = CornerRadius(size.height / 2f, size.height / 2f)
              drawRoundRect(
                color = RecessedTrackFill,
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = r,
              )
              val wFill = (size.width * pillarFill.coerceAtLeast(0.10f)).coerceAtMost(size.width)
              drawRoundRect(
                brush = Brush.horizontalGradient(
                  colors = listOf(pillar.secondaryColor, pillar.accentColor, PureWhite),
                  startX = 0f,
                  endX = wFill,
                ),
                topLeft = Offset.Zero,
                size = Size(wFill, size.height),
                cornerRadius = r,
              )
            }
          }
        }
      }

      // 4. Clean Celebratory Status Strip when Streak is Unlocked
      AnimatedVisibility(
        visible = isStreakAchieved,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .floating3DInnerTile(
              cornerRadiusDp = 9.dp,
              accentColor = StepsStart,
              isHighlighted = true,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "ALL 6 PILLARS ≥$requiredThresholdPercent% • DAILY STREAK LOCKED IN",
            color = StepsStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.6.sp,
            letterSpacing = 0.5.sp,
          )
          Text(
            text = "REPLAY ✦",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.2.sp,
          )
        }
      }
    }
  }
}

// ============================================================================
// FLOATING SECTION 2: PRIORITY #1 TELEMETRY — DAILY STEPS & DISTANCE (GOOGLE FIT 1:1)
// (Consistent with DailyTargetsCommandDeck on Step 04 — NO bulky colored header block!)
// ============================================================================

@Composable
private fun DashboardPriorityStepsCard(
  liveSteps: Int,
  targetSteps: Int,
  liveDistanceKm: Float,
  isActivelyWalking: Boolean,
  streakThresholdPercent: Int,
  borderFlashProgress: Float,
  onRefreshSteps: () -> Unit,
  numberFormat: NumberFormat,
) {
  val safeTargetSteps = targetSteps.coerceAtLeast(500)
  val stepsFraction = (liveSteps.toFloat() / safeTargetSteps.toFloat()).coerceIn(0f, 1f)
  val stepsPct = ((liveSteps.toFloat() / safeTargetSteps.toFloat()) * 100f).roundToInt()
  val remainingSteps = (safeTargetSteps - liveSteps).coerceAtLeast(0)
  val isGoalMet = stepsPct >= streakThresholdPercent

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = StepsStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_priority_steps_card"),
  ) {
    // 1. Clean Ecosystem Header Row: Luminous Dot + Title on left, Plain Colored % + Monochrome SYNC button on right
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        DashboardSyncedHeaderDot(
          startColor = StepsStart,
          endColor = CyberCyan,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "PRIORITY 01 • DAILY STEPS",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = if (isActivelyWalking) "LIVE • $stepsPct%" else "GOOGLE FIT • $stepsPct%",
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          letterSpacing = 0.5.sp,
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(Zinc900)
            .border(1.dp, Zinc700, RoundedCornerShape(7.dp))
            .clickable(role = Role.Button, onClick = onRefreshSteps)
            .padding(horizontal = 9.dp, vertical = 4.5.dp)
            .testTag("dashboard_steps_sync_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SYNC",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.2.sp,
            letterSpacing = 0.6.sp,
          )
        }
      }
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Floating 3D Inner Telemetry Module (Matches DailyTargetsCommandDeck on Step 04)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = StepsStart,
          isHighlighted = isGoalMet,
        )
        .padding(13.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(StepsStart, StepsMid))),
            )
            Text(
              text = "GOOGLE FIT & HEALTH CONNECT 1:1",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.2.sp,
              letterSpacing = 0.8.sp,
            )
          }

          Spacer(modifier = Modifier.height(7.dp))

          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = numberFormat.format(liveSteps),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 33.sp,
              lineHeight = 33.sp,
              letterSpacing = (-1.0).sp,
              modifier = Modifier.testTag("dashboard_live_steps_value"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "/ ${numberFormat.format(safeTargetSteps)} steps",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp,
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
        }

        // Floating Distance Readout Chip (Matches PerMealMacroChip on Step 04)
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Zinc950)
            .border(1.dp, Zinc800, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalAlignment = Alignment.End,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(StepsStart),
            )
            Text(
              text = "DISTANCE",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 9.sp,
              letterSpacing = 0.6.sp,
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = String.format(Locale.US, "%.2f", liveDistanceKm),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.5.sp,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "km",
              color = StepsStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.2.sp,
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Luminous Spectrum Progress Bar with Glowing Reticle & Streak Threshold Gate
      DashboardSpectrumGaugeBar(
        fillFraction = stepsFraction,
        streakThresholdPercent = streakThresholdPercent,
        gradientColors = listOf(StepsEnd, StepsMid, StepsStart, CyberCyan, PureWhite),
        accentColor = StepsStart,
        showThresholdMarker = true,
        barHeight = 9.5.dp,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (remainingSteps > 0) {
            "${numberFormat.format(remainingSteps)} steps remaining to daily goal"
          } else {
            "Daily step target achieved"
          },
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = 11.sp,
        )
        Text(
          text = "Streak Gate ≥$streakThresholdPercent%",
          color = if (isGoalMet) StepsStart else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.5.sp,
        )
      }
    }
  }
}

// ============================================================================
// FLOATING SECTION 3: PRIORITY #2 TELEMETRY — DAILY CALORIES + 3-COLUMN MACRO BENTO
// (Matches `DailyTargetsCommandDeck` & `CompactMacroBentoTile` in `ProtocolSummaryScreen.kt` 1:1)
// ============================================================================

@Composable
private fun DashboardCaloriesAndMacrosCard(
  loggedCalories: Int,
  targetCalories: Int,
  loggedProtein: Int,
  targetProtein: Int,
  loggedCarbs: Int,
  targetCarbs: Int,
  loggedFats: Int,
  targetFats: Int,
  streakThresholdPercent: Int,
  borderFlashProgress: Float,
  numberFormat: NumberFormat,
) {
  val safeTargetCal = targetCalories.coerceAtLeast(800)
  val calFraction = (loggedCalories.toFloat() / safeTargetCal.toFloat()).coerceIn(0f, 1f)
  val calPct = ((loggedCalories.toFloat() / safeTargetCal.toFloat()) * 100f).roundToInt()
  val remainingCalories = (safeTargetCal - loggedCalories).coerceAtLeast(0)
  val isCalGateMet = calPct >= streakThresholdPercent

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = CaloriesStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_metabolic_macros_card"),
  ) {
    // 1. Clean Ecosystem Header Row (Matches DailyTargetsCommandDeck on Step 04 — NO colored badge block!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = CaloriesStart,
          endColor = CaloriesMid,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "METABOLIC ENERGY & MACROS",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Text(
        text = "ENERGY • $calPct%",
        color = CaloriesStart,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
      )
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Floating Primary Calorie Card (Exact match to `target_card_calories` in `ProtocolSummaryScreen.kt`)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = CaloriesStart,
          isHighlighted = isCalGateMet,
        )
        .padding(13.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(CaloriesStart, CaloriesMid))),
            )
            Text(
              text = "DAILY ENERGY • CALORIES",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.2.sp,
              letterSpacing = 0.8.sp,
            )
          }

          Spacer(modifier = Modifier.height(7.dp))

          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = numberFormat.format(loggedCalories),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 32.sp,
              lineHeight = 32.sp,
              letterSpacing = (-1.0).sp,
              modifier = Modifier.testTag("dashboard_calories_logged_value"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "/ ${numberFormat.format(safeTargetCal)} kcal",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp,
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
        }

        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Zinc950)
            .border(1.dp, Zinc800, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
          horizontalAlignment = Alignment.End,
        ) {
          Text(
            text = if (remainingCalories > 0) "REMAINING" else "STATUS",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.8.sp,
            letterSpacing = 0.6.sp,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = if (remainingCalories > 0) numberFormat.format(remainingCalories) else "DONE",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
            )
            if (remainingCalories > 0) {
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "kcal",
                color = CaloriesStart,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.8.sp,
                modifier = Modifier.padding(bottom = 1.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(11.dp))

      DashboardSpectrumGaugeBar(
        fillFraction = calFraction,
        streakThresholdPercent = streakThresholdPercent,
        gradientColors = listOf(CaloriesEnd, CaloriesMid, CaloriesStart, Color(0xFFFFF2DC), PureWhite),
        accentColor = CaloriesStart,
        showThresholdMarker = true,
        barHeight = 9.dp,
      )

      Spacer(modifier = Modifier.height(7.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Target ${numberFormat.format(safeTargetCal)} kcal/day",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 9.6.sp,
        )
        Text(
          text = "Streak Gate ≥$streakThresholdPercent%",
          color = if (isCalGateMet) CaloriesStart else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.5.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. 3-Column Floating Macro Bento Grid (Matches `CompactMacroBentoTile` in `ProtocolSummaryScreen.kt` 1:1)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      DashboardMacroBentoTile(
        title = "Protein",
        loggedGrams = loggedProtein,
        targetGrams = targetProtein,
        streakThresholdPercent = streakThresholdPercent,
        accentColor = ProteinStart,
        gradientColors = listOf(ProteinEnd, ProteinMid, ProteinStart, PureWhite),
        testTag = "dashboard_macro_protein",
        modifier = Modifier.weight(1f),
      )

      DashboardMacroBentoTile(
        title = "Carbs",
        loggedGrams = loggedCarbs,
        targetGrams = targetCarbs,
        streakThresholdPercent = streakThresholdPercent,
        accentColor = CarbsStart,
        gradientColors = listOf(CarbsEnd, CarbsMid, CarbsStart, PureWhite),
        testTag = "dashboard_macro_carbs",
        modifier = Modifier.weight(1f),
      )

      DashboardMacroBentoTile(
        title = "Fats",
        loggedGrams = loggedFats,
        targetGrams = targetFats,
        streakThresholdPercent = streakThresholdPercent,
        accentColor = FatsStart,
        gradientColors = listOf(FatsEnd, FatsMid, FatsStart, PureWhite),
        testTag = "dashboard_macro_fats",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/**
 * 1:1 visual match to `CompactMacroBentoTile` in `ProtocolSummaryScreen.kt` (lines 1966–2072):
 * - Uniform vertical pill indicator (`3.5.dp x 11.dp`) + `title` on the left
 * - Clean colored percentage text (`color = accentColor`) on the right — NO bulky colored block!
 * - Large `PureWhite` logged grams + `/target g`
 * - Luminous spectrum progress bar + bottom target status line
 */
@Composable
private fun DashboardMacroBentoTile(
  title: String,
  loggedGrams: Int,
  targetGrams: Int,
  streakThresholdPercent: Int,
  accentColor: Color,
  gradientColors: List<Color>,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val safeTarget = targetGrams.coerceAtLeast(10)
  val rawFraction = loggedGrams.toFloat() / safeTarget.toFloat()
  val barFraction = rawFraction.coerceIn(0f, 1f)
  val pct = (rawFraction * 100f).roundToInt()
  val isPassed = pct >= streakThresholdPercent
  val remainingGrams = (safeTarget - loggedGrams).coerceAtLeast(0)

  Column(
    modifier = modifier
      .floating3DInnerTile(
        cornerRadiusDp = 11.dp,
        accentColor = accentColor,
        isHighlighted = isPassed,
      )
      .padding(10.5.dp)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .width(3.5.dp)
            .height(11.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(accentColor),
        )
        Text(
          text = title,
          color = Zinc300,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = "$pct%",
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.2.sp,
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "$loggedGrams",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = "/${safeTarget}g",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.5.sp,
        modifier = Modifier.padding(bottom = 2.dp),
        maxLines = 1,
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    DashboardSpectrumGaugeBar(
      fillFraction = barFraction,
      streakThresholdPercent = streakThresholdPercent,
      gradientColors = gradientColors,
      accentColor = accentColor,
      showThresholdMarker = true,
      barHeight = 6.5.dp,
    )

    Spacer(modifier = Modifier.height(5.dp))

    Text(
      text = if (remainingGrams > 0) "${remainingGrams}g left" else "Goal ≥$streakThresholdPercent% met",
      color = if (isPassed) accentColor else Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 9.sp,
      maxLines = 1,
    )
  }
}

/**
 * Luminous 3D Spectrum Progress Gauge Bar matching `AnimatedBmiSpectrumGauge` (Step 02)
 * and `SafeRangeTelemetryBar` (Step 04):
 * - Deep recessed capsule track with subtle 25% / 50% / 75% calibration micro-ticks
 * - Glowing color aura (`barAuraPaint`) + multi-stop gradient fill + specular top glass sheen
 * - Glowing reticle beacon at the current fill tip (even at 0%, a crisp ready-beacon sits at the launch origin so the bar never looks dead or flat)
 */
@Composable
internal fun DashboardSpectrumGaugeBar(
  fillFraction: Float,
  streakThresholdPercent: Int,
  gradientColors: List<Color>,
  accentColor: Color = PureWhite,
  showThresholdMarker: Boolean,
  barHeight: Dp,
  modifier: Modifier = Modifier,
) {
  val animatedFraction by animateFloatAsState(
    targetValue = fillFraction.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 420, easing = DashboardEaseOut),
    label = "dashboard_spectrum_bar_fill",
  )

  Spacer(
    modifier = modifier
      .fillMaxWidth()
      .height(barHeight)
      .drawWithCache {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(h / 2f, h / 2f)

        val barAuraPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              110,
              (accentColor.red * 255).roundToInt(),
              (accentColor.green * 255).roundToInt(),
              (accentColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(safeDashboardBlur(h * 1.15f), BlurMaskFilter.Blur.NORMAL)
          }
        }
        val reticleHaloPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              175,
              (accentColor.red * 255).roundToInt(),
              (accentColor.green * 255).roundToInt(),
              (accentColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(safeDashboardBlur(h * 0.95f), BlurMaskFilter.Blur.NORMAL)
          }
        }

        onDrawBehind {
          if (w <= 2f || h <= 1f) return@onDrawBehind

          // 1. Recessed 3D Capsule Track
          drawRoundRect(
            color = RecessedTrackFill,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = corner,
          )
          drawRoundRect(
            color = RecessedTrackBorder,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx()),
          )

          // Subtle calibration micro-ticks along the track
          for (step in 1..3) {
            val tickX = w * (step * 0.25f)
            drawLine(
              color = Zinc800.copy(alpha = 0.75f),
              start = Offset(tickX, h * 0.22f),
              end = Offset(tickX, h * 0.78f),
              strokeWidth = 1.dp.toPx(),
            )
          }

          // 2. Luminous Fill + Aura (Always keeps a minimum glowing launch capsule so 0% still looks alive)
          val visualFraction = max(animatedFraction, 0.028f)
          val fillW = (w * visualFraction).coerceIn(h, w)

          drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
              0f,
              0f,
              fillW,
              h,
              h / 2f,
              h / 2f,
              barAuraPaint,
            )
          }

          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = gradientColors,
              startX = 0f,
              endX = fillW.coerceAtLeast(h * 2f),
            ),
            topLeft = Offset.Zero,
            size = Size(fillW, h),
            cornerRadius = corner,
          )

          // Top specular glass highlight on the fill bar
          drawRoundRect(
            brush = Brush.verticalGradient(
              colors = listOf(PureWhite.copy(alpha = 0.42f), Color.Transparent),
              startY = 0f,
              endY = h * 0.58f,
            ),
            topLeft = Offset.Zero,
            size = Size(fillW, h),
            cornerRadius = corner,
          )

          // 3. Crisp Streak Gate Tick (X%)
          if (showThresholdMarker) {
            val gateX = (w * (streakThresholdPercent / 100f)).coerceIn(6f, w - 6f)
            drawLine(
              color = PureWhite.copy(alpha = 0.80f),
              start = Offset(gateX, 0f),
              end = Offset(gateX, h),
              strokeWidth = 1.5.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          // 4. Glowing Reticle Beacon at the Fill Tip (Matches PersonalDetailsScreen & ProfileCalibrationScreen)
          val tipX = (fillW - h * 0.45f).coerceIn(h * 0.5f, w - h * 0.5f)
          val tipCenter = Offset(tipX, h / 2f)
          drawIntoCanvas { canvas ->
            canvas.drawCircle(tipCenter, h * 0.72f, reticleHaloPaint)
          }
          drawCircle(
            color = PureWhite,
            radius = (h * 0.28f).coerceAtLeast(2.2f),
            center = tipCenter,
          )
        }
      },
  )
}
