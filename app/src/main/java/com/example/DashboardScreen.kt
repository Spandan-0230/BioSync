package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CaloriesEnd
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsEnd
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.ChronoBirthdayEnd
import com.example.ui.theme.ChronoBirthdayMid
import com.example.ui.theme.ChronoBirthdayStart
import com.example.ui.theme.ChronoDayEnd
import com.example.ui.theme.ChronoDayMid
import com.example.ui.theme.ChronoDayStart
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
import com.example.ui.theme.StreakReadinessEnd
import com.example.ui.theme.StreakReadinessMid
import com.example.ui.theme.StreakReadinessStart
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

// Unified Palette matching Steps 01–04
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
private val CardSurface = Color(0xFF131317)
private val CardBorder = Color(0xFF2D2D34)
private val InnerTileTop = Color(0xFF191920)
private val InnerTileBottom = Color(0xFF101015)
private val RecessedTrackFill = Color(0xFF07070A)
private val RecessedTrackBorder = Color(0xFF27272E)

private val SmoothOutEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

internal fun safeDashboardBlur(px: Float): Float = px.coerceAtLeast(1f)

@Immutable
internal data class DashboardResponsiveMetrics(
  val scale: Float,
  val fontCompensation: Float,
  val contentMaxWidth: Dp,
  val horizontalPadding: Dp,
  val topOffsetPadding: Dp,
  val bottomOffsetPadding: Dp,
  val sectionGap: Dp,
  val cardCornerRadius: Dp,
  val cardPadding: Dp,
  val innerTilePadding: Dp,
  val bentoGap: Dp,
) {
  fun spScaled(base: Float, minVal: Float, maxVal: Float): TextUnit =
    ((base * scale).coerceIn(minVal, maxVal) * fontCompensation).sp
}

private fun computeDashboardMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): DashboardResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val isExpanded = w >= 600f
  val widthFactor = (w / 392f).coerceIn(0.80f, 1.22f)
  val heightFactor = (h / 830f).coerceIn(0.76f, 1.18f)
  val scale = min(widthFactor, heightFactor * 1.05f).coerceIn(0.78f, 1.18f)

  val safeFontScale = fontScale.coerceIn(0.5f, 3.0f)
  val targetFontGrowth = if (safeFontScale >= 1f) {
    1f + (safeFontScale - 1f).coerceAtMost(1.2f) * 0.14f
  } else {
    1f - (1f - safeFontScale).coerceAtMost(0.5f) * 0.20f
  }
  val fontCompensation = targetFontGrowth / safeFontScale

  val contentMaxWidth = when {
    w >= 1100f -> (w * 0.86f).coerceIn(820f, 1080f).dp
    w >= 840f -> (w * 0.84f).coerceIn(680f, 880f).dp
    isExpanded -> (w * 0.82f).coerceIn(520f, 680f).dp
    else -> w.dp
  }

  return DashboardResponsiveMetrics(
    scale = scale,
    fontCompensation = fontCompensation,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (18f * widthFactor).coerceIn(14f, 28f).dp,
    topOffsetPadding = (12f * heightFactor).coerceIn(8f, 18f).dp,
    bottomOffsetPadding = (106f * heightFactor).coerceIn(88f, 126f).dp,
    sectionGap = (15f * heightFactor).coerceIn(11f, 20f).dp,
    cardCornerRadius = (18f * scale).coerceIn(14f, 20f).dp,
    cardPadding = (15f * scale).coerceIn(12f, 19f).dp,
    innerTilePadding = (11f * scale).coerceIn(8.5f, 14f).dp,
    bentoGap = (8f * widthFactor).coerceIn(6f, 11f).dp,
  )
}

/**
 * 3D Floating Command Deck Modifier with toned-down background reflection:
 * - Preserves deep multi-layered black 3D drop shadows for physical elevation
 * - Minimizes outer halo reflection and inner top-sheen haze so the obsidian background stays deep and crisp
 */
internal fun Modifier.floating3DProtocolCard(
  cornerRadiusDp: Dp = 18.dp,
  topRimTint: Color = PureWhite,
  borderFlashProgress: Float = 0f,
): Modifier = this.drawWithCache {
  val cornerPx = cornerRadiusDp.toPx()

  val deepShadowPaint = Paint().apply {
    isAntiAlias = true
    asFrameworkPaint().apply {
      color = android.graphics.Color.argb(230, 0, 0, 0)
      maskFilter = BlurMaskFilter(safeDashboardBlur(24.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }
  }

  val midShadowPaint = Paint().apply {
    isAntiAlias = true
    asFrameworkPaint().apply {
      color = android.graphics.Color.argb(175, 0, 0, 0)
      maskFilter = BlurMaskFilter(safeDashboardBlur(10.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }
  }

  // Subtle, low-reflection rim aura (toned down so there is no bright milky haze around cards)
  val ambientHaloPaint = Paint().apply {
    isAntiAlias = true
    asFrameworkPaint().apply {
      color = android.graphics.Color.argb(
        6,
        (topRimTint.red * 255).roundToInt(),
        (topRimTint.green * 255).roundToInt(),
        (topRimTint.blue * 255).roundToInt(),
      )
      maskFilter = BlurMaskFilter(safeDashboardBlur(6.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }
  }

  val surfaceBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xFF17171C),
      CardSurface,
      Color(0xFF0B0B0E),
    ),
  )

  val topSheenBrush = Brush.verticalGradient(
    colors = listOf(
      topRimTint.copy(alpha = 0.022f),
      Color.Transparent,
    ),
    startY = 0f,
    endY = size.height * 0.22f,
  )

  val baseRimBrush = Brush.verticalGradient(
    colors = listOf(
      PureWhite.copy(alpha = 0.24f),
      topRimTint.copy(alpha = 0.14f),
      CardBorder.copy(alpha = 0.85f),
      Color(0xFF16161B),
    ),
  )

  onDrawBehind {
    drawIntoCanvas { canvas ->
      canvas.drawRoundRect(
        3.dp.toPx(),
        12.dp.toPx(),
        size.width - 3.dp.toPx(),
        size.height + 14.dp.toPx(),
        cornerPx,
        cornerPx,
        deepShadowPaint,
      )
      canvas.drawRoundRect(
        1.dp.toPx(),
        5.dp.toPx(),
        size.width - 1.dp.toPx(),
        size.height + 6.dp.toPx(),
        cornerPx,
        cornerPx,
        midShadowPaint,
      )
      canvas.drawRoundRect(
        0f,
        -1.dp.toPx(),
        size.width,
        size.height + 1.dp.toPx(),
        cornerPx,
        cornerPx,
        ambientHaloPaint,
      )
    }

    drawRoundRect(
      brush = surfaceBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
    )

    drawRoundRect(
      brush = topSheenBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
    )

    drawRoundRect(
      brush = baseRimBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
      style = Stroke(width = 1.15.dp.toPx()),
    )

    if (borderFlashProgress > 0.01f && borderFlashProgress < 0.99f) {
      val bandHalfWidth = (size.width + size.height) * 0.26f
      val centerCoord = -bandHalfWidth +
        (size.width + size.height + bandHalfWidth * 2f) * borderFlashProgress
      val envelope = sin(borderFlashProgress * PI).toFloat().coerceIn(0f, 1f)
      val shimmerBrush = Brush.linearGradient(
        colors = listOf(
          Color.Transparent,
          PureWhite.copy(alpha = 0.12f * envelope),
          PureWhite.copy(alpha = 0.55f * envelope),
          PureWhite.copy(alpha = 0.12f * envelope),
          Color.Transparent,
        ),
        start = Offset(centerCoord - bandHalfWidth, (centerCoord - bandHalfWidth) * 0.5f),
        end = Offset(centerCoord + bandHalfWidth, (centerCoord + bandHalfWidth) * 0.5f),
      )
      drawRoundRect(
        brush = shimmerBrush,
        cornerRadius = CornerRadius(cornerPx, cornerPx),
        style = Stroke(width = 1.4.dp.toPx()),
      )
    }
  }
}

/**
 * 3D Floating Inner Bento Tile Modifier with crisp elevation and minimal reflection haze.
 */
internal fun Modifier.floating3DInnerTile(
  cornerRadiusDp: Dp = 12.dp,
  accentColor: Color = PureWhite,
  isHighlighted: Boolean = false,
): Modifier = this.drawWithCache {
  val cornerPx = cornerRadiusDp.toPx()

  val dropShadowPaint = Paint().apply {
    isAntiAlias = true
    asFrameworkPaint().apply {
      color = android.graphics.Color.argb(185, 0, 0, 0)
      maskFilter = BlurMaskFilter(safeDashboardBlur(8.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
    }
  }

  val surfaceBrush = Brush.verticalGradient(
    colors = listOf(
      InnerTileTop,
      InnerTileBottom,
    ),
  )

  val topSheenBrush = Brush.verticalGradient(
    colors = listOf(
      accentColor.copy(alpha = if (isHighlighted) 0.06f else 0.02f),
      Color.Transparent,
    ),
    startY = 0f,
    endY = size.height * 0.35f,
  )

  val borderBrush = Brush.verticalGradient(
    colors = listOf(
      if (isHighlighted) accentColor.copy(alpha = 0.65f) else PureWhite.copy(alpha = 0.18f),
      if (isHighlighted) accentColor.copy(alpha = 0.28f) else Zinc800.copy(alpha = 0.90f),
      Color(0xFF141419),
    ),
  )

  onDrawBehind {
    drawIntoCanvas { canvas ->
      canvas.drawRoundRect(
        1.dp.toPx(),
        4.dp.toPx(),
        size.width - 1.dp.toPx(),
        size.height + 4.dp.toPx(),
        cornerPx,
        cornerPx,
        dropShadowPaint,
      )
    }

    drawRoundRect(
      brush = surfaceBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
    )

    drawRoundRect(
      brush = topSheenBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
    )

    drawRoundRect(
      brush = borderBrush,
      cornerRadius = CornerRadius(cornerPx, cornerPx),
      style = Stroke(width = 1.dp.toPx()),
    )
  }
}

/**
 * Daily Protocol Dashboard Screen:
 * - Low-reflection 3D floating obsidian cards
 * - Attractive 3x2 Streak Pillars Bento Grid
 * - Redesigned Dual-Telemetry headers & borderless side-by-side metrics for Steps, Calories, and Hydration
 * - Solid Red (`#EF4444`) Cross (`×`) and Delete buttons
 * - Redesigned Chronological Timeline without repetitive top timestamp boxes
 */
@Composable
fun BioSyncDashboardScreen(
  subjectName: String,
  ageYears: Int = 31,
  birthDay: Int = 24,
  birthMonthZeroBased: Int = 9,
  birthYear: Int = 1994,
  targetCaloriesKcal: Int,
  targetProteinGrams: Int,
  targetCarbsGrams: Int,
  targetFatsGrams: Int,
  targetHydrationMl: Int,
  restDayWaterDeductionMl: Int = 0,
  targetStepsCount: Int,
  streakThreshold: StreakThresholdOption,
  mealFrequency: MealFrequencyOption,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  dayOne: DayOfWeekOption = DayOfWeekOption.MONDAY,
  protocolLockedAtEpochMillis: Long = 0L,
  dayOneAnchorStartEpochMillis: Long = 0L,
  dayOneInitialCountdownCompleted: Boolean = true,
  onDayOneCountdownFinished: () -> Unit = {},
  onOpenProtocolSummary: () -> Unit,
  onOpenSettingsPage: () -> Unit = onOpenProtocolSummary,
  onOpenStatsPage: () -> Unit = {},
  scrollState: ScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) },
  showInlineBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  var dashboardData by remember { mutableStateOf(DashboardLocalStore.load(context)) }
  val storeRevision by DashboardLocalStore.storeRevisionFlow.collectAsState()
  var pendingDeletedItem by remember {
    mutableStateOf<PendingDeletedTimelineItem?>(null)
  }
  var liveNowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var showTopRightSettingsMenu by remember { mutableStateOf(false) }
  var lastQuickLogEpochMs by remember { mutableLongStateOf(0L) }

  // Immediately sync when external Widget or Notification action buttons mutate DashboardLocalStore
  LaunchedEffect(storeRevision) {
    val latestFromDisk = DashboardLocalStore.load(context)
    if (latestFromDisk != dashboardData) {
      dashboardData = latestFromDisk
    }
  }

  fun persistDashboardData(updated: DashboardStoredData) {
    dashboardData = updated
    DashboardLocalStore.save(context, updated)
  }

  val isActiveWorkoutDay = remember(activityLevel, selectedWorkoutDays, dashboardData.dayKey) {
    if (activityLevel == ActivityLevelOption.SEDENTARY) {
      false
    } else {
      val cal = Calendar.getInstance()
      val todayOption = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> DayOfWeekOption.MONDAY
        Calendar.TUESDAY -> DayOfWeekOption.TUESDAY
        Calendar.WEDNESDAY -> DayOfWeekOption.WEDNESDAY
        Calendar.THURSDAY -> DayOfWeekOption.THURSDAY
        Calendar.FRIDAY -> DayOfWeekOption.FRIDAY
        Calendar.SATURDAY -> DayOfWeekOption.SATURDAY
        Calendar.SUNDAY -> DayOfWeekOption.SUNDAY
        else -> DayOfWeekOption.MONDAY
      }
      selectedWorkoutDays.contains(todayOption)
    }
  }

  val targetCalories = targetCaloriesKcal.coerceAtLeast(1000)
  val targetProtein = targetProteinGrams.coerceAtLeast(30)
  val targetCarbs = targetCarbsGrams.coerceAtLeast(40)
  val targetFats = targetFatsGrams.coerceAtLeast(20)
  val trainingWaterMl = targetHydrationMl.coerceAtLeast(500)
  val restWaterMl = if (activityLevel == ActivityLevelOption.SEDENTARY || selectedWorkoutDays.isEmpty()) {
    trainingWaterMl
  } else {
    (trainingWaterMl - restDayWaterDeductionMl.coerceAtLeast(0)).coerceAtLeast(1500)
  }
  val targetWaterMl = if (isActiveWorkoutDay) trainingWaterMl else restWaterMl
  val targetSteps = targetStepsCount.coerceAtLeast(1000)

  val stepSyncController = rememberBioSyncStepSyncController(
    decidedDailyStepsGoal = targetSteps,
    autoRequestPermissionsOnStart = false,
  )
  val stepSyncState = stepSyncController.state

  val timelineEntries = dashboardData.todayTimelineEntries
  val allCreatedSingleItems = dashboardData.allCreatedSingleItems
  val savedQuickItems = dashboardData.savedQuickItems

  val loggedCalories = remember(timelineEntries) {
    timelineEntries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
  }
  val loggedProtein = remember(timelineEntries) {
    timelineEntries.sumOf { it.proteinGrams }.coerceAtLeast(0)
  }
  val loggedCarbs = remember(timelineEntries) {
    timelineEntries.sumOf { it.carbsGrams }.coerceAtLeast(0)
  }
  val loggedFats = remember(timelineEntries) {
    timelineEntries.sumOf { it.fatsGrams }.coerceAtLeast(0)
  }
  val loggedWaterMl = remember(timelineEntries) {
    timelineEntries.sumOf { it.hydrationMl }.coerceAtLeast(0)
  }
  val sensorSteps = stepSyncState.liveTodaySteps.coerceAtLeast(0)
  val manualStepsToday = dashboardData.manualStepsToday.coerceAtLeast(0)
  val liveSteps = (sensorSteps + manualStepsToday).coerceAtLeast(0)
  val effectiveDistanceKm = stepSyncState.liveDistanceKm.coerceAtLeast(0f) + (manualStepsToday * 0.000762f)
  var showCustomStepsModal by remember { mutableStateOf(false) }

  val streakThresholdPercent = streakThreshold.percentage

  val stepsQualifies = remember(liveSteps, targetSteps, streakThresholdPercent) {
    targetSteps > 0 && ((liveSteps * 100f) / targetSteps.toFloat()).roundToInt() >= streakThresholdPercent
  }
  val caloriesQualifies = remember(loggedCalories, targetCalories, streakThresholdPercent) {
    targetCalories > 0 && ((loggedCalories * 100f) / targetCalories.toFloat()).roundToInt() >= streakThresholdPercent
  }
  val proteinQualifies = remember(loggedProtein, targetProtein, streakThresholdPercent) {
    targetProtein > 0 && ((loggedProtein * 100f) / targetProtein.toFloat()).roundToInt() >= streakThresholdPercent
  }
  val carbsQualifies = remember(loggedCarbs, targetCarbs, streakThresholdPercent) {
    targetCarbs > 0 && ((loggedCarbs * 100f) / targetCarbs.toFloat()).roundToInt() >= streakThresholdPercent
  }
  val fatsQualifies = remember(loggedFats, targetFats, streakThresholdPercent) {
    targetFats > 0 && ((loggedFats * 100f) / targetFats.toFloat()).roundToInt() >= streakThresholdPercent
  }
  val waterQualifies = remember(loggedWaterMl, targetWaterMl, streakThresholdPercent) {
    targetWaterMl > 0 && ((loggedWaterMl * 100f) / targetWaterMl.toFloat()).roundToInt() >= streakThresholdPercent
  }

  val allSixMet = stepsQualifies &&
    caloriesQualifies &&
    proteinQualifies &&
    carbsQualifies &&
    fatsQualifies &&
    waterQualifies

  LaunchedEffect(allSixMet, dashboardData.dayKey) {
    val todayKey = DashboardTimeFormatter.currentDayKey()
    if (dashboardData.dayKey != todayKey) return@LaunchedEffect

    val eval = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = dashboardData.baseStreakDays,
      lastCompletedDayKey = dashboardData.lastCompletedStreakDayKey,
      todayKey = todayKey,
      todayQualified = allSixMet,
    )
    if (eval.streakDays != dashboardData.baseStreakDays || eval.lastCompletedDayKey != dashboardData.lastCompletedStreakDayKey) {
      if (allSixMet && eval.streakDays > dashboardData.baseStreakDays) {
        haptics.confirm()
      }
      val fallbackLastKey = if (
        !allSixMet &&
        eval.lastCompletedDayKey.isBlank() &&
        eval.streakDays > 0 &&
        !eval.isBufferExpired
      ) {
        BioSyncStreakEngine.findLatestHistoricalCompletedDay(dashboardData.dailyHistoryMap, todayKey)
      } else {
        eval.lastCompletedDayKey
      }
      persistDashboardData(
        dashboardData.copy(
          dayKey = todayKey,
          baseStreakDays = eval.streakDays,
          lastCompletedStreakDayKey = fallbackLastKey,
        ),
      )
    }
  }

  // Continuously archive today's live telemetry & timeline into dailyHistoryMap inside the user's database
  LaunchedEffect(
    timelineEntries,
    liveSteps,
    targetCalories,
    targetProtein,
    targetCarbs,
    targetFats,
    targetWaterMl,
    targetSteps,
    streakThresholdPercent,
    allSixMet,
    isActiveWorkoutDay,
    dashboardData.dayKey,
  ) {
    val currentDayKey = DashboardTimeFormatter.currentDayKey()
    if (dashboardData.dayKey != currentDayKey) {
      val rolledOver = DashboardLocalStore.rolloverToDate(context, dashboardData, currentDayKey)
      dashboardData = rolledOver
      BioSyncHealthStepManager.resetForNewDay(context, currentDayKey)
      return@LaunchedEffect
    }

    val synced = DashboardLocalStore.withSyncedTodayRecord(
      data = dashboardData,
      liveSteps = liveSteps,
      targetSteps = targetSteps,
      targetCalories = targetCalories,
      targetProtein = targetProtein,
      targetCarbs = targetCarbs,
      targetFats = targetFats,
      targetWaterMl = targetWaterMl,
      streakThresholdPercent = streakThresholdPercent,
      streakQualified = allSixMet,
      isWorkoutDay = isActiveWorkoutDay,
    )
    if (synced != dashboardData) {
      persistDashboardData(synced)
    }
  }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000L)
      val tickMs = System.currentTimeMillis()
      liveNowEpochMs = tickMs
      val newDayKey = DashboardTimeFormatter.currentDayKey(tickMs)
      if (newDayKey != dashboardData.dayKey) {
        val rolledOver = DashboardLocalStore.rolloverToDate(context, dashboardData, newDayKey)
        dashboardData = rolledOver
        BioSyncHealthStepManager.resetForNewDay(context, newDayKey)
        pendingDeletedItem = null
      }
      val pending = pendingDeletedItem
      if (pending != null && tickMs >= pending.expiresAtEpochMs) {
        pendingDeletedItem = null
      }
    }
  }

  var borderFlashTriggerCount by remember { mutableIntStateOf(1) }
  var activeFlashCardIndex by remember { mutableIntStateOf(-1) }
  var cardFlashProgress by remember { mutableStateOf(0f) }

  LaunchedEffect(borderFlashTriggerCount) {
    for (cardIdx in 0..6) {
      activeFlashCardIndex = cardIdx
      val durationMs = 240L
      val frameMs = 16L
      val steps = (durationMs / frameMs).toInt().coerceAtLeast(1)
      for (s in 0..steps) {
        cardFlashProgress = s.toFloat() / steps.toFloat()
        delay(frameMs)
      }
      cardFlashProgress = 1f
      delay(30L)
    }
    activeFlashCardIndex = -1
    cardFlashProgress = 0f
  }

  LaunchedEffect(Unit) {
    while (true) {
      delay(18_000L)
      borderFlashTriggerCount += 1
    }
  }

  BackHandler(enabled = showTopRightSettingsMenu) {
    haptics.selection()
    showTopRightSettingsMenu = false
  }

  val infiniteTransition = rememberInfiniteTransition(label = "dashboard_ambient")
  val ambientPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "ambient_pulse",
  )

  val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .testTag("dashboard_screen_root"),
    contentAlignment = Alignment.TopCenter,
  ) {
    val fontScale = LocalDensity.current.fontScale
    val metrics = remember(maxWidth, maxHeight, fontScale) {
      computeDashboardMetrics(maxWidth, maxHeight, fontScale)
    }

    // Low-reflection Obsidian Backdrop Canvas
    DashboardAmbientBackdropCanvas(
      ambientPhase = ambientPulse,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = statusBarTopPadding)
        .verticalScroll(scrollState)
        .padding(
          top = metrics.topOffsetPadding,
          bottom = navBarBottomPadding + metrics.bottomOffsetPadding,
          start = metrics.horizontalPadding,
          end = metrics.horizontalPadding,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = metrics.contentMaxWidth)
          .fillMaxWidth(),
      ) {
        // 1. TOP COMMAND HEADER (Full Name at Top, Age just below it, Automatic ID Badge at Top-Right with Settings option)
        DashboardTopNavigationHeader(
          subjectName = subjectName,
          ageYears = ageYears,
          birthDay = birthDay,
          birthMonthZeroBased = birthMonthZeroBased,
          birthYear = birthYear,
          isActiveWorkoutDay = isActiveWorkoutDay,
          streakThresholdPercent = streakThresholdPercent,
          isSettingsMenuExpanded = showTopRightSettingsMenu,
          onToggleSettingsMenu = {
            haptics.selection()
            showTopRightSettingsMenu = !showTopRightSettingsMenu
          },
          onOpenSettingsPage = {
            haptics.confirm()
            showTopRightSettingsMenu = false
            onOpenSettingsPage()
          },
          onTriggerShimmer = {
            haptics.tick()
            borderFlashTriggerCount += 1
          },
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        val isWaitingForInitialDayOne = !dayOneInitialCountdownCompleted &&
          dayOneAnchorStartEpochMillis > 0L &&
          liveNowEpochMs < dayOneAnchorStartEpochMillis

        val notificationPermLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
        ) { _ -> }

        LaunchedEffect(isWaitingForInitialDayOne, dayOneAnchorStartEpochMillis) {
          if (isWaitingForInitialDayOne) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
              ) == PackageManager.PERMISSION_GRANTED
              if (!granted) {
                runCatching {
                  notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              }
            }
            BioSyncDayOneNotificationHelper.scheduleOneTimeDayOneAlarm(
              context = context,
              triggerAtEpochMillis = dayOneAnchorStartEpochMillis,
            )
          }
        }

        // One-time trigger when the initial Day 01 Anchor countdown reaches 00:00:00
        LaunchedEffect(
          dayOneInitialCountdownCompleted,
          dayOneAnchorStartEpochMillis,
          liveNowEpochMs >= dayOneAnchorStartEpochMillis,
        ) {
          if (
            !dayOneInitialCountdownCompleted &&
            dayOneAnchorStartEpochMillis > 0L &&
            liveNowEpochMs >= dayOneAnchorStartEpochMillis
          ) {
            haptics.confirm()
            BioSyncDayOneNotificationHelper.fireDayOneStartedNotificationOnce(
              context = context,
              subjectName = subjectName,
              dayOne = dayOne,
            )
            onDayOneCountdownFinished()
          }
        }

        if (isWaitingForInitialDayOne) {
          // ONE-TIME INITIAL DAY 01 ANCHOR LAUNCH COUNTDOWN SCREEN
          // Shown ONLY before the user's selected Day 1 weekday arrives for the very first time.
          DashboardInitialDayOneCountdownView(
            dayOne = dayOne,
            nowEpochMs = liveNowEpochMs,
            lockedAtEpochMs = protocolLockedAtEpochMillis,
            anchorStartEpochMs = dayOneAnchorStartEpochMillis,
            ambientPhase = ambientPulse,
            metrics = metrics,
          )
        } else {
          val liveStreakEval = remember(
            dashboardData.baseStreakDays,
            dashboardData.lastCompletedStreakDayKey,
            dashboardData.dailyHistoryMap,
            dashboardData.dayKey,
            allSixMet,
          ) {
            BioSyncStreakEngine.recalculateStreakFromHistory(
              historyMap = dashboardData.dailyHistoryMap,
              todayKey = dashboardData.dayKey,
              todayQualified = allSixMet,
              fallbackBaseStreak = dashboardData.baseStreakDays,
              fallbackLastCompletedKey = dashboardData.lastCompletedStreakDayKey,
            )
          }

          // 2. PRIORITY #1 TOP BLOCK: STREAK PROTOCOL ENGINE (Positioned directly above Chronometric Reverse Telemetry)
          DashboardCombinedStreakHeroCard(
            streakDays = liveStreakEval.streakDays,
            streakThresholdPercent = streakThresholdPercent,
            isInBufferGrace = liveStreakEval.isInBufferGrace,
            bufferDaysRemaining = liveStreakEval.bufferDaysRemaining,
            liveSteps = liveSteps,
            targetSteps = targetSteps,
            loggedCalories = loggedCalories,
            targetCalories = targetCalories,
            loggedProtein = loggedProtein,
            targetProtein = targetProtein,
            loggedCarbs = loggedCarbs,
            targetCarbs = targetCarbs,
            loggedFats = loggedFats,
            targetFats = targetFats,
            loggedWaterMl = loggedWaterMl,
            targetWaterMl = targetWaterMl,
            stepsQualifies = stepsQualifies,
            caloriesQualifies = caloriesQualifies,
            proteinQualifies = proteinQualifies,
            carbsQualifies = carbsQualifies,
            fatsQualifies = fatsQualifies,
            waterQualifies = waterQualifies,
            ambientPhase = ambientPulse,
            borderFlashProgress = if (activeFlashCardIndex == 0) cardFlashProgress else 0f,
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.sectionGap))

          // 3. CHRONOMETRIC REVERSE TELEMETRY (Positioned directly below Streak Protocol Engine)
          DashboardChronometricReverseCountdownCard(
            nowEpochMs = liveNowEpochMs,
            ageYears = ageYears,
            birthDay = birthDay,
            birthMonthZeroBased = birthMonthZeroBased,
            birthYear = birthYear,
            borderFlashProgress = if (activeFlashCardIndex == 6) cardFlashProgress else 0f,
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.sectionGap))

          // 4. FLOATING DAILY STEPS COMMAND CARD (With Custom Steps Input Button — Excluded from Timeline)
          DashboardPrimaryStepsHeroCard(
            liveSteps = liveSteps,
            manualStepsToday = manualStepsToday,
            targetSteps = targetSteps,
            liveDistanceKm = effectiveDistanceKm,
            isActivelyWalking = stepSyncState.isActivelyWalking,
            streakThresholdPercent = streakThresholdPercent,
            stepsQualifies = stepsQualifies,
            borderFlashProgress = if (activeFlashCardIndex == 1) cardFlashProgress else 0f,
            onForceSyncSteps = {
              haptics.selection()
              stepSyncController.forceRefreshNow()
            },
            onOpenCustomStepsModal = {
              haptics.selection()
              showCustomStepsModal = true
            },
            numberFormat = numberFormat,
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. FLOATING METABOLIC ENERGY (CALORIES) & 3-COLUMN MACROS CARD (Clean Calorie Balance Readout)
        DashboardCaloriesAndMacrosCard(
          loggedCalories = loggedCalories,
          targetCalories = targetCalories,
          loggedProtein = loggedProtein,
          targetProtein = targetProtein,
          loggedCarbs = loggedCarbs,
          targetCarbs = targetCarbs,
          loggedFats = loggedFats,
          targetFats = targetFats,
          streakThresholdPercent = streakThresholdPercent,
          caloriesQualifies = caloriesQualifies,
          proteinQualifies = proteinQualifies,
          carbsQualifies = carbsQualifies,
          fatsQualifies = fatsQualifies,
          borderFlashProgress = if (activeFlashCardIndex == 2) cardFlashProgress else 0f,
          numberFormat = numberFormat,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 5. FLOATING DEDICATED HYDRATION (H2O) COMMAND CARD (Clean Glasses Readout)
        DashboardHydrationCommandCard(
          loggedWaterMl = loggedWaterMl,
          targetWaterMl = targetWaterMl,
          streakThresholdPercent = streakThresholdPercent,
          borderFlashProgress = if (activeFlashCardIndex == 3) cardFlashProgress else 0f,
          onLogWaterMl = { addedMl ->
            val newHydrationEntry = DashboardTimelineEntry(
              type = DashboardEntryType.HYDRATION,
              title = "Hydration +${numberFormat.format(addedMl)} ml",
              subtitle = "Cellular H₂O",
              hydrationMl = addedMl,
            )
            persistDashboardData(
              dashboardData.copy(
                todayTimelineEntries = listOf(newHydrationEntry) + dashboardData.todayTimelineEntries,
              ),
            )
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 6. FLOATING QUICK ACTIONS (+) BAR WITH SAVED ITEMS BESIDE IT (Solid Red × Button)
        DashboardQuickActionsHubCard(
          allCreatedSingleItems = allCreatedSingleItems,
          savedQuickItems = savedQuickItems,
          targetCalories = targetCalories,
          targetProtein = targetProtein,
          targetCarbs = targetCarbs,
          targetFats = targetFats,
          mealCount = mealFrequency.count,
          borderFlashProgress = if (activeFlashCardIndex == 4) cardFlashProgress else 0f,
          onLogNutritionItem = { itemToLog, saveToQuickActions ->
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastQuickLogEpochMs >= 280L) {
              lastQuickLogEpochMs = nowMs
              val updatedSingles = if (!itemToLog.isMeal) {
                listOf(itemToLog) + dashboardData.allCreatedSingleItems.filterNot {
                  it.id == itemToLog.id || it.name.equals(itemToLog.name, ignoreCase = true)
                }
              } else {
                dashboardData.allCreatedSingleItems
              }
              val updatedSaved = if (saveToQuickActions) {
                val savedCopy = itemToLog.copy(isSavedToQuickActions = true)
                listOf(savedCopy) + dashboardData.savedQuickItems.filterNot {
                  it.id == savedCopy.id || (it.isMeal == savedCopy.isMeal && it.name.equals(savedCopy.name, ignoreCase = true))
                }
              } else {
                dashboardData.savedQuickItems
              }
              val newEntry = DashboardTimelineEntry(
                type = if (itemToLog.isMeal) DashboardEntryType.MEAL else DashboardEntryType.SINGLE_ITEM,
                title = itemToLog.name,
                subtitle = if (itemToLog.componentNames.isNotEmpty()) {
                  itemToLog.componentNames.joinToString(" + ")
                } else {
                  ""
                },
                caloriesKcal = itemToLog.caloriesKcal,
                proteinGrams = itemToLog.proteinGrams,
                carbsGrams = itemToLog.carbsGrams,
                fatsGrams = itemToLog.fatsGrams,
                hydrationMl = 0,
                colorArgb = itemToLog.colorArgb,
                mixedFromColorArgbs = itemToLog.mixedFromColorArgbs,
              )
              persistDashboardData(
                dashboardData.copy(
                  allCreatedSingleItems = updatedSingles,
                  savedQuickItems = updatedSaved,
                  todayTimelineEntries = listOf(newEntry) + dashboardData.todayTimelineEntries,
                ),
              )
            }
          },
          onQuickTapSavedItem = { savedItem ->
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastQuickLogEpochMs >= 280L) {
              lastQuickLogEpochMs = nowMs
              val newEntry = DashboardTimelineEntry(
                type = if (savedItem.isMeal) DashboardEntryType.MEAL else DashboardEntryType.SINGLE_ITEM,
                title = savedItem.name,
                subtitle = if (savedItem.componentNames.isNotEmpty()) {
                  savedItem.componentNames.joinToString(" + ")
                } else {
                  "Quick Action"
                },
                caloriesKcal = savedItem.caloriesKcal,
                proteinGrams = savedItem.proteinGrams,
                carbsGrams = savedItem.carbsGrams,
                fatsGrams = savedItem.fatsGrams,
                hydrationMl = 0,
                colorArgb = savedItem.colorArgb,
                mixedFromColorArgbs = savedItem.mixedFromColorArgbs,
              )
              persistDashboardData(
                dashboardData.copy(
                  todayTimelineEntries = listOf(newEntry) + dashboardData.todayTimelineEntries,
                ),
              )
            }
          },
          onDeleteSavedQuickItem = { itemId ->
            val targetToDelete = dashboardData.savedQuickItems.firstOrNull { it.id == itemId }
            persistDashboardData(
              dashboardData.copy(
                savedQuickItems = dashboardData.savedQuickItems.filterNot { it.id == itemId },
                allCreatedSingleItems = dashboardData.allCreatedSingleItems.filterNot { item ->
                  item.id == itemId ||
                    (targetToDelete != null &&
                      !targetToDelete.isMeal &&
                      item.name.equals(targetToDelete.name, ignoreCase = true))
                },
              ),
            )
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 7. FLOATING TODAY'S TIMELINE SECTION (Chronological Rail + Solid Red Delete Button)
        DashboardTodayTimelineSection(
          timelineEntries = timelineEntries,
          pendingDeletedItem = pendingDeletedItem,
          borderFlashProgress = if (activeFlashCardIndex == 5) cardFlashProgress else 0f,
          onDeleteEntry = { entryToRemove ->
            val currentList = dashboardData.todayTimelineEntries
            val idx = currentList.indexOfFirst { it.id == entryToRemove.id }
            if (idx >= 0) {
              pendingDeletedItem = PendingDeletedTimelineItem(
                entry = entryToRemove,
                originalIndex = idx,
                expiresAtEpochMs = System.currentTimeMillis() + 3_000L,
              )
              persistDashboardData(
                dashboardData.copy(
                  todayTimelineEntries = currentList.filterNot { it.id == entryToRemove.id },
                ),
              )
            }
          },
          onUndoDelete = {
            val pending = pendingDeletedItem
            if (pending != null) {
              val currentList = dashboardData.todayTimelineEntries.toMutableList()
              val insertIdx = pending.originalIndex.coerceIn(0, currentList.size)
              currentList.add(insertIdx, pending.entry)
              persistDashboardData(
                dashboardData.copy(
                  todayTimelineEntries = currentList,
                ),
              )
              pendingDeletedItem = null
            }
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )
        }
      }
    }

    val isWaitingForInitialDayOneBottomBar = !dayOneInitialCountdownCompleted &&
      dayOneAnchorStartEpochMillis > 0L &&
      liveNowEpochMs < dayOneAnchorStartEpochMillis

    // Centered Floating Bottom Navigation Bar (Home • Stats • Settings; Stats hidden during Day 01 Anchor Lock)
    if (showInlineBottomBar) {
      BioSyncFloatingBottomBar(
        activeDestination = BioSyncFloatingNavDestination.HOME,
        showStatsOption = !isWaitingForInitialDayOneBottomBar,
        onSelectHome = {},
        onSelectStats = {
          if (!isWaitingForInitialDayOneBottomBar) {
            onOpenStatsPage()
          }
        },
        onSelectSettings = onOpenSettingsPage,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }

    if (showCustomStepsModal && !isWaitingForInitialDayOneBottomBar) {
      DashboardCustomStepsInputModal(
        currentManualSteps = manualStepsToday,
        currentTotalSteps = liveSteps,
        targetSteps = targetSteps,
        numberFormat = numberFormat,
        onAddCustomSteps = { addedSteps ->
          haptics.confirm()
          val newManual = (dashboardData.manualStepsToday + addedSteps).coerceIn(0, 150_000)
          persistDashboardData(
            dashboardData.copy(manualStepsToday = newManual),
          )
          showCustomStepsModal = false
        },
        onSetExactCustomSteps = { exactManualSteps ->
          haptics.confirm()
          persistDashboardData(
            dashboardData.copy(manualStepsToday = exactManualSteps.coerceIn(0, 150_000)),
          )
          showCustomStepsModal = false
        },
        onResetCustomSteps = {
          haptics.selection()
          persistDashboardData(
            dashboardData.copy(manualStepsToday = 0),
          )
          showCustomStepsModal = false
        },
        onDismiss = {
          showCustomStepsModal = false
        },
      )
    }
  }
}

// ============================================================================
// ONE-TIME DAY 01 ANCHOR LAUNCH COUNTDOWN VIEW
// Shown strictly once when the user locks in Step 04 with an upcoming Day 1 weekday.
// Once the timer hits 00:00:00, a notification fires and the full Dashboard unlocks permanently.
// ============================================================================

@Composable
private fun DashboardInitialDayOneCountdownView(
  dayOne: DayOfWeekOption,
  nowEpochMs: Long,
  lockedAtEpochMs: Long,
  anchorStartEpochMs: Long,
  ambientPhase: Float,
  metrics: DashboardResponsiveMetrics,
) {
  val remainingMs = (anchorStartEpochMs - nowEpochMs).coerceAtLeast(0L)
  val totalSecs = (remainingMs / 1000L).coerceAtLeast(0L)
  val daysLeft = (totalSecs / 86_400L).toInt()
  val hoursLeft = ((totalSecs % 86_400L) / 3600L).toInt()
  val minsLeft = ((totalSecs % 3600L) / 60L).toInt()
  val secsLeft = (totalSecs % 60L).toInt()

  val totalSpanMs = if (lockedAtEpochMs in 1 until anchorStartEpochMs) {
    (anchorStartEpochMs - lockedAtEpochMs).coerceAtLeast(1_000L)
  } else {
    (7L * 86_400_000L)
  }
  val progressFraction = (1f - (remainingMs.toFloat() / totalSpanMs.toFloat())).coerceIn(0.05f, 0.99f)
  val progressPercent = (progressFraction * 100f).roundToInt().coerceIn(1, 99)

  val targetDateFormatted = remember(anchorStartEpochMs) {
    val cal = Calendar.getInstance().apply { timeInMillis = anchorStartEpochMs }
    val monthStr = DashboardMonthShortNames.getOrElse(cal.get(Calendar.MONTH)) { "NOV" }
    val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val yearNum = cal.get(Calendar.YEAR)
    "$monthStr $dayNum, $yearNum • 12:00 AM"
  }

  val warmTitanium = Color(0xFFE5DEC9)
  val warmTitaniumMuted = Color(0xFFC8BFA8)
  val brushedSilver = Color(0xFFE4E4E7)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = warmTitanium,
      )
      .padding(metrics.cardPadding)
      .testTag("dashboard_initial_day_one_countdown_card"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Status Bar: Horological Dial Node + Sculpted Platinum Emblem
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = PureWhite,
          endColor = warmTitanium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = "DAY 01 ANCHOR LAUNCH VAULT",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.4f, 8.8f, 11.8f),
            letterSpacing = 0.9.sp,
          )
          Text(
            text = "CHRONO SEALED UNTIL 00:00:00",
            color = warmTitaniumMuted,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(7.5f, 6.6f, 8.5f),
            letterSpacing = 0.6.sp,
          )
        }
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(PureWhite, warmTitanium, brushedSilver),
            ),
          )
          .border(0.9.dp, PureWhite, RoundedCornerShape(7.dp))
          .padding(start = 6.dp, end = 8.dp, top = 3.5.dp, bottom = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ObsidianBlack)
            .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
        ) {
          Text(
            text = "01×",
            color = warmTitanium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(7.4f, 6.5f, 8.4f),
          )
        }
        Text(
          text = "LAUNCH GATE",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          letterSpacing = 0.5.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height((18f * metrics.scale).coerceIn(12f, 22f).dp))

    // Sculpted 3D Haute-Horlogerie Platinum & Warm Titanium Orbital Chronometer Ring
    val ringSize = (186f * metrics.scale).coerceIn(150f, 214f).dp
    Box(
      modifier = Modifier.size(ringSize),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val strokePx = 8.5.dp.toPx()
        val innerRadius = (size.minDimension / 2f) - strokePx
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // Subtle radial platinum halo
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              warmTitanium.copy(alpha = 0.10f),
              PureWhite.copy(alpha = 0.03f),
              Color.Transparent,
            ),
            center = centerOffset,
            radius = size.minDimension * 0.52f,
          ),
          radius = size.minDimension * 0.52f,
          center = centerOffset,
        )

        // Outer recessed carbon-obsidian track
        drawCircle(
          color = Color(0xFF0E0E12),
          radius = innerRadius,
          center = centerOffset,
          style = Stroke(width = strokePx),
        )

        // Inner and outer metallic bezel hairlines
        drawCircle(
          color = PureWhite.copy(alpha = 0.16f),
          radius = innerRadius + strokePx * 0.52f,
          center = centerOffset,
          style = Stroke(width = 0.8.dp.toPx()),
        )
        drawCircle(
          color = warmTitanium.copy(alpha = 0.14f),
          radius = innerRadius - strokePx * 0.52f,
          center = centerOffset,
          style = Stroke(width = 0.8.dp.toPx()),
        )

        // Specular Brushed Platinum & Warm Titanium progress arc
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(
              Color(0xFF71717A),
              warmTitaniumMuted,
              warmTitanium,
              PureWhite,
              warmTitanium,
            ),
          ),
          startAngle = -90f,
          sweepAngle = 360f * progressFraction,
          useCenter = false,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        // Inner 60-tooth precision watchmaker bezel tick ring
        val tickRadius = innerRadius - 12.dp.toPx()
        for (i in 0 until 60) {
          val isMajor = i % 5 == 0
          val angleRad = Math.toRadians((i * 6.0) - 90.0 + sin(ambientPhase.toDouble()) * 1.2)
          val startR = tickRadius - if (isMajor) 5.5.dp.toPx() else 2.5.dp.toPx()
          val sx = centerOffset.x + (cos(angleRad) * startR).toFloat()
          val sy = centerOffset.y + (sin(angleRad) * startR).toFloat()
          val ex = centerOffset.x + (cos(angleRad) * tickRadius).toFloat()
          val ey = centerOffset.y + (sin(angleRad) * tickRadius).toFloat()
          drawLine(
            color = if (isMajor) warmTitanium.copy(alpha = 0.52f) else Zinc700.copy(alpha = 0.38f),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = if (isMajor) 1.35.dp.toPx() else 0.8.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "DAY 01 ANCHOR",
          color = warmTitaniumMuted,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
          letterSpacing = 1.0.sp,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = dayOne.fullName.uppercase(),
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(20f, 16.5f, 23f),
          letterSpacing = (-0.4).sp,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(PureWhite, warmTitanium),
              ),
            )
            .border(0.8.dp, PureWhite, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = "$progressPercent% CALIBRATED",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.4f),
            letterSpacing = 0.5.sp,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height((18f * metrics.scale).coerceIn(12f, 22f).dp))

    // 4-Chamber Mechanical Split-Flap / Keycap Countdown Grid (DAYS : HOURS : MINS : SECS)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(metrics.bentoGap),
    ) {
      val timeUnits = listOf(
        "DAYS" to String.format(Locale.US, "%02d", daysLeft),
        "HOURS" to String.format(Locale.US, "%02d", hoursLeft),
        "MINS" to String.format(Locale.US, "%02d", minsLeft),
        "SECS" to String.format(Locale.US, "%02d", secsLeft),
      )
      timeUnits.forEach { (unitLabel, unitValue) ->
        val isSecs = unitLabel == "SECS"
        Column(
          modifier = Modifier
            .weight(1f)
            .floating3DInnerTile(
              cornerRadiusDp = 11.dp,
              accentColor = if (isSecs) warmTitanium else PureWhite,
            )
            .padding(vertical = (11f * metrics.scale).coerceIn(8.5f, 14f).dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = unitValue,
            color = if (isSecs) warmTitanium else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(22f, 18f, 26f),
            letterSpacing = (-0.5).sp,
            maxLines = 1,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (isSecs) warmTitanium.copy(alpha = 0.16f) else Color(0xFF070709))
              .border(
                width = 0.6.dp,
                color = if (isSecs) warmTitanium.copy(alpha = 0.45f) else Color(0xFF232329),
                shape = RoundedCornerShape(4.dp),
              )
              .padding(horizontal = 6.dp, vertical = 1.5.dp),
          ) {
            Text(
              text = unitLabel,
              color = if (isSecs) warmTitanium else Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(7.6f, 6.6f, 8.8f),
              letterSpacing = 0.7.sp,
              maxLines = 1,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Target Unlock Date & Notification Horological Dossier Box
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0B0B0E), Color(0xFF050507)),
          ),
        )
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.32f),
              warmTitanium.copy(alpha = 0.28f),
              Color(0xFF222227),
            ),
          ),
          shape = RoundedCornerShape(12.dp),
        )
        .padding(horizontal = 13.dp, vertical = 11.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "PROTOCOL UNLOCK WINDOW",
          color = warmTitaniumMuted,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
          letterSpacing = 0.7.sp,
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(PureWhite, warmTitanium),
              ),
            )
            .padding(horizontal = 7.5.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = targetDateFormatted,
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.4f, 7.4f, 9.6f),
            letterSpacing = 0.3.sp,
          )
        }
      }

      HorizontalDivider(color = Color(0xFF1F1F24), thickness = 0.8.dp)

      Text(
        text = "Your full Daily Protocol Dashboard will unlock automatically when this one-time chronometer reaches 00:00:00, and a system notification will alert you to begin Day 01.",
        color = Zinc300,
        fontFamily = InterFontFamily,
        fontSize = metrics.spScaled(11.2f, 9.8f, 12.5f),
        lineHeight = metrics.spScaled(16f, 14f, 18f),
      )
    }
  }
}

// ============================================================================
// AMBIENT BACKDROP CANVAS (Toned down reflection so background stays deep obsidian)
// ============================================================================

@Composable
private fun DashboardAmbientBackdropCanvas(
  ambientPhase: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val gridSpacing = 28.dp.toPx()
    val dotRadius = 0.85.dp.toPx()

    var x = gridSpacing * 0.5f
    while (x < w) {
      var y = gridSpacing * 0.5f
      while (y < h) {
        drawCircle(
          color = Color.White.copy(alpha = 0.014f),
          radius = dotRadius,
          center = Offset(x, y),
        )
        y += gridSpacing
      }
      x += gridSpacing
    }

    val driftX = cos(ambientPhase) * (w * 0.03f)
    val driftY = sin(ambientPhase) * (h * 0.015f)

    // Subtle deep emerald aura
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          StepsStart.copy(alpha = 0.028f),
          Color.Transparent,
        ),
        center = Offset(w * 0.84f + driftX, h * 0.12f + driftY),
        radius = w * 0.62f,
      ),
      radius = w * 0.62f,
      center = Offset(w * 0.84f + driftX, h * 0.12f + driftY),
    )

    // Subtle deep cyan aura
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          HydrationStart.copy(alpha = 0.022f),
          Color.Transparent,
        ),
        center = Offset(w * 0.16f - driftX, h * 0.54f - driftY),
        radius = w * 0.58f,
      ),
      radius = w * 0.58f,
      center = Offset(w * 0.16f - driftX, h * 0.54f - driftY),
    )
  }
}

// ============================================================================
// TOP NAVIGATION HEADER (Name + Age below it + Top-Right Automatic ID Badge + Settings Option)
// ============================================================================

private val DashboardMonthShortNames = listOf(
  "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
  "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
)

@Composable
private fun DashboardTopNavigationHeader(
  subjectName: String,
  ageYears: Int,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  isActiveWorkoutDay: Boolean,
  streakThresholdPercent: Int,
  isSettingsMenuExpanded: Boolean,
  onToggleSettingsMenu: () -> Unit,
  onOpenSettingsPage: () -> Unit,
  onTriggerShimmer: () -> Unit,
  metrics: DashboardResponsiveMetrics,
) {
  val todayFormatted = remember {
    val cal = Calendar.getInstance()
    val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)?.uppercase(Locale.US) ?: "TODAY"
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)?.uppercase(Locale.US) ?: ""
    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
    "$dayName, $monthName $dayNum"
  }

  val subjectIdBadge = remember(subjectName) {
    BioSyncLocalStore.computeSubjectIdBadge(subjectName)
  }

  val exactAgeTelemetry = remember(birthDay, birthMonthZeroBased, birthYear, ageYears) {
    val now = Calendar.getInstance()
    val curYear = now.get(Calendar.YEAR)
    val curMonth = now.get(Calendar.MONTH)
    val curDay = now.get(Calendar.DAY_OF_MONTH)
    val safeMonth = birthMonthZeroBased.coerceIn(0, 11)
    var y = curYear - birthYear
    var m = curMonth - safeMonth
    var d = curDay - birthDay
    if (d < 0) {
      m -= 1
      val prevMonthCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, curYear)
        set(Calendar.MONTH, curMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.DAY_OF_MONTH, -1)
      }
      d += prevMonthCal.get(Calendar.DAY_OF_MONTH)
    }
    if (m < 0) {
      y -= 1
      m += 12
    }
    val resolvedYears = if (y > 0) y else ageYears.coerceAtLeast(1)
    val safeM = m.coerceAtLeast(0)
    val safeD = d.coerceAtLeast(0)
    val monthStr = DashboardMonthShortNames.getOrElse(safeMonth) { "OCT" }
    val dayStr = birthDay.coerceIn(1, 31).toString().padStart(2, '0')
    val cycleProgress = ((safeM * 30.4375f + safeD) / 365.25f).coerceIn(0.04f, 0.99f)
    val daysElapsedInYear = (safeM * 30.4375f + safeD).roundToInt().coerceIn(0, 365)
    listOf(
      resolvedYears.toString(),
      "${safeM.toString().padStart(2, '0')}M : ${safeD.toString().padStart(2, '0')}D",
      "$monthStr $dayStr, $birthYear",
      cycleProgress.toString(),
      daysElapsedInYear.toString(),
    )
  }

  val resolvedAgeYears = exactAgeTelemetry[0].toIntOrNull() ?: ageYears
  val exactMonthsDaysLabel = exactAgeTelemetry[1]
  val dobFormattedLabel = exactAgeTelemetry[2]
  val ageCycleProgress = exactAgeTelemetry[3].toFloatOrNull() ?: 0.25f

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_top_header"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left: Eyebrow + Subject Full Name at the Top
      Column(
        modifier = Modifier
          .weight(1f)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTriggerShimmer,
          ),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Box(
            modifier = Modifier
              .size(6.5.dp)
              .clip(CircleShape)
              .background(if (isActiveWorkoutDay) StepsStart else HydrationStart),
          )
          Text(
            text = "$todayFormatted • ${if (isActiveWorkoutDay) "ACTIVE TRAINING DAY" else "REST & RECOVERY DAY"}",
            color = if (isActiveWorkoutDay) StepsStart else HydrationStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(9.6f, 8.2f, 11.2f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Subject Full Name at the Top of the screen (Daily Protocol text removed)
        Text(
          text = subjectName.ifBlank { "Elena Rostova" },
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(26f, 21f, 30f),
          letterSpacing = (-0.75).sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("dashboard_subject_name_text"),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Top-Right Corner: Automatic Subject ID Badge (First letter of 1st word + First letter of 2nd word)
      // Clicking this badge reveals the Settings option
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = if (isSettingsMenuExpanded) {
                listOf(PureWhite, Color(0xFFE4E4E7))
              } else {
                listOf(Color(0xFF1E1E26), Color(0xFF101015))
              },
            ),
          )
          .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
              colors = if (isSettingsMenuExpanded) {
                listOf(PureWhite, PureWhite)
              } else {
                listOf(PureWhite.copy(alpha = 0.38f), Zinc700.copy(alpha = 0.75f), Zinc850)
              },
            ),
            shape = RoundedCornerShape(12.dp),
          )
          .clickable(
            role = Role.Button,
            onClick = onToggleSettingsMenu,
          )
          .padding(horizontal = 12.dp, vertical = 9.dp)
          .testTag("dashboard_top_right_id_badge"),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(if (isSettingsMenuExpanded) ObsidianBlack else StepsStart),
          )
          Text(
            text = subjectIdBadge,
            color = if (isSettingsMenuExpanded) ObsidianBlack else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(14f, 12.5f, 15.5f),
            letterSpacing = 0.8.sp,
          )
          Canvas(modifier = Modifier.size(10.dp)) {
            val w = size.width
            val h = size.height
            val chevron = Path().apply {
              if (isSettingsMenuExpanded) {
                moveTo(w * 0.15f, h * 0.68f)
                lineTo(w * 0.50f, h * 0.30f)
                lineTo(w * 0.85f, h * 0.68f)
              } else {
                moveTo(w * 0.15f, h * 0.32f)
                lineTo(w * 0.50f, h * 0.70f)
                lineTo(w * 0.85f, h * 0.32f)
              }
            }
            drawPath(
              path = chevron,
              color = if (isSettingsMenuExpanded) ObsidianBlack else Zinc300,
              style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height((8f * metrics.scale).coerceIn(6f, 11f).dp))

    // Sculpted 3D Biometric Age & Chronological Telemetry Bar directly below the Name
    val ageCapsuleShape = RoundedCornerShape((12f * metrics.scale).coerceIn(10f, 14f).dp)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(ageCapsuleShape)
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF1A1B22),
              Color(0xFF0E0F14),
            ),
          ),
        )
        .border(
          width = 1.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.32f),
              Zinc700.copy(alpha = 0.55f),
              Zinc850,
            ),
          ),
          shape = ageCapsuleShape,
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onTriggerShimmer,
        )
        .padding(
          horizontal = (10f * metrics.scale).coerceIn(8f, 13f).dp,
          vertical = (7.5f * metrics.scale).coerceIn(6f, 10f).dp,
        )
        .testTag("dashboard_subject_age_row"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Left Primary Emblem: Brushed Platinum/Metallic Age Pill with Dual-Ring Precision Node
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10f).dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape((8f * metrics.scale).coerceIn(6.5f, 10f).dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  PureWhite,
                  Color(0xFFE4E4E7),
                ),
              ),
            )
            .border(
              width = 0.8.dp,
              color = PureWhite,
              shape = RoundedCornerShape((8f * metrics.scale).coerceIn(6.5f, 10f).dp),
            )
            .padding(
              horizontal = (9f * metrics.scale).coerceIn(7f, 11.5f).dp,
              vertical = (4.5f * metrics.scale).coerceIn(3.5f, 6f).dp,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.5.dp),
          ) {
            Canvas(modifier = Modifier.size((11f * metrics.scale).coerceIn(9.5f, 12.5f).dp)) {
              val strokePx = 1.6.dp.toPx()
              drawCircle(
                color = ObsidianBlack.copy(alpha = 0.18f),
                style = Stroke(width = strokePx),
              )
              drawArc(
                color = ObsidianBlack,
                startAngle = -90f,
                sweepAngle = 360f * ageCycleProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
              )
              drawCircle(
                color = ObsidianBlack,
                radius = size.minDimension * 0.19f,
              )
            }
            Text(
              text = "$resolvedAgeYears",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(13.2f, 11.2f, 15f),
              letterSpacing = (-0.3).sp,
            )
            Text(
              text = "YEARS OLD",
              color = Color(0xFF27272A),
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(9.4f, 8f, 10.8f),
              letterSpacing = 0.7.sp,
            )
          }
        }

        // Exact Sub-Year Chronological Precision Readout (+ MM : DD)
        Column(
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "CHRONO PRECISION",
            color = Zinc500,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(7.4f, 6.5f, 8.5f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
          )
          Text(
            text = "+$exactMonthsDaysLabel",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(10.6f, 9f, 12f),
            letterSpacing = 0.4.sp,
            maxLines = 1,
          )
        }
      }

      // Vertical Specular Hairline Divider + Right DOB Origin Stamp
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).coerceIn(6f, 10f).dp),
      ) {
        Box(
          modifier = Modifier
            .width(1.dp)
            .height((22f * metrics.scale).coerceIn(18f, 26f).dp)
            .background(
              brush = Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Zinc700,
                  Color.Transparent,
                ),
              ),
            ),
        )

        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier = Modifier
                .size(4.5.dp)
                .clip(CircleShape)
                .background(ChronoBirthdayStart),
            )
            Text(
              text = "BIOMETRIC DOB",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(7.4f, 6.5f, 8.5f),
              letterSpacing = 0.7.sp,
              maxLines = 1,
            )
          }
          Text(
            text = dobFormattedLabel,
            color = Zinc200,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(10.2f, 8.8f, 11.6f),
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // Floating Top-Right Settings Option Popover Card (Shown when clicking the Top-Right ID Badge)
    AnimatedVisibility(
      visible = isSettingsMenuExpanded,
      enter = expandVertically(animationSpec = tween(220, easing = SmoothOutEasing)) + fadeIn(tween(180)),
      exit = shrinkVertically(animationSpec = tween(180, easing = SmoothOutEasing)) + fadeOut(tween(140)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .floating3DProtocolCard(
            cornerRadiusDp = 15.dp,
            topRimTint = PureWhite,
          )
          .padding(13.dp)
          .testTag("dashboard_id_settings_dropdown"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PureWhite)
                .padding(horizontal = 9.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "ID • $subjectIdBadge",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.2.sp,
                letterSpacing = 0.6.sp,
              )
            }
            Column {
              Text(
                text = subjectName.ifBlank { "Elena Rostova" },
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
              )
              Text(
                text = "$resolvedAgeYears Yrs (+$exactMonthsDaysLabel) • Biometric Dossier Locked",
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.6.sp,
              )
            }
          }

          Text(
            text = "ACTIVE ID",
            color = StepsStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.2.sp,
            letterSpacing = 0.7.sp,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Zinc800, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Primary Settings Option Button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(PureWhite, Color(0xFFEAECEF)),
              ),
            )
            .clickable(
              role = Role.Button,
              onClick = onOpenSettingsPage,
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("dashboard_open_settings_option_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(ObsidianBlack),
              contentAlignment = Alignment.Center,
            ) {
              Canvas(modifier = Modifier.size(14.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 1.5.dp.toPx()
                drawLine(
                  color = PureWhite,
                  start = Offset(w * 0.12f, h * 0.28f),
                  end = Offset(w * 0.88f, h * 0.28f),
                  strokeWidth = stroke,
                  cap = StrokeCap.Round,
                )
                drawCircle(
                  color = StepsStart,
                  radius = 2.2.dp.toPx(),
                  center = Offset(w * 0.34f, h * 0.28f),
                )
                drawLine(
                  color = PureWhite,
                  start = Offset(w * 0.12f, h * 0.72f),
                  end = Offset(w * 0.88f, h * 0.72f),
                  strokeWidth = stroke,
                  cap = StrokeCap.Round,
                )
                drawCircle(
                  color = HydrationStart,
                  radius = 2.2.dp.toPx(),
                  center = Offset(w * 0.68f, h * 0.72f),
                )
              }
            }
            Column {
              Text(
                text = "Settings",
                color = ObsidianBlack,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.8.sp,
                letterSpacing = (-0.2).sp,
              )
              Text(
                text = "Edit all profile, body & blueprint info • Factory reset",
                color = Color(0xFF3F3F46),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.6.sp,
              )
            }
          }

          Text(
            text = "OPEN ↗",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.5.sp,
            letterSpacing = 0.6.sp,
          )
        }
      }
    }
  }
}

// ============================================================================
// CHRONOMETRIC REVERSE COUNTDOWN DECK (Day Remaining to End + Next Birthday Reverse Countdown)
// ============================================================================

@Composable
private fun DashboardChronometricReverseCountdownCard(
  nowEpochMs: Long,
  ageYears: Int,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  borderFlashProgress: Float = 0f,
  metrics: DashboardResponsiveMetrics,
) {
  // 1. Calculate End-of-Day Reverse Countdown (Time Remaining Today until 24:00 Midnight)
  val endOfDayCal = Calendar.getInstance().apply {
    timeInMillis = nowEpochMs
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
  }
  val startOfDayCal = Calendar.getInstance().apply {
    timeInMillis = nowEpochMs
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
  val totalDayMs = (endOfDayCal.timeInMillis - startOfDayCal.timeInMillis + 1L).coerceAtLeast(86_400_000L)
  val remainingDayMs = (endOfDayCal.timeInMillis - nowEpochMs + 1L).coerceIn(0L, totalDayMs)
  val dayRemainingFraction = (remainingDayMs.toFloat() / totalDayMs.toFloat()).coerceIn(0f, 1f)
  val dayRemainingPct = (dayRemainingFraction * 100f).roundToInt().coerceIn(0, 100)

  val remDayTotalSecs = (remainingDayMs / 1000L).toInt().coerceAtLeast(0)
  val dayHoursLeft = remDayTotalSecs / 3600
  val dayMinsLeft = (remDayTotalSecs % 3600) / 60
  val daySecsLeft = remDayTotalSecs % 60
  val dayCountdownFormatted = String.format(
    Locale.US,
    "%02dh %02dm %02ds",
    dayHoursLeft,
    dayMinsLeft,
    daySecsLeft,
  )

  // 2. Calculate Next Birthday Reverse Countdown
  val safeBirthMonth = birthMonthZeroBased.coerceIn(0, 11)
  val nowCal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
  val curYear = nowCal.get(Calendar.YEAR)

  val nextBirthdayCal = Calendar.getInstance().apply {
    timeInMillis = nowEpochMs
    set(Calendar.YEAR, curYear)
    set(Calendar.MONTH, safeBirthMonth)
    val maxD = getActualMaximum(Calendar.DAY_OF_MONTH)
    set(Calendar.DAY_OF_MONTH, birthDay.coerceIn(1, maxD))
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }

  val isBirthdayToday = nowCal.get(Calendar.MONTH) == safeBirthMonth &&
    nowCal.get(Calendar.DAY_OF_MONTH) == birthDay.coerceIn(1, 31)

  if (!isBirthdayToday && nextBirthdayCal.timeInMillis <= nowEpochMs) {
    nextBirthdayCal.add(Calendar.YEAR, 1)
  }

  val prevBirthdayCal = (nextBirthdayCal.clone() as Calendar).apply {
    add(Calendar.YEAR, -1)
  }
  val totalYearCycleMs = (nextBirthdayCal.timeInMillis - prevBirthdayCal.timeInMillis).coerceAtLeast(1L)
  val bdayDiffMs = if (isBirthdayToday) 0L else (nextBirthdayCal.timeInMillis - nowEpochMs).coerceAtLeast(0L)
  val bdayTotalSecs = (bdayDiffMs / 1000L).coerceAtLeast(0L)
  val bdayDaysLeft = (bdayTotalSecs / 86_400L).toInt()
  val bdayHoursLeft = ((bdayTotalSecs % 86_400L) / 3600L).toInt()
  val bdayMinsLeft = ((bdayTotalSecs % 3600L) / 60L).toInt()
  val bdaySecsLeft = (bdayTotalSecs % 60L).toInt()

  // Reverse progress: shows remaining proportion of year or cycle progression
  val bdayCycleProgress = if (isBirthdayToday) {
    1f
  } else {
    (1f - (bdayDiffMs.toFloat() / totalYearCycleMs.toFloat())).coerceIn(0.06f, 0.98f)
  }
  val nextBirthdayMonthName = DashboardMonthShortNames.getOrElse(safeBirthMonth) { "OCT" }
  val nextBirthdayDateLabel = "$nextBirthdayMonthName ${birthDay.coerceIn(1, 31).toString().padStart(2, '0')}"
  val upcomingAge = (nextBirthdayCal.get(Calendar.YEAR) - birthYear).coerceAtLeast(ageYears + 1)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = ChronoDayStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(metrics.cardPadding)
      .testTag("dashboard_chronometric_countdown_card"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = ChronoDayStart,
          endColor = ChronoBirthdayStart,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "CHRONOMETRIC REVERSE TELEMETRY",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(10.6f, 9f, 12.2f),
          letterSpacing = 1.0.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = "LIVE SYNC • 1S",
        color = ChronoBirthdayStart,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = metrics.spScaled(9.2f, 8f, 10.6f),
        letterSpacing = 0.6.sp,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(metrics.bentoGap),
    ) {
      // Chamber 1: Day Remaining to End Reverse Countdown (Signature Electric Amethyst-Violet)
      Column(
        modifier = Modifier
          .weight(1f)
          .floating3DInnerTile(
            cornerRadiusDp = 12.dp,
            accentColor = ChronoDayStart,
          )
          .padding(metrics.innerTilePadding)
          .testTag("dashboard_day_remaining_tile"),
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
                .width(3.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(
                  brush = Brush.verticalGradient(
                    colors = listOf(ChronoDayStart, ChronoDayMid),
                  ),
                ),
            )
            Text(
              text = "DAY REMAINING",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(9.2f, 7.8f, 10.6f),
              letterSpacing = 0.5.sp,
              maxLines = 1,
            )
          }

          Text(
            text = "$dayRemainingPct% LEFT",
            color = ChronoDayStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.8f, 7.5f, 10f),
            maxLines = 1,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = dayCountdownFormatted,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(15.5f, 12.8f, 17.5f),
          letterSpacing = (-0.3).sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("dashboard_day_remaining_value"),
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text = "Until 12:00 AM day end",
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10f, 8.6f, 11.4f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(7.dp))

        // Reverse Progress Bar (Unique Electric Amethyst -> Lavender -> Specular Tip)
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(5.5.dp),
        ) {
          val r = CornerRadius(size.height / 2f, size.height / 2f)
          drawRoundRect(
            color = RecessedTrackFill,
            size = size,
            cornerRadius = r,
          )
          val fillW = (size.width * dayRemainingFraction.coerceIn(0.05f, 1f)).coerceAtLeast(size.height)
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(ChronoDayEnd, ChronoDayMid, ChronoDayStart, PureWhite),
              startX = 0f,
              endX = fillW,
            ),
            size = Size(fillW, size.height),
            cornerRadius = r,
          )
        }
      }

      // Chamber 2: Next Birthday Reverse Countdown (Signature Burnished Copper -> Warm Champagne Rose-Gold)
      Column(
        modifier = Modifier
          .weight(1f)
          .floating3DInnerTile(
            cornerRadiusDp = 12.dp,
            accentColor = ChronoBirthdayStart,
          )
          .padding(metrics.innerTilePadding)
          .testTag("dashboard_next_birthday_tile"),
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
                .width(3.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(
                  brush = Brush.verticalGradient(
                    colors = listOf(ChronoBirthdayStart, ChronoBirthdayMid),
                  ),
                ),
            )
            Text(
              text = "NEXT BIRTHDAY",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(9.2f, 7.8f, 10.6f),
              letterSpacing = 0.5.sp,
              maxLines = 1,
            )
          }

          Text(
            text = nextBirthdayDateLabel,
            color = ChronoBirthdayStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.8f, 7.5f, 10f),
            maxLines = 1,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = if (isBirthdayToday) {
            "TODAY • AGE $upcomingAge"
          } else {
            "${bdayDaysLeft}d ${String.format(Locale.US, "%02dh %02dm %02ds", bdayHoursLeft, bdayMinsLeft, bdaySecsLeft)}"
          },
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(14.5f, 11.8f, 16.5f),
          letterSpacing = (-0.4).sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("dashboard_next_birthday_value"),
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text = "Reverse • Turning $upcomingAge Yrs",
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10f, 8.6f, 11.4f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(7.dp))

        // Annual Birthday Countdown Progress Bar (Unique Deep Bronze -> Burnished Copper -> Warm Champagne)
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(5.5.dp),
        ) {
          val r = CornerRadius(size.height / 2f, size.height / 2f)
          drawRoundRect(
            color = RecessedTrackFill,
            size = size,
            cornerRadius = r,
          )
          val fillW = (size.width * bdayCycleProgress.coerceIn(0.05f, 1f)).coerceAtLeast(size.height)
          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(ChronoBirthdayEnd, ChronoBirthdayMid, ChronoBirthdayStart, PureWhite),
              startX = 0f,
              endX = fillW,
            ),
            size = Size(fillW, size.height),
            cornerRadius = r,
          )
        }
      }
    }
  }
}

// ============================================================================
// COMBINED DAILY STREAK HERO CARD (Redesigned Attractive 3x2 Pillar Bento Grid)
// ============================================================================

@Composable
private fun DashboardCombinedStreakHeroCard(
  streakDays: Int,
  streakThresholdPercent: Int,
  isInBufferGrace: Boolean = false,
  bufferDaysRemaining: Int = BioSyncStreakEngine.BUFFER_DAYS_ALLOWED,
  liveSteps: Int,
  targetSteps: Int,
  loggedCalories: Int,
  targetCalories: Int,
  loggedProtein: Int,
  targetProtein: Int,
  loggedCarbs: Int,
  targetCarbs: Int,
  loggedFats: Int,
  targetFats: Int,
  loggedWaterMl: Int,
  targetWaterMl: Int,
  stepsQualifies: Boolean,
  caloriesQualifies: Boolean,
  proteinQualifies: Boolean,
  carbsQualifies: Boolean,
  fatsQualifies: Boolean,
  waterQualifies: Boolean,
  ambientPhase: Float = 0f,
  borderFlashProgress: Float = 0f,
  metrics: DashboardResponsiveMetrics,
) {
  val qualifiedCount = listOf(
    stepsQualifies,
    caloriesQualifies,
    proteinQualifies,
    carbsQualifies,
    fatsQualifies,
    waterQualifies,
  ).count { it }
  val allSixMet = qualifiedCount == 6

  val stepsProgress = if (targetSteps > 0) (liveSteps.toFloat() / targetSteps).coerceIn(0f, 1f) else 0f
  val calProgress = if (targetCalories > 0) (loggedCalories.toFloat() / targetCalories).coerceIn(0f, 1f) else 0f
  val proProgress = if (targetProtein > 0) (loggedProtein.toFloat() / targetProtein).coerceIn(0f, 1f) else 0f
  val carbProgress = if (targetCarbs > 0) (loggedCarbs.toFloat() / targetCarbs).coerceIn(0f, 1f) else 0f
  val fatProgress = if (targetFats > 0) (loggedFats.toFloat() / targetFats).coerceIn(0f, 1f) else 0f
  val waterProgress = if (targetWaterMl > 0) (loggedWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f) else 0f

  val overallReadinessFraction = (
    (stepsProgress + calProgress + proProgress + carbProgress + fatProgress + waterProgress) / 6f
    ).coerceIn(0f, 1f)

  val animatedReadiness by animateFloatAsState(
    targetValue = overallReadinessFraction,
    animationSpec = tween(durationMillis = 520, easing = SmoothOutEasing),
    label = "streak_readiness_fill",
  )

  // Distinct Streak Ignition Palette (Burnished Solar Gold + Ember Coral + Pure White on Obsidian Black)
  val streakGold = Color(0xFFFFD166)
  val streakEmber = Color(0xFFFF7A45)
  val streakPlatinum = Color(0xFFE4E4E7)

  // Milestone Tier Progression (3D • 7D • 14D • 30D • 90D)
  val (tierName, nextMilestoneDays) = remember(streakDays) {
    when {
      streakDays >= 90 -> "APEX IMMORTAL" to 180
      streakDays >= 30 -> "TITAN PROTOCOL" to 90
      streakDays >= 14 -> "MOMENTUM CORE" to 30
      streakDays >= 7 -> "IGNITION SURGE" to 14
      streakDays >= 3 -> "SPARK ACTIVE" to 7
      streakDays >= 1 -> "EMBER ONLINE" to 3
      else -> "PRIMED FOR DAY 01" to 3
    }
  }
  val daysToNextMilestone = (nextMilestoneDays - streakDays).coerceAtLeast(1)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = if (allSixMet || streakDays > 0) streakGold else PureWhite,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_streak_card"),
  ) {
    // 1. Top Priority Header Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = streakGold,
          endColor = streakEmber,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Column {
          Text(
            text = "PRIORITY 01 • STREAK PROTOCOL ENGINE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.8.sp,
            letterSpacing = 0.9.sp,
          )
          Text(
            text = "$tierName • NEXT TIER IN ${daysToNextMilestone}D",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 8.4.sp,
            letterSpacing = 0.5.sp,
          )
        }
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(
            brush = Brush.horizontalGradient(
              colors = when {
                allSixMet -> listOf(streakGold, streakEmber)
                isInBufferGrace -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                else -> listOf(PureWhite, streakPlatinum)
              },
            ),
          )
          .padding(horizontal = 9.dp, vertical = 4.dp),
      ) {
        Text(
          text = when {
            allSixMet -> "+1 STREAK EXTENDED"
            isInBufferGrace -> "2D BUFFER • ${bufferDaysRemaining}D LEFT"
            streakDays == 0 -> "DAY 01 GATE ≥ $streakThresholdPercent%"
            else -> "GATE ≥ $streakThresholdPercent%"
          },
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.0.sp,
          letterSpacing = 0.6.sp,
        )
      }
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Sculpted 3D Streak Ignition Hero Deck (No redundant pillar progress bars!)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 14.dp,
          accentColor = streakGold,
          isHighlighted = allSixMet || streakDays > 0,
        )
        .padding(horizontal = 14.dp, vertical = 13.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left: 3D Ignition Emblem + Large Addictive Streak Day Counter
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0xFF0A0A0D))
              .border(
                width = 1.1.dp,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    streakGold.copy(alpha = 0.85f),
                    streakEmber.copy(alpha = 0.45f),
                    Zinc800,
                  ),
                ),
                shape = RoundedCornerShape(14.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Canvas(modifier = Modifier.size(34.dp)) {
              val pulse = 0.90f + 0.10f * sin(ambientPhase).toFloat()
              val cx = size.width / 2f
              val cy = size.height / 2f
              drawCircle(
                brush = Brush.radialGradient(
                  colors = listOf(
                    streakGold.copy(alpha = 0.28f * pulse),
                    streakEmber.copy(alpha = 0.10f * pulse),
                    Color.Transparent,
                  ),
                  center = Offset(cx, cy),
                  radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(cx, cy),
              )
              val outerFlame = Path().apply {
                moveTo(cx, size.height * 0.08f)
                cubicTo(
                  size.width * 0.88f, size.height * 0.34f,
                  size.width * 0.90f, size.height * 0.78f,
                  cx, size.height * 0.92f,
                )
                cubicTo(
                  size.width * 0.10f, size.height * 0.78f,
                  size.width * 0.12f, size.height * 0.34f,
                  cx, size.height * 0.08f,
                )
                close()
              }
              drawPath(
                path = outerFlame,
                brush = Brush.verticalGradient(
                  colors = listOf(PureWhite, streakGold, streakEmber),
                ),
              )
              val innerCore = Path().apply {
                moveTo(cx, size.height * 0.36f)
                cubicTo(
                  size.width * 0.70f, size.height * 0.54f,
                  size.width * 0.68f, size.height * 0.80f,
                  cx, size.height * 0.88f,
                )
                cubicTo(
                  size.width * 0.32f, size.height * 0.80f,
                  size.width * 0.30f, size.height * 0.54f,
                  cx, size.height * 0.36f,
                )
                close()
              }
              drawPath(
                path = innerCore,
                color = ObsidianBlack.copy(alpha = 0.72f),
              )
            }
          }

          Column {
            Text(
              text = when {
                isInBufferGrace -> "2-DAY BUFFER SHIELD ACTIVE"
                streakDays == 0 -> "DAY 01 STREAK PRIMED"
                else -> "ACTIVE STREAK CHAIN"
              },
              color = if (isInBufferGrace) streakGold else Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 9.4.sp,
              letterSpacing = 0.9.sp,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$streakDays",
                color = if (streakDays > 0) PureWhite else Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.testTag("dashboard_combined_streak_value"),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (streakDays == 1) "DAY STREAK" else "DAYS STREAK",
                color = if (streakDays > 0) streakGold else Zinc500,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(bottom = 4.dp),
              )
            }
          }
        }

        // Right: Daily Readiness Score + Pillars Qualified Count
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "TODAY'S CHARGE",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.8.sp,
            letterSpacing = 0.7.sp,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${(overallReadinessFraction * 100).roundToInt()}%",
            color = if (allSixMet) streakGold else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$qualifiedCount / 6 PILLARS LOCKED",
            color = if (allSixMet) streakGold else Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.8.sp,
            letterSpacing = 0.4.sp,
          )
        }
      }

      // Unified Master Readiness Gauge Bar
      DashboardSpectrumGaugeBar(
        fillFraction = animatedReadiness,
        thresholdFraction = streakThresholdPercent / 100f,
        startColor = streakEmber,
        midColor = streakGold,
        endColor = PureWhite,
        barHeight = 7.dp,
      )
    }
  }
}

// ============================================================================
// DAILY STEPS HERO CARD (With Custom Steps Input Button — Excluded from Timeline)
// ============================================================================

@Composable
private fun DashboardPrimaryStepsHeroCard(
  liveSteps: Int,
  manualStepsToday: Int = 0,
  targetSteps: Int,
  liveDistanceKm: Float,
  isActivelyWalking: Boolean,
  streakThresholdPercent: Int,
  stepsQualifies: Boolean,
  borderFlashProgress: Float = 0f,
  onForceSyncSteps: () -> Unit,
  onOpenCustomStepsModal: () -> Unit = {},
  numberFormat: NumberFormat,
  metrics: DashboardResponsiveMetrics,
) {
  val safeTarget = targetSteps.coerceAtLeast(500)
  val rawFraction = liveSteps.toFloat() / safeTarget.toFloat()
  val barFraction = rawFraction.coerceIn(0f, 1f)
  val pctAchieved = (rawFraction * 100f).roundToInt()
  val remainingSteps = (safeTarget - liveSteps).coerceAtLeast(0)

  val animatedSteps by animateFloatAsState(
    targetValue = liveSteps.toFloat(),
    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
    label = "live_steps_counter",
  )
  val animatedBarFill by animateFloatAsState(
    targetValue = barFraction,
    animationSpec = tween(durationMillis = 480, easing = SmoothOutEasing),
    label = "steps_bar_fill",
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = StepsStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_steps_priority_card"),
  ) {
    // 1. Header Row: Clean tap-to-refresh live telemetry label on the right (NO boxy [ SYNC ] button!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = StepsStart,
          endColor = StepsMid,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "PRIORITY 01 • DAILY STEPS",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .clickable(role = Role.Button, onClick = onForceSyncSteps)
          .padding(horizontal = 4.dp, vertical = 2.dp)
          .testTag("dashboard_steps_sync_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(StepsStart),
        )
        Text(
          text = if (isActivelyWalking) "WALKING • $pctAchieved%" else "LIVE SYNC • $pctAchieved% ↻",
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp,
          letterSpacing = 0.4.sp,
        )
      }
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Floating Inner Steps + Borderless Dual-Telemetry Split Readout (NO dark box around Distance!)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = StepsStart,
          isHighlighted = stepsQualifies,
        )
        .padding(13.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left: Primary Steps Readout
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(StepsStart, StepsMid))),
            )
            Text(
              text = "LIVE PEDOMETER & FIT",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = numberFormat.format(animatedSteps.roundToInt()),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 32.sp,
              lineHeight = 32.sp,
              letterSpacing = (-1.1).sp,
              modifier = Modifier.testTag("dashboard_live_steps_value"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "/ ${numberFormat.format(safeTarget)}",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp,
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
        }

        // Subtle Vertical Telemetry Divider
        Box(
          modifier = Modifier
            .padding(horizontal = 12.dp)
            .width(1.dp)
            .height(38.dp)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, Zinc700, Color.Transparent),
              ),
            ),
        )

        // Right: Clean Borderless Distance Telemetry Readout
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "DISTANCE",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.2.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = String.format(Locale.US, "%.2f", liveDistanceKm.coerceAtLeast(0f)),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 21.sp,
              lineHeight = 21.sp,
              letterSpacing = (-0.5).sp,
              modifier = Modifier.testTag("dashboard_live_distance_value"),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "km",
              color = StepsStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(11.dp))

      DashboardSpectrumGaugeBar(
        fillFraction = animatedBarFill,
        thresholdFraction = streakThresholdPercent / 100f,
        startColor = StepsStart,
        midColor = StepsMid,
        endColor = StepsEnd,
        barHeight = 8.5.dp,
      )

      Spacer(modifier = Modifier.height(7.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (remainingSteps > 0) {
            "${numberFormat.format(remainingSteps)} steps remaining to daily goal"
          } else {
            "Daily step target completed (+${numberFormat.format(liveSteps - safeTarget)})"
          },
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = 11.sp,
        )
        Text(
          text = "Streak Gate ≥$streakThresholdPercent%",
          color = if (stepsQualifies) StepsStart else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.5.sp,
        )
      }

      Spacer(modifier = Modifier.height(9.dp))

      // Custom Steps Input Action Row (Updates daily steps & stats without cluttering Today's Timeline)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (manualStepsToday > 0) {
              "Custom logged today: +${numberFormat.format(manualStepsToday)} steps (hidden from timeline)"
            } else {
              "Log treadmill or uncounted steps directly (excluded from timeline)"
            },
            color = Zinc400,
            fontFamily = InterFontFamily,
            fontSize = 10.2.sp,
            lineHeight = 13.5.sp,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PureWhite)
            .clickable(role = Role.Button, onClick = onOpenCustomStepsModal)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("dashboard_custom_steps_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "+ CUSTOM STEPS",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.2.sp,
            letterSpacing = 0.5.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "* Note: Step counter is not fully accurate yet; we will resolve this in future updates.",
        color = Zinc400.copy(alpha = 0.88f),
        fontFamily = InterFontFamily,
        fontSize = 9.8.sp,
        lineHeight = 13.sp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_steps_accuracy_disclaimer"),
      )
    }
  }
}

@Composable
private fun DashboardCustomStepsInputModal(
  currentManualSteps: Int,
  currentTotalSteps: Int,
  targetSteps: Int,
  numberFormat: NumberFormat,
  onAddCustomSteps: (Int) -> Unit,
  onSetExactCustomSteps: (Int) -> Unit,
  onResetCustomSteps: () -> Unit,
  onDismiss: () -> Unit,
) {
  var customStepsText by remember { mutableStateOf("") }
  val parsedSteps = customStepsText.filter { it.isDigit() }.toIntOrNull() ?: 0

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 420.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianBlack)
        .border(1.2.dp, PureWhite.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
        .padding(18.dp)
        .testTag("dashboard_custom_steps_modal"),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DIRECT STEP CALIBRATION",
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 9.5.sp,
          letterSpacing = 1.0.sp,
        )
        Text(
          text = "EXCLUDED FROM TIMELINE",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 8.5.sp,
        )
      }

      Text(
        text = "Custom Step Input",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
      )
      Text(
        text = "Add or set custom steps directly into today's step counter and analytics database without creating timeline log entries.",
        color = Zinc400,
        fontFamily = InterFontFamily,
        fontSize = 11.8.sp,
        lineHeight = 16.sp,
      )

      // Quick Add Presets
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        listOf(500, 1000, 2500, 5000).forEach { preset ->
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(9.dp))
              .background(Color(0xFF141419))
              .border(0.9.dp, Zinc700, RoundedCornerShape(9.dp))
              .clickable { onAddCustomSteps(preset) }
              .padding(vertical = 9.dp)
              .testTag("custom_steps_preset_$preset"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "+${numberFormat.format(preset)}",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.5.sp,
            )
          }
        }
      }

      // Custom numeric input field
      BasicTextField(
        value = customStepsText,
        onValueChange = { raw ->
          customStepsText = raw.filter { it.isDigit() }.take(6)
        },
        singleLine = true,
        textStyle = TextStyle(
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
        ),
        cursorBrush = SolidColor(PureWhite),
        decorationBox = { innerTextField ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF111116))
              .border(1.dp, Zinc700, RoundedCornerShape(10.dp))
              .padding(horizontal = 13.dp, vertical = 12.dp)
              .testTag("custom_steps_input_field"),
          ) {
            if (customStepsText.isEmpty()) {
              Text(
                text = "Enter custom step count (e.g. 1500)",
                color = Zinc500,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
              )
            }
            innerTextField()
          }
        },
      )

      if (currentManualSteps > 0) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Current custom steps: +${numberFormat.format(currentManualSteps)}",
            color = Zinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.5.sp,
          )
          Text(
            text = "RESET CUSTOM STEPS",
            color = Color(0xFFEF4444),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onResetCustomSteps)
              .padding(horizontal = 6.dp, vertical = 3.dp)
              .testTag("custom_steps_reset_button"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16161C))
            .border(1.dp, Zinc700, RoundedCornerShape(10.dp))
            .clickable(onClick = onDismiss)
            .padding(vertical = 11.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "CANCEL",
            color = Zinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
          )
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (parsedSteps > 0) PureWhite else Zinc800)
            .clickable(enabled = parsedSteps > 0) {
              onAddCustomSteps(parsedSteps)
            }
            .padding(vertical = 11.dp)
            .testTag("custom_steps_add_confirm_button"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "ADD STEPS",
            color = if (parsedSteps > 0) ObsidianBlack else Zinc500,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
          )
        }
      }
    }
  }
}

// ============================================================================
// METABOLIC ENERGY (CALORIES) & 3-COLUMN MACROS CARD (No clunky STATUS DONE box!)
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
  caloriesQualifies: Boolean,
  proteinQualifies: Boolean,
  carbsQualifies: Boolean,
  fatsQualifies: Boolean,
  borderFlashProgress: Float = 0f,
  numberFormat: NumberFormat,
  metrics: DashboardResponsiveMetrics,
) {
  val safeCalTarget = targetCalories.coerceAtLeast(500)
  val calRawFraction = loggedCalories.toFloat() / safeCalTarget.toFloat()
  val calBarFraction = calRawFraction.coerceIn(0f, 1f)
  val calPct = (calRawFraction * 100f).roundToInt()
  val calDelta = safeCalTarget - loggedCalories

  val animatedCalBar by animateFloatAsState(
    targetValue = calBarFraction,
    animationSpec = tween(durationMillis = 480, easing = SmoothOutEasing),
    label = "calories_bar_fill",
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = CaloriesStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_calories_macros_card"),
  ) {
    // 1. Clean Ecosystem Header Row
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
          endColor = CaloriesEnd,
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

    // 2. Floating Primary Daily Energy Sub-Card (Borderless Balance Readout on right instead of STATUS DONE box)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = CaloriesStart,
          isHighlighted = caloriesQualifies,
        )
        .padding(13.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(CaloriesStart, CaloriesMid))),
            )
            Text(
              text = "DAILY ENERGY • CALORIES",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = numberFormat.format(loggedCalories),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 29.sp,
              lineHeight = 29.sp,
              letterSpacing = (-1.0).sp,
              modifier = Modifier.testTag("dashboard_logged_calories_value"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "/ ${numberFormat.format(safeCalTarget)} kcal",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.5.sp,
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
        }

        // Subtle Vertical Divider
        Box(
          modifier = Modifier
            .padding(horizontal = 12.dp)
            .width(1.dp)
            .height(38.dp)
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, Zinc700, Color.Transparent),
              ),
            ),
        )

        // Right: Clean Borderless Balance Readout (Replaces the clunky [ STATUS / DONE ] box)
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = if (calDelta >= 0) "REMAINING" else "SURPLUS",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.2.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = if (calDelta >= 0) {
                numberFormat.format(calDelta)
              } else {
                "+${numberFormat.format(abs(calDelta))}"
              },
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              lineHeight = 19.sp,
              letterSpacing = (-0.5).sp,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "kcal",
              color = CaloriesStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.5.sp,
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      DashboardSpectrumGaugeBar(
        fillFraction = animatedCalBar,
        thresholdFraction = streakThresholdPercent / 100f,
        startColor = CaloriesEnd,
        midColor = CaloriesStart,
        endColor = CarbsStart,
        barHeight = 8.dp,
      )

      Spacer(modifier = Modifier.height(7.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Daily Target ${numberFormat.format(safeCalTarget)} kcal",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 10.sp,
        )
        Text(
          text = "Streak Gate ≥$streakThresholdPercent%",
          color = if (caloriesQualifies) CaloriesStart else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.5.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. 3-Column Floating Macro Bento Grid: Protein, Carbs, Fats
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(metrics.bentoGap),
    ) {
      DashboardMacroBentoTile(
        label = "Protein",
        loggedGrams = loggedProtein,
        targetGrams = targetProtein,
        streakThresholdPercent = streakThresholdPercent,
        qualifies = proteinQualifies,
        startColor = ProteinStart,
        midColor = ProteinMid,
        endColor = ProteinEnd,
        metrics = metrics,
        testTag = "dashboard_macro_protein_tile",
        modifier = Modifier.weight(1f),
      )

      DashboardMacroBentoTile(
        label = "Carbs",
        loggedGrams = loggedCarbs,
        targetGrams = targetCarbs,
        streakThresholdPercent = streakThresholdPercent,
        qualifies = carbsQualifies,
        startColor = CarbsStart,
        midColor = CarbsMid,
        endColor = CarbsEnd,
        metrics = metrics,
        testTag = "dashboard_macro_carbs_tile",
        modifier = Modifier.weight(1f),
      )

      DashboardMacroBentoTile(
        label = "Fats",
        loggedGrams = loggedFats,
        targetGrams = targetFats,
        streakThresholdPercent = streakThresholdPercent,
        qualifies = fatsQualifies,
        startColor = FatsStart,
        midColor = FatsMid,
        endColor = FatsEnd,
        metrics = metrics,
        testTag = "dashboard_macro_fats_tile",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun DashboardMacroBentoTile(
  label: String,
  loggedGrams: Int,
  targetGrams: Int,
  streakThresholdPercent: Int,
  qualifies: Boolean,
  startColor: Color,
  midColor: Color,
  endColor: Color,
  metrics: DashboardResponsiveMetrics,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val safeTarget = targetGrams.coerceAtLeast(1)
  val rawFraction = loggedGrams.toFloat() / safeTarget.toFloat()
  val barFraction = rawFraction.coerceIn(0f, 1f)
  val pct = (rawFraction * 100f).roundToInt()
  val remGrams = (safeTarget - loggedGrams).coerceAtLeast(0)

  val animatedFill by animateFloatAsState(
    targetValue = barFraction,
    animationSpec = tween(durationMillis = 460, easing = SmoothOutEasing),
    label = "${label}_macro_fill",
  )

  Column(
    modifier = modifier
      .floating3DInnerTile(
        cornerRadiusDp = 12.dp,
        accentColor = startColor,
        isHighlighted = qualifies,
      )
      .padding(metrics.innerTilePadding)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
        modifier = Modifier.weight(1f, fill = false),
      ) {
        Box(
          modifier = Modifier
            .width(3.5.dp)
            .height(11.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.verticalGradient(listOf(startColor, midColor))),
        )
        Text(
          text = label,
          color = Zinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = metrics.spScaled(11.5f, 9.8f, 13f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = "${pct.coerceAtMost(999)}%",
        color = startColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(9.8f, 8.2f, 11.2f),
        maxLines = 1,
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "$loggedGrams",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(18f, 14.5f, 20.5f),
        lineHeight = metrics.spScaled(18f, 14.5f, 20.5f),
        maxLines = 1,
      )
      Text(
        text = " /${safeTarget}g",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = metrics.spScaled(10f, 8.4f, 11.4f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 1.dp),
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    DashboardSpectrumGaugeBar(
      fillFraction = animatedFill,
      thresholdFraction = streakThresholdPercent / 100f,
      startColor = startColor,
      midColor = midColor,
      endColor = endColor,
      barHeight = 5.5.dp,
    )

    Spacer(modifier = Modifier.height(5.dp))

    Text(
      text = if (remGrams > 0) "${remGrams}g left" else "Goal ≥$streakThresholdPercent% met",
      color = if (qualifies) startColor else Zinc400,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 9.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ============================================================================
// SHARED LUMINOUS SPECTRUM GAUGE BAR & HEADER DOT
// ============================================================================

@Composable
internal fun DashboardSyncedHeaderDot(
  startColor: Color,
  endColor: Color,
) {
  Spacer(
    modifier = Modifier
      .size(8.dp)
      .drawWithCache {
        val radius = size.minDimension / 2f
        val glowPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              110,
              (startColor.red * 255).roundToInt(),
              (startColor.green * 255).roundToInt(),
              (startColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(
              safeDashboardBlur(radius * 1.3f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }
        val dotBrush = Brush.linearGradient(colors = listOf(startColor, endColor))

        onDrawBehind {
          drawIntoCanvas { canvas ->
            canvas.drawCircle(center, radius * 1.3f, glowPaint)
          }
          drawCircle(
            brush = dotBrush,
            radius = radius,
            center = center,
          )
        }
      },
  )
}

@Composable
internal fun DashboardSpectrumGaugeBar(
  fillFraction: Float,
  thresholdFraction: Float,
  startColor: Color,
  midColor: Color,
  endColor: Color,
  barHeight: Dp = 7.dp,
  modifier: Modifier = Modifier,
) {
  val clampedFill = fillFraction.coerceIn(0f, 1f)
  val clampedThreshold = thresholdFraction.coerceIn(0.5f, 1f)

  Spacer(
    modifier = modifier
      .fillMaxWidth()
      .height(barHeight)
      .drawWithCache {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(h / 2f, h / 2f)

        val auraPaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              95,
              (startColor.red * 255).roundToInt(),
              (startColor.green * 255).roundToInt(),
              (startColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(
              safeDashboardBlur(h * 1.1f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }

        val reticlePaint = Paint().apply {
          isAntiAlias = true
          asFrameworkPaint().apply {
            color = android.graphics.Color.argb(
              165,
              (startColor.red * 255).roundToInt(),
              (startColor.green * 255).roundToInt(),
              (startColor.blue * 255).roundToInt(),
            )
            maskFilter = BlurMaskFilter(
              safeDashboardBlur(h * 0.9f),
              BlurMaskFilter.Blur.NORMAL,
            )
          }
        }

        onDrawBehind {
          if (w <= 2f || h <= 1f) return@onDrawBehind

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
            style = Stroke(width = 0.9.dp.toPx()),
          )

          for (tick in 1..3) {
            val tickX = w * (tick * 0.25f)
            drawLine(
              color = PureWhite.copy(alpha = 0.06f),
              start = Offset(tickX, h * 0.2f),
              end = Offset(tickX, h * 0.8f),
              strokeWidth = 1.dp.toPx(),
            )
          }

          val visualFill = max(clampedFill, 0.028f)
          val fillW = (w * visualFill).coerceIn(h, w)

          drawIntoCanvas { canvas ->
            canvas.drawRoundRect(0f, 0f, fillW, h, h / 2f, h / 2f, auraPaint)
          }

          drawRoundRect(
            brush = Brush.horizontalGradient(
              colors = listOf(startColor, midColor, endColor),
              startX = 0f,
              endX = fillW.coerceAtLeast(h * 2f),
            ),
            topLeft = Offset.Zero,
            size = Size(fillW, h),
            cornerRadius = corner,
          )

          if (clampedThreshold < 0.995f) {
            val gateX = (w * clampedThreshold).coerceIn(4f, w - 4f)
            drawLine(
              color = PureWhite.copy(alpha = 0.72f),
              start = Offset(gateX, 0f),
              end = Offset(gateX, h),
              strokeWidth = 1.3.dp.toPx(),
              cap = StrokeCap.Round,
            )
          }

          val tipX = (fillW - h * 0.45f).coerceIn(h * 0.5f, w - h * 0.5f)
          val tipCenter = Offset(tipX, h / 2f)
          drawIntoCanvas { canvas ->
            canvas.drawCircle(tipCenter, h * 0.65f, reticlePaint)
          }
          drawCircle(
            color = PureWhite,
            radius = (h * 0.26f).coerceAtLeast(1.8f),
            center = tipCenter,
          )
        }
      },
  )
}
