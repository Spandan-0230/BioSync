package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * BioSync High-Precision Metabolic Home Screen Widget (Non-Resizable 4x2)
 *
 * Displays live telemetry across the 6 core physiological pillars:
 * 1. STEPS (Count & Progress)
 * 2. CALORIES (Kcal & Progress)
 * 3. HYDRATION (Water mL & Progress)
 * 4. PROTEIN (Grams & Progress)
 * 5. CARBS (Grams & Progress)
 * 6. FATS (Grams & Progress)
 * Along with active streak ignition chain and buffer status.
 */
class BioSyncAppWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    for (widgetId in appWidgetIds) {
      val views = buildRemoteViews(context)
      appWidgetManager.updateAppWidget(widgetId, views)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH_WIDGET) {
      updateAllWidgets(context)
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.example.biosync.ACTION_REFRESH_WIDGET"

    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext) ?: return
      val componentName = ComponentName(context.applicationContext, BioSyncAppWidgetProvider::class.java)
      val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
      if (widgetIds.isNotEmpty()) {
        val views = buildRemoteViews(context.applicationContext)
        appWidgetManager.updateAppWidget(widgetIds, views)
      }
    }

    fun buildRemoteViews(context: Context): RemoteViews {
      val views = RemoteViews(context.packageName, R.layout.biosync_home_widget)
      val numFormat = NumberFormat.getIntegerInstance(Locale.US)

      // 1. Load active user profile targets and fresh day telemetry
      val profile = BioSyncLocalStore.load(context)
      val todayKey = DashboardTimeFormatter.currentDayKey()
      val dash = DashboardLocalStore.load(context)
      val activeData = if (dash.dayKey != todayKey) {
        DashboardLocalStore.rolloverToDate(context, dash, todayKey)
      } else {
        dash
      }
      val todayRec = activeData.dailyHistoryMap[todayKey]

      val targetSteps = (todayRec?.targetStepsCount ?: profile.customStepsCount).coerceAtLeast(1000)
      val targetCalories = (todayRec?.targetCaloriesKcal ?: profile.customCaloriesKcal).coerceAtLeast(1000)
      val targetProtein = (todayRec?.targetProteinGrams ?: profile.customProteinGrams).coerceAtLeast(30)
      val targetCarbs = (todayRec?.targetCarbsGrams ?: profile.customCarbsGrams).coerceAtLeast(40)
      val targetFats = (todayRec?.targetFatsGrams ?: profile.customFatsGrams).coerceAtLeast(20)
      val targetWaterMl = (todayRec?.targetHydrationMl ?: profile.customHydrationMl).coerceAtLeast(500)
      val streakThresholdPercent = profile.streakThreshold.percentage


      val entries = activeData.todayTimelineEntries
      val loggedCal = entries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
      val loggedPro = entries.sumOf { it.proteinGrams }.coerceAtLeast(0)
      val loggedCarb = entries.sumOf { it.carbsGrams }.coerceAtLeast(0)
      val loggedFat = entries.sumOf { it.fatsGrams }.coerceAtLeast(0)
      val loggedWater = entries.sumOf { it.hydrationMl }.coerceAtLeast(0)

      val sensorSteps = max(0, profile.todaySyncedSteps)
      val manualSteps = activeData.manualStepsToday.coerceAtLeast(0)
      val totalSteps = (sensorSteps + manualSteps).coerceAtLeast(0)

      // 3. Evaluate streak and buffer status
      val stepsQual = ((totalSteps * 100f) / targetSteps).roundToInt() >= streakThresholdPercent
      val calQual = ((loggedCal * 100f) / targetCalories).roundToInt() >= streakThresholdPercent
      val proQual = ((loggedPro * 100f) / targetProtein).roundToInt() >= streakThresholdPercent
      val carbQual = ((loggedCarb * 100f) / targetCarbs).roundToInt() >= streakThresholdPercent
      val fatQual = ((loggedFat * 100f) / targetFats).roundToInt() >= streakThresholdPercent
      val waterQual = ((loggedWater * 100f) / targetWaterMl).roundToInt() >= streakThresholdPercent

      val qualifiedCount = listOf(stepsQual, calQual, proQual, carbQual, fatQual, waterQual).count { it }
      val allSixMet = qualifiedCount == 6

      val streakEval = BioSyncStreakEngine.evaluateStreak(
        storedBaseStreak = activeData.baseStreakDays,
        lastCompletedDayKey = activeData.lastCompletedStreakDayKey,
        todayKey = todayKey,
        todayQualified = allSixMet,
      )

      // 4. Bind Streak & Subtitle
      val streakLabel = when {
        allSixMet -> "${streakEval.streakDays}D • LOCKED"
        streakEval.isInBufferGrace -> "${streakEval.streakDays}D • BUFFER"
        streakEval.streakDays > 0 -> "${streakEval.streakDays}D STREAK"
        else -> "0D STREAK"
      }
      views.setTextViewText(R.id.widget_streak_text, streakLabel)
      views.setTextViewText(
        R.id.widget_status_subtext,
        "$qualifiedCount / 6 PILLARS LOCKED • TODAY",
      )

      // 5. Bind Pillars: STEPS
      val stepsPct = ((totalSteps * 100f) / targetSteps).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_steps_val, numFormat.format(totalSteps))
      views.setProgressBar(R.id.widget_steps_bar, 100, stepsPct, false)

      // 6. Bind Pillars: CALORIES
      val calPct = ((loggedCal * 100f) / targetCalories).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_kcal_val, "${numFormat.format(loggedCal)} kcal")
      views.setProgressBar(R.id.widget_kcal_bar, 100, calPct, false)

      // 7. Bind Pillars: WATER
      val waterPct = ((loggedWater * 100f) / targetWaterMl).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_water_val, "${numFormat.format(loggedWater)} ml")
      views.setProgressBar(R.id.widget_water_bar, 100, waterPct, false)

      // 8. Bind Pillars: PROTEIN
      val proPct = ((loggedPro * 100f) / targetProtein).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_protein_val, "${loggedPro}g")
      views.setProgressBar(R.id.widget_protein_bar, 100, proPct, false)

      // 9. Bind Pillars: CARBS
      val carbPct = ((loggedCarb * 100f) / targetCarbs).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_carbs_val, "${loggedCarb}g")
      views.setProgressBar(R.id.widget_carbs_bar, 100, carbPct, false)

      // 10. Bind Pillars: FATS
      val fatPct = ((loggedFat * 100f) / targetFats).roundToInt().coerceIn(0, 100)
      views.setTextViewText(R.id.widget_fats_val, "${loggedFat}g")
      views.setProgressBar(R.id.widget_fats_bar, 100, fatPct, false)

      // 11. Launch Main Dashboard upon clicking the widget
      val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

      return views
    }
  }
}
