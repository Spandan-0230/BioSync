package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class BioSyncWidgetTelemetrySnapshot(
  val subjectName: String,
  val subjectIdBadge: String,
  val dayKey: String,
  val streakDays: Int,
  val isInBufferGrace: Boolean,
  val bufferDaysRemaining: Int,
  val streakThresholdPercent: Int,
  val allSixMet: Boolean,
  val overallReadinessPercent: Int,
  val loggedCalories: Int,
  val targetCalories: Int,
  val loggedProtein: Int,
  val targetProtein: Int,
  val liveSteps: Int,
  val targetSteps: Int,
  val loggedCarbs: Int,
  val targetCarbs: Int,
  val loggedFats: Int,
  val targetFats: Int,
  val loggedWaterMl: Int,
  val targetWaterMl: Int,
)

private data class WidgetPillarVisualSpec(
  val code: String,
  val title: String,
  val valueFormatted: String,
  val targetFormatted: String,
  val unit: String,
  val progressFraction: Float,
  val percentInt: Int,
  val qualifiesForStreak: Boolean,
  val startColorArgb: Int,
  val endColorArgb: Int,
)

object BioSyncWidgetAndNotificationEngine {
  const val ACTION_WIDGET_LOG_WATER_250 = "com.example.action.WIDGET_LOG_WATER_250"
  const val ACTION_WIDGET_LOG_WATER_500 = "com.example.action.WIDGET_LOG_WATER_500"
  const val ACTION_WIDGET_LOG_STEPS_250 = "com.example.action.WIDGET_LOG_STEPS_250"
  const val ACTION_WIDGET_LOG_STEPS_500 = "com.example.action.WIDGET_LOG_STEPS_500"
  const val ACTION_WIDGET_LOG_STEPS_1000 = "com.example.action.WIDGET_LOG_STEPS_1000"
  const val ACTION_WIDGET_SYNC_TELEMETRY = "com.example.action.WIDGET_SYNC_TELEMETRY"

  const val LIVE_TELEMETRY_CHANNEL_ID = "biosync_live_telemetry_hud_v3"
  const val LIVE_TELEMETRY_NOTIFICATION_ID = 9420

  fun collectSnapshot(context: Context): BioSyncWidgetTelemetrySnapshot {
    val appCtx = context.applicationContext
    val storedProfile = BioSyncLocalStore.load(appCtx)
    val dashData = DashboardLocalStore.load(appCtx)
    val todayKey = DashboardTimeFormatter.currentDayKey()

    val persistedStepsForToday = if (storedProfile.stepSensorDayKey == todayKey) {
      storedProfile.todaySyncedSteps.coerceAtLeast(0)
    } else {
      0
    }
    val currentSyncState = BioSyncHealthStepManager.syncState.value
    val sensorSteps = if (currentSyncState.lastSyncedEpochMillis > 0L || currentSyncState.liveTodaySteps > 0) {
      currentSyncState.liveTodaySteps.coerceAtLeast(0)
    } else {
      persistedStepsForToday
    }
    val liveSteps = (sensorSteps + dashData.manualStepsToday.coerceAtLeast(0)).coerceAtLeast(0)

    val entries = dashData.todayTimelineEntries
    val loggedCal = entries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
    val loggedPro = entries.sumOf { it.proteinGrams }.coerceAtLeast(0)
    val loggedCarb = entries.sumOf { it.carbsGrams }.coerceAtLeast(0)
    val loggedFat = entries.sumOf { it.fatsGrams }.coerceAtLeast(0)
    val loggedWater = entries.sumOf { it.hydrationMl }.coerceAtLeast(0)

    val targetCal = storedProfile.customCaloriesKcal.coerceAtLeast(1000)
    val targetPro = storedProfile.customProteinGrams.coerceAtLeast(30)
    val targetCarb = storedProfile.customCarbsGrams.coerceAtLeast(40)
    val targetFat = storedProfile.customFatsGrams.coerceAtLeast(20)
    val targetSteps = storedProfile.customStepsCount.coerceAtLeast(1000)
    val baseWater = storedProfile.customHydrationMl.coerceAtLeast(500)

    val isWorkoutToday = if (storedProfile.activityLevel == ActivityLevelOption.SEDENTARY) {
      false
    } else {
      val cal = Calendar.getInstance()
      val todayOpt = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> DayOfWeekOption.MONDAY
        Calendar.TUESDAY -> DayOfWeekOption.TUESDAY
        Calendar.WEDNESDAY -> DayOfWeekOption.WEDNESDAY
        Calendar.THURSDAY -> DayOfWeekOption.THURSDAY
        Calendar.FRIDAY -> DayOfWeekOption.FRIDAY
        Calendar.SATURDAY -> DayOfWeekOption.SATURDAY
        else -> DayOfWeekOption.SUNDAY
      }
      val days = storedProfile.selectedWorkoutDays.ifEmpty {
        DailyTargetsEngine.defaultWorkoutDaysFor(storedProfile.activityLevel)
      }
      todayOpt in days
    }

    val targetWater = if (isWorkoutToday || storedProfile.activityLevel == ActivityLevelOption.SEDENTARY) {
      baseWater
    } else {
      val report = DailyTargetsEngine.calculate(
        ageYears = storedProfile.ageYears,
        biologicalSex = storedProfile.selectedSex,
        heightCm = storedProfile.heightCm,
        weightKg = storedProfile.weightKg,
        activityLevel = storedProfile.activityLevel,
        occupationLoad = storedProfile.occupationLoad,
        objective = storedProfile.objective,
        trainingModality = storedProfile.trainingModality,
        workoutDuration = storedProfile.workoutDuration,
        experienceLevel = storedProfile.experienceLevel,
        targetPace = storedProfile.targetPace,
        mealFrequency = storedProfile.mealFrequency,
        caffeineAlcoholYes = storedProfile.caffeineAlcoholYes,
        sodiumFrequency = storedProfile.sodiumFrequency,
        climateBand = storedProfile.climateBand,
        bodyFatPercentOverride = if (storedProfile.bodyFatOverrideEnabled) storedProfile.bodyFatPercent else null,
        selectedWorkoutDays = storedProfile.selectedWorkoutDays,
      )
      (baseWater - report.exerciseWaterAddOnWorkoutDayMl).coerceAtLeast(1500)
    }

    val thresholdPct = storedProfile.streakThreshold.percentage
    val calMet = ((loggedCal * 100f) / targetCal).roundToInt() >= thresholdPct
    val proMet = ((loggedPro * 100f) / targetPro).roundToInt() >= thresholdPct
    val stepsMet = ((liveSteps * 100f) / targetSteps).roundToInt() >= thresholdPct
    val carbMet = ((loggedCarb * 100f) / targetCarb).roundToInt() >= thresholdPct
    val fatMet = ((loggedFat * 100f) / targetFat).roundToInt() >= thresholdPct
    val waterMet = ((loggedWater * 100f) / targetWater).roundToInt() >= thresholdPct
    val allSixMet = calMet && proMet && stepsMet && carbMet && fatMet && waterMet

    val streakEval = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = dashData.baseStreakDays,
      lastCompletedDayKey = dashData.lastCompletedStreakDayKey,
      todayKey = todayKey,
      todayQualified = allSixMet,
    )

    val avgReadiness = (
      (
        (loggedCal.toFloat() / targetCal).coerceIn(0f, 1f) +
          (loggedPro.toFloat() / targetPro).coerceIn(0f, 1f) +
          (liveSteps.toFloat() / targetSteps).coerceIn(0f, 1f) +
          (loggedCarb.toFloat() / targetCarb).coerceIn(0f, 1f) +
          (loggedFat.toFloat() / targetFat).coerceIn(0f, 1f) +
          (loggedWater.toFloat() / targetWater).coerceIn(0f, 1f)
        ) / 6f * 100f
      ).roundToInt().coerceIn(0, 100)

    val name = storedProfile.subjectName.ifBlank { "Elena Rostova" }
    return BioSyncWidgetTelemetrySnapshot(
      subjectName = name,
      subjectIdBadge = BioSyncLocalStore.computeSubjectIdBadge(name),
      dayKey = todayKey,
      streakDays = streakEval.streakDays,
      isInBufferGrace = streakEval.isInBufferGrace,
      bufferDaysRemaining = streakEval.bufferDaysRemaining,
      streakThresholdPercent = thresholdPct,
      allSixMet = allSixMet,
      overallReadinessPercent = avgReadiness,
      loggedCalories = loggedCal,
      targetCalories = targetCal,
      loggedProtein = loggedPro,
      targetProtein = targetPro,
      liveSteps = liveSteps,
      targetSteps = targetSteps,
      loggedCarbs = loggedCarb,
      targetCarbs = targetCarb,
      loggedFats = loggedFat,
      targetFats = targetFat,
      loggedWaterMl = loggedWater,
      targetWaterMl = targetWater,
    )
  }

  /**
   * Renders a high-DPI, auto-resizable 3D Obsidian Home Screen Widget Bitmap that adapts cleanly
   * to any widget resolution and includes the Widget-Exclusive Step Quick-Add Row (+250, +500, +1,000 Steps)
   * plus Hydration Quick-Add Row (+250ml, +500ml H2O, Open HUD).
   */
  fun renderWidgetTelemetryBitmap(
    snapshot: BioSyncWidgetTelemetrySnapshot,
    widthDp: Float,
    heightDp: Float,
    density: Float,
  ): Bitmap {
    val safeDensity = density.coerceIn(1.75f, 3.5f)
    val safeW = widthDp.coerceIn(180f, 640f)
    val safeH = heightDp.coerceIn(125f, 480f)
    val pxW = (safeW * safeDensity).roundToInt().coerceIn(360, 1800)
    val pxH = (safeH * safeDensity).roundToInt().coerceIn(240, 1400)

    val bmp = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val nf = NumberFormat.getIntegerInstance(Locale.US)

    val w = pxW.toFloat()
    val h = pxH.toFloat()
    val scale = min(w / (330f * safeDensity), h / (225f * safeDensity)).coerceIn(0.68f, 1.38f)
    fun dp(v: Float): Float = v * safeDensity * scale

    val monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    val sansBold = Typeface.create("sans-serif-medium", Typeface.BOLD)
    val sansReg = Typeface.create("sans-serif", Typeface.NORMAL)

    val outerPad = dp(4f)
    val cardRect = RectF(outerPad, outerPad, w - outerPad, h - outerPad)
    val cardRadius = dp(18f)

    // 1. 3D Obsidian Chassis Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        0f,
        cardRect.top,
        0f,
        cardRect.bottom,
        intArrayOf(
          0xFF15161F.toInt(),
          0xFF0B0C12.toInt(),
          0xFF060609.toInt(),
        ),
        floatArrayOf(0f, 0.48f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

    // Top ambient aura
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        cardRect.left,
        cardRect.top,
        cardRect.right,
        cardRect.top + h * 0.35f,
        intArrayOf(
          0x18FFB703,
          0x1210B981,
          0x00000000,
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, auraPaint)

    // Outer metallic specular border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = max(1.5f, dp(1.1f))
      shader = LinearGradient(
        0f,
        cardRect.top,
        0f,
        cardRect.bottom,
        intArrayOf(
          0x77FFFFFF,
          0x3371717A,
          0xFF1C1D26.toInt(),
        ),
        floatArrayOf(0f, 0.4f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

    val contentLeft = cardRect.left + dp(11f)
    val contentRight = cardRect.right - dp(11f)
    var cursorY = cardRect.top + dp(9f)

    // 2. Top Executive Telemetry Header
    val headerH = dp(26f)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (snapshot.allSixMet) 0xFF10B981.toInt() else 0xFFFFB703.toInt()
    }
    canvas.drawCircle(contentLeft + dp(4.5f), cursorY + headerH * 0.34f, dp(3.6f), dotPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      isSubpixelText = true
      typeface = monoBold
      textSize = dp(10.2f)
      color = 0xFFFFFFFF.toInt()
      letterSpacing = 0.06f
    }
    canvas.drawText("BIOSYNC TELEMETRY WIDGET", contentLeft + dp(12.5f), cursorY + dp(11f), titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      isSubpixelText = true
      typeface = sansReg
      textSize = dp(8.2f)
      color = 0xFFD4D4D8.toInt()
    }
    val subText = "${snapshot.subjectName} • ${snapshot.overallReadinessPercent}% READY • GATE ${snapshot.streakThresholdPercent}%"
    canvas.drawText(subText, contentLeft + dp(12.5f), cursorY + dp(21.5f), subPaint)

    // Top-right Streak & Buffer Badge
    val streakLabel = when {
      snapshot.allSixMet -> "🔥 ${snapshot.streakDays}D LOCKED"
      snapshot.isInBufferGrace -> "⏳ ${snapshot.streakDays}D • BUF ${snapshot.bufferDaysRemaining}D"
      else -> "🔥 ${snapshot.streakDays}D STREAK"
    }
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      isSubpixelText = true
      typeface = monoBold
      textSize = dp(8.8f)
      color = 0xFF060609.toInt()
    }
    val badgeTextW = badgeTextPaint.measureText(streakLabel)
    val badgeW = badgeTextW + dp(13f)
    val badgeH = dp(19f)
    val badgeRect = RectF(contentRight - badgeW, cursorY + dp(2.5f), contentRight, cursorY + dp(2.5f) + badgeH)
    val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        badgeRect.left,
        badgeRect.top,
        badgeRect.right,
        badgeRect.bottom,
        if (snapshot.isInBufferGrace) {
          intArrayOf(0xFFFDE047.toInt(), 0xFFF59E0B.toInt())
        } else {
          intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFD166.toInt())
        },
        null,
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(badgeRect, dp(6f), dp(6f), badgeBgPaint)
    canvas.drawText(
      streakLabel,
      badgeRect.left + dp(6.5f),
      badgeRect.centerY() + dp(3.0f),
      badgeTextPaint,
    )

    cursorY += headerH + dp(4f)

    // Divider line
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF23242F.toInt()
      strokeWidth = max(1f, dp(0.8f))
    }
    canvas.drawLine(contentLeft, cursorY, contentRight, cursorY, divPaint)
    cursorY += dp(5f)

    // 3. 6-Pillar Bento Grid (Automatically chooses 3×2 or 2×3 layout depending on aspect ratio)
    val pillars = listOf(
      WidgetPillarVisualSpec(
        code = "CAL",
        title = "CALORIES",
        valueFormatted = nf.format(snapshot.loggedCalories),
        targetFormatted = nf.format(snapshot.targetCalories),
        unit = "kcal",
        progressFraction = (snapshot.loggedCalories.toFloat() / snapshot.targetCalories.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.loggedCalories * 100f) / snapshot.targetCalories.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.loggedCalories * 100f) / snapshot.targetCalories.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFFFFB703.toInt(),
        endColorArgb = 0xFFFB8500.toInt(),
      ),
      WidgetPillarVisualSpec(
        code = "PRO",
        title = "PROTEIN",
        valueFormatted = "${snapshot.loggedProtein}g",
        targetFormatted = "${snapshot.targetProtein}g",
        unit = "g",
        progressFraction = (snapshot.loggedProtein.toFloat() / snapshot.targetProtein.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.loggedProtein * 100f) / snapshot.targetProtein.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.loggedProtein * 100f) / snapshot.targetProtein.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFF38BDF8.toInt(),
        endColorArgb = 0xFF0284C7.toInt(),
      ),
      WidgetPillarVisualSpec(
        code = "STP",
        title = "STEPS",
        valueFormatted = nf.format(snapshot.liveSteps),
        targetFormatted = nf.format(snapshot.targetSteps),
        unit = "steps",
        progressFraction = (snapshot.liveSteps.toFloat() / snapshot.targetSteps.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.liveSteps * 100f) / snapshot.targetSteps.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.liveSteps * 100f) / snapshot.targetSteps.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFF10B981.toInt(),
        endColorArgb = 0xFF059669.toInt(),
      ),
      WidgetPillarVisualSpec(
        code = "CRB",
        title = "CARBS",
        valueFormatted = "${snapshot.loggedCarbs}g",
        targetFormatted = "${snapshot.targetCarbs}g",
        unit = "g",
        progressFraction = (snapshot.loggedCarbs.toFloat() / snapshot.targetCarbs.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.loggedCarbs * 100f) / snapshot.targetCarbs.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.loggedCarbs * 100f) / snapshot.targetCarbs.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFFFACC15.toInt(),
        endColorArgb = 0xFFEAB308.toInt(),
      ),
      WidgetPillarVisualSpec(
        code = "FAT",
        title = "FATS",
        valueFormatted = "${snapshot.loggedFats}g",
        targetFormatted = "${snapshot.targetFats}g",
        unit = "g",
        progressFraction = (snapshot.loggedFats.toFloat() / snapshot.targetFats.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.loggedFats * 100f) / snapshot.targetFats.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.loggedFats * 100f) / snapshot.targetFats.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFFFB7185.toInt(),
        endColorArgb = 0xFFE11D48.toInt(),
      ),
      WidgetPillarVisualSpec(
        code = "H2O",
        title = "HYDRATION",
        valueFormatted = nf.format(snapshot.loggedWaterMl),
        targetFormatted = "${nf.format(snapshot.targetWaterMl)}ml",
        unit = "ml",
        progressFraction = (snapshot.loggedWaterMl.toFloat() / snapshot.targetWaterMl.toFloat()).coerceIn(0f, 1f),
        percentInt = ((snapshot.loggedWaterMl * 100f) / snapshot.targetWaterMl.toFloat()).roundToInt().coerceIn(0, 999),
        qualifiesForStreak = ((snapshot.loggedWaterMl * 100f) / snapshot.targetWaterMl.toFloat()).roundToInt() >= snapshot.streakThresholdPercent,
        startColorArgb = 0xFF06B6D4.toInt(),
        endColorArgb = 0xFF0891B2.toInt(),
      ),
    )

    // 2-Row Dock at bottom of Home Screen Widget (Row 1: +250 / +500 / +1,000 Steps, Row 2: +250ml / +500ml H2O / Open HUD)
    val singleDockRowH = dp(22.5f)
    val dockRowsGap = dp(4f)
    val totalDockHeight = singleDockRowH * 2f + dockRowsGap
    val dockTopGap = dp(5.5f)
    val gridBottom = cardRect.bottom - dp(7f) - totalDockHeight - dockTopGap
    val availableGridH = (gridBottom - cursorY).coerceAtLeast(dp(64f))
    val availableGridW = contentRight - contentLeft

    val aspect = availableGridW / availableGridH
    val cols = if (aspect >= 1.45f) 3 else 2
    val rows = if (cols == 3) 2 else 3
    val colGap = dp(5.5f)
    val rowGap = dp(5.5f)
    val cellW = (availableGridW - colGap * (cols - 1)) / cols.toFloat()
    val cellH = (availableGridH - rowGap * (rows - 1)) / rows.toFloat()

    val chamberBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF101118.toInt()
    }
    val chamberBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = max(1f, dp(0.85f))
    }

    pillars.forEachIndexed { idx, pillar ->
      val r = idx / cols
      val c = idx % cols
      val left = contentLeft + c * (cellW + colGap)
      val top = cursorY + r * (cellH + rowGap)
      val cellRect = RectF(left, top, left + cellW, top + cellH)
      val cellRadius = dp(9.5f)

      canvas.drawRoundRect(cellRect, cellRadius, cellRadius, chamberBgPaint)
      chamberBorderPaint.color = if (pillar.qualifiesForStreak) {
        (pillar.startColorArgb and 0x00FFFFFF) or 0x99000000.toInt()
      } else {
        0xFF252734.toInt()
      }
      canvas.drawRoundRect(cellRect, cellRadius, cellRadius, chamberBorderPaint)

      val padX = dp(7.5f)
      val innerLeft = cellRect.left + padX
      val innerRight = cellRect.right - padX

      // Top row inside cell: Colored Pill + Pillar Name + % Badge
      val topRowY = cellRect.top + dp(10.8f)
      val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillar.startColorArgb
      }
      canvas.drawRoundRect(
        RectF(innerLeft, topRowY - dp(6.6f), innerLeft + dp(3.0f), topRowY + dp(1.1f)),
        dp(1.5f),
        dp(1.5f),
        indicatorPaint,
      )

      val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = monoBold
        textSize = dp(8.0f)
        color = 0xFFF4F4F5.toInt()
      }
      canvas.drawText(pillar.title, innerLeft + dp(6.0f), topRowY, labelPaint)

      val pctString = if (pillar.qualifiesForStreak) "✓ ${pillar.percentInt}%" else "${pillar.percentInt}%"
      val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = monoBold
        textSize = dp(7.8f)
        color = if (pillar.qualifiesForStreak) pillar.startColorArgb else 0xFFD4D4D8.toInt()
        textAlign = Paint.Align.RIGHT
      }
      canvas.drawText(pctString, innerRight, topRowY, pctPaint)

      // Middle row: Primary Value + / Target
      val valueBaselineY = cellRect.centerY() + dp(4.2f)
      val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = sansBold
        textSize = dp(12.2f)
        color = 0xFFFFFFFF.toInt()
      }
      canvas.drawText(pillar.valueFormatted, innerLeft, valueBaselineY, valPaint)
      val valW = valPaint.measureText(pillar.valueFormatted)

      val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = monoBold
        textSize = dp(7.5f)
        color = 0xFFA1A1AA.toInt()
      }
      val targetStr = " / ${pillar.targetFormatted}"
      if (innerLeft + valW + targetPaint.measureText(targetStr) <= innerRight) {
        canvas.drawText(targetStr, innerLeft + valW, valueBaselineY - dp(0.5f), targetPaint)
      }

      // Bottom row: Luminous Progress Track + Streak Threshold Gate Marker
      val barH = dp(4.8f)
      val barTop = cellRect.bottom - dp(5.8f) - barH
      val trackRect = RectF(innerLeft, barTop, innerRight, barTop + barH)
      val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF07070B.toInt()
      }
      canvas.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint)

      if (pillar.progressFraction > 0.01f) {
        val fillW = max(barH, trackRect.width() * pillar.progressFraction)
        val fillRect = RectF(trackRect.left, trackRect.top, min(trackRect.right, trackRect.left + fillW), trackRect.bottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader = LinearGradient(
            fillRect.left,
            fillRect.top,
            fillRect.right,
            fillRect.bottom,
            pillar.startColorArgb,
            pillar.endColorArgb,
            Shader.TileMode.CLAMP,
          )
        }
        canvas.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint)
      }

      val gateX = trackRect.left + trackRect.width() * (snapshot.streakThresholdPercent / 100f).coerceIn(0.5f, 1f)
      val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        strokeWidth = max(1f, dp(1f))
      }
      canvas.drawLine(gateX, trackRect.top - dp(1f), gateX, trackRect.bottom + dp(1f), gatePaint)
    }

    // 4. Bottom 2-Row Interactive Quick-Action Dock (Widget-Exclusive Step Row + Hydration/Open Row)
    val dockRow1Top = gridBottom + dockTopGap
    val dockRow1Bottom = dockRow1Top + singleDockRowH
    val dockRow2Top = dockRow1Bottom + dockRowsGap
    val dockRow2Bottom = dockRow2Top + singleDockRowH

    val btnGap = dp(5.5f)
    val btnW = (availableGridW - btnGap * 2f) / 3f

    // Row 1: Widget-Only Step Quick-Add Buttons (+250 STEPS | +500 STEPS | +1,000 STEPS)
    val stepActions = listOf(
      Triple("+250 STEPS", 0xFF10B981.toInt(), false),
      Triple("+500 STEPS", 0xFF34D399.toInt(), false),
      Triple("+1,000 STEPS", 0xFF10B981.toInt(), false),
    )

    stepActions.forEachIndexed { idx, (label, accentArgb, _) ->
      val bLeft = contentLeft + idx * (btnW + btnGap)
      val bRect = RectF(bLeft, dockRow1Top, bLeft + btnW, dockRow1Bottom)
      val bRadius = dp(6.8f)

      val bBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0E1B16.toInt()
      }
      canvas.drawRoundRect(bRect, bRadius, bRadius, bBgPaint)

      val bStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = max(1f, dp(0.9f))
        color = (accentArgb and 0x00FFFFFF) or 0x99000000.toInt()
      }
      canvas.drawRoundRect(bRect, bRadius, bRadius, bStroke)

      val bTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = monoBold
        textSize = dp(7.9f)
        color = accentArgb
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(label, bRect.centerX(), bRect.centerY() + dp(2.8f), bTextPaint)
    }

    // Row 2: Hydration Quick-Add & Open HUD (+250ML H2O | +500ML H2O | OPEN HUD ↗)
    val waterAndOpenActions = listOf(
      Triple("+250ML H₂O", 0xFF06B6D4.toInt(), false),
      Triple("+500ML H₂O", 0xFF38BDF8.toInt(), false),
      Triple("OPEN HUD ↗", 0xFFFFFFFF.toInt(), true),
    )

    waterAndOpenActions.forEachIndexed { idx, (label, accentArgb, isPrimaryWhite) ->
      val bLeft = contentLeft + idx * (btnW + btnGap)
      val bRect = RectF(bLeft, dockRow2Top, bLeft + btnW, dockRow2Bottom)
      val bRadius = dp(6.8f)

      val bBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        if (isPrimaryWhite) {
          shader = LinearGradient(
            bRect.left,
            bRect.top,
            bRect.right,
            bRect.bottom,
            0xFFFFFFFF.toInt(),
            0xFFE4E4E7.toInt(),
            Shader.TileMode.CLAMP,
          )
        } else {
          color = 0xFF121620.toInt()
        }
      }
      canvas.drawRoundRect(bRect, bRadius, bRadius, bBgPaint)

      if (!isPrimaryWhite) {
        val bStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = max(1f, dp(0.9f))
          color = (accentArgb and 0x00FFFFFF) or 0x88000000.toInt()
        }
        canvas.drawRoundRect(bRect, bRadius, bRadius, bStroke)
      }

      val bTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isSubpixelText = true
        typeface = monoBold
        textSize = dp(7.9f)
        color = if (isPrimaryWhite) 0xFF060609.toInt() else accentArgb
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(label, bRect.centerX(), bRect.centerY() + dp(2.8f), bTextPaint)
    }

    return bmp
  }

  /**
   * Builds the RemoteViews for a specific Home Screen widget instance using its current launcher dimensions
   * and binds the Widget-exclusive +250 / +500 / +1,000 Step buttons alongside +250ml / +500ml H2O and Open HUD.
   */
  fun buildWidgetRemoteViews(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    snapshot: BioSyncWidgetTelemetrySnapshot = collectSnapshot(context),
  ): RemoteViews {
    val options: Bundle? = runCatching { appWidgetManager.getAppWidgetOptions(appWidgetId) }.getOrNull()
    val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)?.takeIf { it > 0 } ?: 280
    val maxW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)?.takeIf { it > 0 } ?: minW
    val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 195)?.takeIf { it > 0 } ?: 195
    val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)?.takeIf { it > 0 } ?: minH

    val resolvedW = ((minW + maxW) / 2f).coerceIn(190f, 560f)
    val resolvedH = ((minH + maxH) / 2f).coerceIn(135f, 420f)
    val density = context.resources.displayMetrics.density.coerceIn(1.75f, 3.2f)

    val bitmap = renderWidgetTelemetryBitmap(
      snapshot = snapshot,
      widthDp = resolvedW,
      heightDp = resolvedH,
      density = density,
    )

    val views = RemoteViews(context.packageName, R.layout.biosync_home_widget)
    views.setImageViewBitmap(R.id.widget_telemetry_canvas, bitmap)

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openPending = PendingIntent.getActivity(
      context,
      9100 + appWidgetId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_open_dashboard, openPending)

    // Widget-Exclusive Step Quick-Add PendingIntents (+250 / +500 / +1,000 Steps)
    val steps250Pending = PendingIntent.getBroadcast(
      context,
      9120 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_STEPS_250 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_steps_250, steps250Pending)

    val steps500Pending = PendingIntent.getBroadcast(
      context,
      9140 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_STEPS_500 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_steps_500, steps500Pending)

    val steps1000Pending = PendingIntent.getBroadcast(
      context,
      9160 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_STEPS_1000 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_steps_1000, steps1000Pending)

    val water250Pending = PendingIntent.getBroadcast(
      context,
      9200 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_WATER_250 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_water_250, water250Pending)

    val water500Pending = PendingIntent.getBroadcast(
      context,
      9300 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_WATER_500 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_water_500, water500Pending)

    val syncPending = PendingIntent.getBroadcast(
      context,
      9400 + appWidgetId,
      Intent(context, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_SYNC_TELEMETRY },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_sync_steps, syncPending)

    return views
  }

  /**
   * Instantly refreshes all placed Home Screen widgets whenever nutrition, hydration, steps, or streaks change.
   */
  fun refreshAllWidgets(context: Context) {
    runCatching {
      val appCtx = context.applicationContext
      val manager = AppWidgetManager.getInstance(appCtx)
      val component = ComponentName(appCtx, BioSyncHomeWidgetProvider::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids != null && ids.isNotEmpty()) {
        val snapshot = collectSnapshot(appCtx)
        for (id in ids) {
          val rv = buildWidgetRemoteViews(appCtx, manager, id, snapshot)
          manager.updateAppWidget(id, rv)
        }
      }
    }
  }

  /**
   * Logs quick steps directly from the Home Screen Widget ONLY (+250, +500, or +1,000 steps),
   * re-evaluates the 2-day buffer streak, saves to `DashboardLocalStore`, and refreshes all surfaces.
   */
  fun handleInstantStepLogFromWidget(context: Context, addedSteps: Int, showToast: Boolean = true) {
    val appCtx = context.applicationContext
    val safeSteps = addedSteps.coerceIn(50, 20000)
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    val currentData = DashboardLocalStore.load(appCtx)

    val newManualSteps = (currentData.manualStepsToday.coerceAtLeast(0) + safeSteps).coerceIn(0, 150_000)
    val draftData = currentData.copy(
      manualStepsToday = newManualSteps,
    )

    DashboardLocalStore.save(appCtx, draftData)
    val snap = collectSnapshot(appCtx)

    val syncedData = DashboardLocalStore.withSyncedTodayRecord(
      data = draftData.copy(
        baseStreakDays = snap.streakDays,
        lastCompletedStreakDayKey = if (snap.allSixMet) snap.dayKey else draftData.lastCompletedStreakDayKey,
      ),
      liveSteps = snap.liveSteps,
      targetSteps = snap.targetSteps,
      targetCalories = snap.targetCalories,
      targetProtein = snap.targetProtein,
      targetCarbs = snap.targetCarbs,
      targetFats = snap.targetFats,
      targetWaterMl = snap.targetWaterMl,
      streakThresholdPercent = snap.streakThresholdPercent,
      streakQualified = snap.allSixMet,
      isWorkoutDay = true,
    )
    DashboardLocalStore.save(appCtx, syncedData)
    refreshAllWidgets(appCtx)
    refreshLiveTelemetryNotificationIfActive(appCtx)
    if (showToast) {
      runCatching {
        Toast.makeText(
          appCtx,
          "+${nf.format(safeSteps)} Steps logged (${nf.format(snap.liveSteps)} / ${nf.format(snap.targetSteps)})",
          Toast.LENGTH_SHORT,
        ).show()
      }
    }
  }

  /**
   * Logs quick hydration directly from either the Home Screen Widget or Notification Action button,
   * re-evaluates the 2-day buffer streak, saves to `DashboardLocalStore`, and refreshes all surfaces.
   */
  fun handleInstantWaterLogFromExternalSurface(context: Context, addedMl: Int, showToast: Boolean = true) {
    val appCtx = context.applicationContext
    val safeMl = addedMl.coerceIn(50, 2500)
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    val currentData = DashboardLocalStore.load(appCtx)

    val newEntry = DashboardTimelineEntry(
      type = DashboardEntryType.HYDRATION,
      title = "Hydration +${nf.format(safeMl)} ml",
      subtitle = "Widget / HUD Instant H₂O",
      hydrationMl = safeMl,
    )
    val updatedEntries = listOf(newEntry) + currentData.todayTimelineEntries
    val draftData = currentData.copy(todayTimelineEntries = updatedEntries)

    DashboardLocalStore.save(appCtx, draftData)
    val snap = collectSnapshot(appCtx)

    val syncedData = DashboardLocalStore.withSyncedTodayRecord(
      data = draftData.copy(
        baseStreakDays = snap.streakDays,
        lastCompletedStreakDayKey = if (snap.allSixMet) snap.dayKey else draftData.lastCompletedStreakDayKey,
      ),
      liveSteps = snap.liveSteps,
      targetSteps = snap.targetSteps,
      targetCalories = snap.targetCalories,
      targetProtein = snap.targetProtein,
      targetCarbs = snap.targetCarbs,
      targetFats = snap.targetFats,
      targetWaterMl = snap.targetWaterMl,
      streakThresholdPercent = snap.streakThresholdPercent,
      streakQualified = snap.allSixMet,
      isWorkoutDay = true,
    )
    DashboardLocalStore.save(appCtx, syncedData)
    refreshAllWidgets(appCtx)
    postInstantTelemetryNotification(appCtx, showToastFeedback = false)
    if (showToast) {
      runCatching {
        Toast.makeText(
          appCtx,
          "+${nf.format(safeMl)}ml H₂O logged (${nf.format(snap.loggedWaterMl)} / ${nf.format(snap.targetWaterMl)} ml)",
          Toast.LENGTH_SHORT,
        ).show()
      }
    }
  }

  fun isPinWidgetSupported(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    return runCatching {
      val manager = AppWidgetManager.getInstance(context)
      manager.isRequestPinAppWidgetSupported
    }.getOrDefault(false)
  }

  fun requestPinWidgetToHomeScreen(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    return runCatching {
      val manager = AppWidgetManager.getInstance(context)
      if (!manager.isRequestPinAppWidgetSupported) return false
      val provider = ComponentName(context, BioSyncHomeWidgetProvider::class.java)
      manager.requestPinAppWidget(provider, null, null)
    }.getOrDefault(false)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
      context.applicationContext,
      Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun ensureLiveTelemetryChannel(context: Context, channelId: String = LIVE_TELEMETRY_CHANNEL_ID) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val importance = if (channelId == LIVE_TELEMETRY_CHANNEL_ID) {
        NotificationManager.IMPORTANCE_DEFAULT
      } else {
        NotificationManager.IMPORTANCE_LOW
      }
      val channel = NotificationChannel(
        channelId,
        "BioSync 6-Pillar Live Telemetry HUD",
        importance,
      ).apply {
        description = "Full-height crisp native 6-pillar telemetry HUD with 1-tap hydration actions."
        enableVibration(false)
        setSound(null, null)
        enableLights(true)
        lightColor = android.graphics.Color.parseColor("#FFB703")
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      nm.createNotificationChannel(channel)
    }
  }

  /**
   * Constructs the Native Custom XML `RemoteViews` 6-Pillar Notification (both collapsed and
   * full-height expanded views) so text is rendered at native screen DPI with zero bitmap blur
   * and zero step-adding buttons (hydration + open HUD actions only).
   */
  fun buildLiveTelemetryHudNotification(
    context: Context,
    ongoing: Boolean = false,
    channelId: String = LIVE_TELEMETRY_CHANNEL_ID,
  ): Notification {
    val appCtx = context.applicationContext
    ensureLiveTelemetryChannel(appCtx, channelId)
    val snap = collectSnapshot(appCtx)
    val nf = NumberFormat.getIntegerInstance(Locale.US)

    val calPct = ((snap.loggedCalories * 100f) / snap.targetCalories.toFloat()).roundToInt().coerceIn(0, 999)
    val proPct = ((snap.loggedProtein * 100f) / snap.targetProtein.toFloat()).roundToInt().coerceIn(0, 999)
    val stepsPct = ((snap.liveSteps * 100f) / snap.targetSteps.toFloat()).roundToInt().coerceIn(0, 999)
    val carbsPct = ((snap.loggedCarbs * 100f) / snap.targetCarbs.toFloat()).roundToInt().coerceIn(0, 999)
    val fatsPct = ((snap.loggedFats * 100f) / snap.targetFats.toFloat()).roundToInt().coerceIn(0, 999)
    val waterPct = ((snap.loggedWaterMl * 100f) / snap.targetWaterMl.toFloat()).roundToInt().coerceIn(0, 999)

    val openIntent = Intent(appCtx, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openPending = PendingIntent.getActivity(
      appCtx,
      9501,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val water250Pending = PendingIntent.getBroadcast(
      appCtx,
      9502,
      Intent(appCtx, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_WATER_250 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val water500Pending = PendingIntent.getBroadcast(
      appCtx,
      9503,
      Intent(appCtx, BioSyncWidgetActionReceiver::class.java).apply { action = ACTION_WIDGET_LOG_WATER_500 },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val streakBadgeShort = when {
      snap.allSixMet -> "🔥 ${snap.streakDays}D LOCKED • ${snap.overallReadinessPercent}%"
      snap.isInBufferGrace -> "⏳ ${snap.streakDays}D (BUF ${snap.bufferDaysRemaining}D) • ${snap.overallReadinessPercent}%"
      else -> "🔥 ${snap.streakDays}D • ${snap.overallReadinessPercent}% READY"
    }

    // 1. Collapsed Native XML RemoteViews
    val collapsedViews = RemoteViews(appCtx.packageName, R.layout.biosync_notification_collapsed).apply {
      setTextViewText(R.id.notif_collapsed_title, "BIOSYNC 6-PILLAR LIVE HUD")
      setTextViewText(R.id.notif_collapsed_streak_badge, streakBadgeShort)
      setTextViewText(R.id.notif_collapsed_cal, "CAL ${calPct}%")
      setTextViewText(R.id.notif_collapsed_pro, "PRO ${proPct}%")
      setTextViewText(R.id.notif_collapsed_steps, "STP ${stepsPct}%")
      setTextViewText(R.id.notif_collapsed_carbs, "CRB ${carbsPct}%")
      setTextViewText(R.id.notif_collapsed_fats, "FAT ${fatsPct}%")
      setTextViewText(R.id.notif_collapsed_water, "H2O ${waterPct}%")
      setOnClickPendingIntent(R.id.notif_collapsed_root, openPending)
    }

    // 2. Expanded Full-Height 6-Pillar Native XML RemoteViews (Crisp system text + progress bars)
    val expandedViews = RemoteViews(appCtx.packageName, R.layout.biosync_notification_expanded).apply {
      setTextViewText(R.id.notif_exp_title, "BIOSYNC 6-PILLAR TELEMETRY HUD")
      setTextViewText(
        R.id.notif_exp_subtitle,
        "${snap.subjectName.uppercase(Locale.US)} • ${snap.overallReadinessPercent}% DAILY READINESS • GATE ${snap.streakThresholdPercent}%",
      )
      setTextViewText(R.id.notif_exp_streak_badge, streakBadgeShort)

      // Pillar 1: Calories
      setTextViewText(R.id.notif_pct_cal, if (calPct >= snap.streakThresholdPercent) "✓ ${calPct}%" else "${calPct}%")
      setTextViewText(R.id.notif_val_cal, "${nf.format(snap.loggedCalories)} / ${nf.format(snap.targetCalories)} kcal")
      setProgressBar(R.id.notif_prog_cal, 100, calPct.coerceIn(0, 100), false)

      // Pillar 2: Protein
      setTextViewText(R.id.notif_pct_pro, if (proPct >= snap.streakThresholdPercent) "✓ ${proPct}%" else "${proPct}%")
      setTextViewText(R.id.notif_val_pro, "${snap.loggedProtein}g / ${snap.targetProtein}g")
      setProgressBar(R.id.notif_prog_pro, 100, proPct.coerceIn(0, 100), false)

      // Pillar 3: Steps (Read-Only Telemetry in Notification; Step Quick-Add is Widget-Only)
      setTextViewText(R.id.notif_pct_steps, if (stepsPct >= snap.streakThresholdPercent) "✓ ${stepsPct}%" else "${stepsPct}%")
      setTextViewText(R.id.notif_val_steps, "${nf.format(snap.liveSteps)} / ${nf.format(snap.targetSteps)}")
      setProgressBar(R.id.notif_prog_steps, 100, stepsPct.coerceIn(0, 100), false)

      // Pillar 4: Carbs
      setTextViewText(R.id.notif_pct_carbs, if (carbsPct >= snap.streakThresholdPercent) "✓ ${carbsPct}%" else "${carbsPct}%")
      setTextViewText(R.id.notif_val_carbs, "${snap.loggedCarbs}g / ${snap.targetCarbs}g")
      setProgressBar(R.id.notif_prog_carbs, 100, carbsPct.coerceIn(0, 100), false)

      // Pillar 5: Fats
      setTextViewText(R.id.notif_pct_fats, if (fatsPct >= snap.streakThresholdPercent) "✓ ${fatsPct}%" else "${fatsPct}%")
      setTextViewText(R.id.notif_val_fats, "${snap.loggedFats}g / ${snap.targetFats}g")
      setProgressBar(R.id.notif_prog_fats, 100, fatsPct.coerceIn(0, 100), false)

      // Pillar 6: Hydration
      setTextViewText(R.id.notif_pct_water, if (waterPct >= snap.streakThresholdPercent) "✓ ${waterPct}%" else "${waterPct}%")
      setTextViewText(R.id.notif_val_water, "${nf.format(snap.loggedWaterMl)} / ${nf.format(snap.targetWaterMl)} ml")
      setProgressBar(R.id.notif_prog_water, 100, waterPct.coerceIn(0, 100), false)

      // Notification Quick Actions: Strictly Hydration + Open HUD ONLY (Never Step Buttons)
      setOnClickPendingIntent(R.id.notif_btn_water_250, water250Pending)
      setOnClickPendingIntent(R.id.notif_btn_water_500, water500Pending)
      setOnClickPendingIntent(R.id.notif_btn_open_hud, openPending)
      setOnClickPendingIntent(R.id.notif_expanded_root, openPending)
    }

    val streakHeadline = when {
      snap.allSixMet -> "🔥 ${snap.streakDays}d Streak Locked • ${snap.overallReadinessPercent}% Ready"
      snap.isInBufferGrace -> "⏳ ${snap.streakDays}d Streak (Buffer ${snap.bufferDaysRemaining}d) • ${snap.overallReadinessPercent}% Ready"
      else -> "⚡ ${snap.streakDays}d Streak • ${snap.overallReadinessPercent}% Daily Readiness"
    }
    val summaryCompact = "${nf.format(snap.loggedCalories)}/${nf.format(snap.targetCalories)} kcal • " +
      "${snap.loggedProtein}g P • ${nf.format(snap.liveSteps)} steps • ${nf.format(snap.loggedWaterMl)}ml H₂O"

    return NotificationCompat.Builder(appCtx, channelId)
      .setSmallIcon(R.drawable.ic_widget_pulse)
      .setColor(0xFFFFB703.toInt())
      .setContentTitle(streakHeadline)
      .setContentText(summaryCompact)
      .setSubText("BIOSYNC 6-PILLAR HUD")
      .setCustomContentView(collapsedViews)
      .setCustomBigContentView(expandedViews)
      .setCustomHeadsUpContentView(collapsedViews)
      .setPriority(if (ongoing) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT)
      .setCategory(if (ongoing) NotificationCompat.CATEGORY_SERVICE else NotificationCompat.CATEGORY_STATUS)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(ongoing)
      .setOnlyAlertOnce(true)
      .setAutoCancel(!ongoing)
      .setWhen(System.currentTimeMillis())
      .setShowWhen(true)
      .setContentIntent(openPending)
      .build()
  }

  /**
   * Builds and posts the high-contrast Obsidian 6-Pillar native XML notification with
   * full-height expanded window and 1-tap `+250ml H₂O` and `+500ml H₂O` actions.
   */
  fun postInstantTelemetryNotification(
    context: Context,
    showToastFeedback: Boolean = false,
  ): Boolean {
    val appCtx = context.applicationContext
    ensureLiveTelemetryChannel(appCtx, LIVE_TELEMETRY_CHANNEL_ID)

    if (!hasNotificationPermission(appCtx)) {
      if (showToastFeedback) {
        Toast.makeText(appCtx, "Enable Notification permission to display the Live 6-Pillar Telemetry HUD.", Toast.LENGTH_SHORT).show()
      }
      return false
    }

    val notification = buildLiveTelemetryHudNotification(
      context = appCtx,
      ongoing = false,
      channelId = LIVE_TELEMETRY_CHANNEL_ID,
    )

    return runCatching {
      NotificationManagerCompat.from(appCtx).notify(LIVE_TELEMETRY_NOTIFICATION_ID, notification)
      if (showToastFeedback) {
        Toast.makeText(appCtx, "Live 6-Pillar Telemetry HUD notification synced!", Toast.LENGTH_SHORT).show()
      }
      true
    }.getOrDefault(false)
  }

  /**
   * Automatically refreshes the Live 6-Pillar Telemetry HUD notification whenever steps, hydration,
   * or nutrition logs mutate if the user has locked in their protocol and granted notification permission.
   */
  fun refreshLiveTelemetryNotificationIfActive(context: Context) {
    runCatching {
      val appCtx = context.applicationContext
      if (!hasNotificationPermission(appCtx)) return
      val stored = BioSyncLocalStore.load(appCtx)
      if (!stored.protocolLockedIn) return
      val notification = buildLiveTelemetryHudNotification(
        context = appCtx,
        ongoing = false,
        channelId = LIVE_TELEMETRY_CHANNEL_ID,
      )
      NotificationManagerCompat.from(appCtx).notify(LIVE_TELEMETRY_NOTIFICATION_ID, notification)
    }
  }
}

class BioSyncHomeWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val snapshot = BioSyncWidgetAndNotificationEngine.collectSnapshot(context)
    for (widgetId in appWidgetIds) {
      val rv = BioSyncWidgetAndNotificationEngine.buildWidgetRemoteViews(
        context = context,
        appWidgetManager = appWidgetManager,
        appWidgetId = widgetId,
        snapshot = snapshot,
      )
      appWidgetManager.updateAppWidget(widgetId, rv)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle?,
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    val snapshot = BioSyncWidgetAndNotificationEngine.collectSnapshot(context)
    val rv = BioSyncWidgetAndNotificationEngine.buildWidgetRemoteViews(
      context = context,
      appWidgetManager = appWidgetManager,
      appWidgetId = appWidgetId,
      snapshot = snapshot,
    )
    appWidgetManager.updateAppWidget(appWidgetId, rv)
  }
}

class BioSyncWidgetActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    when (action) {
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_WATER_250 -> {
        BioSyncWidgetAndNotificationEngine.handleInstantWaterLogFromExternalSurface(context, 250)
      }
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_WATER_500 -> {
        BioSyncWidgetAndNotificationEngine.handleInstantWaterLogFromExternalSurface(context, 500)
      }
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_250 -> {
        BioSyncWidgetAndNotificationEngine.handleInstantStepLogFromWidget(context, 250)
      }
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_500 -> {
        BioSyncWidgetAndNotificationEngine.handleInstantStepLogFromWidget(context, 500)
      }
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_LOG_STEPS_1000 -> {
        BioSyncWidgetAndNotificationEngine.handleInstantStepLogFromWidget(context, 1000)
      }
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_SYNC_TELEMETRY -> {
        BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
        BioSyncWidgetAndNotificationEngine.refreshLiveTelemetryNotificationIfActive(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        runCatching { context.startActivity(openIntent) }
      }
    }
  }
}

/**
 * Interactive In-App Home Screen Widget & Live Notification Command Studio Card:
 * - Shows a live, real-time rendered preview of the 6-Pillar Obsidian Widget (`CAL`, `PRO`, `STEPS`, `CARBS`, `FATS`, `H2O`)
 * - Includes Widget-Exclusive Quick Step Actuators (`+250`, `+500`, `+1,000 Steps`) right in the studio
 * - Lets the user test auto-resizing across 3 resolution presets (`COMPACT 3×2`, `STANDARD 4×3`, `WIDE 5×3`)
 * - Provides 1-tap **"PIN WIDGET TO HOME SCREEN"** and **"SYNC 6-PILLAR NOTIFICATION"** actions.
 */
@Composable
fun BioSyncHomeWidgetAndNotificationStudioCard(
  haptics: BioSyncHapticController,
  onTriggerNotificationPermissionIfNeeded: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedSizePreset by remember { mutableIntStateOf(1) } // 0 = Compact, 1 = Standard, 2 = Wide
  var refreshTick by remember { mutableIntStateOf(0) }
  var statusFeedback by remember { mutableStateOf<String?>(null) }
  val liveStepState by BioSyncHealthStepManager.syncState.collectAsState()
  val storeRevision by DashboardLocalStore.storeRevisionFlow.collectAsState()

  val snapshot = remember(refreshTick, liveStepState.liveTodaySteps, liveStepState.lastSyncedEpochMillis, storeRevision) {
    BioSyncWidgetAndNotificationEngine.collectSnapshot(context)
  }

  val presetDimensions = when (selectedSizePreset) {
    0 -> 265f to 175f
    2 -> 420f to 215f
    else -> 340f to 220f
  }

  val previewBitmap = remember(snapshot, selectedSizePreset) {
    BioSyncWidgetAndNotificationEngine.renderWidgetTelemetryBitmap(
      snapshot = snapshot,
      widthDp = presetDimensions.first,
      heightDp = presetDimensions.second,
      density = 2.6f,
    )
  }

  val cardShape = RoundedCornerShape(18.dp)
  val platinumMid = Color(0xFFD4D4D8)
  val goldAccent = Color(0xFFFFD166)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(cardShape)
      .drawWithCache {
        val cornerPx = 18.dp.toPx()
        val bodyBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to Color(0xFF171820),
            0.45f to Color(0xFF0E0F15),
            1.0f to Color(0xFF07070A),
          ),
        )
        val borderBrush = Brush.verticalGradient(
          colorStops = arrayOf(
            0.0f to PureWhite.copy(alpha = 0.65f),
            0.35f to platinumMid.copy(alpha = 0.32f),
            1.0f to Color(0xFF1D1E28),
          ),
        )
        onDrawBehind {
          drawRoundRect(
            brush = bodyBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )
          drawRoundRect(
            brush = borderBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = 1.2.dp.toPx()),
          )
          drawLine(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color.Transparent,
                PureWhite.copy(alpha = 0.88f),
                StepsStart.copy(alpha = 0.65f),
                PureWhite.copy(alpha = 0.85f),
                Color.Transparent,
              ),
            ),
            start = Offset(cornerPx * 0.8f, 1.dp.toPx()),
            end = Offset(size.width - cornerPx * 0.8f, 1.dp.toPx()),
            strokeWidth = 1.2.dp.toPx(),
          )
        }
      }
      .padding(16.dp)
      .testTag("settings_widget_and_notification_studio_card"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Header Row
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
              .size(7.dp)
              .clip(CircleShape)
              .background(StepsStart),
          )
          Text(
            text = "HOME WIDGET & 6-PILLAR NOTIFICATION HUD",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.6.sp,
            letterSpacing = 0.8.sp,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Widget-exclusive step controls (+250 / +500 / +1,000) & full-height native 6-pillar notification shade",
          color = Color(0xFFD4D4D8),
          fontFamily = InterFontFamily,
          fontSize = 11.2.sp,
          lineHeight = 15.5.sp,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(7.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(PureWhite, goldAccent),
            ),
          )
          .padding(horizontal = 8.5.dp, vertical = 4.dp),
      ) {
        Text(
          text = "NATIVE DPI",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 8.4.sp,
          letterSpacing = 0.5.sp,
        )
      }
    }

    HorizontalDivider(color = Color(0xFF232430), thickness = 0.8.dp)

    // 2. Resolution / Aspect Ratio Preview Switcher
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val presets = listOf(
        0 to "COMPACT (3×2)",
        1 to "STANDARD (4×3)",
        2 to "EXPANDED (5×3)",
      )
      presets.forEach { (idx, label) ->
        val selected = selectedSizePreset == idx
        Box(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else Color(0xFF101117))
            .border(
              width = 1.dp,
              color = if (selected) PureWhite else Color(0xFF272732),
              shape = RoundedCornerShape(8.dp),
            )
            .clickable(role = Role.Tab) {
              haptics.selection()
              selectedSizePreset = idx
            }
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("widget_size_preset_$idx"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            color = if (selected) ObsidianBlack else Color(0xFFD4D4D8),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // 3. Live High-DPI Rendered Widget Preview
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF050508))
        .border(1.dp, Color(0xFF282A38), RoundedCornerShape(14.dp))
        .padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        bitmap = previewBitmap.asImageBitmap(),
        contentDescription = "BioSync 6-Pillar Home Widget Live Preview",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(11.dp)),
      )
    }

    // 4. Interactive Widget-Only Step Quick-Add Strip (Matches Home Screen Widget Dock Row 1)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(11.dp))
        .background(Color(0xFF0A0D0C))
        .border(1.dp, StepsStart.copy(alpha = 0.42f), RoundedCornerShape(11.dp))
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "WIDGET-ONLY STEP ACTUATORS (EXCLUDED FROM NOTIFICATIONS)",
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 8.0.sp,
          letterSpacing = 0.5.sp,
        )
        Text(
          text = "${NumberFormat.getIntegerInstance(Locale.US).format(snapshot.liveSteps)} STEPS",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 8.4.sp,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        val stepIncrements = listOf(
          250 to "+250 STEPS",
          500 to "+500 STEPS",
          1000 to "+1,000 STEPS",
        )
        stepIncrements.forEach { (stepDelta, label) ->
          Box(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 38.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF10211B))
              .border(1.dp, StepsStart.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
              .clickable(role = Role.Button) {
                haptics.confirm()
                BioSyncWidgetAndNotificationEngine.handleInstantStepLogFromWidget(
                  context = context,
                  addedSteps = stepDelta,
                  showToast = false,
                )
                refreshTick += 1
                statusFeedback = "Logged +$stepDelta Steps via Widget Engine • Home Widget & 6-Pillar HUD synced!"
              }
              .padding(horizontal = 6.dp, vertical = 7.dp)
              .testTag("widget_quick_add_steps_$stepDelta"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = label,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.5.sp,
              maxLines = 1,
            )
          }
        }
      }
    }

    // 5. Primary Action Buttons: Pin to Home Screen + Sync Full-Height 6-Pillar Live Notification
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(PureWhite, Color(0xFFE4E4E7)),
            ),
          )
          .border(1.dp, PureWhite, RoundedCornerShape(10.dp))
          .clickable(role = Role.Button) {
            haptics.confirm()
            BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
            val pinned = BioSyncWidgetAndNotificationEngine.requestPinWidgetToHomeScreen(context)
            statusFeedback = if (pinned) {
              "Home Screen Widget pin prompt opened! Includes +250, +500 & +1,000 Step buttons."
            } else {
              "Widget synced! Long-press your device Home Screen → Widgets → BioSync to place & resize."
            }
            refreshTick += 1
          }
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .testTag("pin_home_widget_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "PIN WIDGET TO HOME",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.5.sp,
          letterSpacing = 0.4.sp,
          maxLines = 1,
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF141622))
          .border(1.dp, HydrationStart.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
          .clickable(role = Role.Button) {
            haptics.confirm()
            onTriggerNotificationPermissionIfNeeded()
            val sent = BioSyncWidgetAndNotificationEngine.postInstantTelemetryNotification(
              context = context,
              showToastFeedback = true,
            )
            statusFeedback = if (sent) {
              "Full-height 6-Pillar Native XML Notification dispatched to status bar (Hydration + Open HUD actions only)!"
            } else {
              "Grant Notification permission to view the live 6-Pillar Telemetry HUD notification."
            }
            refreshTick += 1
          }
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .testTag("send_instant_hud_notification_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "SYNC 6-PILLAR NOTIF",
          color = HydrationStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.5.sp,
          letterSpacing = 0.4.sp,
          maxLines = 1,
        )
      }
    }

    if (!statusFeedback.isNullOrBlank()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0B1612))
          .border(1.dp, StepsStart.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp),
      ) {
        Text(
          text = "✓ ${statusFeedback.orEmpty()}",
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 8.8.sp,
        )
      }
    }
  }
}
