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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
  const val ACTION_WIDGET_SYNC_TELEMETRY = "com.example.action.WIDGET_SYNC_TELEMETRY"

  private const val LIVE_TELEMETRY_CHANNEL_ID = "biosync_live_telemetry_hud_v2"
  private const val LIVE_TELEMETRY_NOTIFICATION_ID = 9420

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
    val sensorSteps = maxOf(
      BioSyncHealthStepManager.syncState.value.liveTodaySteps.coerceAtLeast(0),
      persistedStepsForToday,
    )
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
   * Renders a high-DPI, auto-resizable 3D Obsidian Widget Bitmap that adapts cleanly to ANY
   * widget resolution, launcher grid cell size, and aspect ratio (Compact, Standard, or Wide/Expanded).
   */
  fun renderWidgetTelemetryBitmap(
    snapshot: BioSyncWidgetTelemetrySnapshot,
    widthDp: Float,
    heightDp: Float,
    density: Float,
  ): Bitmap {
    val safeDensity = density.coerceIn(1.5f, 3.5f)
    val safeW = widthDp.coerceIn(180f, 640f)
    val safeH = heightDp.coerceIn(115f, 480f)
    val pxW = (safeW * safeDensity).roundToInt().coerceIn(320, 1800)
    val pxH = (safeH * safeDensity).roundToInt().coerceIn(210, 1400)

    val bmp = Bitmap.createBitmap(pxW, pxH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val nf = NumberFormat.getIntegerInstance(Locale.US)

    val w = pxW.toFloat()
    val h = pxH.toFloat()
    // Adaptive scale factor across compact, standard, and expanded resolutions
    val scale = min(w / (330f * safeDensity), h / (215f * safeDensity)).coerceIn(0.68f, 1.38f)
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

    // Subtle top ambient aura
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader = LinearGradient(
        cardRect.left,
        cardRect.top,
        cardRect.right,
        cardRect.top + h * 0.35f,
        intArrayOf(
          0x18FFB703,
          0x1206B6D4,
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
          0x66FFFFFF,
          0x3371717A,
          0xFF1C1D26.toInt(),
        ),
        floatArrayOf(0f, 0.4f, 1f),
        Shader.TileMode.CLAMP,
      )
    }
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

    val contentLeft = cardRect.left + dp(12f)
    val contentRight = cardRect.right - dp(12f)
    var cursorY = cardRect.top + dp(10f)

    // 2. Top Executive Telemetry Header
    val headerH = dp(28f)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = if (snapshot.allSixMet) 0xFF10B981.toInt() else 0xFFFFB703.toInt()
    }
    canvas.drawCircle(contentLeft + dp(4.5f), cursorY + headerH * 0.34f, dp(3.8f), dotPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = monoBold
      textSize = dp(10.5f)
      color = 0xFFFFFFFF.toInt()
      letterSpacing = 0.06f
    }
    canvas.drawText("BIOSYNC TELEMETRY HUD", contentLeft + dp(13f), cursorY + dp(11.5f), titlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = sansReg
      textSize = dp(8.5f)
      color = 0xFFA1A1AA.toInt()
    }
    val subText = "${snapshot.subjectName} • ${snapshot.overallReadinessPercent}% DAILY READINESS • GATE ${snapshot.streakThresholdPercent}%"
    canvas.drawText(subText, contentLeft + dp(13f), cursorY + dp(23f), subPaint)

    // Top-right Streak & Buffer Badge
    val streakLabel = when {
      snapshot.allSixMet -> "🔥 ${snapshot.streakDays}D LOCKED"
      snapshot.isInBufferGrace -> "⏳ ${snapshot.streakDays}D • BUF ${snapshot.bufferDaysRemaining}D"
      else -> "🔥 ${snapshot.streakDays}D STREAK"
    }
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      typeface = monoBold
      textSize = dp(9.0f)
      color = 0xFF060609.toInt()
    }
    val badgeTextW = badgeTextPaint.measureText(streakLabel)
    val badgeW = badgeTextW + dp(14f)
    val badgeH = dp(20f)
    val badgeRect = RectF(contentRight - badgeW, cursorY + dp(3f), contentRight, cursorY + dp(3f) + badgeH)
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
    canvas.drawRoundRect(badgeRect, dp(6.5f), dp(6.5f), badgeBgPaint)
    canvas.drawText(
      streakLabel,
      badgeRect.left + dp(7f),
      badgeRect.centerY() + dp(3.2f),
      badgeTextPaint,
    )

    cursorY += headerH + dp(5f)

    // Divider line
    val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFF23242F.toInt()
      strokeWidth = max(1f, dp(0.8f))
    }
    canvas.drawLine(contentLeft, cursorY, contentRight, cursorY, divPaint)
    cursorY += dp(6f)

    // 3. 6-Pillar Bento Grid (Automatically chooses 3×2 or 2×3 layout depending on aspect ratio!)
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

    val dockHeight = dp(28f)
    val dockTopGap = dp(6f)
    val gridBottom = cardRect.bottom - dp(8f) - dockHeight - dockTopGap
    val availableGridH = (gridBottom - cursorY).coerceAtLeast(dp(70f))
    val availableGridW = contentRight - contentLeft

    val aspect = availableGridW / availableGridH
    val cols = if (aspect >= 1.55f) 3 else 2
    val rows = if (cols == 3) 2 else 3
    val colGap = dp(6f)
    val rowGap = dp(6f)
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
      val cellRadius = dp(10f)

      canvas.drawRoundRect(cellRect, cellRadius, cellRadius, chamberBgPaint)
      chamberBorderPaint.color = if (pillar.qualifiesForStreak) {
        (pillar.startColorArgb and 0x00FFFFFF) or 0x88000000.toInt()
      } else {
        0xFF242632.toInt()
      }
      canvas.drawRoundRect(cellRect, cellRadius, cellRadius, chamberBorderPaint)

      val padX = dp(8f)
      val innerLeft = cellRect.left + padX
      val innerRight = cellRect.right - padX

      // Top row inside cell: Colored Pill + Pillar Name + % Badge
      val topRowY = cellRect.top + dp(11.5f)
      val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillar.startColorArgb
      }
      canvas.drawRoundRect(
        RectF(innerLeft, topRowY - dp(7f), innerLeft + dp(3.2f), topRowY + dp(1.2f)),
        dp(1.6f),
        dp(1.6f),
        indicatorPaint,
      )

      val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoBold
        textSize = dp(8.2f)
        color = 0xFFE4E4E7.toInt()
      }
      canvas.drawText(pillar.title, innerLeft + dp(6.5f), topRowY, labelPaint)

      val pctString = if (pillar.qualifiesForStreak) "✓ ${pillar.percentInt}%" else "${pillar.percentInt}%"
      val pctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoBold
        textSize = dp(8.0f)
        color = if (pillar.qualifiesForStreak) pillar.startColorArgb else 0xFFA1A1AA.toInt()
        textAlign = Paint.Align.RIGHT
      }
      canvas.drawText(pctString, innerRight, topRowY, pctPaint)

      // Middle row: Primary Value + / Target
      val valueBaselineY = cellRect.centerY() + dp(4.5f)
      val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sansBold
        textSize = dp(12.8f)
        color = 0xFFFFFFFF.toInt()
      }
      canvas.drawText(pillar.valueFormatted, innerLeft, valueBaselineY, valPaint)
      val valW = valPaint.measureText(pillar.valueFormatted)

      val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoBold
        textSize = dp(7.8f)
        color = 0xFF71717A.toInt()
      }
      val targetStr = " / ${pillar.targetFormatted}"
      if (innerLeft + valW + targetPaint.measureText(targetStr) <= innerRight) {
        canvas.drawText(targetStr, innerLeft + valW, valueBaselineY - dp(0.5f), targetPaint)
      }

      // Bottom row: Luminous Progress Track + Streak Threshold Gate Marker
      val barH = dp(5.2f)
      val barTop = cellRect.bottom - dp(6.5f) - barH
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

      // Streak gate marker line on progress track
      val gateX = trackRect.left + trackRect.width() * (snapshot.streakThresholdPercent / 100f).coerceIn(0.5f, 1f)
      val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        strokeWidth = max(1f, dp(1f))
      }
      canvas.drawLine(gateX, trackRect.top - dp(1f), gateX, trackRect.bottom + dp(1f), gatePaint)
    }

    // 4. Bottom Interactive Quick-Action Dock (+250ml H2O | +500ml H2O | SYNC & OPEN)
    val dockTop = gridBottom + dockTopGap
    val dockBottom = cardRect.bottom - dp(8f)
    val btnGap = dp(6f)
    val btnW = (availableGridW - btnGap * 2f) / 3f

    val actions = listOf(
      Triple("+250ML H₂O", 0xFF06B6D4.toInt(), false),
      Triple("+500ML H₂O", 0xFF38BDF8.toInt(), false),
      Triple("OPEN HUD ↗", 0xFFFFFFFF.toInt(), true),
    )

    actions.forEachIndexed { idx, (label, accentArgb, isPrimaryWhite) ->
      val bLeft = contentLeft + idx * (btnW + btnGap)
      val bRect = RectF(bLeft, dockTop, bLeft + btnW, dockBottom)
      val bRadius = dp(7.5f)

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
          color = 0xFF13151E.toInt()
        }
      }
      canvas.drawRoundRect(bRect, bRadius, bRadius, bBgPaint)

      if (!isPrimaryWhite) {
        val bStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = max(1f, dp(0.9f))
          color = (accentArgb and 0x00FFFFFF) or 0x77000000.toInt()
        }
        canvas.drawRoundRect(bRect, bRadius, bRadius, bStroke)
      }

      val bTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoBold
        textSize = dp(8.2f)
        color = if (isPrimaryWhite) 0xFF060609.toInt() else accentArgb
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(label, bRect.centerX(), bRect.centerY() + dp(2.9f), bTextPaint)
    }

    return bmp
  }

  /**
   * Builds the RemoteViews for a specific widget instance using its current launcher dimensions
   * so it scales and auto-resizes cleanly across all screen densities and grid configurations.
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
    val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 185)?.takeIf { it > 0 } ?: 185
    val maxH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)?.takeIf { it > 0 } ?: minH

    val resolvedW = ((minW + maxW) / 2f).coerceIn(190f, 560f)
    val resolvedH = ((minH + maxH) / 2f).coerceIn(125f, 420f)
    val density = context.resources.displayMetrics.density.coerceIn(1.5f, 3.2f)

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

    val water250Intent = Intent(context, BioSyncWidgetActionReceiver::class.java).apply {
      action = ACTION_WIDGET_LOG_WATER_250
    }
    val water250Pending = PendingIntent.getBroadcast(
      context,
      9200 + appWidgetId,
      water250Intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_water_250, water250Pending)

    val water500Intent = Intent(context, BioSyncWidgetActionReceiver::class.java).apply {
      action = ACTION_WIDGET_LOG_WATER_500
    }
    val water500Pending = PendingIntent.getBroadcast(
      context,
      9300 + appWidgetId,
      water500Intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    views.setOnClickPendingIntent(R.id.widget_btn_water_500, water500Pending)

    val syncIntent = Intent(context, BioSyncWidgetActionReceiver::class.java).apply {
      action = ACTION_WIDGET_SYNC_TELEMETRY
    }
    val syncPending = PendingIntent.getBroadcast(
      context,
      9400 + appWidgetId,
      syncIntent,
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
   * Logs quick hydration directly from either the Home Screen Widget or Notification Action button,
   * re-evaluates the 2-day buffer streak, saves to `DashboardLocalStore`, and refreshes all surfaces.
   */
  fun handleInstantWaterLogFromExternalSurface(context: Context, addedMl: Int) {
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

    // Save once so snapshot calculation sees the new entry
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

  fun ensureLiveTelemetryChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val channel = NotificationChannel(
        LIVE_TELEMETRY_CHANNEL_ID,
        "BioSync Obsidian Telemetry HUD",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Instant visual 6-pillar telemetry HUD notifications with 1-tap hydration logging."
        enableVibration(true)
        enableLights(true)
        lightColor = android.graphics.Color.parseColor("#FFB703")
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      nm.createNotificationChannel(channel)
    }
  }

  /**
   * Builds and posts an instant, high-contrast Obsidian 6-Pillar visual notification with
   * BigPicture telemetry preview + 1-tap `+250ml H₂O` and `+500ml H₂O` action buttons.
   */
  fun postInstantTelemetryNotification(
    context: Context,
    showToastFeedback: Boolean = false,
  ): Boolean {
    val appCtx = context.applicationContext
    ensureLiveTelemetryChannel(appCtx)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        appCtx,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        if (showToastFeedback) {
          Toast.makeText(appCtx, "Enable Notification permission to display the Live Telemetry HUD.", Toast.LENGTH_SHORT).show()
        }
        return false
      }
    }

    val snap = collectSnapshot(appCtx)
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    val bannerBitmap = renderWidgetTelemetryBitmap(
      snapshot = snap,
      widthDp = 360f,
      heightDp = 195f,
      density = 2.4f,
    )

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

    val streakHeadline = when {
      snap.allSixMet -> "🔥 ${snap.streakDays}d Streak Locked • ${snap.overallReadinessPercent}% Ready"
      snap.isInBufferGrace -> "⏳ ${snap.streakDays}d Streak (Buffer ${snap.bufferDaysRemaining}d) • ${snap.overallReadinessPercent}% Ready"
      else -> "⚡ ${snap.streakDays}d Streak • ${snap.overallReadinessPercent}% Daily Readiness"
    }
    val summaryCompact = "${nf.format(snap.loggedCalories)}/${nf.format(snap.targetCalories)} kcal • " +
      "${snap.loggedProtein}g P • ${nf.format(snap.liveSteps)} steps • ${nf.format(snap.loggedWaterMl)}ml H₂O"

    val notification = NotificationCompat.Builder(appCtx, LIVE_TELEMETRY_CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_widget_pulse)
      .setColor(0xFFFFB703.toInt())
      .setColorized(false)
      .setContentTitle(streakHeadline)
      .setContentText(summaryCompact)
      .setSubText("BIOSYNC 6-PILLAR HUD")
      .setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(bannerBitmap)
          .setBigContentTitle(streakHeadline)
          .setSummaryText(
            "CAL ${nf.format(snap.loggedCalories)} • PRO ${snap.loggedProtein}g • " +
              "CARB ${snap.loggedCarbs}g • FAT ${snap.loggedFats}g • " +
              "H₂O ${nf.format(snap.loggedWaterMl)}ml • STEPS ${nf.format(snap.liveSteps)}",
          ),
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOnlyAlertOnce(true)
      .setAutoCancel(true)
      .setWhen(System.currentTimeMillis())
      .setShowWhen(true)
      .setContentIntent(openPending)
      .addAction(R.drawable.ic_widget_plus, "+250ml H₂O", water250Pending)
      .addAction(R.drawable.ic_widget_plus, "+500ml H₂O", water500Pending)
      .addAction(R.drawable.ic_widget_flame, "Open HUD", openPending)
      .build()

    return runCatching {
      NotificationManagerCompat.from(appCtx).notify(LIVE_TELEMETRY_NOTIFICATION_ID, notification)
      if (showToastFeedback) {
        Toast.makeText(appCtx, "Live Obsidian Telemetry HUD notification sent!", Toast.LENGTH_SHORT).show()
      }
      true
    }.getOrDefault(false)
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
      BioSyncWidgetAndNotificationEngine.ACTION_WIDGET_SYNC_TELEMETRY -> {
        BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
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
 * - Lets the user test auto-resizing across 3 resolution presets (`COMPACT 3×2`, `STANDARD 4×3`, `WIDE 5×3`)
 * - Provides 1-tap **"PIN WIDGET TO HOME SCREEN"** and **"SEND INSTANT HUD NOTIFICATION"** actions.
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

  val snapshot = remember(refreshTick) {
    BioSyncWidgetAndNotificationEngine.collectSnapshot(context)
  }

  val presetDimensions = when (selectedSizePreset) {
    0 -> 250f to 150f
    2 -> 410f to 195f
    else -> 330f to 205f
  }

  val previewBitmap = remember(snapshot, selectedSizePreset) {
    BioSyncWidgetAndNotificationEngine.renderWidgetTelemetryBitmap(
      snapshot = snapshot,
      widthDp = presetDimensions.first,
      heightDp = presetDimensions.second,
      density = 2.5f,
    )
  }

  val cardShape = RoundedCornerShape(15.dp)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(cardShape)
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF13131A), Color(0xFF09090D)),
        ),
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(PureWhite.copy(alpha = 0.26f), Color(0xFF272732), Color(0xFF16161D)),
        ),
        shape = cardShape,
      )
      .padding(15.dp)
      .testTag("settings_widget_and_notification_studio_card"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Header Row
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
              .background(CaloriesStart),
          )
          Text(
            text = "HOME SCREEN WIDGET & LIVE HUD ENGINE",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.8.sp,
            letterSpacing = 0.9.sp,
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Auto-resizable 6-pillar widget (Cal, Pro, Steps, Carbs, Fats, Hydration) & instant HUD notification",
          color = Color(0xFFA1A1AA),
          fontFamily = InterFontFamily,
          fontSize = 11.2.sp,
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(
            Brush.horizontalGradient(
              colors = listOf(PureWhite, Color(0xFFFFD166)),
            ),
          )
          .padding(horizontal = 8.dp, vertical = 3.5.dp),
      ) {
        Text(
          text = "AUTO-RESIZE",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 8.5.sp,
          letterSpacing = 0.5.sp,
        )
      }
    }

    HorizontalDivider(color = Color(0xFF1E1E28), thickness = 0.8.dp)

    // Resolution / Aspect Ratio Preview Switcher
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
            .heightIn(min = 34.dp)
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
            fontSize = 8.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // Live High-DPI Rendered Widget Preview
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF050508))
        .border(1.dp, Color(0xFF252632), RoundedCornerShape(14.dp))
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

    // Action Buttons: Pin to Home Screen + Send Instant Live Notification
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
              "Home Screen Widget pin prompt opened! You can also resize it freely on your launcher."
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
          fontSize = 9.6.sp,
          letterSpacing = 0.4.sp,
          maxLines = 1,
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF14151E))
          .border(1.dp, HydrationStart.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
          .clickable(role = Role.Button) {
            haptics.confirm()
            onTriggerNotificationPermissionIfNeeded()
            val sent = BioSyncWidgetAndNotificationEngine.postInstantTelemetryNotification(
              context = context,
              showToastFeedback = true,
            )
            statusFeedback = if (sent) {
              "Instant 6-Pillar Obsidian HUD Notification dispatched to status bar!"
            } else {
              "Grant Notification permission to view the live Obsidian HUD notification."
            }
            refreshTick += 1
          }
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .testTag("send_instant_hud_notification_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "INSTANT HUD NOTIF",
          color = HydrationStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.6.sp,
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
          .background(Color(0xFF0E1318))
          .border(1.dp, StepsStart.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp),
      ) {
        Text(
          text = statusFeedback.orEmpty(),
          color = StepsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.2.sp,
        )
      }
    }
  }
}
