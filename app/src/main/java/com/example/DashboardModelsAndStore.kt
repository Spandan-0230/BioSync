package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

enum class DashboardEntryType(
  val badgeLabel: String,
) {
  SINGLE_ITEM("SINGLE ITEM"),
  MEAL("MEAL"),
  HYDRATION("HYDRATION"),
}

/**
 * Curated & Custom Color Alchemy engine for Quick Actions Single Items & Combined Meals.
 * Supports:
 * - 12 distinct signature color swatches
 * - Custom 360° HSV spectrum & Hex color creation
 * - Perceptual optical color mixing when combining 2+ Single Items into a Meal
 */
@Immutable
data class QuickColorPreset(
  val name: String,
  val argb: Long,
)

object QuickItemColorAlchemy {
  val presets: List<QuickColorPreset> = listOf(
    QuickColorPreset("Solar Amber", 0xFFFFB703L),
    QuickColorPreset("Crimson Ruby", 0xFFF43F5EL),
    QuickColorPreset("Electric Amethyst", 0xFFA855F7L),
    QuickColorPreset("Neon Orchid", 0xFFEC4899L),
    QuickColorPreset("Champagne Gold", 0xFFE5DEC9L),
    QuickColorPreset("Sunset Coral", 0xFFFF6B4AL),
    QuickColorPreset("Cyber Emerald", 0xFF10B981L),
    QuickColorPreset("Arctic Cyan", 0xFF06B6D4L),
    QuickColorPreset("Royal Indigo", 0xFF6366F1L),
    QuickColorPreset("Lime Chartreuse", 0xFFA3E635L),
    QuickColorPreset("Burnished Copper", 0xFFD97736L),
    QuickColorPreset("Brushed Platinum", 0xFFE4E4E7L),
  )

  fun defaultColorForItem(name: String, isMeal: Boolean): Long {
    val hash = kotlin.math.abs(name.trim().lowercase(Locale.US).hashCode())
    return if (isMeal) {
      presets[(hash + 2) % presets.size].argb
    } else {
      presets[hash % presets.size].argb
    }
  }

  fun argbToColor(argb: Long): Color {
    val safe = if (argb == 0L) 0xFFFFB703L else (argb or 0xFF000000L)
    return Color(safe.toInt())
  }

  fun colorToArgbLong(color: Color): Long {
    return (color.toArgb().toLong() and 0xFFFFFFFFL) or 0xFF000000L
  }

  /**
   * Mixes 2 or more Single Item colors into a vibrant, perceptually balanced Combined Meal color
   * using root-mean-square RGB + chroma preservation so mixed colors stay luminous and rich.
   */
  fun mixItemColors(argbs: List<Long>): Long {
    val valid = argbs.filter { it != 0L }
    if (valid.isEmpty()) return 0xFFA855F7L
    if (valid.size == 1) return valid.first() or 0xFF000000L

    var sumR2 = 0.0
    var sumG2 = 0.0
    var sumB2 = 0.0
    for (raw in valid) {
      val c = argbToColor(raw)
      sumR2 += (c.red * c.red).toDouble()
      sumG2 += (c.green * c.green).toDouble()
      sumB2 += (c.blue * c.blue).toDouble()
    }
    val n = valid.size.toDouble()
    var r = sqrt(sumR2 / n).toFloat().coerceIn(0f, 1f)
    var g = sqrt(sumG2 / n).toFloat().coerceIn(0f, 1f)
    var b = sqrt(sumB2 / n).toFloat().coerceIn(0f, 1f)

    // Boost saturation slightly if opposite colors partially cancelled out
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
      (r * 255).roundToInt().coerceIn(0, 255),
      (g * 255).roundToInt().coerceIn(0, 255),
      (b * 255).roundToInt().coerceIn(0, 255),
      hsv,
    )
    hsv[1] = (hsv[1] * 1.18f).coerceIn(0.35f, 0.95f)
    hsv[2] = hsv[2].coerceIn(0.78f, 1.0f)
    val boostedInt = android.graphics.Color.HSVToColor(hsv)
    return (boostedInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L
  }

  fun hsvToArgbLong(hue: Float, saturation: Float, value: Float): Long {
    val hsv = floatArrayOf(
      hue.coerceIn(0f, 360f),
      saturation.coerceIn(0.15f, 1f),
      value.coerceIn(0.45f, 1f),
    )
    val cInt = android.graphics.Color.HSVToColor(hsv)
    return (cInt.toLong() and 0xFFFFFFFFL) or 0xFF000000L
  }

  fun argbToHexCode(argb: Long): String {
    val rgb = (argb and 0xFFFFFFL).toInt()
    return String.format(Locale.US, "#%06X", rgb)
  }

  fun parseHexToArgbOrNull(rawHex: String): Long? {
    val cleaned = rawHex.trim().removePrefix("#").uppercase(Locale.US)
    if (cleaned.length != 6) return null
    val parsed = cleaned.toLongOrNull(16) ?: return null
    return parsed or 0xFF000000L
  }
}

/**
 * A user-saved Single Item or Meal stored in the Quick Actions shelf.
 * Also used as the building block for selecting multiple Single Items or
 * holding for 2 seconds and overlapping one Single Item onto another to create a Meal.
 */
@Immutable
data class SavedQuickNutritionItem(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val isMeal: Boolean,
  val caloriesKcal: Int,
  val proteinGrams: Int,
  val carbsGrams: Int,
  val fatsGrams: Int,
  val componentNames: List<String> = emptyList(),
  val isSavedToQuickActions: Boolean = true,
  val colorArgb: Long = QuickItemColorAlchemy.defaultColorForItem(name, isMeal),
  val mixedFromColorArgbs: List<Long> = emptyList(),
  val createdAtEpochMs: Long = System.currentTimeMillis(),
)

/**
 * A chronological log entry shown in "Today's Timeline" at the bottom of the Dashboard.
 */
@Immutable
data class DashboardTimelineEntry(
  val id: String = UUID.randomUUID().toString(),
  val type: DashboardEntryType,
  val title: String,
  val subtitle: String = "",
  val caloriesKcal: Int = 0,
  val proteinGrams: Int = 0,
  val carbsGrams: Int = 0,
  val fatsGrams: Int = 0,
  val hydrationMl: Int = 0,
  val colorArgb: Long = 0L,
  val mixedFromColorArgbs: List<Long> = emptyList(),
  val timestampEpochMs: Long = System.currentTimeMillis(),
  val formattedTimeAmPm: String = DashboardTimeFormatter.formatAmPm(timestampEpochMs),
)

/**
 * Holds a recently deleted timeline entry so the user can Undo the deletion within 3 seconds.
 */
@Immutable
data class PendingDeletedTimelineItem(
  val entry: DashboardTimelineEntry,
  val originalIndex: Int,
  val expiresAtEpochMs: Long,
)

/**
 * Snapshot of the user's fair-estimated or manually entered nutrient values inside the
 * Single Item or Create Meal composer.
 */
@Immutable
data class ResolvedNutrientDraft(
  val caloriesKcal: Int,
  val proteinGrams: Int,
  val carbsGrams: Int,
  val fatsGrams: Int,
  val isCaloriesFairEstimated: Boolean,
  val isProteinFairEstimated: Boolean,
  val isCarbsFairEstimated: Boolean,
  val isFatsFairEstimated: Boolean,
)

object DashboardTimeFormatter {
  fun formatAmPm(epochMs: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(epochMs))
  }

  fun currentDayKey(epochMs: Long = System.currentTimeMillis()): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMs))
  }
}

/**
 * Dynamic Fair Nutrient Estimator for the inline "Don't Know / Fair" option inside each
 * individual input box (`Calories`, `Protein`, `Carbs`, `Fats`).
 *
 * Ensures that if a user does not know a specific macro or calorie number, the engine
 * computes a physiologically fair, proportional value using:
 * 1. The user's personalized daily targets (`targetCalories`, `targetProtein`, `targetCarbs`, `targetFats`)
 * 2. The user's configured meal frequency (`mealCount`)
 * 3. Standard Atwater energy densities (`Protein = 4 kcal/g`, `Carbs = 4 kcal/g`, `Fats = 9 kcal/g`)
 */
object FairNutrientEstimator {

  fun resolveDraft(
    rawCaloriesText: String,
    rawProteinText: String,
    rawCarbsText: String,
    rawFatsText: String,
    caloriesDontKnow: Boolean,
    proteinDontKnow: Boolean,
    carbsDontKnow: Boolean,
    fatsDontKnow: Boolean,
    targetCalories: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int,
    mealCount: Int,
    isMealMode: Boolean,
  ): ResolvedNutrientDraft {
    val safeTargetCal = targetCalories.coerceAtLeast(1200)
    val safeTargetP = targetProtein.coerceAtLeast(40)
    val safeTargetC = targetCarbs.coerceAtLeast(80)
    val safeTargetF = targetFats.coerceAtLeast(30)
    val safeMeals = mealCount.coerceIn(2, 6)

    // Default fair portion fraction of daily target when nothing else anchors it:
    // A full Meal defaults to 1 / mealCount of daily targets; a Single Item defaults to ~45% of one meal.
    val defaultPortionFraction = if (isMealMode) {
      1f / safeMeals.toFloat()
    } else {
      (1f / safeMeals.toFloat()) * 0.45f
    }

    val manualCal = rawCaloriesText.trim().toIntOrNull()?.coerceIn(0, 5000)
    val manualP = rawProteinText.trim().toIntOrNull()?.coerceIn(0, 400)
    val manualC = rawCarbsText.trim().toIntOrNull()?.coerceIn(0, 600)
    val manualF = rawFatsText.trim().toIntOrNull()?.coerceIn(0, 300)

    val hasKnownP = !proteinDontKnow && manualP != null
    val hasKnownC = !carbsDontKnow && manualC != null
    val hasKnownF = !fatsDontKnow && manualF != null
    val hasKnownCal = !caloriesDontKnow && manualCal != null

    // Target calorie shares for fair distribution
    val totalTargetMacroKcal = (4 * safeTargetP + 4 * safeTargetC + 9 * safeTargetF).coerceAtLeast(1)
    val targetShareP = (4f * safeTargetP) / totalTargetMacroKcal.toFloat()
    val targetShareC = (4f * safeTargetC) / totalTargetMacroKcal.toFloat()
    val targetShareF = (9f * safeTargetF) / totalTargetMacroKcal.toFloat()

    // Step 1: Determine anchor total calories if possible
    val knownMacroKcal = (if (hasKnownP) 4 * (manualP ?: 0) else 0) +
      (if (hasKnownC) 4 * (manualC ?: 0) else 0) +
      (if (hasKnownF) 9 * (manualF ?: 0) else 0)

    val knownMacroShareSum = (if (hasKnownP) targetShareP else 0f) +
      (if (hasKnownC) targetShareC else 0f) +
      (if (hasKnownF) targetShareF else 0f)

    val anchorCalories: Int = when {
      hasKnownCal -> manualCal ?: 0
      caloriesDontKnow -> {
        when {
          knownMacroKcal > 0 && knownMacroShareSum >= 0.95f -> knownMacroKcal
          knownMacroKcal > 0 && knownMacroShareSum > 0.12f -> {
            // Scale up fairly based on the known macro's share of the user's diet
            (knownMacroKcal / knownMacroShareSum.coerceIn(0.18f, 1f)).roundToInt().coerceIn(knownMacroKcal, 2500)
          }
          else -> ((safeTargetCal * defaultPortionFraction) / 5f).roundToInt() * 5
        }
      }
      else -> {
        // User left Calories blank without toggling Don't Know, or is still typing
        if (knownMacroKcal > 0 && !proteinDontKnow && !carbsDontKnow && !fatsDontKnow) {
          0
        } else if (proteinDontKnow || carbsDontKnow || fatsDontKnow) {
          if (knownMacroKcal > 0 && knownMacroShareSum > 0.12f) {
            (knownMacroKcal / knownMacroShareSum.coerceIn(0.18f, 1f)).roundToInt().coerceIn(knownMacroKcal, 2500)
          } else {
            ((safeTargetCal * defaultPortionFraction) / 5f).roundToInt() * 5
          }
        } else {
          0
        }
      }
    }

    // Step 2: Distribute remaining calories fairly across any macros marked "Don't Know"
    val unknownShareSum = (if (proteinDontKnow) targetShareP else 0f) +
      (if (carbsDontKnow) targetShareC else 0f) +
      (if (fatsDontKnow) targetShareF else 0f)

    val remainingKcalForUnknownMacros = if (anchorCalories > knownMacroKcal) {
      anchorCalories - knownMacroKcal
    } else {
      ((safeTargetCal * defaultPortionFraction) * unknownShareSum).roundToInt()
    }

    val resolvedP: Int = when {
      hasKnownP -> manualP ?: 0
      proteinDontKnow -> {
        val shareWithinUnknown = if (unknownShareSum > 0f) targetShareP / unknownShareSum else 0.30f
        val kcalForP = remainingKcalForUnknownMacros * shareWithinUnknown
        max(1, (kcalForP / 4f).roundToInt())
      }
      else -> 0
    }

    val resolvedC: Int = when {
      hasKnownC -> manualC ?: 0
      carbsDontKnow -> {
        val shareWithinUnknown = if (unknownShareSum > 0f) targetShareC / unknownShareSum else 0.45f
        val kcalForC = remainingKcalForUnknownMacros * shareWithinUnknown
        max(1, (kcalForC / 4f).roundToInt())
      }
      else -> 0
    }

    val resolvedF: Int = when {
      hasKnownF -> manualF ?: 0
      fatsDontKnow -> {
        val shareWithinUnknown = if (unknownShareSum > 0f) targetShareF / unknownShareSum else 0.25f
        val kcalForF = remainingKcalForUnknownMacros * shareWithinUnknown
        max(1, (kcalForF / 9f).roundToInt())
      }
      else -> 0
    }

    val resolvedCal: Int = when {
      hasKnownCal -> manualCal ?: 0
      caloriesDontKnow -> {
        val computedFromMacros = 4 * resolvedP + 4 * resolvedC + 9 * resolvedF
        if (computedFromMacros > 0) {
          ((computedFromMacros / 5f).roundToInt() * 5).coerceAtLeast(10)
        } else {
          anchorCalories.coerceAtLeast(25)
        }
      }
      else -> 0
    }

    return ResolvedNutrientDraft(
      caloriesKcal = resolvedCal,
      proteinGrams = resolvedP,
      carbsGrams = resolvedC,
      fatsGrams = resolvedF,
      isCaloriesFairEstimated = caloriesDontKnow,
      isProteinFairEstimated = proteinDontKnow,
      isCarbsFairEstimated = carbsDontKnow,
      isFatsFairEstimated = fatsDontKnow,
    )
  }
}

/**
 * Full daily telemetry & timeline record stored inside the user's local database,
 * keyed by ISO date string (`yyyy-MM-dd`). Tracks all 6 daily pillars, targets,
 * streak qualification, and chronological timeline entries for deep analytics & custom calendar inspection.
 */
@Immutable
data class DailyHistoricalRecord(
  val dayKey: String,
  val stepsCount: Int = 0,
  val targetStepsCount: Int = 10000,
  val caloriesKcal: Int = 0,
  val targetCaloriesKcal: Int = 2500,
  val proteinGrams: Int = 0,
  val targetProteinGrams: Int = 180,
  val carbsGrams: Int = 0,
  val targetCarbsGrams: Int = 250,
  val fatsGrams: Int = 0,
  val targetFatsGrams: Int = 70,
  val hydrationMl: Int = 0,
  val targetHydrationMl: Int = 3500,
  val streakThresholdPercent: Int = 80,
  val streakQualified: Boolean = false,
  val isWorkoutDay: Boolean = false,
  val timelineEntries: List<DashboardTimelineEntry> = emptyList(),
  val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

/**
 * BioSync Streak Protocol Engine
 *
 * Efficient Streak Architecture with 2-Day Grace Buffer:
 * 1. Pillar Qualification: A calendar day is qualified for the streak when all 6 pillars
 *    (Steps, Calories, Protein, Carbs, Fats, Water) meet or exceed the streak threshold (e.g. 80%).
 * 2. Daily Reset: On every new calendar day, today's telemetry (calories, macros, hydration, steps,
 *    and timeline entries) resets to 0 fresh. The previous day's telemetry is archived into dailyHistoryMap.
 * 3. 2-Day Buffer Rule:
 *    - If the user achieves consecutive days (e.g. 3 days), the streak counter grows (1, 2, 3...).
 *    - If the user misses a day, a grace buffer of up to 2 days is granted to protect the streak.
 *    - Day +1 after last completed day: 0 full missed days (today active), streak held.
 *    - Day +2 after last completed day: 1 full missed day, buffer day 1 active, streak held.
 *    - If the user achieves within the 2-day buffer (daysElapsed <= 2), the streak extends by +1 (e.g. 3 -> 4).
 *    - If the user misses both buffer days and still has not achieved the desired goal (daysElapsed > 2),
 *      the 2-day buffer expires and the streak number immediately resets to 0!
 *    - When the streak is 0, achieving the goal begins a fresh streak at 1.
 */
object BioSyncStreakEngine {
  const val BUFFER_DAYS_ALLOWED = 2

  /**
   * Calculates the number of whole calendar days between two dayKey strings (YYYY-MM-DD).
   * Returns a positive integer when `toDayKey` is after `fromDayKey`.
   */
  fun daysBetween(fromDayKey: String, toDayKey: String): Int {
    if (fromDayKey.isBlank() || toDayKey.isBlank()) return 0
    if (fromDayKey == toDayKey) return 0
    return runCatching {
      val partsFrom = fromDayKey.split("-").map { it.toInt() }
      val partsTo = toDayKey.split("-").map { it.toInt() }
      if (partsFrom.size != 3 || partsTo.size != 3) return 0
      val c1 = Calendar.getInstance().apply {
        set(partsFrom[0], partsFrom[1] - 1, partsFrom[2], 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val c2 = Calendar.getInstance().apply {
        set(partsTo[0], partsTo[1] - 1, partsTo[2], 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val diffMillis = c2.timeInMillis - c1.timeInMillis
      ((diffMillis + 43_200_000L) / 86_400_000L).toInt()
    }.getOrDefault(0)
  }

  data class StreakEvaluation(
    val streakDays: Int,
    val lastCompletedDayKey: String,
    val isInBufferGrace: Boolean,
    val bufferDaysRemaining: Int,
    val isBufferExpired: Boolean,
  )

  /**
   * Evaluates the active streak given stored progress, today's completion status, and the 2-day buffer.
   */
  fun evaluateStreak(
    storedBaseStreak: Int,
    lastCompletedDayKey: String,
    todayKey: String,
    todayQualified: Boolean,
  ): StreakEvaluation {
    if (lastCompletedDayKey.isBlank() || storedBaseStreak <= 0) {
      return if (todayQualified) {
        StreakEvaluation(
          streakDays = 1,
          lastCompletedDayKey = todayKey,
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = false,
        )
      } else {
        StreakEvaluation(
          streakDays = 0,
          lastCompletedDayKey = "",
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = false,
        )
      }
    }

    val daysElapsed = daysBetween(lastCompletedDayKey, todayKey)

    if (daysElapsed <= 0) {
      // Completed today (or future)
      return if (todayQualified) {
        StreakEvaluation(
          streakDays = storedBaseStreak.coerceAtLeast(1),
          lastCompletedDayKey = todayKey,
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = false,
        )
      } else {
        // Was marked completed today, but un-qualified (e.g. food item deleted)
        val reverted = (storedBaseStreak - 1).coerceAtLeast(0)
        StreakEvaluation(
          streakDays = reverted,
          lastCompletedDayKey = "",
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = false,
        )
      }
    }

    // Days elapsed > 0
    if (todayQualified) {
      return if (daysElapsed <= BUFFER_DAYS_ALLOWED) {
        // Completed within the 2-day buffer window! Streak continues and increments!
        StreakEvaluation(
          streakDays = storedBaseStreak + 1,
          lastCompletedDayKey = todayKey,
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = false,
        )
      } else {
        // Buffer was exceeded! Restart streak from 1.
        StreakEvaluation(
          streakDays = 1,
          lastCompletedDayKey = todayKey,
          isInBufferGrace = false,
          bufferDaysRemaining = BUFFER_DAYS_ALLOWED,
          isBufferExpired = true,
        )
      }
    } else {
      // Today is NOT yet qualified
      return if (daysElapsed <= BUFFER_DAYS_ALLOWED) {
        // Still within the 2-day buffer! Streak is preserved.
        val bufferUsed = (daysElapsed - 1).coerceAtLeast(0)
        val remaining = (BUFFER_DAYS_ALLOWED - bufferUsed).coerceAtLeast(0)
        StreakEvaluation(
          streakDays = storedBaseStreak,
          lastCompletedDayKey = lastCompletedDayKey,
          isInBufferGrace = bufferUsed > 0,
          bufferDaysRemaining = remaining,
          isBufferExpired = false,
        )
      } else {
        // Buffer has been exceeded! Streak resets to 0.
        StreakEvaluation(
          streakDays = 0,
          lastCompletedDayKey = "",
          isInBufferGrace = false,
          bufferDaysRemaining = 0,
          isBufferExpired = true,
        )
      }
    }
  }

  fun findLatestHistoricalCompletedDay(
    historyMap: Map<String, DailyHistoricalRecord>,
    excludingDayKey: String,
  ): String {
    return historyMap.values
      .filter { it.streakQualified && it.dayKey != excludingDayKey }
      .maxByOrNull { it.dayKey }
      ?.dayKey ?: ""
  }
}

/**
 * Snapshot of the persisted Dashboard state (Saved Quick Items, Created Single Items for the
 * Meal Builder, Today's Timeline logs, Streak counter, and Complete Historical Day-by-Day Database).
 */
@Immutable
data class DashboardStoredData(
  val dayKey: String = DashboardTimeFormatter.currentDayKey(),
  val baseStreakDays: Int = 0,
  val lastCompletedStreakDayKey: String = "",
  val manualStepsToday: Int = 0,
  val allCreatedSingleItems: List<SavedQuickNutritionItem> = emptyList(),
  val savedQuickItems: List<SavedQuickNutritionItem> = emptyList(),
  val todayTimelineEntries: List<DashboardTimelineEntry> = emptyList(),
  val dailyHistoryMap: Map<String, DailyHistoricalRecord> = emptyMap(),
)

object DashboardLocalStore {
  private const val PREFS_NAME = "biosync_dashboard_telemetry_v1"
  private const val KEY_DAY_KEY = "dashboard_day_key"
  private const val KEY_BASE_STREAK_DAYS = "dashboard_base_streak_days"
  private const val KEY_LAST_COMPLETED_STREAK_DAY = "dashboard_last_completed_streak_day"
  private const val KEY_MANUAL_STEPS_TODAY = "dashboard_manual_steps_today"
  private const val KEY_CREATED_SINGLE_ITEMS_JSON = "dashboard_created_single_items_json"
  private const val KEY_SAVED_QUICK_ITEMS_JSON = "dashboard_saved_quick_items_json"
  private const val KEY_TIMELINE_ENTRIES_JSON = "dashboard_timeline_entries_json"
  private const val KEY_DAILY_HISTORY_MAP_JSON = "dashboard_daily_history_map_json"

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun rolloverToDate(
    context: Context,
    currentData: DashboardStoredData,
    targetDayKey: String,
  ): DashboardStoredData {
    if (currentData.dayKey == targetDayKey) return currentData

    val oldDayKey = currentData.dayKey
    val historyMap = currentData.dailyHistoryMap.toMutableMap()

    // 1. Archive the previous day's telemetry into dailyHistoryMap
    val existingOld = historyMap[oldDayKey]
    val entries = currentData.todayTimelineEntries
    val oldCal = entries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
    val oldPro = entries.sumOf { it.proteinGrams }.coerceAtLeast(0)
    val oldCarb = entries.sumOf { it.carbsGrams }.coerceAtLeast(0)
    val oldFat = entries.sumOf { it.fatsGrams }.coerceAtLeast(0)
    val oldWat = entries.sumOf { it.hydrationMl }.coerceAtLeast(0)
    val oldSteps = currentData.manualStepsToday.coerceAtLeast(0)

    if (entries.isNotEmpty() || oldSteps > 0 || existingOld != null) {
      historyMap[oldDayKey] = (existingOld ?: DailyHistoricalRecord(dayKey = oldDayKey)).copy(
        stepsCount = max(existingOld?.stepsCount ?: 0, oldSteps),
        caloriesKcal = max(existingOld?.caloriesKcal ?: 0, oldCal),
        proteinGrams = max(existingOld?.proteinGrams ?: 0, oldPro),
        carbsGrams = max(existingOld?.carbsGrams ?: 0, oldCarb),
        fatsGrams = max(existingOld?.fatsGrams ?: 0, oldFat),
        hydrationMl = max(existingOld?.hydrationMl ?: 0, oldWat),
        timelineEntries = entries,
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    }

    // 2. Evaluate streak according to the 2-day buffer rule:
    // When a new day begins, today's goals have NOT been met yet (todayQualified = false)
    val eval = BioSyncStreakEngine.evaluateStreak(
      storedBaseStreak = currentData.baseStreakDays,
      lastCompletedDayKey = currentData.lastCompletedStreakDayKey,
      todayKey = targetDayKey,
      todayQualified = false,
    )

    // 3. New day state: ALL progress bars strictly reset to 0, timeline empty
    val newDayData = currentData.copy(
      dayKey = targetDayKey,
      baseStreakDays = eval.streakDays,
      lastCompletedStreakDayKey = eval.lastCompletedDayKey,
      manualStepsToday = 0,
      todayTimelineEntries = emptyList(),
      dailyHistoryMap = historyMap,
    )

    save(context, newDayData)
    return newDayData
  }

  @Volatile
  private var isRestoringDashboardGuard = false

  fun load(context: Context): DashboardStoredData {
    val p = prefs(context)
    if (!isRestoringDashboardGuard && p.all.isEmpty()) {
      isRestoringDashboardGuard = true
      try {
        BioSyncBackupManager.restoreFromAutoMirrorIfPrimaryMissing(context)
      } finally {
        isRestoringDashboardGuard = false
      }
    }

    val todayKey = DashboardTimeFormatter.currentDayKey()
    val storedDayKey = p.getString(KEY_DAY_KEY, todayKey) ?: todayKey
    val baseStreak = p.getInt(KEY_BASE_STREAK_DAYS, 0).coerceAtLeast(0)
    val lastStreakDay = p.getString(KEY_LAST_COMPLETED_STREAK_DAY, "") ?: ""
    val storedManualSteps = p.getInt(KEY_MANUAL_STEPS_TODAY, 0).coerceAtLeast(0)

    val createdSingles = parseQuickItemsJson(p.getString(KEY_CREATED_SINGLE_ITEMS_JSON, "[]") ?: "[]")
    val savedQuick = parseQuickItemsJson(p.getString(KEY_SAVED_QUICK_ITEMS_JSON, "[]") ?: "[]")
    val parsedStoredTimeline = parseTimelineJson(p.getString(KEY_TIMELINE_ENTRIES_JSON, "[]") ?: "[]")
    val loadedHistoryMap = parseDailyHistoryMapJson(
      p.getString(KEY_DAILY_HISTORY_MAP_JSON, "{}") ?: "{}",
    ).toMutableMap()

    val rawData = DashboardStoredData(
      dayKey = storedDayKey,
      baseStreakDays = baseStreak,
      lastCompletedStreakDayKey = lastStreakDay,
      manualStepsToday = storedManualSteps,
      allCreatedSingleItems = createdSingles,
      savedQuickItems = savedQuick,
      todayTimelineEntries = parsedStoredTimeline,
      dailyHistoryMap = loadedHistoryMap,
    )

    return if (storedDayKey != todayKey) {
      rolloverToDate(context, rawData, todayKey)
    } else {
      rawData
    }
  }

  fun save(context: Context, data: DashboardStoredData, mirrorToVault: Boolean = true) {
    prefs(context).edit().apply {
      putString(KEY_DAY_KEY, data.dayKey)
      putInt(KEY_BASE_STREAK_DAYS, data.baseStreakDays.coerceAtLeast(0))
      putString(KEY_LAST_COMPLETED_STREAK_DAY, data.lastCompletedStreakDayKey)
      putInt(KEY_MANUAL_STEPS_TODAY, data.manualStepsToday.coerceAtLeast(0))
      putString(KEY_CREATED_SINGLE_ITEMS_JSON, serializeQuickItemsJson(data.allCreatedSingleItems))
      putString(KEY_SAVED_QUICK_ITEMS_JSON, serializeQuickItemsJson(data.savedQuickItems))
      putString(KEY_TIMELINE_ENTRIES_JSON, serializeTimelineJson(data.todayTimelineEntries))
      putString(KEY_DAILY_HISTORY_MAP_JSON, serializeDailyHistoryMapJson(data.dailyHistoryMap))
      apply()
    }
    val hasMeaningfulData = data.todayTimelineEntries.isNotEmpty() ||
      data.dailyHistoryMap.isNotEmpty() ||
      data.savedQuickItems.isNotEmpty() ||
      data.allCreatedSingleItems.isNotEmpty() ||
      data.baseStreakDays > 0 ||
      data.manualStepsToday > 0
    if (mirrorToVault && hasMeaningfulData && !isRestoringDashboardGuard) {
      BioSyncBackupManager.autoMirrorCurrentDatabase(context)
    }
    BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
  }

  /**
   * Synchronizes today's live telemetry (steps, macros, hydration, targets, streak qualification,
   * and timeline entries) into the persistent `dailyHistoryMap` inside `DashboardStoredData`.
   */
  fun withSyncedTodayRecord(
    data: DashboardStoredData,
    liveSteps: Int,
    targetSteps: Int,
    targetCalories: Int,
    targetProtein: Int,
    targetCarbs: Int,
    targetFats: Int,
    targetWaterMl: Int,
    streakThresholdPercent: Int,
    streakQualified: Boolean,
    isWorkoutDay: Boolean,
  ): DashboardStoredData {
    val todayKey = data.dayKey.ifBlank { DashboardTimeFormatter.currentDayKey() }
    val entries = data.todayTimelineEntries
    val loggedCal = entries.sumOf { it.caloriesKcal }.coerceAtLeast(0)
    val loggedPro = entries.sumOf { it.proteinGrams }.coerceAtLeast(0)
    val loggedCarb = entries.sumOf { it.carbsGrams }.coerceAtLeast(0)
    val loggedFat = entries.sumOf { it.fatsGrams }.coerceAtLeast(0)
    val loggedWater = entries.sumOf { it.hydrationMl }.coerceAtLeast(0)
    val existingToday = data.dailyHistoryMap[todayKey]
    val resolvedSteps = max(liveSteps.coerceAtLeast(0), existingToday?.stepsCount ?: 0)

    val updatedRecord = DailyHistoricalRecord(
      dayKey = todayKey,
      stepsCount = resolvedSteps,
      targetStepsCount = targetSteps.coerceAtLeast(1000),
      caloriesKcal = loggedCal,
      targetCaloriesKcal = targetCalories.coerceAtLeast(1000),
      proteinGrams = loggedPro,
      targetProteinGrams = targetProtein.coerceAtLeast(30),
      carbsGrams = loggedCarb,
      targetCarbsGrams = targetCarbs.coerceAtLeast(40),
      fatsGrams = loggedFat,
      targetFatsGrams = targetFats.coerceAtLeast(20),
      hydrationMl = loggedWater,
      targetHydrationMl = targetWaterMl.coerceAtLeast(500),
      streakThresholdPercent = streakThresholdPercent,
      streakQualified = streakQualified,
      isWorkoutDay = isWorkoutDay,
      timelineEntries = entries,
      updatedAtEpochMs = System.currentTimeMillis(),
    )
    val updatedMap = data.dailyHistoryMap.toMutableMap()
    updatedMap[todayKey] = updatedRecord
    return data.copy(dailyHistoryMap = updatedMap)
  }

  /**
   * Updates or inserts a specific date's `DailyHistoricalRecord` in the user's local database
   * (also keeps `todayTimelineEntries` in sync if `dayKey == data.dayKey`).
   */
  fun withUpdatedDayRecord(
    data: DashboardStoredData,
    record: DailyHistoricalRecord,
  ): DashboardStoredData {
    val updatedMap = data.dailyHistoryMap.toMutableMap()
    updatedMap[record.dayKey] = record
    return if (record.dayKey == data.dayKey) {
      data.copy(
        todayTimelineEntries = record.timelineEntries,
        dailyHistoryMap = updatedMap,
      )
    } else {
      data.copy(dailyHistoryMap = updatedMap)
    }
  }

  /**
   * Factory Reset: Wipes all persisted dashboard logs, saved quick items, created items, history, and streaks.
   */
  fun clearAll(context: Context) {
    prefs(context).edit().clear().commit()
    BioSyncBackupManager.clearAutoMirrorFiles(context)
    BioSyncWidgetAndNotificationEngine.refreshAllWidgets(context)
  }

  internal fun serializeDailyHistoryMapJson(map: Map<String, DailyHistoricalRecord>): String {
    val root = JSONObject()
    for ((dayKey, rec) in map) {
      if (dayKey.isBlank()) continue
      val obj = JSONObject().apply {
        put("dayKey", rec.dayKey)
        put("stepsCount", rec.stepsCount)
        put("targetStepsCount", rec.targetStepsCount)
        put("caloriesKcal", rec.caloriesKcal)
        put("targetCaloriesKcal", rec.targetCaloriesKcal)
        put("proteinGrams", rec.proteinGrams)
        put("targetProteinGrams", rec.targetProteinGrams)
        put("carbsGrams", rec.carbsGrams)
        put("targetCarbsGrams", rec.targetCarbsGrams)
        put("fatsGrams", rec.fatsGrams)
        put("targetFatsGrams", rec.targetFatsGrams)
        put("hydrationMl", rec.hydrationMl)
        put("targetHydrationMl", rec.targetHydrationMl)
        put("streakThresholdPercent", rec.streakThresholdPercent)
        put("streakQualified", rec.streakQualified)
        put("isWorkoutDay", rec.isWorkoutDay)
        put("timelineEntriesJson", serializeTimelineJson(rec.timelineEntries))
        put("updatedAtEpochMs", rec.updatedAtEpochMs)
      }
      root.put(dayKey, obj)
    }
    return root.toString()
  }

  internal fun parseDailyHistoryMapJson(rawJson: String): Map<String, DailyHistoricalRecord> {
    return runCatching {
      val root = JSONObject(rawJson)
      val map = LinkedHashMap<String, DailyHistoricalRecord>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val obj = root.optJSONObject(key) ?: continue
        val timelineRaw = obj.optString("timelineEntriesJson", "[]")
        val parsedTimeline = parseTimelineJson(timelineRaw)
        map[key] = DailyHistoricalRecord(
          dayKey = obj.optString("dayKey", key),
          stepsCount = obj.optInt("stepsCount", 0).coerceAtLeast(0),
          targetStepsCount = obj.optInt("targetStepsCount", 10000).coerceAtLeast(1000),
          caloriesKcal = obj.optInt("caloriesKcal", parsedTimeline.sumOf { it.caloriesKcal }).coerceAtLeast(0),
          targetCaloriesKcal = obj.optInt("targetCaloriesKcal", 2500).coerceAtLeast(1000),
          proteinGrams = obj.optInt("proteinGrams", parsedTimeline.sumOf { it.proteinGrams }).coerceAtLeast(0),
          targetProteinGrams = obj.optInt("targetProteinGrams", 180).coerceAtLeast(30),
          carbsGrams = obj.optInt("carbsGrams", parsedTimeline.sumOf { it.carbsGrams }).coerceAtLeast(0),
          targetCarbsGrams = obj.optInt("targetCarbsGrams", 250).coerceAtLeast(40),
          fatsGrams = obj.optInt("fatsGrams", parsedTimeline.sumOf { it.fatsGrams }).coerceAtLeast(0),
          targetFatsGrams = obj.optInt("targetFatsGrams", 70).coerceAtLeast(20),
          hydrationMl = obj.optInt("hydrationMl", parsedTimeline.sumOf { it.hydrationMl }).coerceAtLeast(0),
          targetHydrationMl = obj.optInt("targetHydrationMl", 3500).coerceAtLeast(500),
          streakThresholdPercent = obj.optInt("streakThresholdPercent", 80).coerceIn(50, 100),
          streakQualified = obj.optBoolean("streakQualified", false),
          isWorkoutDay = obj.optBoolean("isWorkoutDay", false),
          timelineEntries = parsedTimeline,
          updatedAtEpochMs = obj.optLong("updatedAtEpochMs", System.currentTimeMillis()),
        )
      }
      map
    }.getOrDefault(emptyMap())
  }

  internal fun serializeQuickItemsJson(items: List<SavedQuickNutritionItem>): String {
    val arr = JSONArray()
    for (item in items) {
      val obj = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("isMeal", item.isMeal)
        put("caloriesKcal", item.caloriesKcal)
        put("proteinGrams", item.proteinGrams)
        put("carbsGrams", item.carbsGrams)
        put("fatsGrams", item.fatsGrams)
        put("isSavedToQuickActions", item.isSavedToQuickActions)
        put("colorArgb", item.colorArgb)
        put("createdAtEpochMs", item.createdAtEpochMs)
        val comps = JSONArray()
        item.componentNames.forEach { comps.put(it) }
        put("componentNames", comps)
        val mixedArr = JSONArray()
        item.mixedFromColorArgbs.forEach { mixedArr.put(it) }
        put("mixedFromColorArgbs", mixedArr)
      }
      arr.put(obj)
    }
    return arr.toString()
  }

  internal fun parseQuickItemsJson(rawJson: String): List<SavedQuickNutritionItem> {
    return runCatching {
      val arr = JSONArray(rawJson)
      val list = ArrayList<SavedQuickNutritionItem>(arr.length())
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val compsArr = obj.optJSONArray("componentNames")
        val comps = mutableListOf<String>()
        if (compsArr != null) {
          for (j in 0 until compsArr.length()) {
            val c = compsArr.optString(j)
            if (c.isNotBlank()) comps.add(c)
          }
        }
        val mixedArr = obj.optJSONArray("mixedFromColorArgbs")
        val mixedColors = mutableListOf<Long>()
        if (mixedArr != null) {
          for (j in 0 until mixedArr.length()) {
            val cVal = mixedArr.optLong(j, 0L)
            if (cVal != 0L) mixedColors.add(cVal)
          }
        }
        val parsedName = obj.optString("name", "Item")
        val parsedIsMeal = obj.optBoolean("isMeal", false)
        val defaultColor = QuickItemColorAlchemy.defaultColorForItem(parsedName, parsedIsMeal)
        list.add(
          SavedQuickNutritionItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = parsedName,
            isMeal = parsedIsMeal,
            caloriesKcal = obj.optInt("caloriesKcal", 0),
            proteinGrams = obj.optInt("proteinGrams", 0),
            carbsGrams = obj.optInt("carbsGrams", 0),
            fatsGrams = obj.optInt("fatsGrams", 0),
            componentNames = comps,
            isSavedToQuickActions = obj.optBoolean("isSavedToQuickActions", true),
            colorArgb = obj.optLong("colorArgb", defaultColor).let { if (it == 0L) defaultColor else it },
            mixedFromColorArgbs = mixedColors,
            createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
          ),
        )
      }
      list
    }.getOrDefault(emptyList())
  }

  internal fun serializeTimelineJson(entries: List<DashboardTimelineEntry>): String {
    val arr = JSONArray()
    for (e in entries) {
      val obj = JSONObject().apply {
        put("id", e.id)
        put("type", e.type.name)
        put("title", e.title)
        put("subtitle", e.subtitle)
        put("caloriesKcal", e.caloriesKcal)
        put("proteinGrams", e.proteinGrams)
        put("carbsGrams", e.carbsGrams)
        put("fatsGrams", e.fatsGrams)
        put("hydrationMl", e.hydrationMl)
        put("colorArgb", e.colorArgb)
        val mixedArr = JSONArray()
        e.mixedFromColorArgbs.forEach { mixedArr.put(it) }
        put("mixedFromColorArgbs", mixedArr)
        put("timestampEpochMs", e.timestampEpochMs)
        put("formattedTimeAmPm", e.formattedTimeAmPm)
      }
      arr.put(obj)
    }
    return arr.toString()
  }

  internal fun parseTimelineJson(rawJson: String): List<DashboardTimelineEntry> {
    return runCatching {
      val arr = JSONArray(rawJson)
      val list = ArrayList<DashboardTimelineEntry>(arr.length())
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val typeName = obj.optString("type", DashboardEntryType.SINGLE_ITEM.name)
        val parsedType = runCatching { DashboardEntryType.valueOf(typeName) }
          .getOrDefault(DashboardEntryType.SINGLE_ITEM)
        val ts = obj.optLong("timestampEpochMs", System.currentTimeMillis())
        val mixedArr = obj.optJSONArray("mixedFromColorArgbs")
        val mixedColors = mutableListOf<Long>()
        if (mixedArr != null) {
          for (j in 0 until mixedArr.length()) {
            val cVal = mixedArr.optLong(j, 0L)
            if (cVal != 0L) mixedColors.add(cVal)
          }
        }
        list.add(
          DashboardTimelineEntry(
            id = obj.optString("id", UUID.randomUUID().toString()),
            type = parsedType,
            title = obj.optString("title", "Logged Entry"),
            subtitle = obj.optString("subtitle", ""),
            caloriesKcal = obj.optInt("caloriesKcal", 0),
            proteinGrams = obj.optInt("proteinGrams", 0),
            carbsGrams = obj.optInt("carbsGrams", 0),
            fatsGrams = obj.optInt("fatsGrams", 0),
            hydrationMl = obj.optInt("hydrationMl", 0),
            colorArgb = obj.optLong("colorArgb", 0L),
            mixedFromColorArgbs = mixedColors,
            timestampEpochMs = ts,
            formattedTimeAmPm = obj.optString("formattedTimeAmPm", DashboardTimeFormatter.formatAmPm(ts)),
          ),
        )
      }
      list
    }.getOrDefault(emptyList())
  }
}

// ============================================================================
// COMPLETE DATABASE BACKUP, RECOVERY VAULT & RESTORE ENGINE
// Packages & restores ALL data across Steps 01–04 (BioSyncLocalStore) AND
// Dashboard / Quick Actions / Historical Calendar Timelines (DashboardLocalStore).
// ============================================================================

@Immutable
data class BioSyncBackupMeta(
  val createdAtEpochMs: Long,
  val formattedTimestamp: String,
  val subjectName: String,
  val ageYears: Int,
  val streakDays: Int,
  val savedQuickItemsCount: Int,
  val createdSingleItemsCount: Int,
  val historicalDaysCount: Int,
  val totalTimelineLogsCount: Int,
  val targetCaloriesKcal: Int,
  val targetProteinGrams: Int,
  val targetHydrationMl: Int,
  val targetStepsCount: Int,
)

@Immutable
data class BioSyncFullBackupBundle(
  val meta: BioSyncBackupMeta,
  val storedState: BioSyncStoredState,
  val dashboardData: DashboardStoredData,
)

object BioSyncBackupManager {
  private const val VAULT_PREFS_NAME = "biosync_recovery_vault_v1"
  private const val KEY_VAULT_BACKUP_JSON = "vault_full_backup_json"
  private const val KEY_VAULT_CREATED_AT_MS = "vault_created_at_epoch_ms"

  private fun vaultPrefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(VAULT_PREFS_NAME, Context.MODE_PRIVATE)

  fun formatBackupTimestamp(epochMs: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    return sdf.format(Date(epochMs))
  }

  /**
   * Serializes the entire on-device database (`BioSyncStoredState` + `DashboardStoredData`)
   * into a self-contained JSON string for file export, clipboard copy, or Recovery Vault storage.
   */
  fun exportFullBackupJson(context: Context): String {
    val profile = BioSyncLocalStore.load(context)
    val dash = DashboardLocalStore.load(context)
    val nowMs = System.currentTimeMillis()

    val profileObj = JSONObject().apply {
      put("subjectName", profile.subjectName)
      put("selectedSex", profile.selectedSex)
      put("birthDay", profile.birthDay)
      put("birthMonthZeroBased", profile.birthMonthZeroBased)
      put("birthYear", profile.birthYear)
      put("ageYears", profile.ageYears)
      put("heightCm", profile.heightCm.toDouble())
      put("heightUnit", profile.heightUnit.name)
      put("weightKg", profile.weightKg.toDouble())
      put("weightUnit", profile.weightUnit.name)
      put("activityLevel", profile.activityLevel.name)
      put("occupationLoad", profile.occupationLoad.name)
      put("objective", profile.objective.name)
      put("trainingModality", profile.trainingModality.name)
      put("workoutDuration", profile.workoutDuration.name)
      put("experienceLevel", profile.experienceLevel.name)
      put("targetPace", profile.targetPace.name)
      put("dayOne", profile.dayOne.name)
      put("selectedWorkoutDaysCsv", profile.selectedWorkoutDays.joinToString(",") { it.name })
      put("mealFrequency", profile.mealFrequency.name)
      put("caffeineAlcoholYes", profile.caffeineAlcoholYes)
      put("sodiumFrequency", profile.sodiumFrequency.name)
      put("streakThresholdPercent", profile.streakThreshold.percentage)
      put("climateBand", profile.climateBand.name)
      put("bodyFatOverrideEnabled", profile.bodyFatOverrideEnabled)
      put("bodyFatPercent", profile.bodyFatPercent.toDouble())
      put("isCustomTargetsMode", profile.isCustomTargetsMode)
      put("customCaloriesKcal", profile.customCaloriesKcal)
      put("customProteinGrams", profile.customProteinGrams)
      put("customCarbsGrams", profile.customCarbsGrams)
      put("customFatsGrams", profile.customFatsGrams)
      put("customHydrationMl", profile.customHydrationMl)
      put("customStepsCount", profile.customStepsCount)
      put("protocolLockedIn", profile.protocolLockedIn)
      put("wakeUpHour", profile.wakeUpHour)
      put("wakeUpMinute", profile.wakeUpMinute)
      put("sleepHour", profile.sleepHour)
      put("sleepMinute", profile.sleepMinute)
      put("protocolLockedAtEpochMillis", profile.protocolLockedAtEpochMillis)
      put("dayOneAnchorStartEpochMillis", profile.dayOneAnchorStartEpochMillis)
      put("dayOneInitialCountdownCompleted", profile.dayOneInitialCountdownCompleted)
      put("dayOneStartNotificationSent", profile.dayOneStartNotificationSent)
      put("decidedDailyStepsGoal", profile.decidedDailyStepsGoal)
    }

    val dashObj = JSONObject().apply {
      put("dayKey", dash.dayKey)
      put("baseStreakDays", dash.baseStreakDays)
      put("lastCompletedStreakDayKey", dash.lastCompletedStreakDayKey)
      put("manualStepsToday", dash.manualStepsToday)
      put("allCreatedSingleItemsJson", DashboardLocalStore.serializeQuickItemsJson(dash.allCreatedSingleItems))
      put("savedQuickItemsJson", DashboardLocalStore.serializeQuickItemsJson(dash.savedQuickItems))
      put("todayTimelineEntriesJson", DashboardLocalStore.serializeTimelineJson(dash.todayTimelineEntries))
      put("dailyHistoryMapJson", DashboardLocalStore.serializeDailyHistoryMapJson(dash.dailyHistoryMap))
    }

    val root = JSONObject().apply {
      put("schema", "BIOSYNC_FULL_BACKUP_V1")
      put("createdAtEpochMs", nowMs)
      put("profile", profileObj)
      put("dashboard", dashObj)
    }
    return root.toString(2)
  }

  private const val AUTO_MIRROR_FILE_NAME = "biosync_durable_state_mirror.json"
  private const val KEY_AUTO_MIRROR_JSON = "vault_auto_mirror_json"

  @Volatile
  private var isMirroringGuard = false

  /**
   * Automatically mirrors the active database to durable internal storage (`filesDir`, `noBackupFilesDir`,
   * and `biosync_recovery_vault_v1`) so app updates (`MY_PACKAGE_REPLACED`), process restarts, or
   * OS cache cleanups never wipe out the user's profile, streaks, quick actions, or daily logs.
   */
  fun autoMirrorCurrentDatabase(context: Context) {
    if (isMirroringGuard) return
    isMirroringGuard = true
    try {
      val appCtx = context.applicationContext
      val json = exportFullBackupJson(appCtx)
      val nowMs = System.currentTimeMillis()
      vaultPrefs(appCtx).edit().apply {
        putString(KEY_AUTO_MIRROR_JSON, json)
        // Also populate the primary Recovery Vault if no manual snapshot exists yet
        if (vaultPrefs(appCtx).getString(KEY_VAULT_BACKUP_JSON, null).isNullOrBlank()) {
          putString(KEY_VAULT_BACKUP_JSON, json)
          putLong(KEY_VAULT_CREATED_AT_MS, nowMs)
        }
        apply()
      }
      runCatching {
        File(appCtx.filesDir, AUTO_MIRROR_FILE_NAME).writeText(json, Charsets.UTF_8)
      }
      runCatching {
        File(appCtx.noBackupFilesDir, AUTO_MIRROR_FILE_NAME).writeText(json, Charsets.UTF_8)
      }
    } finally {
      isMirroringGuard = false
    }
  }

  /**
   * Restores the user's profile and dashboard database from the durable mirror if the primary
   * SharedPreferences were missing or cleared after installing a new version of the app.
   */
  fun restoreFromAutoMirrorIfPrimaryMissing(context: Context): Boolean {
    if (isMirroringGuard) return false
    isMirroringGuard = true
    return try {
      val appCtx = context.applicationContext
      val candidateJson = runCatching {
        val f1 = File(appCtx.filesDir, AUTO_MIRROR_FILE_NAME)
        if (f1.exists() && f1.length() > 20L) f1.readText(Charsets.UTF_8) else null
      }.getOrNull()
        ?: runCatching {
          val f2 = File(appCtx.noBackupFilesDir, AUTO_MIRROR_FILE_NAME)
          if (f2.exists() && f2.length() > 20L) f2.readText(Charsets.UTF_8) else null
        }.getOrNull()
        ?: vaultPrefs(appCtx).getString(KEY_AUTO_MIRROR_JSON, null)?.takeIf { it.isNotBlank() }

      if (candidateJson.isNullOrBlank()) return false
      val bundle = parseBackupBundle(candidateJson).getOrNull() ?: return false
      if (!bundle.storedState.protocolLockedIn &&
        bundle.dashboardData.dailyHistoryMap.isEmpty() &&
        bundle.dashboardData.todayTimelineEntries.isEmpty() &&
        bundle.dashboardData.savedQuickItems.isEmpty()
      ) {
        return false
      }

      BioSyncLocalStore.save(
        context = appCtx,
        state = bundle.storedState.copy(protocolLockedIn = true),
        mirrorToVault = false,
      )
      val todayKey = DashboardTimeFormatter.currentDayKey()
      val resolvedDash = if (bundle.dashboardData.dayKey != todayKey) {
        DashboardLocalStore.rolloverToDate(appCtx, bundle.dashboardData, todayKey)
      } else {
        bundle.dashboardData
      }
      DashboardLocalStore.save(
        context = appCtx,
        data = resolvedDash,
        mirrorToVault = false,
      )
      true
    } catch (_: Throwable) {
      false
    } finally {
      isMirroringGuard = false
    }
  }

  fun clearAutoMirrorFiles(context: Context) {
    val appCtx = context.applicationContext
    vaultPrefs(appCtx).edit().remove(KEY_AUTO_MIRROR_JSON).commit()
    runCatching { File(appCtx.filesDir, AUTO_MIRROR_FILE_NAME).delete() }
    runCatching { File(appCtx.noBackupFilesDir, AUTO_MIRROR_FILE_NAME).delete() }
  }

  /**
   * Saves a complete snapshot of the user's current database into the independent Recovery Vault
   * (which survives Factory Resets so accidental resets can be 1-tap recovered).
   */
  fun saveSnapshotToRecoveryVault(context: Context): BioSyncBackupMeta {
    val json = exportFullBackupJson(context)
    val nowMs = System.currentTimeMillis()
    vaultPrefs(context).edit()
      .putString(KEY_VAULT_BACKUP_JSON, json)
      .putString(KEY_AUTO_MIRROR_JSON, json)
      .putLong(KEY_VAULT_CREATED_AT_MS, nowMs)
      .commit()
    runCatching {
      File(context.applicationContext.filesDir, AUTO_MIRROR_FILE_NAME).writeText(json, Charsets.UTF_8)
      File(context.applicationContext.noBackupFilesDir, AUTO_MIRROR_FILE_NAME).writeText(json, Charsets.UTF_8)
    }
    return parseBackupBundle(json).getOrNull()?.meta ?: BioSyncBackupMeta(
      createdAtEpochMs = nowMs,
      formattedTimestamp = formatBackupTimestamp(nowMs),
      subjectName = BioSyncLocalStore.load(context).subjectName,
      ageYears = BioSyncLocalStore.load(context).ageYears,
      streakDays = DashboardLocalStore.load(context).baseStreakDays,
      savedQuickItemsCount = DashboardLocalStore.load(context).savedQuickItems.size,
      createdSingleItemsCount = DashboardLocalStore.load(context).allCreatedSingleItems.size,
      historicalDaysCount = DashboardLocalStore.load(context).dailyHistoryMap.size,
      totalTimelineLogsCount = DashboardLocalStore.load(context).todayTimelineEntries.size,
      targetCaloriesKcal = BioSyncLocalStore.load(context).customCaloriesKcal,
      targetProteinGrams = BioSyncLocalStore.load(context).customProteinGrams,
      targetHydrationMl = BioSyncLocalStore.load(context).customHydrationMl,
      targetStepsCount = BioSyncLocalStore.load(context).customStepsCount,
    )
  }

  fun getRecoveryVaultJson(context: Context): String? {
    val appCtx = context.applicationContext
    val raw = vaultPrefs(appCtx).getString(KEY_VAULT_BACKUP_JSON, null)
      ?.takeIf { it.isNotBlank() }
      ?: vaultPrefs(appCtx).getString(KEY_AUTO_MIRROR_JSON, null)?.takeIf { it.isNotBlank() }
      ?: runCatching {
        val f = File(appCtx.filesDir, AUTO_MIRROR_FILE_NAME)
        if (f.exists() && f.length() > 20L) f.readText(Charsets.UTF_8) else null
      }.getOrNull()
    return if (raw.isNullOrBlank()) null else raw
  }

  fun getRecoveryVaultMeta(context: Context): BioSyncBackupMeta? {
    val raw = getRecoveryVaultJson(context) ?: return null
    return parseBackupBundle(raw).getOrNull()?.meta
  }

  fun clearRecoveryVault(context: Context) {
    vaultPrefs(context).edit().clear().commit()
    clearAutoMirrorFiles(context)
  }

  /**
   * Parses a backup JSON string into a strongly-typed `BioSyncFullBackupBundle`.
   */
  fun parseBackupBundle(rawJson: String): Result<BioSyncFullBackupBundle> {
    return runCatching {
      val trimmed = rawJson.trim()
      require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
        "Invalid backup format: expected a JSON backup object."
      }
      val root = JSONObject(trimmed)
      val profileObj = root.optJSONObject("profile")
        ?: throw IllegalArgumentException("Backup is missing 'profile' section.")
      val dashObj = root.optJSONObject("dashboard") ?: JSONObject()

      val createdAtMs = root.optLong("createdAtEpochMs", System.currentTimeMillis())
      val defaults = BioSyncStoredState()

      val activityLevel = runCatching {
        ActivityLevelOption.valueOf(profileObj.optString("activityLevel", defaults.activityLevel.name))
      }.getOrDefault(defaults.activityLevel)

      val workoutDaysCsv = profileObj.optString("selectedWorkoutDaysCsv", "")
      val parsedWorkoutDays = if (activityLevel == ActivityLevelOption.SEDENTARY) {
        emptySet()
      } else {
        val set = workoutDaysCsv.split(",")
          .mapNotNull { token ->
            val t = token.trim()
            if (t.isEmpty()) null else runCatching { DayOfWeekOption.valueOf(t) }.getOrNull()
          }
          .toSet()
        if (set.isEmpty()) DailyTargetsEngine.defaultWorkoutDaysFor(activityLevel) else set
      }

      val restoredProfile = BioSyncStoredState(
        subjectName = profileObj.optString("subjectName", defaults.subjectName).ifBlank { defaults.subjectName },
        selectedSex = profileObj.optString("selectedSex", defaults.selectedSex),
        birthDay = profileObj.optInt("birthDay", defaults.birthDay).coerceIn(1, 31),
        birthMonthZeroBased = profileObj.optInt("birthMonthZeroBased", defaults.birthMonthZeroBased).coerceIn(0, 11),
        birthYear = profileObj.optInt("birthYear", defaults.birthYear).coerceIn(1900, 2026),
        ageYears = profileObj.optInt("ageYears", defaults.ageYears).coerceIn(5, 120),
        heightCm = profileObj.optDouble("heightCm", defaults.heightCm.toDouble()).toFloat().coerceIn(80f, 260f),
        heightUnit = runCatching {
          HeightUnit.valueOf(profileObj.optString("heightUnit", defaults.heightUnit.name))
        }.getOrDefault(defaults.heightUnit),
        weightKg = profileObj.optDouble("weightKg", defaults.weightKg.toDouble()).toFloat().coerceIn(20f, 350f),
        weightUnit = runCatching {
          WeightUnit.valueOf(profileObj.optString("weightUnit", defaults.weightUnit.name))
        }.getOrDefault(defaults.weightUnit),
        activityLevel = activityLevel,
        occupationLoad = runCatching {
          OccupationLoadOption.valueOf(profileObj.optString("occupationLoad", defaults.occupationLoad.name))
        }.getOrDefault(defaults.occupationLoad),
        objective = runCatching {
          BlueprintObjectiveOption.valueOf(profileObj.optString("objective", defaults.objective.name))
        }.getOrDefault(defaults.objective),
        trainingModality = runCatching {
          TrainingModalityOption.valueOf(profileObj.optString("trainingModality", defaults.trainingModality.name))
        }.getOrDefault(defaults.trainingModality),
        workoutDuration = runCatching {
          WorkoutDurationOption.valueOf(profileObj.optString("workoutDuration", defaults.workoutDuration.name))
        }.getOrDefault(defaults.workoutDuration),
        experienceLevel = runCatching {
          ExperienceLevelOption.valueOf(profileObj.optString("experienceLevel", defaults.experienceLevel.name))
        }.getOrDefault(defaults.experienceLevel),
        targetPace = runCatching {
          TargetPaceOption.valueOf(profileObj.optString("targetPace", defaults.targetPace.name))
        }.getOrDefault(defaults.targetPace),
        dayOne = runCatching {
          DayOfWeekOption.valueOf(profileObj.optString("dayOne", defaults.dayOne.name))
        }.getOrDefault(defaults.dayOne),
        selectedWorkoutDays = parsedWorkoutDays,
        mealFrequency = runCatching {
          MealFrequencyOption.valueOf(profileObj.optString("mealFrequency", defaults.mealFrequency.name))
        }.getOrDefault(defaults.mealFrequency),
        caffeineAlcoholYes = profileObj.optBoolean("caffeineAlcoholYes", defaults.caffeineAlcoholYes),
        sodiumFrequency = runCatching {
          SodiumFrequencyOption.valueOf(profileObj.optString("sodiumFrequency", defaults.sodiumFrequency.name))
        }.getOrDefault(defaults.sodiumFrequency),
        streakThreshold = StreakThresholdOption.fromPercent(
          profileObj.optInt("streakThresholdPercent", defaults.streakThreshold.percentage),
        ),
        climateBand = runCatching {
          ClimateBandOption.valueOf(profileObj.optString("climateBand", defaults.climateBand.name))
        }.getOrDefault(defaults.climateBand),
        bodyFatOverrideEnabled = profileObj.optBoolean("bodyFatOverrideEnabled", defaults.bodyFatOverrideEnabled),
        bodyFatPercent = profileObj.optDouble("bodyFatPercent", defaults.bodyFatPercent.toDouble()).toFloat().coerceIn(4f, 55f),
        isCustomTargetsMode = profileObj.optBoolean("isCustomTargetsMode", defaults.isCustomTargetsMode),
        customCaloriesKcal = profileObj.optInt("customCaloriesKcal", defaults.customCaloriesKcal).coerceIn(800, 7500),
        customProteinGrams = profileObj.optInt("customProteinGrams", defaults.customProteinGrams).coerceIn(20, 450),
        customCarbsGrams = profileObj.optInt("customCarbsGrams", defaults.customCarbsGrams).coerceIn(0, 1000),
        customFatsGrams = profileObj.optInt("customFatsGrams", defaults.customFatsGrams).coerceIn(10, 300),
        customHydrationMl = profileObj.optInt("customHydrationMl", defaults.customHydrationMl).coerceIn(500, 8000),
        customStepsCount = profileObj.optInt("customStepsCount", defaults.customStepsCount).coerceIn(1000, 35000),
        protocolLockedIn = profileObj.optBoolean("protocolLockedIn", true),
        wakeUpHour = profileObj.optInt("wakeUpHour", defaults.wakeUpHour).coerceIn(0, 23),
        wakeUpMinute = profileObj.optInt("wakeUpMinute", defaults.wakeUpMinute).coerceIn(0, 59),
        sleepHour = profileObj.optInt("sleepHour", defaults.sleepHour).coerceIn(0, 23),
        sleepMinute = profileObj.optInt("sleepMinute", defaults.sleepMinute).coerceIn(0, 59),
        protocolLockedAtEpochMillis = profileObj.optLong("protocolLockedAtEpochMillis", System.currentTimeMillis()),
        dayOneAnchorStartEpochMillis = profileObj.optLong("dayOneAnchorStartEpochMillis", 0L),
        dayOneInitialCountdownCompleted = profileObj.optBoolean("dayOneInitialCountdownCompleted", true),
        dayOneStartNotificationSent = profileObj.optBoolean("dayOneStartNotificationSent", true),
        decidedDailyStepsGoal = profileObj.optInt("decidedDailyStepsGoal", defaults.customStepsCount),
      )

      val todayKey = DashboardTimeFormatter.currentDayKey()
      val storedDayKey = dashObj.optString("dayKey", todayKey).ifBlank { todayKey }
      val createdSingles = DashboardLocalStore.parseQuickItemsJson(
        dashObj.optString("allCreatedSingleItemsJson", "[]"),
      )
      val savedQuick = DashboardLocalStore.parseQuickItemsJson(
        dashObj.optString("savedQuickItemsJson", "[]"),
      )
      val todayEntries = DashboardLocalStore.parseTimelineJson(
        dashObj.optString("todayTimelineEntriesJson", "[]"),
      )
      val historyMap = DashboardLocalStore.parseDailyHistoryMapJson(
        dashObj.optString("dailyHistoryMapJson", "{}"),
      ).toMutableMap()

      // Ensure that day's timeline entries are preserved
      val resolvedDayEntries = if (todayEntries.isNotEmpty()) {
        todayEntries
      } else {
        historyMap[storedDayKey]?.timelineEntries ?: emptyList()
      }

      val restoredDash = DashboardStoredData(
        dayKey = storedDayKey,
        baseStreakDays = dashObj.optInt("baseStreakDays", 0).coerceAtLeast(0),
        lastCompletedStreakDayKey = dashObj.optString("lastCompletedStreakDayKey", ""),
        manualStepsToday = dashObj.optInt("manualStepsToday", 0).coerceAtLeast(0),
        allCreatedSingleItems = createdSingles,
        savedQuickItems = savedQuick,
        todayTimelineEntries = resolvedDayEntries,
        dailyHistoryMap = historyMap,
      )

      val totalTimelineLogs = max(
        resolvedDayEntries.size,
        historyMap.values.sumOf { it.timelineEntries.size },
      )

      val meta = BioSyncBackupMeta(
        createdAtEpochMs = createdAtMs,
        formattedTimestamp = formatBackupTimestamp(createdAtMs),
        subjectName = restoredProfile.subjectName,
        ageYears = restoredProfile.ageYears,
        streakDays = restoredDash.baseStreakDays,
        savedQuickItemsCount = restoredDash.savedQuickItems.size,
        createdSingleItemsCount = restoredDash.allCreatedSingleItems.size,
        historicalDaysCount = max(1, restoredDash.dailyHistoryMap.size),
        totalTimelineLogsCount = totalTimelineLogs,
        targetCaloriesKcal = restoredProfile.customCaloriesKcal,
        targetProteinGrams = restoredProfile.customProteinGrams,
        targetHydrationMl = restoredProfile.customHydrationMl,
        targetStepsCount = restoredProfile.customStepsCount,
      )

      BioSyncFullBackupBundle(
        meta = meta,
        storedState = restoredProfile,
        dashboardData = restoredDash,
      )
    }
  }

  /**
   * Parses and immediately persists the restored bundle into both `BioSyncLocalStore`
   * and `DashboardLocalStore` (rolling over if the backup was from a previous day, and updating the Recovery Vault).
   */
  fun importAndPersistBackupJson(
    context: Context,
    rawJson: String,
  ): Result<BioSyncFullBackupBundle> {
    val parsedResult = parseBackupBundle(rawJson)
    return parsedResult.map { bundle ->
      val todayKey = DashboardTimeFormatter.currentDayKey()
      val finalDash = if (bundle.dashboardData.dayKey != todayKey) {
        DashboardLocalStore.rolloverToDate(context, bundle.dashboardData, todayKey)
      } else {
        bundle.dashboardData
      }
      BioSyncLocalStore.save(context, bundle.storedState)
      DashboardLocalStore.save(context, finalDash)
      vaultPrefs(context).edit()
        .putString(KEY_VAULT_BACKUP_JSON, rawJson)
        .putString(KEY_AUTO_MIRROR_JSON, rawJson)
        .putLong(KEY_VAULT_CREATED_AT_MS, bundle.meta.createdAtEpochMs)
        .apply()
      bundle.copy(dashboardData = finalDash)
    }
  }
}

