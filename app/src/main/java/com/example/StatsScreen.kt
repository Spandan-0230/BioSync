package com.example

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaloriesEnd
import com.example.ui.theme.CaloriesMid
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsEnd
import com.example.ui.theme.CarbsMid
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.ChronoBirthdayStart
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val StatsZinc200 = Color(0xFFE4E4E7)
private val StatsZinc300 = Color(0xFFD4D4D8)
private val StatsZinc400 = Color(0xFFA1A1AA)
private val StatsZinc500 = Color(0xFF71717A)
private val StatsZinc600 = Color(0xFF52525B)
private val StatsZinc700 = Color(0xFF3F3F46)
private val StatsZinc800 = Color(0xFF27272A)
private val StatsZinc850 = Color(0xFF1E1E22)
private val StatsZinc900 = Color(0xFF18181B)
private val StatsZinc950 = Color(0xFF0A0A0E)
private val StatsDangerRed = Color(0xFFEF4444)
private val StatsDangerRedBorder = Color(0xFFF87171)

private val StatsEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private val StatsMonthShortNames = listOf(
  "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
  "JUL", "AUG", "SEP", "OCT", "NOV", "DEC",
)
private val StatsWeekdayShortNames = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

enum class BioSyncFloatingNavDestination(
  val label: String,
) {
  HOME("HOME"),
  STATS("STATS"),
  SETTINGS("SETTINGS"),
}

enum class StatsCategoryOption(
  val shortLabel: String,
  val fullTitle: String,
  val unitLabel: String,
  val startColor: Color,
  val midColor: Color,
  val endColor: Color,
) {
  ALL("OVERVIEW", "6-Pillar Composite Score", "%", StepsStart, StepsMid, StepsEnd),
  CALORIES("CALORIES", "Metabolic Energy", "kcal", CaloriesStart, CaloriesMid, CaloriesEnd),
  PROTEIN("PROTEIN", "Anabolic Protein", "g", ProteinStart, ProteinMid, ProteinEnd),
  CARBS("CARBS", "Glycogen Carbs", "g", CarbsStart, CarbsMid, CarbsEnd),
  FATS("FATS", "Endocrine Lipids", "g", FatsStart, FatsMid, FatsEnd),
  HYDRATION("H₂O", "Cellular Hydration", "ml", HydrationStart, HydrationMid, HydrationEnd),
  STEPS("STEPS", "Daily Locomotion", "steps", StepsStart, StepsMid, StepsEnd),
}

enum class StatsTimeRangeOption(
  val shortLabel: String,
  val subtitle: String,
) {
  WEEKLY("WEEKLY", "7-Day Window"),
  MONTHLY("MONTHLY", "30-Day / 4-Wk"),
  YEARLY("YEARLY", "12-Month Trend"),
  CUSTOM_DATE("CALENDAR", "Day & Timeline"),
}

enum class StatsGraphVisualMode(
  val shortLabel: String,
  val subtitle: String,
) {
  ALL_GRAPHS("ALL 3 GRAPHS", "3-D Column • Line Markers • 3-D Pie"),
  COLUMN_3D("3-D COLUMN", "3-D Clustered Column"),
  LINE_MARKERS("LINE MARKERS", "Line with Markers"),
  PIE_3D("3-D PIE", "3-D Pie Breakdown"),
}

@Immutable
internal data class StatsResponsiveMetrics(
  val scale: Float,
  val fontCompensation: Float,
  val isTabletOrWide: Boolean,
  val contentMaxWidth: Dp,
  val horizontalPadding: Dp,
  val topOffsetPadding: Dp,
  val bottomOffsetPadding: Dp,
  val sectionGap: Dp,
  val cardCornerRadius: Dp,
  val cardPadding: Dp,
) {
  fun spScaled(base: Float, minVal: Float, maxVal: Float): TextUnit =
    ((base * scale).coerceIn(minVal, maxVal) * fontCompensation).sp
}

internal fun computeStatsMetrics(
  maxWidth: Dp,
  maxHeight: Dp,
  fontScale: Float,
): StatsResponsiveMetrics {
  val w = maxWidth.value.coerceAtLeast(280f)
  val h = maxHeight.value.coerceAtLeast(480f)
  val isExpanded = w >= 600f
  val isTabletOrWide = w >= 720f
  val widthFactor = (w / 392f).coerceIn(0.80f, 1.26f)
  val heightFactor = (h / 830f).coerceIn(0.76f, 1.18f)
  val scale = min(widthFactor, heightFactor * 1.05f).coerceIn(0.78f, 1.20f)

  val safeFontScale = fontScale.coerceIn(0.5f, 3.0f)
  val targetFontGrowth = if (safeFontScale >= 1f) {
    1f + (safeFontScale - 1f).coerceAtMost(1.2f) * 0.14f
  } else {
    1f - (1f - safeFontScale).coerceAtMost(0.5f) * 0.20f
  }
  val fontCompensation = targetFontGrowth / safeFontScale

  val contentMaxWidth = when {
    w >= 1100f -> (w * 0.84f).coerceIn(840f, 1060f).dp
    w >= 840f -> (w * 0.86f).coerceIn(680f, 880f).dp
    isExpanded -> (w * 0.88f).coerceIn(520f, 720f).dp
    else -> w.dp
  }

  return StatsResponsiveMetrics(
    scale = scale,
    fontCompensation = fontCompensation,
    isTabletOrWide = isTabletOrWide,
    contentMaxWidth = contentMaxWidth,
    horizontalPadding = (18f * widthFactor).coerceIn(14f, 32f).dp,
    topOffsetPadding = (12f * heightFactor).coerceIn(8f, 18f).dp,
    bottomOffsetPadding = (104f * heightFactor).coerceIn(88f, 124f).dp,
    sectionGap = (14f * heightFactor).coerceIn(11f, 20f).dp,
    cardCornerRadius = (18f * scale).coerceIn(14f, 20f).dp,
    cardPadding = (15f * scale).coerceIn(12f, 19f).dp,
  )
}

@Immutable
private data class StatsChartBucket(
  val label: String,
  val subLabel: String,
  val dayKeyOrRange: String,
  val actualValue: Int,
  val targetValue: Int,
  val completionPercent: Int,
  val qualifiesStreak: Boolean,
  val hasLoggedData: Boolean,
)

@Immutable
private data class StatsPieSlice(
  val label: String,
  val valueText: String,
  val rawValue: Float,
  val sharePercent: Int,
  val topColor: Color,
  val sideColor: Color,
)

// ============================================================================
// SCULPTED 3D FLOATING BOTTOM NAVIGATION BAR (HOME • STATS • SETTINGS)
// Placed at the bottom-center of Dashboard, Stats, and Settings screens.
// ============================================================================

@Composable
fun BioSyncFloatingBottomBar(
  activeDestination: BioSyncFloatingNavDestination,
  onSelectHome: () -> Unit,
  onSelectStats: () -> Unit,
  onSelectSettings: () -> Unit,
  showStatsOption: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val haptics = rememberBioSyncHaptics()
  val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = 20.dp,
        end = 20.dp,
        bottom = navBarBottomPadding + 14.dp,
      ),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Row(
      modifier = Modifier
        .widthIn(max = if (showStatsOption) 380.dp else 280.dp)
        .fillMaxWidth()
        .drawWithCache {
          val cornerPx = 26.dp.toPx()
          val deepShadow = Paint().apply {
            isAntiAlias = true
            asFrameworkPaint().apply {
              color = android.graphics.Color.argb(235, 0, 0, 0)
              maskFilter = BlurMaskFilter(safeDashboardBlur(22.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
            }
          }
          val rimGlow = Paint().apply {
            isAntiAlias = true
            asFrameworkPaint().apply {
              color = android.graphics.Color.argb(26, 255, 255, 255)
              maskFilter = BlurMaskFilter(safeDashboardBlur(8.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
            }
          }
          val barBgBrush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFF1B1C24),
              Color(0xFF101116),
              Color(0xFF08080C),
            ),
          )
          val rimBrush = Brush.verticalGradient(
            colors = listOf(
              PureWhite.copy(alpha = 0.36f),
              StatsZinc700.copy(alpha = 0.72f),
              Color(0xFF16161D),
            ),
          )
          onDrawBehind {
            drawIntoCanvas { canvas ->
              canvas.drawRoundRect(
                2.dp.toPx(),
                10.dp.toPx(),
                size.width - 2.dp.toPx(),
                size.height + 12.dp.toPx(),
                cornerPx,
                cornerPx,
                deepShadow,
              )
              canvas.drawRoundRect(
                0f,
                -1.dp.toPx(),
                size.width,
                size.height + 1.dp.toPx(),
                cornerPx,
                cornerPx,
                rimGlow,
              )
            }
            drawRoundRect(
              brush = barBgBrush,
              cornerRadius = CornerRadius(cornerPx, cornerPx),
            )
            drawRoundRect(
              brush = rimBrush,
              cornerRadius = CornerRadius(cornerPx, cornerPx),
              style = Stroke(width = 1.2.dp.toPx()),
            )
          }
        }
        .padding(horizontal = 7.dp, vertical = 6.dp)
        .testTag("biosync_floating_bottom_bar"),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FloatingNavPillButton(
        destination = BioSyncFloatingNavDestination.HOME,
        isSelected = activeDestination == BioSyncFloatingNavDestination.HOME,
        accentColor = StepsStart,
        onClick = {
          if (activeDestination != BioSyncFloatingNavDestination.HOME) {
            haptics.confirm()
            onSelectHome()
          } else {
            haptics.tick()
          }
        },
        testTag = "floating_nav_home",
        modifier = Modifier.weight(1f),
      )

      if (showStatsOption) {
        FloatingNavPillButton(
          destination = BioSyncFloatingNavDestination.STATS,
          isSelected = activeDestination == BioSyncFloatingNavDestination.STATS,
          accentColor = HydrationStart,
          onClick = {
            if (activeDestination != BioSyncFloatingNavDestination.STATS) {
              haptics.confirm()
              onSelectStats()
            } else {
              haptics.tick()
            }
          },
          testTag = "floating_nav_stats",
          modifier = Modifier.weight(1f),
        )
      }

      FloatingNavPillButton(
        destination = BioSyncFloatingNavDestination.SETTINGS,
        isSelected = activeDestination == BioSyncFloatingNavDestination.SETTINGS,
        accentColor = CaloriesStart,
        onClick = {
          // Exact same haptic + navigation logic as clicking the top-right Profile badge -> Settings option
          if (activeDestination != BioSyncFloatingNavDestination.SETTINGS) {
            haptics.confirm()
            onSelectSettings()
          } else {
            haptics.tick()
          }
        },
        testTag = "floating_nav_settings",
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun FloatingNavPillButton(
  destination: BioSyncFloatingNavDestination,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val textColor by animateColorAsState(
    targetValue = if (isSelected) ObsidianBlack else StatsZinc300,
    animationSpec = tween(220, easing = StatsEaseOut),
    label = "${destination.name}_text_color",
  )
  val dotSize by animateDpAsState(
    targetValue = if (isSelected) 6.dp else 4.5.dp,
    animationSpec = tween(220, easing = StatsEaseOut),
    label = "${destination.name}_dot_size",
  )

  val pillShape = RoundedCornerShape(20.dp)

  Row(
    modifier = modifier
      .heightIn(min = 44.dp)
      .clip(pillShape)
      .background(
        brush = if (isSelected) {
          Brush.horizontalGradient(
            colors = listOf(PureWhite, Color(0xFFEAECEF)),
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(Color(0xFF15161D), Color(0xFF0E0F14)),
          )
        },
      )
      .border(
        width = 1.dp,
        color = if (isSelected) PureWhite else StatsZinc800.copy(alpha = 0.75f),
        shape = pillShape,
      )
      .clickable(
        role = Role.Tab,
        onClick = onClick,
      )
      .padding(horizontal = 10.dp, vertical = 9.dp)
      .testTag(testTag),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Custom Vector Icon per tab
    Canvas(modifier = Modifier.size(14.dp)) {
      val w = size.width
      val h = size.height
      val stroke = 1.6.dp.toPx()
      val iconColor = if (isSelected) ObsidianBlack else accentColor
      when (destination) {
        BioSyncFloatingNavDestination.HOME -> {
          val path = Path().apply {
            moveTo(w * 0.14f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.14f)
            lineTo(w * 0.86f, h * 0.48f)
            lineTo(w * 0.86f, h * 0.86f)
            lineTo(w * 0.14f, h * 0.86f)
            close()
          }
          drawPath(
            path = path,
            color = iconColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
          drawCircle(
            color = if (isSelected) accentColor else PureWhite,
            radius = 1.8.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.62f),
          )
        }
        BioSyncFloatingNavDestination.STATS -> {
          // 3D Column Bars icon
          drawRoundRect(
            color = iconColor,
            topLeft = Offset(w * 0.12f, h * 0.50f),
            size = Size(w * 0.20f, h * 0.38f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
          )
          drawRoundRect(
            color = if (isSelected) ObsidianBlack else PureWhite,
            topLeft = Offset(w * 0.40f, h * 0.24f),
            size = Size(w * 0.20f, h * 0.64f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
          )
          drawRoundRect(
            color = iconColor,
            topLeft = Offset(w * 0.68f, h * 0.36f),
            size = Size(w * 0.20f, h * 0.52f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
          )
        }
        BioSyncFloatingNavDestination.SETTINGS -> {
          // Precision calibration sliders (matching top-right Profile -> Settings icon)
          drawLine(
            color = iconColor,
            start = Offset(w * 0.12f, h * 0.28f),
            end = Offset(w * 0.88f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = if (isSelected) accentColor else PureWhite,
            radius = 2.1.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.28f),
          )
          drawLine(
            color = iconColor,
            start = Offset(w * 0.12f, h * 0.72f),
            end = Offset(w * 0.88f, h * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = if (isSelected) accentColor else PureWhite,
            radius = 2.1.dp.toPx(),
            center = Offset(w * 0.66f, h * 0.72f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    Text(
      text = destination.label,
      color = textColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 10.6.sp,
      letterSpacing = 0.6.sp,
      maxLines = 1,
    )

    Spacer(modifier = Modifier.width(5.dp))

    Box(
      modifier = Modifier
        .size(dotSize)
        .clip(CircleShape)
        .background(if (isSelected) ObsidianBlack else accentColor.copy(alpha = 0.75f)),
    )
  }
}

// ============================================================================
// DEEP ANALYTICS & CUSTOM CALENDAR TIMELINE SCREEN (STATS TAB)
// ============================================================================

@Composable
fun BioSyncStatsScreen(
  subjectName: String,
  ageYears: Int,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  targetCaloriesKcal: Int,
  targetProteinGrams: Int,
  targetCarbsGrams: Int,
  targetFatsGrams: Int,
  targetHydrationMl: Int,
  restDayWaterDeductionMl: Int,
  targetStepsCount: Int,
  streakThreshold: StreakThresholdOption,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
  protocolLockedAtEpochMillis: Long = 0L,
  dayOneAnchorStartEpochMillis: Long = 0L,
  dayOneInitialCountdownCompleted: Boolean = false,
  showStatsOption: Boolean = true,
  onNavigateHome: () -> Unit,
  onOpenSettingsPage: () -> Unit,
  scrollState: ScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) },
  showInlineBottomBar: Boolean = true,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onNavigateHome)

  val context = LocalContext.current
  val haptics = rememberBioSyncHaptics()
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  var dashboardData by remember { mutableStateOf(DashboardLocalStore.load(context)) }
  val storeRevision by DashboardLocalStore.storeRevisionFlow.collectAsState()
  var showTopRightSettingsMenu by remember { mutableStateOf(false) }
  var lastStatsQuickLogEpochMs by remember { mutableLongStateOf(0L) }

  LaunchedEffect(storeRevision) {
    val latestFromDisk = DashboardLocalStore.load(context)
    if (latestFromDisk != dashboardData) {
      dashboardData = latestFromDisk
    }
  }

  val targetCalories = targetCaloriesKcal.coerceAtLeast(1000)
  val targetProtein = targetProteinGrams.coerceAtLeast(30)
  val targetCarbs = targetCarbsGrams.coerceAtLeast(40)
  val targetFats = targetFatsGrams.coerceAtLeast(20)
  val trainingWaterTargetMl = targetHydrationMl.coerceAtLeast(500)
  val restWaterTargetMl = if (activityLevel == ActivityLevelOption.SEDENTARY || selectedWorkoutDays.isEmpty()) {
    trainingWaterTargetMl
  } else {
    (trainingWaterTargetMl - restDayWaterDeductionMl).coerceAtLeast(1500)
  }
  val targetSteps = targetStepsCount.coerceAtLeast(1000)
  val streakThresholdPercent = streakThreshold.percentage

  val stepSyncController = rememberBioSyncStepSyncController(
    decidedDailyStepsGoal = targetSteps,
    autoRequestPermissionsOnStart = false,
  )
  val liveSteps = (stepSyncController.state.liveTodaySteps.coerceAtLeast(0) + dashboardData.manualStepsToday.coerceAtLeast(0))

  val isTodayWorkoutDay = remember(activityLevel, selectedWorkoutDays, dashboardData.dayKey) {
    isCalendarWorkoutDay(Calendar.getInstance(), activityLevel, selectedWorkoutDays)
  }
  val todayEffectiveWaterTarget = if (isTodayWorkoutDay) trainingWaterTargetMl else restWaterTargetMl

  // Always keep today's record synced into the local database (`dailyHistoryMap`)
  LaunchedEffect(
    dashboardData.todayTimelineEntries,
    liveSteps,
    targetCalories,
    targetProtein,
    targetCarbs,
    targetFats,
    todayEffectiveWaterTarget,
    targetSteps,
    streakThresholdPercent,
    isTodayWorkoutDay,
    dashboardData.dayKey,
  ) {
    val currentDayKey = DashboardTimeFormatter.currentDayKey()
    if (dashboardData.dayKey != currentDayKey) {
      val rolledOver = DashboardLocalStore.rolloverToDate(context, dashboardData, currentDayKey)
      dashboardData = rolledOver
      BioSyncHealthStepManager.resetForNewDay(context, currentDayKey)
      return@LaunchedEffect
    }

    val entries = dashboardData.todayTimelineEntries
    val cal = entries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
    val pro = entries.sumOf { it.proteinGrams }.coerceAtLeast(0)
    val carb = entries.sumOf { it.carbsGrams }.coerceAtLeast(0)
    val fat = entries.sumOf { it.fatsGrams }.coerceAtLeast(0)
    val wat = entries.sumOf { it.hydrationMl }.coerceAtLeast(0)
    val allMet = ((liveSteps * 100f) / targetSteps).roundToInt() >= streakThresholdPercent &&
      ((cal * 100f) / targetCalories).roundToInt() >= streakThresholdPercent &&
      ((pro * 100f) / targetProtein).roundToInt() >= streakThresholdPercent &&
      ((carb * 100f) / targetCarbs).roundToInt() >= streakThresholdPercent &&
      ((fat * 100f) / targetFats).roundToInt() >= streakThresholdPercent &&
      ((wat * 100f) / todayEffectiveWaterTarget).roundToInt() >= streakThresholdPercent

    val synced = DashboardLocalStore.withSyncedTodayRecord(
      data = dashboardData,
      liveSteps = liveSteps,
      targetSteps = targetSteps,
      targetCalories = targetCalories,
      targetProtein = targetProtein,
      targetCarbs = targetCarbs,
      targetFats = targetFats,
      targetWaterMl = todayEffectiveWaterTarget,
      streakThresholdPercent = streakThresholdPercent,
      streakQualified = allMet,
      isWorkoutDay = isTodayWorkoutDay,
    )
    dashboardData = synced
    DashboardLocalStore.save(context, synced)
  }

  var selectedCategoryName by rememberSaveable { mutableStateOf(StatsCategoryOption.ALL.name) }
  var selectedTimeRangeName by rememberSaveable { mutableStateOf(StatsTimeRangeOption.WEEKLY.name) }
  var selectedGraphModeName by rememberSaveable { mutableStateOf(StatsGraphVisualMode.ALL_GRAPHS.name) }

  // Backtrack offset (0 = current week/month/year; clamped so user cannot backtrack before login date)
  var periodOffset by rememberSaveable { mutableIntStateOf(0) }

  val todayKey = remember { DashboardTimeFormatter.currentDayKey() }
  val loginStartDayKey = remember(protocolLockedAtEpochMillis, todayKey) {
    if (protocolLockedAtEpochMillis > 0L) {
      val lockedKey = DashboardTimeFormatter.currentDayKey(protocolLockedAtEpochMillis)
      if (lockedKey <= todayKey) lockedKey else todayKey
    } else {
      todayKey
    }
  }
  val loginStartCal = remember(loginStartDayKey) { parseDayKeyToCalendar(loginStartDayKey) }
  val loginStartYear = loginStartCal.get(Calendar.YEAR)
  val loginStartMonthZeroBased = loginStartCal.get(Calendar.MONTH)
  val loginStartDayOfMonth = loginStartCal.get(Calendar.DAY_OF_MONTH)

  // Selected custom calendar date (clamped to >= loginStartDayKey)
  var selectedCustomDateKey by rememberSaveable { mutableStateOf(todayKey) }
  LaunchedEffect(loginStartDayKey) {
    if (selectedCustomDateKey < loginStartDayKey) {
      selectedCustomDateKey = loginStartDayKey
    }
  }
  val initialCal = remember { Calendar.getInstance() }
  var calendarViewYear by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
  var calendarViewMonthZeroBased by rememberSaveable { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
  LaunchedEffect(loginStartYear, loginStartMonthZeroBased) {
    if (calendarViewYear < loginStartYear ||
      (calendarViewYear == loginStartYear && calendarViewMonthZeroBased < loginStartMonthZeroBased)
    ) {
      calendarViewYear = loginStartYear
      calendarViewMonthZeroBased = loginStartMonthZeroBased
    }
  }

  val selectedCategory = remember(selectedCategoryName) {
    runCatching { StatsCategoryOption.valueOf(selectedCategoryName) }.getOrDefault(StatsCategoryOption.ALL)
  }
  val selectedTimeRange = remember(selectedTimeRangeName) {
    runCatching { StatsTimeRangeOption.valueOf(selectedTimeRangeName) }.getOrDefault(StatsTimeRangeOption.WEEKLY)
  }
  val selectedGraphMode = remember(selectedGraphModeName) {
    runCatching { StatsGraphVisualMode.valueOf(selectedGraphModeName) }.getOrDefault(StatsGraphVisualMode.ALL_GRAPHS)
  }

  var selectedBucketIndex by remember(selectedCategory, selectedTimeRange, periodOffset, selectedCustomDateKey) {
    mutableIntStateOf(-1)
  }

  val entryAnim = remember { Animatable(0f) }
  LaunchedEffect(selectedCategory, selectedTimeRange, periodOffset, selectedCustomDateKey) {
    entryAnim.snapTo(0.15f)
    entryAnim.animateTo(1f, animationSpec = tween(520, easing = StatsEaseOut))
  }

  val infiniteTransition = rememberInfiniteTransition(label = "stats_ambient")
  val ambientPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "stats_ambient_pulse",
  )

  // Helper to resolve any date's `DailyHistoricalRecord` from the user's database
  fun resolveDayRecord(dayKey: String): DailyHistoricalRecord {
    val existing = dashboardData.dailyHistoryMap[dayKey]
    val calForDay = parseDayKeyToCalendar(dayKey)
    val isWorkout = isCalendarWorkoutDay(calForDay, activityLevel, selectedWorkoutDays)
    val dayWaterGoal = if (isWorkout) trainingWaterTargetMl else restWaterTargetMl
    if (existing != null) {
      return existing.copy(
        targetStepsCount = existing.targetStepsCount.coerceAtLeast(1000),
        targetCaloriesKcal = existing.targetCaloriesKcal.coerceAtLeast(1000),
        targetProteinGrams = existing.targetProteinGrams.coerceAtLeast(30),
        targetCarbsGrams = existing.targetCarbsGrams.coerceAtLeast(40),
        targetFatsGrams = existing.targetFatsGrams.coerceAtLeast(20),
        targetHydrationMl = existing.targetHydrationMl.coerceAtLeast(500),
      )
    }
    return DailyHistoricalRecord(
      dayKey = dayKey,
      stepsCount = if (dayKey == todayKey) liveSteps else 0,
      targetStepsCount = targetSteps,
      caloriesKcal = 0,
      targetCaloriesKcal = targetCalories,
      proteinGrams = 0,
      targetProteinGrams = targetProtein,
      carbsGrams = 0,
      targetCarbsGrams = targetCarbs,
      fatsGrams = 0,
      targetFatsGrams = targetFats,
      hydrationMl = 0,
      targetHydrationMl = dayWaterGoal,
      streakThresholdPercent = streakThresholdPercent,
      streakQualified = false,
      isWorkoutDay = isWorkout,
      timelineEntries = emptyList(),
    )
  }

  // Build chart buckets for the active category + time range + periodOffset (strictly from loginStartDayKey to todayKey)
  val chartBuckets: List<StatsChartBucket> = remember(
    dashboardData.dailyHistoryMap,
    selectedCategory,
    selectedTimeRange,
    periodOffset,
    selectedCustomDateKey,
    loginStartDayKey,
    todayKey,
    liveSteps,
    targetCalories,
    targetProtein,
    targetCarbs,
    targetFats,
    trainingWaterTargetMl,
    restWaterTargetMl,
    targetSteps,
    streakThresholdPercent,
  ) {
    buildChartBucketsForSelection(
      category = selectedCategory,
      timeRange = selectedTimeRange,
      periodOffset = periodOffset,
      selectedCustomDateKey = selectedCustomDateKey,
      loginStartDayKey = loginStartDayKey,
      todayKey = todayKey,
      resolveDay = ::resolveDayRecord,
      streakThresholdPercent = streakThresholdPercent,
    )
  }

  // Build 3-D Pie Chart slices across ALL categories (or selected category breakdown) from loginStartDayKey to todayKey
  val pieSlices: List<StatsPieSlice> = remember(
    dashboardData.dailyHistoryMap,
    selectedCategory,
    selectedTimeRange,
    periodOffset,
    selectedCustomDateKey,
    loginStartDayKey,
    todayKey,
    liveSteps,
  ) {
    buildPieSlicesForSelection(
      category = selectedCategory,
      timeRange = selectedTimeRange,
      periodOffset = periodOffset,
      selectedCustomDateKey = selectedCustomDateKey,
      loginStartDayKey = loginStartDayKey,
      todayKey = todayKey,
      resolveDay = ::resolveDayRecord,
      numberFormat = numberFormat,
    )
  }

  val inspectedDayRecord = remember(
    dashboardData.dailyHistoryMap,
    selectedCustomDateKey,
    liveSteps,
    targetCalories,
    targetProtein,
    targetCarbs,
    targetFats,
    trainingWaterTargetMl,
    restWaterTargetMl,
    targetSteps,
  ) {
    resolveDayRecord(selectedCustomDateKey)
  }

  val isFutureCustomDateSelected = remember(selectedTimeRange, selectedCustomDateKey, todayKey) {
    selectedTimeRange == StatsTimeRangeOption.CUSTOM_DATE && selectedCustomDateKey > todayKey
  }

  val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .testTag("stats_screen_root"),
    contentAlignment = Alignment.TopCenter,
  ) {
    val fontScale = LocalDensity.current.fontScale
    val metrics = remember(maxWidth, maxHeight, fontScale) {
      computeStatsMetrics(maxWidth, maxHeight, fontScale)
    }

    // Apply status bar top inset BEFORE verticalScroll so scrolling content is clipped cleanly
    // below the system status bar and never overlaps the clock or battery icons.
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
        // 1. TOP EXECUTIVE HEADER (Matches Dashboard Top Header + Same Profile -> Settings logic)
        StatsTopExecutiveHeader(
          subjectName = subjectName,
          ageYears = ageYears,
          birthDay = birthDay,
          birthMonthZeroBased = birthMonthZeroBased,
          birthYear = birthYear,
          streakDays = dashboardData.baseStreakDays,
          totalTrackedDays = dashboardData.dailyHistoryMap.count { (_, rec) ->
            rec.caloriesKcal > 0 || rec.hydrationMl > 0 || rec.stepsCount > 0 || rec.timelineEntries.isNotEmpty()
          }.coerceAtLeast(1),
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
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 2. CATEGORY-WISE SELECTION DECK (Overview, Calories, Protein, Carbs, Fats, Hydration, Steps)
        val canBacktrackPeriod = remember(selectedTimeRange, periodOffset, loginStartDayKey, todayKey) {
          when (selectedTimeRange) {
            StatsTimeRangeOption.WEEKLY -> {
              val prevEndCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, (periodOffset - 1) * 7) }
              DashboardTimeFormatter.currentDayKey(prevEndCal.timeInMillis) >= loginStartDayKey
            }
            StatsTimeRangeOption.MONTHLY -> {
              val prevMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, periodOffset - 1) }
              val y = prevMonthCal.get(Calendar.YEAR)
              val m = prevMonthCal.get(Calendar.MONTH)
              y > loginStartYear || (y == loginStartYear && m >= loginStartMonthZeroBased)
            }
            StatsTimeRangeOption.YEARLY -> {
              val prevYear = Calendar.getInstance().get(Calendar.YEAR) + (periodOffset - 1)
              prevYear >= loginStartYear
            }
            StatsTimeRangeOption.CUSTOM_DATE -> false
          }
        }

        StatsCategoryAndTimeControlDeck(
          selectedCategory = selectedCategory,
          onSelectCategory = { cat ->
            haptics.selection()
            selectedCategoryName = cat.name
          },
          selectedTimeRange = selectedTimeRange,
          onSelectTimeRange = { range ->
            haptics.selection()
            selectedTimeRangeName = range.name
            periodOffset = 0
          },
          selectedGraphMode = selectedGraphMode,
          onSelectGraphMode = { mode ->
            haptics.selection()
            selectedGraphModeName = mode.name
          },
          periodOffset = periodOffset,
          canBacktrackPeriod = canBacktrackPeriod,
          loginStartDayKey = loginStartDayKey,
          todayKey = todayKey,
          onChangePeriodOffset = { delta ->
            if (delta < 0 && !canBacktrackPeriod) {
              haptics.tick()
            } else {
              haptics.selection()
              periodOffset = (periodOffset + delta).coerceAtMost(0)
            }
          },
          selectedCustomDateKey = selectedCustomDateKey,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 3. KPI SUMMARY STRIP FOR SELECTED CATEGORY & TIME WINDOW ("Less text, more info")
        StatsWindowKpiSummaryStrip(
          category = selectedCategory,
          timeRange = selectedTimeRange,
          buckets = chartBuckets,
          isFutureDateLocked = isFutureCustomDateSelected,
          lockedDateLabel = formatHumanReadableDateKey(selectedCustomDateKey),
          streakThresholdPercent = streakThresholdPercent,
          numberFormat = numberFormat,
          metrics = metrics,
        )

        Spacer(modifier = Modifier.height(metrics.sectionGap))

        // 4. INTERACTIVE CUSTOM CALENDAR & DAY TIMELINE INSPECTOR (Clamped from login date onward; logging only on todayKey)
        if (selectedTimeRange == StatsTimeRangeOption.CUSTOM_DATE) {
          StatsCustomCalendarAndDayTimelineSection(
            calendarYear = calendarViewYear,
            calendarMonthZeroBased = calendarViewMonthZeroBased,
            loginStartYear = loginStartYear,
            loginStartMonthZeroBased = loginStartMonthZeroBased,
            loginStartDayOfMonth = loginStartDayOfMonth,
            loginStartDayKey = loginStartDayKey,
            selectedDateKey = selectedCustomDateKey,
            todayKey = todayKey,
            dailyHistoryMap = dashboardData.dailyHistoryMap,
            inspectedRecord = inspectedDayRecord,
            savedQuickItems = dashboardData.savedQuickItems,
            streakThresholdPercent = streakThresholdPercent,
            onNavigateMonth = { deltaMonths ->
              val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, calendarViewYear)
                set(Calendar.MONTH, calendarViewMonthZeroBased)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, deltaMonths)
              }
              val targetY = cal.get(Calendar.YEAR)
              val targetM = cal.get(Calendar.MONTH)
              if (targetY > loginStartYear || (targetY == loginStartYear && targetM >= loginStartMonthZeroBased)) {
                haptics.selection()
                calendarViewYear = targetY
                calendarViewMonthZeroBased = targetM
              } else {
                haptics.tick()
              }
            },
            onSelectYearMonthDay = { y, mZero, d ->
              val clampedY = y.coerceAtLeast(loginStartYear)
              val clampedM = if (clampedY == loginStartYear) mZero.coerceIn(loginStartMonthZeroBased, 11) else mZero.coerceIn(0, 11)
              val tempCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, clampedY)
                set(Calendar.MONTH, clampedM)
                set(Calendar.DAY_OF_MONTH, 1)
              }
              val maxDaysInM = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
              val minDayInM = if (clampedY == loginStartYear && clampedM == loginStartMonthZeroBased) loginStartDayOfMonth else 1
              val clampedD = d.coerceIn(minDayInM, maxDaysInM)
              haptics.selection()
              calendarViewYear = clampedY
              calendarViewMonthZeroBased = clampedM
              selectedCustomDateKey = formatYearMonthDayKey(clampedY, clampedM, clampedD)
            },
            onJumpToToday = {
              haptics.confirm()
              val nowCal = Calendar.getInstance()
              calendarViewYear = nowCal.get(Calendar.YEAR)
              calendarViewMonthZeroBased = nowCal.get(Calendar.MONTH)
              selectedCustomDateKey = todayKey
            },
            onSelectDateKey = { chosenKey ->
              if (chosenKey >= loginStartDayKey) {
                haptics.selection()
                selectedCustomDateKey = chosenKey
              } else {
                haptics.tick()
              }
            },
            onQuickLogEntryToSelectedDate = { newEntry ->
              // Strictly block logging into past or future dates (only todayKey is writable)
              if (selectedCustomDateKey == todayKey) {
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastStatsQuickLogEpochMs >= 280L) {
                  lastStatsQuickLogEpochMs = nowMs
                  haptics.confirm()
                  val currentRec = resolveDayRecord(selectedCustomDateKey)
                  val updatedEntries = listOf(newEntry) + currentRec.timelineEntries
                  val newCal = updatedEntries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
                  val newPro = updatedEntries.sumOf { it.proteinGrams }.coerceAtLeast(0)
                  val newCarb = updatedEntries.sumOf { it.carbsGrams }.coerceAtLeast(0)
                  val newFat = updatedEntries.sumOf { it.fatsGrams }.coerceAtLeast(0)
                  val newWat = updatedEntries.sumOf { it.hydrationMl }.coerceAtLeast(0)
                  val allMet = ((currentRec.stepsCount * 100f) / currentRec.targetStepsCount).roundToInt() >= streakThresholdPercent &&
                    ((newCal * 100f) / currentRec.targetCaloriesKcal).roundToInt() >= streakThresholdPercent &&
                    ((newPro * 100f) / currentRec.targetProteinGrams).roundToInt() >= streakThresholdPercent &&
                    ((newCarb * 100f) / currentRec.targetCarbsGrams).roundToInt() >= streakThresholdPercent &&
                    ((newFat * 100f) / currentRec.targetFatsGrams).roundToInt() >= streakThresholdPercent &&
                    ((newWat * 100f) / currentRec.targetHydrationMl).roundToInt() >= streakThresholdPercent
                  val updatedRec = currentRec.copy(
                    caloriesKcal = newCal,
                    proteinGrams = newPro,
                    carbsGrams = newCarb,
                    fatsGrams = newFat,
                    hydrationMl = newWat,
                    streakQualified = allMet,
                    timelineEntries = updatedEntries,
                    updatedAtEpochMs = nowMs,
                  )
                  val updatedWithDay = DashboardLocalStore.withUpdatedDayRecord(dashboardData, updatedRec)
                  val streakEval = BioSyncStreakEngine.recalculateStreakFromHistory(
                    historyMap = updatedWithDay.dailyHistoryMap,
                    todayKey = todayKey,
                    todayQualified = allMet,
                    fallbackBaseStreak = dashboardData.baseStreakDays,
                    fallbackLastCompletedKey = dashboardData.lastCompletedStreakDayKey,
                  )
                  val updatedStore = updatedWithDay.copy(
                    baseStreakDays = streakEval.streakDays,
                    lastCompletedStreakDayKey = streakEval.lastCompletedDayKey,
                  )
                  dashboardData = updatedStore
                  DashboardLocalStore.save(context, updatedStore)
                }
              } else {
                haptics.warning()
              }
            },
            onDeleteEntryFromSelectedDate = { entryToDelete ->
              // Strictly block deleting from past or future dates (only todayKey is writable)
              if (selectedCustomDateKey == todayKey) {
                haptics.warning()
                val currentRec = resolveDayRecord(selectedCustomDateKey)
                val updatedEntries = currentRec.timelineEntries.filterNot { it.id == entryToDelete.id }
                val newCal = updatedEntries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
                val newPro = updatedEntries.sumOf { it.proteinGrams }.coerceAtLeast(0)
                val newCarb = updatedEntries.sumOf { it.carbsGrams }.coerceAtLeast(0)
                val newFat = updatedEntries.sumOf { it.fatsGrams }.coerceAtLeast(0)
                val newWat = updatedEntries.sumOf { it.hydrationMl }.coerceAtLeast(0)
                val allMet = ((currentRec.stepsCount * 100f) / currentRec.targetStepsCount).roundToInt() >= streakThresholdPercent &&
                  ((newCal * 100f) / currentRec.targetCaloriesKcal).roundToInt() >= streakThresholdPercent &&
                  ((newPro * 100f) / currentRec.targetProteinGrams).roundToInt() >= streakThresholdPercent &&
                  ((newCarb * 100f) / currentRec.targetCarbsGrams).roundToInt() >= streakThresholdPercent &&
                  ((newFat * 100f) / currentRec.targetFatsGrams).roundToInt() >= streakThresholdPercent &&
                  ((newWat * 100f) / currentRec.targetHydrationMl).roundToInt() >= streakThresholdPercent
                val updatedRec = currentRec.copy(
                  caloriesKcal = newCal,
                  proteinGrams = newPro,
                  carbsGrams = newCarb,
                  fatsGrams = newFat,
                  hydrationMl = newWat,
                  streakQualified = allMet,
                  timelineEntries = updatedEntries,
                  updatedAtEpochMs = System.currentTimeMillis(),
                )
                val updatedWithDay = DashboardLocalStore.withUpdatedDayRecord(dashboardData, updatedRec)
                val streakEval = BioSyncStreakEngine.recalculateStreakFromHistory(
                  historyMap = updatedWithDay.dailyHistoryMap,
                  todayKey = todayKey,
                  todayQualified = allMet,
                  fallbackBaseStreak = dashboardData.baseStreakDays,
                  fallbackLastCompletedKey = dashboardData.lastCompletedStreakDayKey,
                )
                val updatedStore = updatedWithDay.copy(
                  baseStreakDays = streakEval.streakDays,
                  lastCompletedStreakDayKey = streakEval.lastCompletedDayKey,
                )
                dashboardData = updatedStore
                DashboardLocalStore.save(context, updatedStore)
              } else {
                haptics.warning()
              }
            },
            numberFormat = numberFormat,
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.sectionGap))
        }

        // 5, 6 & 7. DEEP ANALYTICS GRAPHS (Locked when viewing a Future Date in Custom Calendar!)
        if (isFutureCustomDateSelected) {
          StatsFutureDateGraphsLockedCard(
            selectedFutureDateKey = selectedCustomDateKey,
            todayKey = todayKey,
            selectedCategory = selectedCategory,
            onJumpBackToToday = {
              haptics.confirm()
              val nowCal = Calendar.getInstance()
              calendarViewYear = nowCal.get(Calendar.YEAR)
              calendarViewMonthZeroBased = nowCal.get(Calendar.MONTH)
              selectedCustomDateKey = todayKey
            },
            metrics = metrics,
          )

          Spacer(modifier = Modifier.height(metrics.sectionGap))
        } else {
          // 5. GRAPH #1: 3-D CLUSTERED COLUMN CHART (Reference Image 1: Isometric 3D Columns Actual vs Target)
          if (
            selectedGraphMode == StatsGraphVisualMode.ALL_GRAPHS ||
            selectedGraphMode == StatsGraphVisualMode.COLUMN_3D
          ) {
            Stats3DClusteredColumnChartCard(
              category = selectedCategory,
              timeRange = selectedTimeRange,
              buckets = chartBuckets,
              selectedBucketIndex = selectedBucketIndex,
              onSelectBucketIndex = { idx ->
                haptics.selection()
                selectedBucketIndex = if (selectedBucketIndex == idx) -1 else idx
              },
              animFraction = entryAnim.value,
              numberFormat = numberFormat,
              metrics = metrics,
            )

            Spacer(modifier = Modifier.height(metrics.sectionGap))
          }

          // 6. GRAPH #2: LINE WITH MARKERS PROGRESS TREND CHART (Reference Image 3: Dual Lines with Square Markers)
          if (
            selectedGraphMode == StatsGraphVisualMode.ALL_GRAPHS ||
            selectedGraphMode == StatsGraphVisualMode.LINE_MARKERS
          ) {
            StatsLineWithMarkersChartCard(
              category = selectedCategory,
              timeRange = selectedTimeRange,
              buckets = chartBuckets,
              selectedBucketIndex = selectedBucketIndex,
              onSelectBucketIndex = { idx ->
                haptics.selection()
                selectedBucketIndex = if (selectedBucketIndex == idx) -1 else idx
              },
              animFraction = entryAnim.value,
              numberFormat = numberFormat,
              metrics = metrics,
            )

            Spacer(modifier = Modifier.height(metrics.sectionGap))
          }

          // 7. GRAPH #3: 3-D PIE CHART (All 6 Categories + Dynamic Selected Category Breakdown)
          if (
            selectedGraphMode == StatsGraphVisualMode.ALL_GRAPHS ||
            selectedGraphMode == StatsGraphVisualMode.PIE_3D
          ) {
            Stats3DPieChartCard(
              category = selectedCategory,
              slices = pieSlices,
              timeRange = selectedTimeRange,
              selectedDateKey = selectedCustomDateKey,
              animFraction = entryAnim.value,
              ambientPhase = ambientPulse,
              metrics = metrics,
            )

            Spacer(modifier = Modifier.height(metrics.sectionGap))
          }
        }

        // If user is in Weekly/Monthly/Yearly mode, also give a quick 1-tap gateway to the Custom Calendar Day & Timeline Inspector below
        if (selectedTimeRange != StatsTimeRangeOption.CUSTOM_DATE) {
          StatsCalendarQuickGatewayBanner(
            selectedCustomDateKey = selectedCustomDateKey,
            inspectedRecord = inspectedDayRecord,
            onOpenCalendarInspector = {
              haptics.confirm()
              selectedTimeRangeName = StatsTimeRangeOption.CUSTOM_DATE.name
            },
            numberFormat = numberFormat,
            metrics = metrics,
          )
        }
      }
    }

    val isWaitingForInitialDayOneBottomBar = !dayOneInitialCountdownCompleted &&
      dayOneAnchorStartEpochMillis > 0L &&
      System.currentTimeMillis() < dayOneAnchorStartEpochMillis
    val effectiveShowStats = showStatsOption && !isWaitingForInitialDayOneBottomBar

    // Centered Floating Bottom Bar (Home • Stats • Settings)
    if (showInlineBottomBar) {
      BioSyncFloatingBottomBar(
        activeDestination = BioSyncFloatingNavDestination.STATS,
        showStatsOption = effectiveShowStats,
        onSelectHome = onNavigateHome,
        onSelectStats = {},
        onSelectSettings = onOpenSettingsPage,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

// ============================================================================
// 1. STATS TOP EXECUTIVE HEADER (WITH SAME PROFILE -> SETTINGS DROPDOWN LOGIC)
// ============================================================================

@Composable
private fun StatsTopExecutiveHeader(
  subjectName: String,
  ageYears: Int,
  birthDay: Int,
  birthMonthZeroBased: Int,
  birthYear: Int,
  streakDays: Int,
  totalTrackedDays: Int,
  isSettingsMenuExpanded: Boolean,
  onToggleSettingsMenu: () -> Unit,
  onOpenSettingsPage: () -> Unit,
  metrics: StatsResponsiveMetrics,
) {
  val subjectIdBadge = remember(subjectName) {
    BioSyncLocalStore.computeSubjectIdBadge(subjectName)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stats_top_header"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Box(
            modifier = Modifier
              .size(6.5.dp)
              .clip(CircleShape)
              .background(HydrationStart),
          )
          Text(
            text = "DEEP TELEMETRY • ON-DEVICE DATABASE",
            color = HydrationStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = metrics.spScaled(9.4f, 8.2f, 11.0f),
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Analytics & Timeline",
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(24.5f, 20.5f, 29f),
          letterSpacing = (-0.7).sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("stats_title_text"),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Top-Right Subject ID Badge (same logic as Dashboard)
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
                listOf(PureWhite.copy(alpha = 0.38f), StatsZinc700.copy(alpha = 0.75f), StatsZinc850)
              },
            ),
            shape = RoundedCornerShape(12.dp),
          )
          .clickable(role = Role.Button, onClick = onToggleSettingsMenu)
          .padding(horizontal = 12.dp, vertical = 9.dp)
          .testTag("stats_top_right_id_badge"),
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
              .background(if (isSettingsMenuExpanded) ObsidianBlack else HydrationStart),
          )
          Text(
            text = subjectIdBadge,
            color = if (isSettingsMenuExpanded) ObsidianBlack else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(14f, 12.5f, 15.5f),
            letterSpacing = 0.8.sp,
          )
        }
      }
    }

    // Same Top-Right Settings Popover when tapping Subject ID Badge
    AnimatedVisibility(
      visible = isSettingsMenuExpanded,
      enter = expandVertically(tween(220, easing = StatsEaseOut)) + fadeIn(tween(180)),
      exit = shrinkVertically(tween(180, easing = StatsEaseOut)) + fadeOut(tween(140)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .floating3DProtocolCard(cornerRadiusDp = 15.dp, topRimTint = PureWhite)
          .padding(13.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.horizontalGradient(listOf(PureWhite, Color(0xFFEAECEF))))
            .clickable(role = Role.Button, onClick = onOpenSettingsPage)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag("stats_open_settings_option_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "OPEN SETTINGS & DOSSIER ($subjectIdBadge)",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
          )
          Text(
            text = "EDIT ›",
            color = ObsidianBlack,
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
// 2. CATEGORY, TIME RANGE & GRAPH MODE SELECTOR DECK (SYMMETRICAL BENTO GRID)
// ============================================================================

@Composable
private fun StatsCategoryAndTimeControlDeck(
  selectedCategory: StatsCategoryOption,
  onSelectCategory: (StatsCategoryOption) -> Unit,
  selectedTimeRange: StatsTimeRangeOption,
  onSelectTimeRange: (StatsTimeRangeOption) -> Unit,
  selectedGraphMode: StatsGraphVisualMode,
  onSelectGraphMode: (StatsGraphVisualMode) -> Unit,
  periodOffset: Int,
  canBacktrackPeriod: Boolean = true,
  loginStartDayKey: String = "",
  todayKey: String = "",
  onChangePeriodOffset: (Int) -> Unit,
  selectedCustomDateKey: String,
  metrics: StatsResponsiveMetrics,
) {
  val isFutureSelected = selectedTimeRange == StatsTimeRangeOption.CUSTOM_DATE &&
    todayKey.isNotEmpty() &&
    selectedCustomDateKey > todayKey

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = selectedCategory.startColor,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_control_deck"),
  ) {
    // A. Header Row with Left Weight(1f) so Right Badge Never Wraps or Collides
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = selectedCategory.startColor,
          endColor = selectedCategory.midColor,
        )
        Text(
          text = "SELECT CATEGORY",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
          letterSpacing = 0.8.sp,
          maxLines = 1,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StatsZinc950)
          .border(1.dp, selectedCategory.startColor.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = "${selectedCategory.fullTitle.uppercase(Locale.US)} (${selectedCategory.unitLabel})",
          color = selectedCategory.startColor,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.6f, 7.5f, 9.8f),
          maxLines = 1,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // B. 2-Row Symmetrical 7-Category Bento Grid (Zero Clipped Chips on Screen Edges!)
    val row1Categories = remember {
      listOf(
        StatsCategoryOption.ALL,
        StatsCategoryOption.CALORIES,
        StatsCategoryOption.PROTEIN,
        StatsCategoryOption.CARBS,
      )
    }
    val row2Categories = remember {
      listOf(
        StatsCategoryOption.FATS,
        StatsCategoryOption.HYDRATION,
        StatsCategoryOption.STEPS,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        row1Categories.forEach { cat ->
          StatsCategoryBentoChip(
            cat = cat,
            isSelected = cat == selectedCategory,
            onClick = { onSelectCategory(cat) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        row2Categories.forEach { cat ->
          StatsCategoryBentoChip(
            cat = cat,
            isSelected = cat == selectedCategory,
            onClick = { onSelectCategory(cat) },
            metrics = metrics,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // C. 4-Column Time Range Selector (WEEKLY • MONTHLY • YEARLY • CALENDAR)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StatsTimeRangeOption.entries.forEach { range ->
        val isSelected = range == selectedTimeRange
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) PureWhite else StatsZinc950)
            .border(
              width = 1.dp,
              color = if (isSelected) PureWhite else StatsZinc800,
              shape = RoundedCornerShape(9.dp),
            )
            .clickable(role = Role.Tab) { onSelectTimeRange(range) }
            .padding(horizontal = 4.dp, vertical = 7.dp)
            .testTag("stats_range_tab_${range.name.lowercase(Locale.US)}"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = range.shortLabel,
            color = if (isSelected) ObsidianBlack else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
            maxLines = 1,
          )
          Spacer(modifier = Modifier.height(1.dp))
          Text(
            text = range.subtitle,
            color = if (isSelected) Color(0xFF3F3F46) else StatsZinc400,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // D. 4-Column Graph Visual Mode Selector (Aligned 1:1 with the 4 Time Range Columns above!)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StatsGraphVisualMode.entries.forEach { mode ->
        val active = mode == selectedGraphMode
        val chipText = when (mode) {
          StatsGraphVisualMode.ALL_GRAPHS -> "ALL GRAPHS"
          StatsGraphVisualMode.COLUMN_3D -> "3-D BAR"
          StatsGraphVisualMode.LINE_MARKERS -> "LINE TREND"
          StatsGraphVisualMode.PIE_3D -> "3-D PIE"
        }
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) selectedCategory.startColor else StatsZinc950)
            .border(
              width = 1.dp,
              color = if (active) selectedCategory.startColor else StatsZinc800,
              shape = RoundedCornerShape(8.dp),
            )
            .clickable { onSelectGraphMode(mode) }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("stats_graph_mode_${mode.name.lowercase(Locale.US)}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = chipText,
            color = if (active) ObsidianBlack else StatsZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // E. Dedicated Full-Width Active Window & Period Navigation Strip (Never crowds the graph mode chips!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(9.dp))
        .background(Color(0xFF0B0C10))
        .border(1.dp, StatsZinc850, RoundedCornerShape(9.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (selectedTimeRange != StatsTimeRangeOption.CUSTOM_DATE) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(5.5.dp)
              .clip(CircleShape)
              .background(selectedCategory.startColor),
          )
          Text(
            text = if (loginStartDayKey.isNotEmpty() && periodOffset == 0) {
              "ACTIVE WINDOW • SINCE LOGIN (${formatHumanReadableDateKey(loginStartDayKey)})"
            } else {
              "WINDOW • ${formatActivePeriodWindowLabel(selectedTimeRange, periodOffset)}"
            },
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (canBacktrackPeriod) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StatsZinc900)
                .border(1.dp, StatsZinc700, RoundedCornerShape(6.dp))
                .clickable { onChangePeriodOffset(-1) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("stats_period_prev_btn"),
            ) {
              Text(
                text = "‹ PREV",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
              )
            }
          } else {
            Text(
              text = "LOGIN BOUND",
              color = StatsZinc500,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(7.8f, 7.0f, 8.8f),
            )
          }

          if (periodOffset < 0) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StatsZinc900)
                .border(1.dp, StatsZinc700, RoundedCornerShape(6.dp))
                .clickable { onChangePeriodOffset(1) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("stats_period_next_btn"),
            ) {
              Text(
                text = "NEXT ›",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
              )
            }
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(5.5.dp)
              .clip(CircleShape)
              .background(if (isFutureSelected) CaloriesStart else HydrationStart),
          )
          Text(
            text = "SELECTED DAY • ${formatHumanReadableDateKey(selectedCustomDateKey)}",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (isFutureSelected) CaloriesStart.copy(alpha = 0.16f) else StepsStart.copy(alpha = 0.16f))
            .border(
              width = 0.9.dp,
              color = if (isFutureSelected) CaloriesStart.copy(alpha = 0.7f) else StepsStart.copy(alpha = 0.7f),
              shape = RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        ) {
          Text(
            text = when {
              isFutureSelected -> "FUTURE • GRAPHS LOCKED"
              selectedCustomDateKey == todayKey -> "TODAY • LIVE SYNC"
              else -> "ARCHIVE • VERIFIED"
            },
            color = if (isFutureSelected) CaloriesStart else StepsStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun StatsCategoryBentoChip(
  cat: StatsCategoryOption,
  isSelected: Boolean,
  onClick: () -> Unit,
  metrics: StatsResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .background(
        if (isSelected) {
          Brush.horizontalGradient(listOf(PureWhite, Color(0xFFE4E4E7)))
        } else {
          Brush.verticalGradient(listOf(Color(0xFF1A1B22), Color(0xFF101015)))
        },
      )
      .border(
        width = 1.dp,
        color = if (isSelected) PureWhite else StatsZinc800,
        shape = RoundedCornerShape(9.dp),
      )
      .clickable(role = Role.Tab, onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 7.5.dp)
      .testTag("stats_category_chip_${cat.name.lowercase(Locale.US)}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier = Modifier
        .size(5.5.dp)
        .clip(CircleShape)
        .background(if (isSelected) ObsidianBlack else cat.startColor),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = cat.shortLabel,
      color = if (isSelected) ObsidianBlack else PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(9.1f, 7.9f, 10.3f),
      letterSpacing = 0.3.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

// ============================================================================
// 3. KPI SUMMARY STRIP ("Less text, more info" + Locked State for Future Dates)
// ============================================================================

@Composable
private fun StatsWindowKpiSummaryStrip(
  category: StatsCategoryOption,
  timeRange: StatsTimeRangeOption,
  buckets: List<StatsChartBucket>,
  isFutureDateLocked: Boolean = false,
  lockedDateLabel: String = "",
  streakThresholdPercent: Int,
  numberFormat: NumberFormat,
  metrics: StatsResponsiveMetrics,
) {
  val activeBuckets = buckets.filter { it.hasLoggedData || it.actualValue > 0 }
  val sampleBuckets = if (activeBuckets.isNotEmpty()) activeBuckets else buckets
  val avgActual = if (isFutureDateLocked) {
    0
  } else if (sampleBuckets.isNotEmpty()) {
    sampleBuckets.map { it.actualValue }.average().roundToInt()
  } else {
    0
  }
  val avgTarget = if (buckets.isNotEmpty()) {
    buckets.map { it.targetValue }.average().roundToInt()
  } else {
    100
  }
  val peakActual = if (isFutureDateLocked) 0 else (buckets.maxOfOrNull { it.actualValue } ?: 0)
  val avgCompletionPct = if (isFutureDateLocked) {
    0
  } else if (avgTarget > 0) {
    ((avgActual * 100f) / avgTarget.toFloat()).roundToInt()
  } else {
    0
  }
  val qualifiedCount = if (isFutureDateLocked) 0 else buckets.count { it.qualifiesStreak }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stats_kpi_summary_strip"),
    horizontalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    StatsKpiMiniCard(
      label = "AVG ACTUAL",
      value = if (isFutureDateLocked) "0 ${category.unitLabel}" else "${numberFormat.format(avgActual)} ${category.unitLabel}",
      subText = "Target ${numberFormat.format(avgTarget)} ${category.unitLabel}",
      accentColor = if (isFutureDateLocked) StatsZinc400 else category.startColor,
      metrics = metrics,
      modifier = Modifier.weight(1f),
    )
    StatsKpiMiniCard(
      label = "PEAK RECORD",
      value = if (isFutureDateLocked) "0 ${category.unitLabel}" else "${numberFormat.format(peakActual)} ${category.unitLabel}",
      subText = if (isFutureDateLocked) "Future • Locked" else "${activeBuckets.size}/${buckets.size} active",
      accentColor = if (isFutureDateLocked) StatsZinc400 else PureWhite,
      metrics = metrics,
      modifier = Modifier.weight(1f),
    )
    StatsKpiMiniCard(
      label = "ADHERENCE",
      value = if (isFutureDateLocked) "LOCKED" else "$avgCompletionPct%",
      subText = if (isFutureDateLocked) "Unlocks $lockedDateLabel" else "$qualifiedCount/${buckets.size} ≥$streakThresholdPercent% gate",
      accentColor = when {
        isFutureDateLocked -> CaloriesStart
        avgCompletionPct >= streakThresholdPercent -> StepsStart
        else -> CaloriesStart
      },
      metrics = metrics,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun StatsKpiMiniCard(
  label: String,
  value: String,
  subText: String,
  accentColor: Color,
  metrics: StatsResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .floating3DProtocolCard(
        cornerRadiusDp = 13.dp,
        topRimTint = accentColor,
      )
      .padding(horizontal = 10.dp, vertical = 9.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.5.dp),
    ) {
      Box(
        modifier = Modifier
          .width(3.dp)
          .height(9.dp)
          .clip(RoundedCornerShape(1.5.dp))
          .background(accentColor),
      )
      Text(
        text = label,
        color = StatsZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
        maxLines = 1,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(13.5f, 11.5f, 15.5f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = subText,
      color = accentColor,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Medium,
      fontSize = metrics.spScaled(8.4f, 7.4f, 9.5f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun StatsFutureDateGraphsLockedCard(
  selectedFutureDateKey: String,
  todayKey: String,
  selectedCategory: StatsCategoryOption,
  onJumpBackToToday: () -> Unit,
  metrics: StatsResponsiveMetrics,
) {
  val humanFutureDate = remember(selectedFutureDateKey) {
    formatHumanReadableDateKey(selectedFutureDateKey)
  }
  val humanTodayDate = remember(todayKey) {
    formatHumanReadableDateKey(todayKey)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = CaloriesStart,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_future_graphs_locked_card"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = CaloriesStart,
          endColor = PureWhite,
        )
        Column {
          Text(
            text = "DEEP ANALYTICS GRAPHS • FUTURE DATE LOCKED",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.2f, 9.0f, 11.6f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Selected date ($humanFutureDate) has not arrived yet",
            color = StatsZinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(9.0f, 7.8f, 10.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(CaloriesStart.copy(alpha = 0.16f))
          .border(1.dp, CaloriesStart.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = "LOCKED",
          color = CaloriesStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0B0C11))
        .border(1.dp, StatsZinc850, RoundedCornerShape(12.dp))
        .padding(horizontal = 16.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Sculpted 3D Padlock Emblem
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(13.dp))
          .background(Color(0xFF14151C))
          .border(1.dp, CaloriesStart.copy(alpha = 0.65f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(22.dp)) {
          val w = size.width
          val h = size.height
          // Shackle arc
          drawArc(
            color = PureWhite,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.10f),
            size = Size(w * 0.52f, h * 0.48f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          )
          // Lock body
          drawRoundRect(
            color = CaloriesStart,
            topLeft = Offset(w * 0.16f, h * 0.44f),
            size = Size(w * 0.68f, h * 0.48f),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
          )
          // Keyhole dot
          drawCircle(
            color = ObsidianBlack,
            radius = 2.2.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.66f),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = "Graphs Locked for $humanFutureDate",
        color = PureWhite,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(14.5f, 12.8f, 16.5f),
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "3-D Clustered Column, Line with Markers, and 3-D Pie charts only render up to Today ($humanTodayDate). Future calendar dates remain locked until updated on that day.",
        color = StatsZinc400,
        fontFamily = InterFontFamily,
        fontSize = metrics.spScaled(10.2f, 9.0f, 11.4f),
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf("3-D COLUMN", "LINE MARKERS", "3-D PIE").forEach { graphTitle ->
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(7.dp))
              .background(StatsZinc950)
              .border(1.dp, StatsZinc800, RoundedCornerShape(7.dp))
              .padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "🔒 $graphTitle",
              color = StatsZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
              maxLines = 1,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Brush.horizontalGradient(listOf(PureWhite, Color(0xFFE4E4E7))))
          .clickable(role = Role.Button, onClick = onJumpBackToToday)
          .padding(vertical = 10.dp, horizontal = 14.dp)
          .testTag("stats_future_locked_jump_today_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "RETURN TO TODAY'S LIVE GRAPHS ($humanTodayDate)",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          letterSpacing = 0.5.sp,
          maxLines = 1,
        )
      }
    }
  }
}

// ============================================================================
// 4. GRAPH #1: 3-D CLUSTERED COLUMN CHART (Matching User Reference Image 1)
// Renders Isometric 3-D Extruded Columns (Front Face + Right Depth Wall + Top Face)
// comparing Actual (Blue/Category Color) vs Protocol Target (Amber/Orange) side-by-side.
// ============================================================================

@Composable
private fun Stats3DClusteredColumnChartCard(
  category: StatsCategoryOption,
  timeRange: StatsTimeRangeOption,
  buckets: List<StatsChartBucket>,
  selectedBucketIndex: Int,
  onSelectBucketIndex: (Int) -> Unit,
  animFraction: Float,
  numberFormat: NumberFormat,
  metrics: StatsResponsiveMetrics,
) {
  val inspectedBucket = buckets.getOrNull(selectedBucketIndex) ?: buckets.lastOrNull()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = category.startColor,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_3d_clustered_column_card"),
  ) {
    // Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = category.startColor,
          endColor = CaloriesMid,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "3-D CLUSTERED COLUMN • ${category.shortLabel}",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Tap any 3-D column pair to inspect Actual vs Target",
            color = StatsZinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(9.0f, 7.8f, 10.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Legend Pills (Actual vs Target matching reference image 1 Blue + Orange 3D columns)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(category.startColor),
          )
          Text(
            text = "ACTUAL",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
            maxLines = 1,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Color(0xFFF97316)),
          )
          Text(
            text = "TARGET",
            color = StatsZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Inspected Bucket Callout Banner
    if (inspectedBucket != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(9.dp))
          .background(StatsZinc950)
          .border(1.dp, StatsZinc800, RoundedCornerShape(9.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${inspectedBucket.label} (${inspectedBucket.subLabel})",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(9.4f, 8.2f, 10.6f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(0.42f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${numberFormat.format(inspectedBucket.actualValue)} / ${numberFormat.format(inspectedBucket.targetValue)} ${category.unitLabel} (${inspectedBucket.completionPercent}%)",
          color = category.startColor,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(9.2f, 8.0f, 10.4f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.End,
          modifier = Modifier.weight(0.58f),
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    // 3-D Clustered Column Canvas
    val chartHeight = if (metrics.isTabletOrWide) 220.dp else 188.dp
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(chartHeight)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0B0C11))
        .border(1.dp, StatsZinc850, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      val maxVal = remember(buckets) {
        val peak = buckets.maxOfOrNull { max(it.actualValue, it.targetValue) } ?: 100
        (peak * 1.16f).coerceAtLeast(10f)
      }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(buckets) {
            detectTapGestures { tapOffset ->
              if (buckets.isNotEmpty() && size.width > 0) {
                val leftPad = 26.dp.toPx()
                val usableW = (size.width - leftPad - 12.dp.toPx()).coerceAtLeast(1f)
                val relX = (tapOffset.x - leftPad).coerceIn(0f, usableW - 1f)
                val idx = ((relX / usableW) * buckets.size).toInt().coerceIn(0, buckets.lastIndex)
                onSelectBucketIndex(idx)
              }
            }
          },
      ) {
        val leftAxisX = 24.dp.toPx()
        val bottomAxisY = size.height - 24.dp.toPx()
        val topAxisY = 18.dp.toPx()
        val rightAxisX = size.width - 12.dp.toPx()
        val chartW = (rightAxisX - leftAxisX).coerceAtLeast(10f)
        val chartH = (bottomAxisY - topAxisY).coerceAtLeast(10f)

        val depthDx = 6.5.dp.toPx()
        val depthDy = 5.5.dp.toPx()

        // 1. Draw 3-D Isometric Axes (Y vertical axis, X horizontal axis, Z diagonal depth axis)
        val axisColor = StatsZinc600
        // Horizontal grid planes with 3D depth offset
        for (step in 0..3) {
          val frac = step / 3f
          val y = bottomAxisY - chartH * frac
          drawLine(
            color = StatsZinc800.copy(alpha = 0.65f),
            start = Offset(leftAxisX, y),
            end = Offset(leftAxisX + depthDx, y - depthDy),
            strokeWidth = 1.dp.toPx(),
          )
          drawLine(
            color = StatsZinc800.copy(alpha = 0.65f),
            start = Offset(leftAxisX + depthDx, y - depthDy),
            end = Offset(rightAxisX, y - depthDy),
            strokeWidth = 1.dp.toPx(),
          )
        }

        // Y-Axis vertical line + top arrow
        drawLine(
          color = axisColor,
          start = Offset(leftAxisX, bottomAxisY),
          end = Offset(leftAxisX, topAxisY - 6.dp.toPx()),
          strokeWidth = 1.4.dp.toPx(),
        )
        // X-Axis horizontal line + right arrow
        drawLine(
          color = axisColor,
          start = Offset(leftAxisX, bottomAxisY),
          end = Offset(rightAxisX + 4.dp.toPx(), bottomAxisY),
          strokeWidth = 1.4.dp.toPx(),
        )
        // Z-Axis diagonal depth line (bottom-left matching reference image 1)
        drawLine(
          color = axisColor,
          start = Offset(leftAxisX, bottomAxisY),
          end = Offset(leftAxisX - depthDx * 1.6f, bottomAxisY + depthDy * 1.6f),
          strokeWidth = 1.4.dp.toPx(),
        )

        // 2. Draw Clustered 3-D Columns per Bucket (Actual Column + Target Column)
        val count = buckets.size.coerceAtLeast(1)
        val slotW = chartW / count.toFloat()
        val barW = (slotW * 0.28f).coerceIn(5.dp.toPx(), 18.dp.toPx())
        val pairGap = (slotW * 0.06f).coerceIn(1.5.dp.toPx(), 4.dp.toPx())

        val actualFrontTop = category.startColor
        val actualFrontBottom = category.midColor
        val actualSideColor = category.endColor.copy(alpha = 0.88f)
        val actualRoofColor = PureWhite.copy(alpha = 0.35f)

        val targetFrontTop = Color(0xFFFB923C)
        val targetFrontBottom = Color(0xFFEA580C)
        val targetSideColor = Color(0xFF9A3412)

        buckets.forEachIndexed { idx, bucket ->
          val slotCenterX = leftAxisX + slotW * (idx + 0.5f)
          val isSelected = idx == selectedBucketIndex

          if (isSelected) {
            drawRoundRect(
              color = PureWhite.copy(alpha = 0.06f),
              topLeft = Offset(leftAxisX + slotW * idx + 2f, topAxisY - depthDy),
              size = Size(slotW - 4f, chartH + depthDy),
              cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            )
          }

          val actualFrac = ((bucket.actualValue / maxVal) * animFraction).coerceIn(0.03f, 1f)
          val targetFrac = ((bucket.targetValue / maxVal) * animFraction).coerceIn(0.03f, 1f)

          val actualH = (chartH * actualFrac).coerceAtLeast(4.dp.toPx())
          val targetH = (chartH * targetFrac).coerceAtLeast(4.dp.toPx())

          val actualX = slotCenterX - barW - pairGap * 0.5f
          val targetX = slotCenterX + pairGap * 0.5f

          // Draw Column 1: Actual 3-D Extruded Column
          draw3DExtrudedColumn(
            x = actualX,
            bottomY = bottomAxisY,
            width = barW,
            height = actualH,
            dx = depthDx * 0.78f,
            dy = depthDy * 0.78f,
            frontTopColor = actualFrontTop,
            frontBottomColor = actualFrontBottom,
            sideColor = actualSideColor,
            topColor = actualFrontTop,
            topHighlightOverlay = actualRoofColor,
            isSelected = isSelected,
          )

          // Draw Column 2: Target 3-D Extruded Column
          draw3DExtrudedColumn(
            x = targetX,
            bottomY = bottomAxisY,
            width = barW,
            height = targetH,
            dx = depthDx * 0.78f,
            dy = depthDy * 0.78f,
            frontTopColor = targetFrontTop,
            frontBottomColor = targetFrontBottom,
            sideColor = targetSideColor,
            topColor = Color(0xFFFDBA74),
            topHighlightOverlay = PureWhite.copy(alpha = 0.22f),
            isSelected = isSelected,
          )
        }
      }

      // X-Axis Bucket Labels along the bottom
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(start = 24.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        val stepStride = if (buckets.size > 10) 2 else 1
        buckets.forEachIndexed { index, bucket ->
          if (index % stepStride == 0) {
            Text(
              text = bucket.label,
              color = if (index == selectedBucketIndex) PureWhite else StatsZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = if (index == selectedBucketIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
              fontSize = metrics.spScaled(8.0f, 7.0f, 9.2f),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.draw3DExtrudedColumn(
  x: Float,
  bottomY: Float,
  width: Float,
  height: Float,
  dx: Float,
  dy: Float,
  frontTopColor: Color,
  frontBottomColor: Color,
  sideColor: Color,
  topColor: Color,
  topHighlightOverlay: Color,
  isSelected: Boolean,
) {
  val topY = bottomY - height

  // 1. Right 3-D Depth Wall
  val rightWallPath = Path().apply {
    moveTo(x + width, topY)
    lineTo(x + width + dx, topY - dy)
    lineTo(x + width + dx, bottomY - dy)
    lineTo(x + width, bottomY)
    close()
  }
  drawPath(path = rightWallPath, color = sideColor)

  // 2. Top 3-D Roof Face
  val topRoofPath = Path().apply {
    moveTo(x, topY)
    lineTo(x + dx, topY - dy)
    lineTo(x + width + dx, topY - dy)
    lineTo(x + width, topY)
    close()
  }
  drawPath(path = topRoofPath, color = topColor)
  drawPath(path = topRoofPath, color = topHighlightOverlay)

  // 3. Front Primary Face
  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(frontTopColor, frontBottomColor),
      startY = topY,
      endY = bottomY,
    ),
    topLeft = Offset(x, topY),
    size = Size(width, height),
  )

  if (isSelected) {
    drawRect(
      color = PureWhite,
      topLeft = Offset(x, topY),
      size = Size(width, height),
      style = Stroke(width = 1.2.dp.toPx()),
    )
  }
}

// ============================================================================
// 5. GRAPH #2: LINE WITH MARKERS PROGRESS TREND CHART (Matching Reference Image 3)
// Renders Dual Progression Lines with Glowing Square Markers (Actual vs Target/Trend).
// ============================================================================

@Composable
private fun StatsLineWithMarkersChartCard(
  category: StatsCategoryOption,
  timeRange: StatsTimeRangeOption,
  buckets: List<StatsChartBucket>,
  selectedBucketIndex: Int,
  onSelectBucketIndex: (Int) -> Unit,
  animFraction: Float,
  numberFormat: NumberFormat,
  metrics: StatsResponsiveMetrics,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = StepsStart,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_line_with_markers_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = category.startColor,
          endColor = StepsStart,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "LINE WITH MARKERS • PROGRESS TREND",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Actual trajectory markers vs Target benchmark line",
            color = StatsZinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(9.0f, 7.8f, 10.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .background(category.startColor),
          )
          Text(
            text = "ACTUAL",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
            maxLines = 1,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .background(Color(0xFF84CC16)),
          )
          Text(
            text = "TARGET",
            color = StatsZinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    val chartHeight = if (metrics.isTabletOrWide) 205.dp else 176.dp
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(chartHeight)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0B0C11))
        .border(1.dp, StatsZinc850, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
      val maxVal = remember(buckets) {
        val peak = buckets.maxOfOrNull { max(it.actualValue, it.targetValue) } ?: 100
        (peak * 1.18f).coerceAtLeast(10f)
      }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(buckets) {
            detectTapGestures { tapOffset ->
              if (buckets.isNotEmpty() && size.width > 0) {
                val leftPad = 20.dp.toPx()
                val usableW = (size.width - leftPad - 12.dp.toPx()).coerceAtLeast(1f)
                val relX = (tapOffset.x - leftPad).coerceIn(0f, usableW - 1f)
                val idx = ((relX / usableW) * buckets.size).toInt().coerceIn(0, buckets.lastIndex)
                onSelectBucketIndex(idx)
              }
            }
          },
      ) {
        val leftX = 20.dp.toPx()
        val rightX = size.width - 14.dp.toPx()
        val topY = 14.dp.toPx()
        val bottomY = size.height - 22.dp.toPx()
        val plotW = (rightX - leftX).coerceAtLeast(10f)
        val plotH = (bottomY - topY).coerceAtLeast(10f)

        // Axes & Horizontal Gridlines
        for (i in 0..3) {
          val y = bottomY - plotH * (i / 3f)
          drawLine(
            color = StatsZinc800.copy(alpha = 0.65f),
            start = Offset(leftX, y),
            end = Offset(rightX, y),
            strokeWidth = 1.dp.toPx(),
          )
        }
        drawLine(
          color = StatsZinc600,
          start = Offset(leftX, bottomY),
          end = Offset(leftX, topY - 4.dp.toPx()),
          strokeWidth = 1.4.dp.toPx(),
        )
        drawLine(
          color = StatsZinc600,
          start = Offset(leftX, bottomY),
          end = Offset(rightX + 4.dp.toPx(), bottomY),
          strokeWidth = 1.4.dp.toPx(),
        )

        val n = buckets.size
        if (n > 0) {
          val stepX = if (n > 1) plotW / (n - 1).toFloat() else 0f

          val actualPoints = buckets.mapIndexed { idx, b ->
            val x = if (n > 1) leftX + stepX * idx else leftX + plotW * 0.5f
            val yFrac = ((b.actualValue / maxVal) * animFraction).coerceIn(0.02f, 1f)
            Offset(x, bottomY - plotH * yFrac)
          }
          val targetPoints = buckets.mapIndexed { idx, b ->
            val x = if (n > 1) leftX + stepX * idx else leftX + plotW * 0.5f
            val yFrac = ((b.targetValue / maxVal) * animFraction).coerceIn(0.02f, 1f)
            Offset(x, bottomY - plotH * yFrac)
          }

          // Area fill under Actual Line
          if (actualPoints.size >= 2) {
            val fillPath = Path().apply {
              moveTo(actualPoints.first().x, bottomY)
              actualPoints.forEach { pt -> lineTo(pt.x, pt.y) }
              lineTo(actualPoints.last().x, bottomY)
              close()
            }
            drawPath(
              path = fillPath,
              brush = Brush.verticalGradient(
                colors = listOf(
                  category.startColor.copy(alpha = 0.24f),
                  Color.Transparent,
                ),
                startY = topY,
                endY = bottomY,
              ),
            )
          }

          // Target Line (Lime/Emerald series from reference image 3)
          val targetSeriesColor = Color(0xFF84CC16)
          if (targetPoints.size >= 2) {
            val targetPath = Path().apply {
              moveTo(targetPoints.first().x, targetPoints.first().y)
              for (i in 1 until targetPoints.size) {
                lineTo(targetPoints[i].x, targetPoints[i].y)
              }
            }
            drawPath(
              path = targetPath,
              color = targetSeriesColor.copy(alpha = 0.85f),
              style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          }

          // Actual Line (Primary Category / Cyan-Blue series from reference image 3)
          if (actualPoints.size >= 2) {
            val actualPath = Path().apply {
              moveTo(actualPoints.first().x, actualPoints.first().y)
              for (i in 1 until actualPoints.size) {
                lineTo(actualPoints[i].x, actualPoints[i].y)
              }
            }
            drawPath(
              path = actualPath,
              color = category.startColor,
              style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
          }

          // Square Markers on both series (matching "Line with Markers" reference image 3)
          val markerSide = 6.5.dp.toPx()
          val halfM = markerSide / 2f

          targetPoints.forEach { pt ->
            drawRect(
              color = targetSeriesColor,
              topLeft = Offset(pt.x - halfM * 0.85f, pt.y - halfM * 0.85f),
              size = Size(markerSide * 0.85f, markerSide * 0.85f),
            )
            drawRect(
              color = ObsidianBlack,
              topLeft = Offset(pt.x - halfM * 0.85f, pt.y - halfM * 0.85f),
              size = Size(markerSide * 0.85f, markerSide * 0.85f),
              style = Stroke(width = 0.9.dp.toPx()),
            )
          }

          actualPoints.forEachIndexed { idx, pt ->
            val isSelected = idx == selectedBucketIndex
            val scaleM = if (isSelected) 1.35f else 1f
            val mSize = markerSide * scaleM
            drawRect(
              color = if (isSelected) PureWhite else category.startColor,
              topLeft = Offset(pt.x - mSize / 2f, pt.y - mSize / 2f),
              size = Size(mSize, mSize),
            )
            drawRect(
              color = PureWhite,
              topLeft = Offset(pt.x - mSize / 2f, pt.y - mSize / 2f),
              size = Size(mSize, mSize),
              style = Stroke(width = 1.dp.toPx()),
            )
          }
        }
      }

      // Bottom Labels
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(start = 18.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val stepStride = if (buckets.size > 10) 2 else 1
        buckets.forEachIndexed { index, bucket ->
          if (index % stepStride == 0) {
            Text(
              text = bucket.label,
              color = if (index == selectedBucketIndex) PureWhite else StatsZinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = if (index == selectedBucketIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
              fontSize = metrics.spScaled(8.0f, 7.0f, 9.2f),
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

// ============================================================================
// 6. GRAPH #3: 3-D PIE CHART (Matching Reference Image 2)
// Renders Isometric Tilted 3-D Extruded Pie Chart with Cylinder Depth & Exploded Slices,
// paired with a symmetrical 2x3 6-Pillar Bento Legend Grid so no text or badge ever clips.
// ============================================================================

@Composable
private fun Stats3DPieChartCard(
  category: StatsCategoryOption,
  slices: List<StatsPieSlice>,
  timeRange: StatsTimeRangeOption,
  selectedDateKey: String,
  animFraction: Float,
  ambientPhase: Float,
  metrics: StatsResponsiveMetrics,
) {
  var selectedSliceIndex by remember(category, slices.size) { mutableIntStateOf(0) }
  val activeSlice = slices.getOrNull(selectedSliceIndex) ?: slices.firstOrNull()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = category.startColor,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_3d_pie_card"),
  ) {
    // Header Row with weighted left column so right badge never squishes vertically
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = category.startColor,
          endColor = PureWhite,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (category == StatsCategoryOption.ALL) {
              "3-D PIE • ALL 6 CATEGORIES SHARE"
            } else {
              "3-D PIE • ${category.shortLabel} & ALL-PILLAR SHARE"
            },
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "6-pillar distribution (Steps • Kcal • Pro • Carb • Fat • H₂O)",
            color = StatsZinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(9.0f, 7.8f, 10.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(StatsZinc950)
          .border(1.dp, StatsZinc800, RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = "${slices.size} SLICES",
          color = category.startColor,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
          maxLines = 1,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0B0C11))
        .border(1.dp, StatsZinc850, RoundedCornerShape(12.dp))
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Stage: 3-D Isometric Extruded Pie Canvas + Selected Slice Spotlight
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .weight(1.1f)
            .height(if (metrics.isTabletOrWide) 156.dp else 138.dp)
            .clickable {
              if (slices.isNotEmpty()) {
                selectedSliceIndex = (selectedSliceIndex + 1) % slices.size
              }
            },
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.50f
            val cy = size.height * 0.44f
            val rx = min(size.width * 0.43f, size.height * 0.58f).coerceAtLeast(32f)
            val ry = (rx * 0.58f).coerceAtLeast(20f) // Isometric 3-D tilt aspect ratio
            val extrusionDepth = 16.dp.toPx()

            val totalRaw = slices.sumOf { it.rawValue.toDouble() }.toFloat().coerceAtLeast(1f)

            // Pass 1: Draw 3-D Extruded Cylinder Depth Walls
            val depthLayers = 14
            for (layer in depthLayers downTo 1) {
              val layerYOffset = (layer.toFloat() / depthLayers.toFloat()) * extrusionDepth
              var startDeg = -35f
              slices.forEachIndexed { idx, slice ->
                val sweepDeg = ((slice.rawValue / totalRaw) * 360f * animFraction).coerceAtLeast(4f)
                val midRad = ((startDeg + sweepDeg * 0.5f) * PI / 180.0)
                val explodeDist = if (idx == selectedSliceIndex) 5.5.dp.toPx() else 2.2.dp.toPx()
                val ox = (cos(midRad) * explodeDist).toFloat()
                val oy = (sin(midRad) * explodeDist * 0.58f).toFloat()

                drawArc(
                  color = slice.sideColor,
                  startAngle = startDeg,
                  sweepAngle = (sweepDeg - 1.8f).coerceAtLeast(2f),
                  useCenter = true,
                  topLeft = Offset(cx - rx + ox, cy - ry + oy + layerYOffset),
                  size = Size(rx * 2f, ry * 2f),
                )
                startDeg += sweepDeg
              }
            }

            // Pass 2: Draw Specular Top 3-D Pie Faces
            var topStartDeg = -35f
            slices.forEachIndexed { idx, slice ->
              val sweepDeg = ((slice.rawValue / totalRaw) * 360f * animFraction).coerceAtLeast(4f)
              val midRad = ((topStartDeg + sweepDeg * 0.5f) * PI / 180.0)
              val explodeDist = if (idx == selectedSliceIndex) 5.5.dp.toPx() else 2.2.dp.toPx()
              val ox = (cos(midRad) * explodeDist).toFloat()
              val oy = (sin(midRad) * explodeDist * 0.58f).toFloat()

              drawArc(
                color = slice.topColor,
                startAngle = topStartDeg,
                sweepAngle = (sweepDeg - 1.8f).coerceAtLeast(2f),
                useCenter = true,
                topLeft = Offset(cx - rx + ox, cy - ry + oy),
                size = Size(rx * 2f, ry * 2f),
              )
              drawArc(
                color = PureWhite.copy(alpha = if (idx == selectedSliceIndex) 0.6f else 0.24f),
                startAngle = topStartDeg,
                sweepAngle = (sweepDeg - 1.8f).coerceAtLeast(2f),
                useCenter = true,
                topLeft = Offset(cx - rx + ox, cy - ry + oy),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = if (idx == selectedSliceIndex) 1.5.dp.toPx() else 0.8.dp.toPx()),
              )
              topStartDeg += sweepDeg
            }
          }
        }

        // Right: Selected Slice Live Spotlight Panel
        if (activeSlice != null) {
          Column(
            modifier = Modifier
              .weight(0.9f)
              .clip(RoundedCornerShape(10.dp))
              .background(StatsZinc950)
              .border(1.dp, activeSlice.topColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(activeSlice.topColor),
              )
              Text(
                text = "INSPECTED SLICE",
                color = StatsZinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
                maxLines = 1,
              )
            }
            Text(
              text = activeSlice.label.uppercase(),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(12.2f, 10.6f, 13.8f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${activeSlice.sharePercent}% SHARE",
              color = activeSlice.topColor,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(15.5f, 13.5f, 17.5f),
              maxLines = 1,
            )
            Text(
              text = activeSlice.valueText,
              color = StatsZinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Bottom Stage: Symmetrical 2-Column x 3-Row Bento Grid for all 6 Categories
      val slicePairs = slices.chunked(2)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        slicePairs.forEachIndexed { rowIdx, pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            pair.forEachIndexed { colIdx, slice ->
              val globalIdx = rowIdx * 2 + colIdx
              val isSelected = globalIdx == selectedSliceIndex
              Row(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) StatsZinc900 else StatsZinc950)
                  .border(
                    width = 1.dp,
                    color = if (isSelected) slice.topColor.copy(alpha = 0.78f) else StatsZinc850,
                    shape = RoundedCornerShape(8.dp),
                  )
                  .clickable { selectedSliceIndex = globalIdx }
                  .padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(RoundedCornerShape(2.5.dp))
                      .background(slice.topColor),
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = slice.label,
                      color = PureWhite,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = metrics.spScaled(9.8f, 8.6f, 11.0f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = slice.valueText,
                      color = StatsZinc400,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${slice.sharePercent}%",
                  color = slice.topColor,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = metrics.spScaled(10.6f, 9.2f, 12.0f),
                  maxLines = 1,
                )
              }
            }
            if (pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

// ============================================================================
// 7. INTERACTIVE CUSTOM CALENDAR & FULL DAY TIMELINE DEEP-DIVE SECTION
// ============================================================================

@Composable
private fun StatsCustomCalendarAndDayTimelineSection(
  calendarYear: Int,
  calendarMonthZeroBased: Int,
  loginStartYear: Int,
  loginStartMonthZeroBased: Int,
  loginStartDayOfMonth: Int,
  loginStartDayKey: String,
  selectedDateKey: String,
  todayKey: String,
  dailyHistoryMap: Map<String, DailyHistoricalRecord>,
  inspectedRecord: DailyHistoricalRecord,
  savedQuickItems: List<SavedQuickNutritionItem>,
  streakThresholdPercent: Int,
  onNavigateMonth: (Int) -> Unit,
  onSelectYearMonthDay: (Int, Int, Int) -> Unit,
  onJumpToToday: () -> Unit,
  onSelectDateKey: (String) -> Unit,
  onQuickLogEntryToSelectedDate: (DashboardTimelineEntry) -> Unit,
  onDeleteEntryFromSelectedDate: (DashboardTimelineEntry) -> Unit,
  numberFormat: NumberFormat,
  metrics: StatsResponsiveMetrics,
) {
  val monthName = StatsMonthShortNames.getOrElse(calendarMonthZeroBased.coerceIn(0, 11)) { "NOV" }
  val canGoPrevMonth = calendarYear > loginStartYear ||
    (calendarYear == loginStartYear && calendarMonthZeroBased > loginStartMonthZeroBased)
  val isSelectedDateToday = selectedDateKey == todayKey
  val isSelectedDateFuture = selectedDateKey > todayKey

  val selectedCal = remember(selectedDateKey) { parseDayKeyToCalendar(selectedDateKey) }
  val selectedDayOfMonth = selectedCal.get(Calendar.DAY_OF_MONTH)

  var activeSelectorMode by remember { mutableStateOf<String?>(null) } // "YEAR", "MONTH", "DATE"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = metrics.cardCornerRadius,
        topRimTint = HydrationStart,
      )
      .padding(metrics.cardPadding)
      .testTag("stats_custom_calendar_section"),
  ) {
    // 1. Calendar Header Row (Weighted Left Title + Horizontal TODAY Pill on Right — never squishes)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(startColor = HydrationStart, endColor = StepsStart)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "ACTIVE & FUTURE CALENDAR • SINCE LOGIN",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(10.2f, 9.0f, 11.6f),
            letterSpacing = 0.7.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "Locked from login (${formatHumanReadableDateKey(loginStartDayKey)}) onward",
            color = StatsZinc400,
            fontFamily = InterFontFamily,
            fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (selectedDateKey != todayKey) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(PureWhite, Color(0xFFE4E4E7))))
            .clickable(role = Role.Button, onClick = onJumpToToday)
            .padding(horizontal = 10.dp, vertical = 5.5.dp)
            .testTag("calendar_jump_today_btn"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "↺ TODAY",
            color = ObsidianBlack,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 1A. Dedicated Symmetrical Month & Year Navigation Bar (< PREV | MONTH YEAR | NEXT >)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(StatsZinc950)
        .border(1.dp, StatsZinc850, RoundedCornerShape(10.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(if (canGoPrevMonth) StatsZinc900 else Color(0xFF0E0F14))
          .border(1.dp, if (canGoPrevMonth) StatsZinc700 else StatsZinc850, RoundedCornerShape(7.dp))
          .clickable(enabled = canGoPrevMonth) { onNavigateMonth(-1) }
          .padding(horizontal = 10.dp, vertical = 4.5.dp)
          .testTag("calendar_prev_month_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "‹ PREV",
          color = if (canGoPrevMonth) PureWhite else StatsZinc600,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
          maxLines = 1,
        )
      }

      Text(
        text = "$monthName $calendarYear",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.spScaled(11.2f, 9.8f, 12.6f),
        letterSpacing = 0.8.sp,
        maxLines = 1,
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(StatsZinc900)
          .border(1.dp, StatsZinc700, RoundedCornerShape(7.dp))
          .clickable { onNavigateMonth(1) }
          .padding(horizontal = 10.dp, vertical = 4.5.dp)
          .testTag("calendar_next_month_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "NEXT ›",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
          maxLines = 1,
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 1B. Direct YEAR • MONTH • DATE Selector Strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      val pickerPillModifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(9.dp))
        .background(StatsZinc950)

      // YEAR selector button
      Row(
        modifier = pickerPillModifier
          .border(
            width = 1.dp,
            color = if (activeSelectorMode == "YEAR") PureWhite else StatsZinc800,
            shape = RoundedCornerShape(9.dp),
          )
          .clickable {
            activeSelectorMode = if (activeSelectorMode == "YEAR") null else "YEAR"
          }
          .padding(horizontal = 9.dp, vertical = 7.dp)
          .testTag("calendar_select_year_pill"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "YEAR",
          color = StatsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          maxLines = 1,
        )
        Text(
          text = "$calendarYear ▾",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(9.8f, 8.6f, 11.0f),
          maxLines = 1,
        )
      }

      // MONTH selector button
      Row(
        modifier = pickerPillModifier
          .border(
            width = 1.dp,
            color = if (activeSelectorMode == "MONTH") PureWhite else StatsZinc800,
            shape = RoundedCornerShape(9.dp),
          )
          .clickable {
            activeSelectorMode = if (activeSelectorMode == "MONTH") null else "MONTH"
          }
          .padding(horizontal = 9.dp, vertical = 7.dp)
          .testTag("calendar_select_month_pill"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "MONTH",
          color = StatsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          maxLines = 1,
        )
        Text(
          text = "$monthName ▾",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(9.8f, 8.6f, 11.0f),
          maxLines = 1,
        )
      }

      // DATE selector button
      Row(
        modifier = pickerPillModifier
          .border(
            width = 1.dp,
            color = if (activeSelectorMode == "DATE") PureWhite else StatsZinc800,
            shape = RoundedCornerShape(9.dp),
          )
          .clickable {
            activeSelectorMode = if (activeSelectorMode == "DATE") null else "DATE"
          }
          .padding(horizontal = 9.dp, vertical = 7.dp)
          .testTag("calendar_select_date_pill"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "DATE",
          color = StatsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          maxLines = 1,
        )
        Text(
          text = "D-${selectedDayOfMonth.toString().padStart(2, '0')} ▾",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(9.8f, 8.6f, 11.0f),
          maxLines = 1,
        )
      }
    }

    // Expandable Non-Clipped Multi-Column Grid Picker for Year / Month / Date (Clamped to Login Date onward)
    if (activeSelectorMode != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF0E0F14))
          .border(1.dp, StatsZinc800, RoundedCornerShape(10.dp))
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        when (activeSelectorMode) {
          "YEAR" -> {
            val yearList = (loginStartYear..(loginStartYear + 5)).toList()
            yearList.chunked(3).forEach { rowYears ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                rowYears.forEach { yr ->
                  val isSel = yr == calendarYear
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(7.dp))
                      .background(if (isSel) PureWhite else StatsZinc900)
                      .border(0.9.dp, if (isSel) PureWhite else StatsZinc700, RoundedCornerShape(7.dp))
                      .clickable {
                        onSelectYearMonthDay(yr, calendarMonthZeroBased, selectedDayOfMonth)
                        activeSelectorMode = null
                      }
                      .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = yr.toString(),
                      color = if (isSel) ObsidianBlack else PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 10.2.sp,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }
          "MONTH" -> {
            val minMonth = if (calendarYear == loginStartYear) loginStartMonthZeroBased else 0
            val availableMonths = (minMonth..11).toList()
            availableMonths.chunked(4).forEach { rowMonths ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                rowMonths.forEach { mIdx ->
                  val isSel = mIdx == calendarMonthZeroBased
                  val mName = StatsMonthShortNames.getOrElse(mIdx) { "M" }
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(7.dp))
                      .background(if (isSel) PureWhite else StatsZinc900)
                      .border(0.9.dp, if (isSel) PureWhite else StatsZinc700, RoundedCornerShape(7.dp))
                      .clickable {
                        onSelectYearMonthDay(calendarYear, mIdx, selectedDayOfMonth)
                        activeSelectorMode = null
                      }
                      .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = mName,
                      color = if (isSel) ObsidianBlack else PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 10.0.sp,
                      maxLines = 1,
                    )
                  }
                }
                repeat((4 - rowMonths.size).coerceAtLeast(0)) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
          "DATE" -> {
            val monthCal = Calendar.getInstance().apply {
              set(Calendar.YEAR, calendarYear)
              set(Calendar.MONTH, calendarMonthZeroBased)
              set(Calendar.DAY_OF_MONTH, 1)
            }
            val maxDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val minDay = if (calendarYear == loginStartYear && calendarMonthZeroBased == loginStartMonthZeroBased) {
              loginStartDayOfMonth
            } else {
              1
            }
            val availableDays = (minDay..maxDays).toList()
            availableDays.chunked(7).forEach { rowDays ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                rowDays.forEach { dNum ->
                  val dKey = formatYearMonthDayKey(calendarYear, calendarMonthZeroBased, dNum)
                  val isSel = dKey == selectedDateKey
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(7.dp))
                      .background(if (isSel) PureWhite else StatsZinc900)
                      .border(0.9.dp, if (isSel) PureWhite else StatsZinc700, RoundedCornerShape(7.dp))
                      .clickable {
                        onSelectYearMonthDay(calendarYear, calendarMonthZeroBased, dNum)
                        activeSelectorMode = null
                      }
                      .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = dNum.toString().padStart(2, '0'),
                      color = if (isSel) ObsidianBlack else PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 9.8.sp,
                      maxLines = 1,
                    )
                  }
                }
                repeat((7 - rowDays.size).coerceAtLeast(0)) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. 7-Day Weekday Header Strip
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      StatsWeekdayShortNames.forEach { wd ->
        Text(
          text = wd,
          color = StatsZinc500,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.4f, 7.4f, 9.4f),
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 3. Interactive Month Calendar Grid (Days prior to loginStartDayKey are hidden/blanked!)
    val calendarCells = remember(calendarYear, calendarMonthZeroBased) {
      buildMonthCalendarCells(calendarYear, calendarMonthZeroBased)
    }
    val rows = calendarCells.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      rows.forEach { weekRow ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          weekRow.forEach { dayNumberOrNull ->
            val cellKey = if (dayNumberOrNull != null) {
              formatYearMonthDayKey(calendarYear, calendarMonthZeroBased, dayNumberOrNull)
            } else {
              ""
            }
            // Hide any calendar day prior to the user's login day
            if (dayNumberOrNull == null || cellKey < loginStartDayKey) {
              Spacer(modifier = Modifier.weight(1f).height(36.dp))
            } else {
              val isSelected = cellKey == selectedDateKey
              val isToday = cellKey == todayKey
              val dayRec = dailyHistoryMap[cellKey]
              val hasData = dayRec != null && (
                dayRec.caloriesKcal > 0 ||
                  dayRec.hydrationMl > 0 ||
                  dayRec.stepsCount > 0 ||
                  dayRec.timelineEntries.isNotEmpty()
                )
              val metStreak = dayRec?.streakQualified == true

              val dotColor = when {
                metStreak -> StepsStart
                hasData -> CaloriesStart
                isToday -> HydrationStart
                else -> Color.Transparent
              }

              Column(
                modifier = Modifier
                  .weight(1f)
                  .height(36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    when {
                      isSelected -> PureWhite
                      isToday -> Color(0xFF1E2230)
                      hasData -> Color(0xFF151720)
                      else -> StatsZinc950
                    },
                  )
                  .border(
                    width = 1.dp,
                    color = when {
                      isSelected -> PureWhite
                      isToday -> HydrationStart.copy(alpha = 0.7f)
                      hasData -> StatsZinc700
                      else -> StatsZinc850
                    },
                    shape = RoundedCornerShape(8.dp),
                  )
                  .clickable { onSelectDateKey(cellKey) }
                  .testTag("calendar_day_cell_$cellKey"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Text(
                  text = dayNumberOrNull.toString(),
                  color = if (isSelected) ObsidianBlack else PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                  fontSize = metrics.spScaled(10.2f, 9.0f, 11.4f),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .size(4.5.dp)
                    .clip(CircleShape)
                    .background(if (isSelected && dotColor != Color.Transparent) ObsidianBlack else dotColor),
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = StatsZinc800, thickness = 0.8.dp)
    Spacer(modifier = Modifier.height(12.dp))

    // 4. SELECTED DATE FULL 6-PILLAR TELEMETRY MATRIX
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "DAY SNAPSHOT • ${formatHumanReadableDateKey(selectedDateKey)}",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(10.4f, 9.2f, 11.8f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (inspectedRecord.isWorkoutDay) {
            "Training Day Protocol • H₂O Goal ${numberFormat.format(inspectedRecord.targetHydrationMl)} ml"
          } else {
            "Rest & Recovery Day • H₂O Goal ${numberFormat.format(inspectedRecord.targetHydrationMl)} ml"
          },
          color = if (inspectedRecord.isWorkoutDay) StepsStart else HydrationStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = metrics.spScaled(8.6f, 7.6f, 9.8f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (inspectedRecord.streakQualified && !isSelectedDateFuture) StepsStart.copy(alpha = 0.18f) else StatsZinc900)
          .border(
            width = 1.dp,
            color = if (inspectedRecord.streakQualified && !isSelectedDateFuture) StepsStart else StatsZinc700,
            shape = RoundedCornerShape(6.dp),
          )
          .padding(horizontal = 8.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = when {
            isSelectedDateFuture -> "FUTURE (LOCKED)"
            !isSelectedDateToday -> "PAST (READ-ONLY)"
            inspectedRecord.streakQualified -> "STREAK ≥$streakThresholdPercent% MET"
            else -> "${inspectedRecord.timelineEntries.size} LOGS SAVED"
          },
          color = if (inspectedRecord.streakQualified && !isSelectedDateFuture) StepsStart else StatsZinc300,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
          maxLines = 1,
        )
      }
    }

    Spacer(modifier = Modifier.height(9.dp))

    // 3x2 Pillar Bento Grid for Selected Calendar Day
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CalendarDayPillarMiniTile(
          label = "STEPS",
          actual = numberFormat.format(inspectedRecord.stepsCount),
          target = numberFormat.format(inspectedRecord.targetStepsCount),
          pct = ((inspectedRecord.stepsCount * 100f) / inspectedRecord.targetStepsCount.coerceAtLeast(1)).roundToInt(),
          accentColor = StepsStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        CalendarDayPillarMiniTile(
          label = "CALORIES",
          actual = "${numberFormat.format(inspectedRecord.caloriesKcal)}",
          target = "${numberFormat.format(inspectedRecord.targetCaloriesKcal)} kcal",
          pct = ((inspectedRecord.caloriesKcal * 100f) / inspectedRecord.targetCaloriesKcal.coerceAtLeast(1)).roundToInt(),
          accentColor = CaloriesStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        CalendarDayPillarMiniTile(
          label = "HYDRATION",
          actual = "${numberFormat.format(inspectedRecord.hydrationMl)}",
          target = "${numberFormat.format(inspectedRecord.targetHydrationMl)} ml",
          pct = ((inspectedRecord.hydrationMl * 100f) / inspectedRecord.targetHydrationMl.coerceAtLeast(1)).roundToInt(),
          accentColor = HydrationStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        CalendarDayPillarMiniTile(
          label = "PROTEIN",
          actual = "${inspectedRecord.proteinGrams}g",
          target = "${inspectedRecord.targetProteinGrams}g",
          pct = ((inspectedRecord.proteinGrams * 100f) / inspectedRecord.targetProteinGrams.coerceAtLeast(1)).roundToInt(),
          accentColor = ProteinStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        CalendarDayPillarMiniTile(
          label = "CARBS",
          actual = "${inspectedRecord.carbsGrams}g",
          target = "${inspectedRecord.targetCarbsGrams}g",
          pct = ((inspectedRecord.carbsGrams * 100f) / inspectedRecord.targetCarbsGrams.coerceAtLeast(1)).roundToInt(),
          accentColor = CarbsStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
        CalendarDayPillarMiniTile(
          label = "FATS",
          actual = "${inspectedRecord.fatsGrams}g",
          target = "${inspectedRecord.targetFatsGrams}g",
          pct = ((inspectedRecord.fatsGrams * 100f) / inspectedRecord.targetFatsGrams.coerceAtLeast(1)).roundToInt(),
          accentColor = FatsStart,
          metrics = metrics,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "* Step counter is not fully accurate yet; we will improve it in future updates.",
      color = StatsZinc500,
      fontFamily = InterFontFamily,
      fontSize = metrics.spScaled(8.6f, 7.6f, 9.6f),
      modifier = Modifier.testTag("stats_steps_accuracy_disclaimer"),
    )

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = StatsZinc800, thickness = 0.8.dp)
    Spacer(modifier = Modifier.height(11.dp))

    // 5. SELECTED DAY'S COMPLETE CHRONOLOGICAL TIMELINE (Logging allowed ONLY on todayKey!)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        DashboardSyncedHeaderDot(startColor = PureWhite, endColor = StatsZinc300)
        Text(
          text = "TIMELINE OF ${formatHumanReadableDateKey(selectedDateKey)}",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = metrics.spScaled(10.5f, 9.2f, 11.8f),
          letterSpacing = 0.8.sp,
        )
      }

      if (isSelectedDateToday) {
        // Quick +250ml H₂O button ONLY when viewing Today
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(HydrationStart.copy(alpha = 0.16f))
              .border(1.dp, HydrationStart.copy(alpha = 0.65f), RoundedCornerShape(7.dp))
              .clickable {
                onQuickLogEntryToSelectedDate(
                  DashboardTimelineEntry(
                    type = DashboardEntryType.HYDRATION,
                    title = "Hydration +250 ml",
                    subtitle = "Cellular H₂O",
                    hydrationMl = 250,
                  ),
                )
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("calendar_day_quick_add_water_btn"),
          ) {
            Text(
              text = "+250ml H₂O",
              color = HydrationStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = metrics.spScaled(8.4f, 7.4f, 9.4f),
            )
          }
        }
      } else {
        Text(
          text = if (isSelectedDateFuture) "FUTURE • LOCKED" else "PAST • READ-ONLY",
          color = StatsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
        )
      }
    }

    // 1-tap Saved Quick Items bar ONLY when viewing Today (never allow logging into past or future dates)
    if (isSelectedDateToday && savedQuickItems.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "QUICK LOG:",
          color = StatsZinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
        )
        savedQuickItems.take(8).forEach { savedItem ->
          val itemColor = QuickItemColorAlchemy.argbToColor(savedItem.colorArgb)
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(StatsZinc950)
              .border(1.dp, StatsZinc800, RoundedCornerShape(7.dp))
              .clickable {
                onQuickLogEntryToSelectedDate(
                  DashboardTimelineEntry(
                    type = if (savedItem.isMeal) DashboardEntryType.MEAL else DashboardEntryType.SINGLE_ITEM,
                    title = savedItem.name,
                    subtitle = if (savedItem.componentNames.isNotEmpty()) {
                      savedItem.componentNames.joinToString(" + ")
                    } else {
                      "Calendar Log"
                    },
                    caloriesKcal = savedItem.caloriesKcal,
                    proteinGrams = savedItem.proteinGrams,
                    carbsGrams = savedItem.carbsGrams,
                    fatsGrams = savedItem.fatsGrams,
                    hydrationMl = 0,
                    colorArgb = savedItem.colorArgb,
                    mixedFromColorArgbs = savedItem.mixedFromColorArgbs,
                  ),
                )
              }
              .padding(horizontal = 8.dp, vertical = 4.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(itemColor),
            )
            Text(
              text = "+ ${savedItem.name} (${savedItem.caloriesKcal} kcal)",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
              maxLines = 1,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(9.dp))

    // Timeline Entries List for Selected Calendar Day
    val dayEntries = inspectedRecord.timelineEntries
    if (dayEntries.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(StatsZinc950)
          .border(1.dp, StatsZinc850, RoundedCornerShape(10.dp))
          .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = when {
            isSelectedDateFuture -> "Future date (${formatHumanReadableDateKey(selectedDateKey)}). Logging is locked until this day arrives."
            !isSelectedDateToday -> "No timeline entries were recorded on ${formatHumanReadableDateKey(selectedDateKey)}. Past dates are locked for read-only inspection."
            else -> "No timeline entries recorded today (${formatHumanReadableDateKey(selectedDateKey)}) yet. Tap +250ml H₂O or any Quick Log item above to record."
          },
          color = StatsZinc400,
          fontFamily = InterFontFamily,
          fontSize = metrics.spScaled(10.0f, 8.8f, 11.2f),
          textAlign = TextAlign.Center,
        )
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        dayEntries.forEach { entry ->
          val entryAccent = when {
            entry.colorArgb != 0L -> QuickItemColorAlchemy.argbToColor(entry.colorArgb)
            entry.type == DashboardEntryType.HYDRATION -> HydrationStart
            entry.type == DashboardEntryType.MEAL -> StepsStart
            else -> CaloriesStart
          }
          val compactTypeTag = when (entry.type) {
            DashboardEntryType.HYDRATION -> "H₂O"
            DashboardEntryType.MEAL -> "MEAL"
            DashboardEntryType.SINGLE_ITEM -> "ITEM"
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(StatsZinc950)
              .border(1.dp, StatsZinc850, RoundedCornerShape(10.dp))
              .padding(horizontal = 11.dp, vertical = 9.dp)
              .testTag("stats_calendar_timeline_entry_${entry.id}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
              Box(
                modifier = Modifier
                  .width(3.5.dp)
                  .height(26.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(entryAccent),
              )
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = entry.title,
                    color = PureWhite,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = metrics.spScaled(12.2f, 10.6f, 13.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  Text(
                    text = compactTypeTag,
                    color = entryAccent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = metrics.spScaled(7.8f, 7.0f, 8.8f),
                  )
                }
                val subLine = if (entry.type == DashboardEntryType.HYDRATION) {
                  "+${numberFormat.format(entry.hydrationMl)} ml cellular hydration"
                } else {
                  "+${numberFormat.format(entry.caloriesKcal)} kcal • ${entry.proteinGrams}g P • ${entry.carbsGrams}g C • ${entry.fatsGrams}g F"
                }
                Text(
                  text = subLine,
                  color = StatsZinc300,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = metrics.spScaled(9.4f, 8.2f, 10.4f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = entry.formattedTimeAmPm,
                color = StatsZinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = metrics.spScaled(9.2f, 8.0f, 10.2f),
              )
              if (isSelectedDateToday) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(StatsDangerRed)
                    .border(1.dp, StatsDangerRedBorder, RoundedCornerShape(7.dp))
                    .clickable { onDeleteEntryFromSelectedDate(entry) },
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "×",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarDayPillarMiniTile(
  label: String,
  actual: String,
  target: String,
  pct: Int,
  accentColor: Color,
  metrics: StatsResponsiveMetrics,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(9.dp))
      .background(StatsZinc950)
      .border(1.dp, StatsZinc850, RoundedCornerShape(9.dp))
      .padding(horizontal = 8.dp, vertical = 7.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = label,
        color = StatsZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = metrics.spScaled(7.8f, 6.8f, 8.8f),
        maxLines = 1,
      )
      Text(
        text = "${pct.coerceIn(0, 999)}%",
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.spScaled(8.2f, 7.2f, 9.2f),
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = actual,
      color = PureWhite,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.ExtraBold,
      fontSize = metrics.spScaled(12.2f, 10.5f, 13.8f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "/ $target",
      color = StatsZinc500,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = metrics.spScaled(8.0f, 7.0f, 9.0f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun StatsCalendarQuickGatewayBanner(
  selectedCustomDateKey: String,
  inspectedRecord: DailyHistoricalRecord,
  onOpenCalendarInspector: () -> Unit,
  numberFormat: NumberFormat,
  metrics: StatsResponsiveMetrics,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 14.dp,
        topRimTint = HydrationStart,
      )
      .clickable(role = Role.Button, onClick = onOpenCalendarInspector)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag("stats_open_custom_calendar_banner"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "CUSTOM DATE CALENDAR & DAY TIMELINE",
        color = PureWhite,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.spScaled(10.2f, 9.0f, 11.5f),
      )
      Text(
        text = "${formatHumanReadableDateKey(selectedCustomDateKey)} • ${inspectedRecord.timelineEntries.size} timeline logs • ${numberFormat.format(inspectedRecord.caloriesKcal)} kcal",
        color = StatsZinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
      )
    }
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(PureWhite)
        .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Text(
        text = "OPEN CALENDAR ›",
        color = ObsidianBlack,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = metrics.spScaled(8.8f, 7.8f, 10.0f),
      )
    }
  }
}

// ============================================================================
// DATA AGGREGATION & CALENDAR HELPERS (ON-DEVICE USER DATABASE)
// ============================================================================

private fun isCalendarWorkoutDay(
  cal: Calendar,
  activityLevel: ActivityLevelOption,
  selectedWorkoutDays: Set<DayOfWeekOption>,
): Boolean {
  if (activityLevel == ActivityLevelOption.SEDENTARY || selectedWorkoutDays.isEmpty()) return false
  val dayOption = when (cal.get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> DayOfWeekOption.MONDAY
    Calendar.TUESDAY -> DayOfWeekOption.TUESDAY
    Calendar.WEDNESDAY -> DayOfWeekOption.WEDNESDAY
    Calendar.THURSDAY -> DayOfWeekOption.THURSDAY
    Calendar.FRIDAY -> DayOfWeekOption.FRIDAY
    Calendar.SATURDAY -> DayOfWeekOption.SATURDAY
    Calendar.SUNDAY -> DayOfWeekOption.SUNDAY
    else -> DayOfWeekOption.MONDAY
  }
  return dayOption in selectedWorkoutDays
}

private fun parseDayKeyToCalendar(dayKey: String): Calendar {
  val cal = Calendar.getInstance()
  runCatching {
    val parts = dayKey.split("-")
    if (parts.size == 3) {
      cal.set(Calendar.YEAR, parts[0].toInt())
      cal.set(Calendar.MONTH, (parts[1].toInt() - 1).coerceIn(0, 11))
      cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt().coerceIn(1, 31))
    }
  }
  return cal
}

private fun formatYearMonthDayKey(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
  val m = (monthZeroBased + 1).toString().padStart(2, '0')
  val d = dayOfMonth.toString().padStart(2, '0')
  return "$year-$m-$d"
}

private fun formatHumanReadableDateKey(dayKey: String): String {
  val cal = parseDayKeyToCalendar(dayKey)
  val monthStr = StatsMonthShortNames.getOrElse(cal.get(Calendar.MONTH)) { "NOV" }
  val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
  val yearNum = cal.get(Calendar.YEAR)
  return "$monthStr $dayNum, $yearNum"
}

private fun buildMonthCalendarCells(year: Int, monthZeroBased: Int): List<Int?> {
  val cal = Calendar.getInstance().apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
    set(Calendar.DAY_OF_MONTH, 1)
  }
  val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
  val leadingBlanks = (firstDayOfWeek - Calendar.SUNDAY).coerceIn(0, 6)
  val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val cells = mutableListOf<Int?>()
  repeat(leadingBlanks) { cells.add(null) }
  for (d in 1..maxDays) {
    cells.add(d)
  }
  while (cells.size % 7 != 0) {
    cells.add(null)
  }
  return cells
}

private fun formatActivePeriodWindowLabel(
  timeRange: StatsTimeRangeOption,
  periodOffset: Int,
): String {
  return when (timeRange) {
    StatsTimeRangeOption.WEEKLY -> if (periodOffset == 0) "CURRENT 7 DAYS" else "${abs(periodOffset)}W AGO"
    StatsTimeRangeOption.MONTHLY -> {
      val cal = Calendar.getInstance().apply { add(Calendar.MONTH, periodOffset) }
      "${StatsMonthShortNames.getOrElse(cal.get(Calendar.MONTH)) { "NOV" }} ${cal.get(Calendar.YEAR)}"
    }
    StatsTimeRangeOption.YEARLY -> {
      val cal = Calendar.getInstance().apply { add(Calendar.YEAR, periodOffset) }
      "YEAR ${cal.get(Calendar.YEAR)}"
    }
    StatsTimeRangeOption.CUSTOM_DATE -> "CALENDAR DAY"
  }
}

private fun extractCategoryActualAndTarget(
  record: DailyHistoricalRecord,
  category: StatsCategoryOption,
): Pair<Int, Int> {
  return when (category) {
    StatsCategoryOption.ALL -> {
      val sPct = ((record.stepsCount * 100f) / record.targetStepsCount.coerceAtLeast(1)).coerceIn(0f, 150f)
      val cPct = ((record.caloriesKcal * 100f) / record.targetCaloriesKcal.coerceAtLeast(1)).coerceIn(0f, 150f)
      val pPct = ((record.proteinGrams * 100f) / record.targetProteinGrams.coerceAtLeast(1)).coerceIn(0f, 150f)
      val kPct = ((record.carbsGrams * 100f) / record.targetCarbsGrams.coerceAtLeast(1)).coerceIn(0f, 150f)
      val fPct = ((record.fatsGrams * 100f) / record.targetFatsGrams.coerceAtLeast(1)).coerceIn(0f, 150f)
      val wPct = ((record.hydrationMl * 100f) / record.targetHydrationMl.coerceAtLeast(1)).coerceIn(0f, 150f)
      val composite = ((sPct + cPct + pPct + kPct + fPct + wPct) / 6f).roundToInt()
      composite to 100
    }
    StatsCategoryOption.CALORIES -> record.caloriesKcal to record.targetCaloriesKcal
    StatsCategoryOption.PROTEIN -> record.proteinGrams to record.targetProteinGrams
    StatsCategoryOption.CARBS -> record.carbsGrams to record.targetCarbsGrams
    StatsCategoryOption.FATS -> record.fatsGrams to record.targetFatsGrams
    StatsCategoryOption.HYDRATION -> record.hydrationMl to record.targetHydrationMl
    StatsCategoryOption.STEPS -> record.stepsCount to record.targetStepsCount
  }
}

private fun buildChartBucketsForSelection(
  category: StatsCategoryOption,
  timeRange: StatsTimeRangeOption,
  periodOffset: Int,
  selectedCustomDateKey: String,
  loginStartDayKey: String,
  todayKey: String,
  resolveDay: (String) -> DailyHistoricalRecord,
  streakThresholdPercent: Int,
): List<StatsChartBucket> {
  val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

  return when (timeRange) {
    StatsTimeRangeOption.WEEKLY -> {
      // Only include dates from loginStartDayKey up to todayKey (eliminates pre-login & future redundancy)
      val rawDays = (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance().apply {
          add(Calendar.DAY_OF_YEAR, periodOffset * 7 - daysAgo)
        }
        val dKey = keyFormatter.format(cal.time)
        cal to dKey
      }.filter { (_, dKey) ->
        dKey >= loginStartDayKey && dKey <= todayKey
      }.ifEmpty {
        val todayCal = parseDayKeyToCalendar(todayKey)
        listOf(todayCal to todayKey)
      }

      rawDays.map { (cal, dKey) ->
        val rec = resolveDay(dKey)
        val (act, tgt) = extractCategoryActualAndTarget(rec, category)
        val pct = ((act * 100f) / tgt.coerceAtLeast(1)).roundToInt()
        val wdLabel = StatsWeekdayShortNames.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "DAY" }
        val dateSub = "${StatsMonthShortNames.getOrElse(cal.get(Calendar.MONTH)) { "NOV" }} ${cal.get(Calendar.DAY_OF_MONTH)}"
        StatsChartBucket(
          label = wdLabel,
          subLabel = dateSub,
          dayKeyOrRange = dKey,
          actualValue = act,
          targetValue = tgt,
          completionPercent = pct,
          qualifiesStreak = pct >= streakThresholdPercent,
          hasLoggedData = rec.caloriesKcal > 0 || rec.hydrationMl > 0 || rec.stepsCount > 0 || rec.timelineEntries.isNotEmpty(),
        )
      }
    }

    StatsTimeRangeOption.MONTHLY -> {
      // Only include 5-day windows within the selected month that fall in [loginStartDayKey, todayKey]
      val monthCal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, periodOffset)
      }
      val year = monthCal.get(Calendar.YEAR)
      val month = monthCal.get(Calendar.MONTH)
      val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
      val monthShort = StatsMonthShortNames.getOrElse(month) { "NOV" }
      val ranges = listOf(1..5, 6..10, 11..15, 16..20, 21..25, 26..maxDay)

      val validBuckets = ranges.mapNotNull { dayRange ->
        val validDayKeys = dayRange
          .map { d -> d to formatYearMonthDayKey(year, month, d) }
          .filter { (_, k) -> k >= loginStartDayKey && k <= todayKey }
        if (validDayKeys.isEmpty()) {
          null
        } else {
          val records = validDayKeys.map { (_, k) -> resolveDay(k) }
          val activeRecs = records.filter {
            it.caloriesKcal > 0 || it.hydrationMl > 0 || it.stepsCount > 0 || it.timelineEntries.isNotEmpty()
          }
          val sample = if (activeRecs.isNotEmpty()) activeRecs else records
          val pairs = sample.map { extractCategoryActualAndTarget(it, category) }
          val avgAct = pairs.map { it.first }.average().roundToInt()
          val avgTgt = pairs.map { it.second }.average().roundToInt().coerceAtLeast(1)
          val pct = ((avgAct * 100f) / avgTgt).roundToInt()
          val firstD = validDayKeys.first().first
          val lastD = validDayKeys.last().first
          val rangeLbl = if (firstD == lastD) "$monthShort $firstD" else "${firstD}–${lastD}"
          StatsChartBucket(
            label = rangeLbl,
            subLabel = "$monthShort $rangeLbl",
            dayKeyOrRange = "$year-${month + 1}",
            actualValue = avgAct,
            targetValue = avgTgt,
            completionPercent = pct,
            qualifiesStreak = pct >= streakThresholdPercent,
            hasLoggedData = activeRecs.isNotEmpty(),
          )
        }
      }
      validBuckets.ifEmpty {
        val rec = resolveDay(todayKey)
        val (act, tgt) = extractCategoryActualAndTarget(rec, category)
        val pct = ((act * 100f) / tgt.coerceAtLeast(1)).roundToInt()
        listOf(
          StatsChartBucket(
            label = "TODAY",
            subLabel = formatHumanReadableDateKey(todayKey),
            dayKeyOrRange = todayKey,
            actualValue = act,
            targetValue = tgt,
            completionPercent = pct,
            qualifiesStreak = pct >= streakThresholdPercent,
            hasLoggedData = true,
          ),
        )
      }
    }

    StatsTimeRangeOption.YEARLY -> {
      // Only include months from loginStartDayKey up to todayKey
      val loginCal = parseDayKeyToCalendar(loginStartDayKey)
      val todayCal = parseDayKeyToCalendar(todayKey)
      val targetYear = todayCal.get(Calendar.YEAR) + periodOffset
      val minMonth = if (targetYear <= loginCal.get(Calendar.YEAR)) loginCal.get(Calendar.MONTH) else 0
      val maxMonth = if (targetYear >= todayCal.get(Calendar.YEAR)) todayCal.get(Calendar.MONTH) else 11

      (minMonth..maxMonth.coerceAtLeast(minMonth)).map { mIdx ->
        val mCal = Calendar.getInstance().apply {
          set(Calendar.YEAR, targetYear)
          set(Calendar.MONTH, mIdx)
          set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInM = mCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val validKeys = (1..daysInM)
          .map { d -> formatYearMonthDayKey(targetYear, mIdx, d) }
          .filter { k -> k >= loginStartDayKey && k <= todayKey }
        val records = if (validKeys.isNotEmpty()) validKeys.map { resolveDay(it) } else listOf(resolveDay(todayKey))
        val activeRecs = records.filter {
          it.caloriesKcal > 0 || it.hydrationMl > 0 || it.stepsCount > 0 || it.timelineEntries.isNotEmpty()
        }
        val sample = if (activeRecs.isNotEmpty()) activeRecs else listOf(records.first())
        val pairs = sample.map { extractCategoryActualAndTarget(it, category) }
        val avgAct = if (activeRecs.isNotEmpty()) pairs.map { it.first }.average().roundToInt() else 0
        val avgTgt = pairs.map { it.second }.average().roundToInt().coerceAtLeast(1)
        val pct = ((avgAct * 100f) / avgTgt).roundToInt()
        val mShort = StatsMonthShortNames.getOrElse(mIdx) { "M" }
        StatsChartBucket(
          label = mShort,
          subLabel = "$mShort $targetYear",
          dayKeyOrRange = "$targetYear-${mIdx + 1}",
          actualValue = avgAct,
          targetValue = avgTgt,
          completionPercent = pct,
          qualifiesStreak = pct >= streakThresholdPercent,
          hasLoggedData = activeRecs.isNotEmpty(),
        )
      }
    }

    StatsTimeRangeOption.CUSTOM_DATE -> {
      val effectiveKey = if (selectedCustomDateKey < loginStartDayKey) loginStartDayKey else selectedCustomDateKey
      val isFutureDate = effectiveKey > todayKey
      val rec = resolveDay(effectiveKey)
      if (category == StatsCategoryOption.ALL) {
        val pillars = listOf(
          Triple("STEPS", if (isFutureDate) 0 else rec.stepsCount, rec.targetStepsCount),
          Triple("KCAL", if (isFutureDate) 0 else rec.caloriesKcal, rec.targetCaloriesKcal),
          Triple("PRO", if (isFutureDate) 0 else rec.proteinGrams, rec.targetProteinGrams),
          Triple("CARB", if (isFutureDate) 0 else rec.carbsGrams, rec.targetCarbsGrams),
          Triple("FAT", if (isFutureDate) 0 else rec.fatsGrams, rec.targetFatsGrams),
          Triple("H₂O", if (isFutureDate) 0 else rec.hydrationMl, rec.targetHydrationMl),
        )
        pillars.map { (lbl, act, tgt) ->
          val pct = ((act * 100f) / tgt.coerceAtLeast(1)).roundToInt()
          StatsChartBucket(
            label = lbl,
            subLabel = "$act / $tgt",
            dayKeyOrRange = effectiveKey,
            actualValue = pct.coerceIn(0, 150),
            targetValue = 100,
            completionPercent = pct,
            qualifiesStreak = !isFutureDate && pct >= streakThresholdPercent,
            hasLoggedData = !isFutureDate && act > 0,
          )
        }
      } else {
        if (isFutureDate) {
          val anchorCal = parseDayKeyToCalendar(effectiveKey)
          val (_, tgt) = extractCategoryActualAndTarget(rec, category)
          val wd = StatsWeekdayShortNames.getOrElse(anchorCal.get(Calendar.DAY_OF_WEEK) - 1) { "DAY" }
          listOf(
            StatsChartBucket(
              label = wd,
              subLabel = formatHumanReadableDateKey(effectiveKey),
              dayKeyOrRange = effectiveKey,
              actualValue = 0,
              targetValue = tgt,
              completionPercent = 0,
              qualifiesStreak = false,
              hasLoggedData = false,
            ),
          )
        } else {
          val anchorCal = parseDayKeyToCalendar(effectiveKey)
          val validDays = (6 downTo 0).map { daysBefore ->
            val c = (anchorCal.clone() as Calendar).apply {
              add(Calendar.DAY_OF_YEAR, -daysBefore)
            }
            c to keyFormatter.format(c.time)
          }.filter { (_, dKey) ->
            dKey >= loginStartDayKey && dKey <= todayKey
          }.ifEmpty {
            listOf(anchorCal to effectiveKey)
          }
          validDays.map { (c, dKey) ->
            val dRec = resolveDay(dKey)
            val (act, tgt) = extractCategoryActualAndTarget(dRec, category)
            val pct = ((act * 100f) / tgt.coerceAtLeast(1)).roundToInt()
            val wd = StatsWeekdayShortNames.getOrElse(c.get(Calendar.DAY_OF_WEEK) - 1) { "DAY" }
            StatsChartBucket(
              label = wd,
              subLabel = formatHumanReadableDateKey(dKey),
              dayKeyOrRange = dKey,
              actualValue = act,
              targetValue = tgt,
              completionPercent = pct,
              qualifiesStreak = pct >= streakThresholdPercent,
              hasLoggedData = dRec.caloriesKcal > 0 || dRec.hydrationMl > 0 || dRec.stepsCount > 0,
            )
          }
        }
      }
    }
  }
}

/**
 * Builds 3-D Pie Chart slices across ALL 6 categories (`STEPS`, `CALORIES`, `PROTEIN`, `CARBS`, `FATS`, `H₂O`)
 * and dynamically adapts to the user's selected `category` and active post-login date window (`loginStartDayKey..todayKey`).
 */
private fun buildPieSlicesForSelection(
  category: StatsCategoryOption,
  timeRange: StatsTimeRangeOption,
  periodOffset: Int,
  selectedCustomDateKey: String,
  loginStartDayKey: String,
  todayKey: String,
  resolveDay: (String) -> DailyHistoricalRecord,
  numberFormat: NumberFormat,
): List<StatsPieSlice> {
  val keyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  val rawKeys: List<String> = when (timeRange) {
    StatsTimeRangeOption.CUSTOM_DATE -> {
      val effectiveKey = if (selectedCustomDateKey < loginStartDayKey) loginStartDayKey else selectedCustomDateKey
      listOf(effectiveKey)
    }
    StatsTimeRangeOption.WEEKLY -> (0..6).map { d ->
      val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, periodOffset * 7 - d) }
      keyFormatter.format(c.time)
    }.filter { it >= loginStartDayKey && it <= todayKey }.ifEmpty { listOf(todayKey) }
    StatsTimeRangeOption.MONTHLY -> (0..29).map { d ->
      val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, periodOffset * 30 - d) }
      keyFormatter.format(c.time)
    }.filter { it >= loginStartDayKey && it <= todayKey }.ifEmpty { listOf(todayKey) }
    StatsTimeRangeOption.YEARLY -> (0..60 step 2).map { d ->
      val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, periodOffset * 365 - d) }
      keyFormatter.format(c.time)
    }.filter { it >= loginStartDayKey && it <= todayKey }.ifEmpty { listOf(todayKey) }
  }

  val records = rawKeys.map { resolveDay(it) }
  val dayCount = records.size.coerceAtLeast(1)

  val avgSteps = (records.sumOf { it.stepsCount } / dayCount)
  val tgtSteps = (records.sumOf { it.targetStepsCount } / dayCount).coerceAtLeast(1)
  val stepsPct = ((avgSteps * 100f) / tgtSteps).coerceIn(0f, 150f)

  val avgCal = (records.sumOf { it.caloriesKcal } / dayCount)
  val tgtCal = (records.sumOf { it.targetCaloriesKcal } / dayCount).coerceAtLeast(1)
  val calPct = ((avgCal * 100f) / tgtCal).coerceIn(0f, 150f)

  val avgPro = (records.sumOf { it.proteinGrams } / dayCount)
  val tgtPro = (records.sumOf { it.targetProteinGrams } / dayCount).coerceAtLeast(1)
  val proPct = ((avgPro * 100f) / tgtPro).coerceIn(0f, 150f)

  val avgCarb = (records.sumOf { it.carbsGrams } / dayCount)
  val tgtCarb = (records.sumOf { it.targetCarbsGrams } / dayCount).coerceAtLeast(1)
  val carbPct = ((avgCarb * 100f) / tgtCarb).coerceIn(0f, 150f)

  val avgFat = (records.sumOf { it.fatsGrams } / dayCount)
  val tgtFat = (records.sumOf { it.targetFatsGrams } / dayCount).coerceAtLeast(1)
  val fatPct = ((avgFat * 100f) / tgtFat).coerceIn(0f, 150f)

  val avgWat = (records.sumOf { it.hydrationMl } / dayCount)
  val tgtWat = (records.sumOf { it.targetHydrationMl } / dayCount).coerceAtLeast(1)
  val watPct = ((avgWat * 100f) / tgtWat).coerceIn(0f, 150f)

  val anyLogged = (avgSteps + avgCal + avgPro + avgCarb + avgFat + avgWat) > 0

  data class PillarRawSlice(
    val cat: StatsCategoryOption,
    val label: String,
    val valueText: String,
    val weight: Float,
    val topColor: Color,
    val sideColor: Color,
  )

  val allSixRaw = listOf(
    PillarRawSlice(
      cat = StatsCategoryOption.STEPS,
      label = "Steps",
      valueText = "${numberFormat.format(avgSteps)} / ${numberFormat.format(tgtSteps)} (${stepsPct.roundToInt()}%)",
      weight = if (anyLogged) stepsPct.coerceAtLeast(6f) else 100f,
      topColor = StepsStart,
      sideColor = StepsEnd,
    ),
    PillarRawSlice(
      cat = StatsCategoryOption.CALORIES,
      label = "Calories",
      valueText = "${numberFormat.format(avgCal)} / ${numberFormat.format(tgtCal)} kcal (${calPct.roundToInt()}%)",
      weight = if (anyLogged) calPct.coerceAtLeast(6f) else 100f,
      topColor = CaloriesStart,
      sideColor = CaloriesEnd,
    ),
    PillarRawSlice(
      cat = StatsCategoryOption.PROTEIN,
      label = "Protein",
      valueText = "${avgPro}g / ${tgtPro}g (${proPct.roundToInt()}%)",
      weight = if (anyLogged) proPct.coerceAtLeast(6f) else 100f,
      topColor = ProteinStart,
      sideColor = ProteinEnd,
    ),
    PillarRawSlice(
      cat = StatsCategoryOption.CARBS,
      label = "Carbs",
      valueText = "${avgCarb}g / ${tgtCarb}g (${carbPct.roundToInt()}%)",
      weight = if (anyLogged) carbPct.coerceAtLeast(6f) else 100f,
      topColor = CarbsStart,
      sideColor = CarbsEnd,
    ),
    PillarRawSlice(
      cat = StatsCategoryOption.FATS,
      label = "Fats",
      valueText = "${avgFat}g / ${tgtFat}g (${fatPct.roundToInt()}%)",
      weight = if (anyLogged) fatPct.coerceAtLeast(6f) else 100f,
      topColor = FatsStart,
      sideColor = FatsEnd,
    ),
    PillarRawSlice(
      cat = StatsCategoryOption.HYDRATION,
      label = "Hydration H₂O",
      valueText = "${numberFormat.format(avgWat)} / ${numberFormat.format(tgtWat)} ml (${watPct.roundToInt()}%)",
      weight = if (anyLogged) watPct.coerceAtLeast(6f) else 100f,
      topColor = HydrationStart,
      sideColor = HydrationEnd,
    ),
  )

  // Reorder so if a specific category is selected, it is highlighted first at index 0
  val orderedRaw = if (category == StatsCategoryOption.ALL) {
    allSixRaw
  } else {
    val primary = allSixRaw.filter { it.cat == category }
    val others = allSixRaw.filter { it.cat != category }
    primary + others
  }

  val totalWeight = orderedRaw.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
  return orderedRaw.map { item ->
    val share = ((item.weight * 100f) / totalWeight).roundToInt().coerceIn(1, 99)
    StatsPieSlice(
      label = item.label,
      valueText = item.valueText,
      rawValue = item.weight,
      sharePercent = share,
      topColor = item.topColor,
      sideColor = item.sideColor,
    )
  }
}
