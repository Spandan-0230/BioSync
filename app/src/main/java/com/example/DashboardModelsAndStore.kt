package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
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
 * A user-saved Single Item or Meal stored in the Quick Actions shelf.
 * Also used as the building block for selecting multiple Single Items or
 * dragging one Single Item onto another to create a Meal.
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
 * Snapshot of the persisted Dashboard state (Saved Quick Items, Created Single Items for the
 * Meal Builder, Today's Timeline logs, and Streak counter).
 */
@Immutable
data class DashboardStoredData(
  val dayKey: String = DashboardTimeFormatter.currentDayKey(),
  val baseStreakDays: Int = 0,
  val lastCompletedStreakDayKey: String = "",
  val allCreatedSingleItems: List<SavedQuickNutritionItem> = emptyList(),
  val savedQuickItems: List<SavedQuickNutritionItem> = emptyList(),
  val todayTimelineEntries: List<DashboardTimelineEntry> = emptyList(),
)

object DashboardLocalStore {
  private const val PREFS_NAME = "biosync_dashboard_telemetry_v1"
  private const val KEY_DAY_KEY = "dashboard_day_key"
  private const val KEY_BASE_STREAK_DAYS = "dashboard_base_streak_days"
  private const val KEY_LAST_COMPLETED_STREAK_DAY = "dashboard_last_completed_streak_day"
  private const val KEY_CREATED_SINGLE_ITEMS_JSON = "dashboard_created_single_items_json"
  private const val KEY_SAVED_QUICK_ITEMS_JSON = "dashboard_saved_quick_items_json"
  private const val KEY_TIMELINE_ENTRIES_JSON = "dashboard_timeline_entries_json"

  private fun prefs(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun load(context: Context): DashboardStoredData {
    val p = prefs(context)
    val todayKey = DashboardTimeFormatter.currentDayKey()
    val storedDayKey = p.getString(KEY_DAY_KEY, todayKey) ?: todayKey
    val baseStreak = p.getInt(KEY_BASE_STREAK_DAYS, 0).coerceAtLeast(0)
    val lastStreakDay = p.getString(KEY_LAST_COMPLETED_STREAK_DAY, "") ?: ""

    val createdSingles = parseQuickItemsJson(p.getString(KEY_CREATED_SINGLE_ITEMS_JSON, "[]") ?: "[]")
    val savedQuick = parseQuickItemsJson(p.getString(KEY_SAVED_QUICK_ITEMS_JSON, "[]") ?: "[]")

    // If the calendar day rolled over, today's timeline starts fresh at 0 while keeping saved items & streak
    val timelineEntries = if (storedDayKey == todayKey) {
      parseTimelineJson(p.getString(KEY_TIMELINE_ENTRIES_JSON, "[]") ?: "[]")
    } else {
      emptyList()
    }

    return DashboardStoredData(
      dayKey = todayKey,
      baseStreakDays = baseStreak,
      lastCompletedStreakDayKey = lastStreakDay,
      allCreatedSingleItems = createdSingles,
      savedQuickItems = savedQuick,
      todayTimelineEntries = timelineEntries,
    )
  }

  fun save(context: Context, data: DashboardStoredData) {
    prefs(context).edit().apply {
      putString(KEY_DAY_KEY, data.dayKey)
      putInt(KEY_BASE_STREAK_DAYS, data.baseStreakDays.coerceAtLeast(0))
      putString(KEY_LAST_COMPLETED_STREAK_DAY, data.lastCompletedStreakDayKey)
      putString(KEY_CREATED_SINGLE_ITEMS_JSON, serializeQuickItemsJson(data.allCreatedSingleItems))
      putString(KEY_SAVED_QUICK_ITEMS_JSON, serializeQuickItemsJson(data.savedQuickItems))
      putString(KEY_TIMELINE_ENTRIES_JSON, serializeTimelineJson(data.todayTimelineEntries))
      apply()
    }
  }

  private fun serializeQuickItemsJson(items: List<SavedQuickNutritionItem>): String {
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
        put("createdAtEpochMs", item.createdAtEpochMs)
        val comps = JSONArray()
        item.componentNames.forEach { comps.put(it) }
        put("componentNames", comps)
      }
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun parseQuickItemsJson(rawJson: String): List<SavedQuickNutritionItem> {
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
        list.add(
          SavedQuickNutritionItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "Item"),
            isMeal = obj.optBoolean("isMeal", false),
            caloriesKcal = obj.optInt("caloriesKcal", 0),
            proteinGrams = obj.optInt("proteinGrams", 0),
            carbsGrams = obj.optInt("carbsGrams", 0),
            fatsGrams = obj.optInt("fatsGrams", 0),
            componentNames = comps,
            isSavedToQuickActions = obj.optBoolean("isSavedToQuickActions", true),
            createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
          ),
        )
      }
      list
    }.getOrDefault(emptyList())
  }

  private fun serializeTimelineJson(entries: List<DashboardTimelineEntry>): String {
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
        put("timestampEpochMs", e.timestampEpochMs)
        put("formattedTimeAmPm", e.formattedTimeAmPm)
      }
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun parseTimelineJson(rawJson: String): List<DashboardTimelineEntry> {
    return runCatching {
      val arr = JSONArray(rawJson)
      val list = ArrayList<DashboardTimelineEntry>(arr.length())
      for (i in 0 until arr.length()) {
        val obj = arr.optJSONObject(i) ?: continue
        val typeName = obj.optString("type", DashboardEntryType.SINGLE_ITEM.name)
        val parsedType = runCatching { DashboardEntryType.valueOf(typeName) }
          .getOrDefault(DashboardEntryType.SINGLE_ITEM)
        val ts = obj.optLong("timestampEpochMs", System.currentTimeMillis())
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
            timestampEpochMs = ts,
            formattedTimeAmPm = obj.optString("formattedTimeAmPm", DashboardTimeFormatter.formatAmPm(ts)),
          ),
        )
      }
      list
    }.getOrDefault(emptyList())
  }
}
