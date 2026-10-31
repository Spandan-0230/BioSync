package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
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
  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val scrollState = rememberScrollState()
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  var dashboardData by remember { mutableStateOf(DashboardLocalStore.load(context)) }
  var pendingDeletedItem by remember {
    mutableStateOf<PendingDeletedTimelineItem?>(null)
  }

  fun persistDashboardData(updated: DashboardStoredData) {
    dashboardData = updated
    DashboardLocalStore.save(context, updated)
  }

  val isActiveWorkoutDay = remember(activityLevel, selectedWorkoutDays) {
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
  val targetWaterMl = targetHydrationMl.coerceAtLeast(500)
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
  val liveSteps = stepSyncState.liveTodaySteps.coerceAtLeast(0)

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
    if (allSixMet && dashboardData.lastCompletedStreakDayKey != todayKey) {
      haptics.confirm()
      persistDashboardData(
        dashboardData.copy(
          dayKey = todayKey,
          baseStreakDays = dashboardData.baseStreakDays + 1,
          lastCompletedStreakDayKey = todayKey,
        ),
      )
    } else if (!allSixMet && dashboardData.lastCompletedStreakDayKey == todayKey) {
      persistDashboardData(
        dashboardData.copy(
          dayKey = todayKey,
          baseStreakDays = (dashboardData.baseStreakDays - 1).coerceAtLeast(0),
          lastCompletedStreakDayKey = "",
        ),
      )
    }
  }

  LaunchedEffect(Unit) {
    while (true) {
      delay(1_000L)
      val tickMs = System.currentTimeMillis()
      val newDayKey = DashboardTimeFormatter.currentDayKey(tickMs)
      if (newDayKey != dashboardData.dayKey) {
        dashboardData = DashboardLocalStore.load(context)
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
    for (cardIdx in 0..5) {
      activeFlashCardIndex = cardIdx
      val durationMs = 260L
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

  BackHandler {
    haptics.selection()
    onOpenProtocolSummary()
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

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .testTag("dashboard_screen_root"),
  ) {
    // Low-reflection Obsidian Backdrop Canvas
    DashboardAmbientBackdropCanvas(
      ambientPhase = ambientPulse,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(
          top = statusBarTopPadding + 14.dp,
          bottom = navBarBottomPadding + 36.dp,
          start = 20.dp,
          end = 20.dp,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 540.dp)
          .fillMaxWidth(),
      ) {
        // 1. TOP COMMAND HEADER
        DashboardTopNavigationHeader(
          subjectName = subjectName,
          isActiveWorkoutDay = isActiveWorkoutDay,
          isCustomTargetsMode = false,
          streakThresholdPercent = streakThresholdPercent,
          onTriggerShimmer = {
            haptics.tick()
            borderFlashTriggerCount += 1
          },
          onOpenProtocolSettings = {
            haptics.selection()
            onOpenProtocolSummary()
          },
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 2. FLOATING COMBINED DAILY STREAK CARD (Attractive 3x2 Pillar Bento Grid)
        DashboardCombinedStreakHeroCard(
          streakDays = dashboardData.baseStreakDays,
          streakThresholdPercent = streakThresholdPercent,
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
          borderFlashProgress = if (activeFlashCardIndex == 0) cardFlashProgress else 0f,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. PRIORITY #1: FLOATING DAILY STEPS COMMAND CARD (Dual-Telemetry Layout without clunky boxes)
        DashboardPrimaryStepsHeroCard(
          liveSteps = liveSteps,
          targetSteps = targetSteps,
          liveDistanceKm = stepSyncState.liveDistanceKm,
          isActivelyWalking = stepSyncState.isActivelyWalking,
          streakThresholdPercent = streakThresholdPercent,
          stepsQualifies = stepsQualifies,
          borderFlashProgress = if (activeFlashCardIndex == 1) cardFlashProgress else 0f,
          onForceSyncSteps = {
            haptics.selection()
            stepSyncController.forceRefreshNow()
          },
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(16.dp))

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
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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
            )
            persistDashboardData(
              dashboardData.copy(
                allCreatedSingleItems = updatedSingles,
                savedQuickItems = updatedSaved,
                todayTimelineEntries = listOf(newEntry) + dashboardData.todayTimelineEntries,
              ),
            )
          },
          onQuickTapSavedItem = { savedItem ->
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
            )
            persistDashboardData(
              dashboardData.copy(
                todayTimelineEntries = listOf(newEntry) + dashboardData.todayTimelineEntries,
              ),
            )
          },
          onDeleteSavedQuickItem = { itemId ->
            persistDashboardData(
              dashboardData.copy(
                savedQuickItems = dashboardData.savedQuickItems.filterNot { it.id == itemId },
              ),
            )
          },
          haptics = haptics,
          numberFormat = numberFormat,
        )

        Spacer(modifier = Modifier.height(16.dp))

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
// TOP NAVIGATION HEADER
// ============================================================================

@Composable
private fun DashboardTopNavigationHeader(
  subjectName: String,
  isActiveWorkoutDay: Boolean,
  isCustomTargetsMode: Boolean,
  streakThresholdPercent: Int,
  onTriggerShimmer: () -> Unit,
  onOpenProtocolSettings: () -> Unit,
) {
  val todayFormatted = remember {
    val cal = Calendar.getInstance()
    val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US)?.uppercase(Locale.US) ?: "TODAY"
    val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US)?.uppercase(Locale.US) ?: ""
    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
    "$dayName, $monthName $dayNum"
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onTriggerShimmer,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Zinc900)
            .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
            .clickable(
              role = Role.Button,
              onClick = onOpenProtocolSettings,
            )
            .testTag("dashboard_recalibrate_button"),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.6.dp.toPx()
            drawLine(
              color = PureWhite,
              start = Offset(w * 0.12f, h * 0.28f),
              end = Offset(w * 0.88f, h * 0.28f),
              strokeWidth = stroke,
              cap = StrokeCap.Round,
            )
            drawCircle(
              color = StepsStart,
              radius = 2.4.dp.toPx(),
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
              radius = 2.4.dp.toPx(),
              center = Offset(w * 0.68f, h * 0.72f),
            )
          }
        }

        Column {
          Text(
            text = "ACTIVE PROTOCOL • LIVE",
            color = Zinc500,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Daily Protocol",
            color = PureWhite,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = (-0.8).sp,
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = todayFormatted,
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          letterSpacing = 0.6.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = if (isCustomTargetsMode) {
            "CUSTOM OVERRIDE"
          } else if (isActiveWorkoutDay) {
            "ACTIVE TRAINING DAY"
          } else {
            "REST & RECOVERY DAY"
          },
          color = if (isActiveWorkoutDay) StepsStart else HydrationStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 9.5.sp,
          letterSpacing = 0.5.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "${subjectName.ifBlank { "Athlete" }} • Daily streak unlocks at ≥$streakThresholdPercent% across all 6 pillars.",
      color = Zinc400,
      fontFamily = InterFontFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.2.sp,
    )
  }
}

// ============================================================================
// COMBINED DAILY STREAK HERO CARD (Redesigned Attractive 3x2 Pillar Bento Grid)
// ============================================================================

@Composable
private fun DashboardCombinedStreakHeroCard(
  streakDays: Int,
  streakThresholdPercent: Int,
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
  borderFlashProgress: Float = 0f,
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

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = if (allSixMet) StepsStart else PureWhite,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_streak_card"),
  ) {
    // 1. Header Row (Single solid status badge on the screen, matching Step 02 OPTIMAL RANGE)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = if (allSixMet) StepsStart else PureWhite,
          endColor = if (allSixMet) StepsMid else Zinc300,
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

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (allSixMet) StepsStart else HydrationStart)
          .padding(horizontal = 9.dp, vertical = 4.dp),
      ) {
        Text(
          text = if (allSixMet) "STREAK UNLOCKED" else "OPTIMAL GATE ≥ $streakThresholdPercent%",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.2.sp,
          letterSpacing = 0.6.sp,
        )
      }
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Floating Inner Streak Readout Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = if (allSixMet) StepsStart else PureWhite,
          isHighlighted = allSixMet,
        )
        .padding(horizontal = 13.dp, vertical = 11.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
                .background(if (allSixMet) StepsStart else PureWhite),
            )
            Text(
              text = "COMBINED DAILY STREAK",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp,
              letterSpacing = 0.8.sp,
            )
          }
          Spacer(modifier = Modifier.height(5.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "$streakDays",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 30.sp,
              lineHeight = 30.sp,
              letterSpacing = (-1.0).sp,
              modifier = Modifier.testTag("dashboard_combined_streak_value"),
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

        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "READINESS ",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 9.5.sp,
              modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
              text = "${(overallReadinessFraction * 100).roundToInt()}%",
              color = if (allSixMet) StepsStart else PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$qualifiedCount of 6 pillars ≥$streakThresholdPercent%",
            color = if (allSixMet) StepsStart else Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.8.sp,
          )
        }
      }

      Spacer(modifier = Modifier.height(9.dp))

      DashboardSpectrumGaugeBar(
        fillFraction = animatedReadiness,
        thresholdFraction = streakThresholdPercent / 100f,
        startColor = if (allSixMet) StepsStart else Zinc300,
        midColor = if (allSixMet) StepsMid else PureWhite,
        endColor = PureWhite,
        barHeight = 6.5.dp,
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. REDESIGNED 3x2 PILLAR BENTO GRID (Spacious, attractive, zero congestion, not over-elaborated)
    val stepsPct = if (targetSteps > 0) ((liveSteps.toFloat() / targetSteps) * 100f).roundToInt() else 0
    val calPct = if (targetCalories > 0) ((loggedCalories.toFloat() / targetCalories) * 100f).roundToInt() else 0
    val proPct = if (targetProtein > 0) ((loggedProtein.toFloat() / targetProtein) * 100f).roundToInt() else 0
    val carbPct = if (targetCarbs > 0) ((loggedCarbs.toFloat() / targetCarbs) * 100f).roundToInt() else 0
    val fatPct = if (targetFats > 0) ((loggedFats.toFloat() / targetFats) * 100f).roundToInt() else 0
    val waterPct = if (targetWaterMl > 0) ((loggedWaterMl.toFloat() / targetWaterMl) * 100f).roundToInt() else 0

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Row 1: STEPS • KCAL • PROTEIN
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StreakPillarCompactCard(
          label = "STEPS",
          pct = stepsPct,
          thresholdPct = streakThresholdPercent,
          qualifies = stepsQualifies,
          accentColor = StepsStart,
          accentMid = StepsMid,
          modifier = Modifier.weight(1f),
        )
        StreakPillarCompactCard(
          label = "CALORIES",
          pct = calPct,
          thresholdPct = streakThresholdPercent,
          qualifies = caloriesQualifies,
          accentColor = CaloriesStart,
          accentMid = CaloriesMid,
          modifier = Modifier.weight(1f),
        )
        StreakPillarCompactCard(
          label = "PROTEIN",
          pct = proPct,
          thresholdPct = streakThresholdPercent,
          qualifies = proteinQualifies,
          accentColor = ProteinStart,
          accentMid = ProteinMid,
          modifier = Modifier.weight(1f),
        )
      }

      // Row 2: CARBS • FATS • H₂O
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StreakPillarCompactCard(
          label = "CARBS",
          pct = carbPct,
          thresholdPct = streakThresholdPercent,
          qualifies = carbsQualifies,
          accentColor = CarbsStart,
          accentMid = CarbsMid,
          modifier = Modifier.weight(1f),
        )
        StreakPillarCompactCard(
          label = "FATS",
          pct = fatPct,
          thresholdPct = streakThresholdPercent,
          qualifies = fatsQualifies,
          accentColor = FatsStart,
          accentMid = FatsMid,
          modifier = Modifier.weight(1f),
        )
        StreakPillarCompactCard(
          label = "H₂O",
          pct = waterPct,
          thresholdPct = streakThresholdPercent,
          qualifies = waterQualifies,
          accentColor = HydrationStart,
          accentMid = HydrationMid,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

/**
 * Sleek, spacious, non-congested 3x2 Bento Pillar Card for the Streak Protocol Engine:
 * - Row 1: Colored dot + Pillar Label (`STEPS`, `CALORIES`, etc.) on the left, and live `%` + `✓` on the right
 * - Row 2: Luminous micro-spectrum gauge bar with the `90%` threshold gate marker
 */
@Composable
private fun StreakPillarCompactCard(
  label: String,
  pct: Int,
  thresholdPct: Int,
  qualifies: Boolean,
  accentColor: Color,
  accentMid: Color,
  modifier: Modifier = Modifier,
) {
  val barFill = (pct / 100f).coerceIn(0f, 1f)
  val animatedFill by animateFloatAsState(
    targetValue = barFill,
    animationSpec = tween(durationMillis = 420, easing = SmoothOutEasing),
    label = "pillar_${label}_fill",
  )

  Column(
    modifier = modifier
      .floating3DInnerTile(
        cornerRadiusDp = 10.dp,
        accentColor = accentColor,
        isHighlighted = qualifies,
      )
      .padding(horizontal = 10.dp, vertical = 9.dp),
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
            .size(6.dp)
            .clip(CircleShape)
            .background(accentColor),
        )
        Text(
          text = label,
          color = if (qualifies) PureWhite else Zinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 9.5.sp,
          letterSpacing = 0.4.sp,
          maxLines = 1,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = "${pct.coerceAtMost(999)}%",
          color = if (qualifies) accentColor else PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.5.sp,
        )
        if (qualifies) {
          Text(
            text = "✓",
            color = accentColor,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.5.sp,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(7.dp))

    DashboardSpectrumGaugeBar(
      fillFraction = animatedFill,
      thresholdFraction = thresholdPct / 100f,
      startColor = accentColor,
      midColor = accentMid,
      endColor = PureWhite,
      barHeight = 4.5.dp,
    )
  }
}

// ============================================================================
// PRIORITY #1: DAILY STEPS HERO CARD (No clunky SYNC or DISTANCE box!)
// ============================================================================

@Composable
private fun DashboardPrimaryStepsHeroCard(
  liveSteps: Int,
  targetSteps: Int,
  liveDistanceKm: Float,
  isActivelyWalking: Boolean,
  streakThresholdPercent: Int,
  stepsQualifies: Boolean,
  borderFlashProgress: Float = 0f,
  onForceSyncSteps: () -> Unit,
  numberFormat: NumberFormat,
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
      horizontalArrangement = Arrangement.spacedBy(8.dp),
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
      .padding(11.dp)
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
            .background(Brush.verticalGradient(listOf(startColor, midColor))),
        )
        Text(
          text = label,
          color = Zinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.5.sp,
          maxLines = 1,
        )
      }

      Text(
        text = "${pct.coerceAtMost(999)}%",
        color = startColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.8.sp,
      )
    }

    Spacer(modifier = Modifier.height(7.dp))

    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = "$loggedGrams",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 18.sp,
      )
      Text(
        text = " /${safeTarget}g",
        color = Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 10.sp,
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
