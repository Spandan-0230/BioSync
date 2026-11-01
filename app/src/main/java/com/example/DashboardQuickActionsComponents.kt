package com.example

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.ui.theme.CaloriesStart
import com.example.ui.theme.CarbsStart
import com.example.ui.theme.FatsStart
import com.example.ui.theme.HydrationEnd
import com.example.ui.theme.HydrationMid
import com.example.ui.theme.HydrationStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ProteinStart
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StepsStart
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Core BioSync Monochrome Palette matching Steps 01–04
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
private val RecessedTrackFill = Color(0xFF07070A)
private val RecessedTrackBorder = Color(0xFF27272E)
private val SolidDangerRed = Color(0xFFEF4444)
private val SolidDangerRedBorder = Color(0xFFF87171)

private val SmoothOutEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

private enum class QuickShelfCategoryFilter(val label: String) {
  ALL("ALL"),
  MEALS("MEALS"),
  SINGLE_ITEMS("SINGLE ITEMS"),
}

private enum class QuickMacroSmartSort(val label: String) {
  RECENT("RECENT"),
  HIGH_PROTEIN("HIGH-P"),
  HIGH_CAL("HIGH-CAL"),
  LOW_CAL("LOW-CAL"),
  A_TO_Z("A–Z"),
}

// ============================================================================
// 1. FLOATING QUICK ACTIONS ENGINE:
//    - Dynamic Categorization (Meals vs Single Items)
//    - Multi-Row Bento Vault + Instant Search & Smart Filter for 20+ items (no frustrating sideways scroll!)
//    - Strict 2.0-Second Hold-to-Overlap & Fusion Slot Combining
//    - Per-Item & Per-Meal Custom Color Studio + Optical Color Mixing for Combined Meals
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardQuickActionsHubCard(
  allCreatedSingleItems: List<SavedQuickNutritionItem>,
  savedQuickItems: List<SavedQuickNutritionItem>,
  targetCalories: Int,
  targetProtein: Int,
  targetCarbs: Int,
  targetFats: Int,
  mealCount: Int,
  borderFlashProgress: Float = 0f,
  onLogNutritionItem: (
    item: SavedQuickNutritionItem,
    saveToQuickActions: Boolean,
  ) -> Unit,
  onQuickTapSavedItem: (SavedQuickNutritionItem) -> Unit,
  onDeleteSavedQuickItem: (String) -> Unit,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val warmTitanium = Color(0xFFE5DEC9)
  val warmTitaniumMuted = Color(0xFFC8BFA8)

  var showQuickActionPopup by remember { mutableStateOf(false) }
  var popupInitialIsMealTab by remember { mutableStateOf(false) }
  var popupPreselectedPair by remember {
    mutableStateOf<Pair<SavedQuickNutritionItem, SavedQuickNutritionItem>?>(null)
  }

  // Dynamic categorization state
  val savedMeals = remember(savedQuickItems) { savedQuickItems.filter { it.isMeal } }
  val savedSingles = remember(savedQuickItems) { savedQuickItems.filter { !it.isMeal } }

  var activeCategory by remember { mutableStateOf(QuickShelfCategoryFilter.ALL) }
  var isVaultGridMode by remember { mutableStateOf(true) }
  var searchQuery by remember { mutableStateOf("") }
  var showSearchAndSortBar by remember { mutableStateOf(false) }
  var activeSmartSort by remember { mutableStateOf(QuickMacroSmartSort.RECENT) }

  // Separate Manual Expand/Collapse for Meals & Single Items (Collapsed by default)
  var isMealsExpanded by remember { mutableStateOf(false) }
  var isSinglesExpanded by remember { mutableStateOf(false) }

  // Reset category filter if a category becomes empty
  LaunchedEffect(savedMeals.size, savedSingles.size) {
    if (activeCategory == QuickShelfCategoryFilter.MEALS && savedMeals.isEmpty()) {
      activeCategory = QuickShelfCategoryFilter.ALL
    } else if (activeCategory == QuickShelfCategoryFilter.SINGLE_ITEMS && savedSingles.isEmpty()) {
      activeCategory = QuickShelfCategoryFilter.ALL
    }
  }

  // 2.0-Second Hold-to-Overlap & Fusion Slot state
  var holdingItemId by remember { mutableStateOf<String?>(null) }
  var holdProgress by remember { mutableFloatStateOf(0f) } // 0.0 -> 1.0 over 2000ms
  var draggingItemId by remember { mutableStateOf<String?>(null) }
  var dragOffsetX by remember { mutableFloatStateOf(0f) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  var hoveredTargetItemId by remember { mutableStateOf<String?>(null) }
  var armedFusionSlotItem by remember { mutableStateOf<SavedQuickNutritionItem?>(null) }
  val singleItemBounds = remember { mutableStateMapOf<String, Rect>() }

  // Clear armed fusion item if deleted
  LaunchedEffect(savedQuickItems) {
    val currentArmed = armedFusionSlotItem
    if (currentArmed != null && savedQuickItems.none { it.id == currentArmed.id }) {
      armedFusionSlotItem = null
    }
  }

  // Strictly sync available Single Items with currently saved Single Items in the Quick Actions Vault
  // so any deleted or removed item from Quick Actions NEVER appears inside Create Meal.
  val availableSingleItems = savedSingles

  fun applyFilterAndSort(items: List<SavedQuickNutritionItem>): List<SavedQuickNutritionItem> {
    val q = searchQuery.trim().lowercase(Locale.US)
    val filtered = if (q.isEmpty()) {
      items
    } else {
      items.filter {
        it.name.lowercase(Locale.US).contains(q) ||
          it.componentNames.any { c -> c.lowercase(Locale.US).contains(q) }
      }
    }
    return when (activeSmartSort) {
      QuickMacroSmartSort.RECENT -> filtered
      QuickMacroSmartSort.HIGH_PROTEIN -> filtered.sortedByDescending { it.proteinGrams }
      QuickMacroSmartSort.HIGH_CAL -> filtered.sortedByDescending { it.caloriesKcal }
      QuickMacroSmartSort.LOW_CAL -> filtered.sortedBy { it.caloriesKcal }
      QuickMacroSmartSort.A_TO_Z -> filtered.sortedBy { it.name.lowercase(Locale.US) }
    }
  }

  val filteredMeals = remember(savedMeals, searchQuery, activeSmartSort) {
    applyFilterAndSort(savedMeals)
  }
  val filteredSingles = remember(savedSingles, searchQuery, activeSmartSort) {
    applyFilterAndSort(savedSingles)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = PureWhite,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_quick_actions_hub"),
  ) {
    // 1. Clean Ecosystem Header Row (Matches HYDRATION ENGINE & TODAY'S TIMELINE)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
      ) {
        DashboardSyncedHeaderDot(
          startColor = PureWhite,
          endColor = Zinc300,
        )
        Text(
          text = "QUICK ACTIONS ENGINE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (savedQuickItems.isNotEmpty()) {
          // Search / Sort Toggle Pill
          val isSearchOpen = showSearchAndSortBar
          val hasActiveFilterOrSort = searchQuery.isNotBlank() || activeSmartSort != QuickMacroSmartSort.RECENT
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(
                when {
                  isSearchOpen -> PureWhite
                  hasActiveFilterOrSort -> Color(0xFF1D1D28)
                  else -> Color(0xFF121217)
                },
              )
              .border(
                width = 1.dp,
                color = when {
                  isSearchOpen -> PureWhite
                  hasActiveFilterOrSort -> PureWhite.copy(alpha = 0.65f)
                  else -> Zinc700
                },
                shape = RoundedCornerShape(7.dp),
              )
              .clickable(role = Role.Button) {
                haptics.selection()
                val nextOpen = !showSearchAndSortBar
                showSearchAndSortBar = nextOpen
                if (nextOpen) {
                  // Auto-open relevant sections so user can immediately see searched & sorted items
                  if (savedMeals.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.SINGLE_ITEMS) {
                    isMealsExpanded = true
                  }
                  if (savedSingles.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.MEALS) {
                    isSinglesExpanded = true
                  }
                }
              }
              .padding(horizontal = 8.5.dp, vertical = 4.dp)
              .testTag("quick_actions_search_toggle"),
          ) {
            Text(
              text = when {
                isSearchOpen -> "FIND / SORT ▲"
                searchQuery.isNotBlank() -> "FILTERED • ${filteredMeals.size + filteredSingles.size}"
                activeSmartSort != QuickMacroSmartSort.RECENT -> "SORT: ${activeSmartSort.label}"
                else -> "FIND / SORT ▼"
              },
              color = if (isSearchOpen) ObsidianBlack else PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.4.sp,
              letterSpacing = 0.4.sp,
            )
          }

          // Vault Grid vs Dock Switcher
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(7.dp))
              .background(PureWhite)
              .border(1.dp, PureWhite, RoundedCornerShape(7.dp))
              .clickable(role = Role.Button) {
                haptics.selection()
                isVaultGridMode = !isVaultGridMode
              }
              .padding(horizontal = 8.5.dp, vertical = 4.dp)
              .testTag("quick_actions_view_mode_toggle"),
          ) {
            Text(
              text = if (isVaultGridMode) "GRID ⊞" else "DOCK ▤",
              color = ObsidianBlack,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.4.sp,
              letterSpacing = 0.4.sp,
            )
          }
        } else {
          Text(
            text = "0 SAVED",
            color = Zinc400,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.8.sp,
            letterSpacing = 0.5.sp,
          )
        }
      }
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Empty State Console vs Categorized Vault Console
    if (savedQuickItems.isEmpty()) {
      // =======================================================================
      // EXECUTIVE 2-CHAMBER LAUNCH CONSOLE (When 0 Saved — Crisp, Balanced, Pro)
      // =======================================================================
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(13.dp))
          .background(Color(0xFF0A0A0E))
          .border(1.dp, Color(0xFF25252E), RoundedCornerShape(13.dp))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Top Specification Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060608))
            .border(1.dp, Color(0xFF1E1E26), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.5.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .width(3.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(PureWhite),
            )
            Text(
              text = "NUTRITION VAULT & MEAL ARCHITECT",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 9.2.sp,
              letterSpacing = 0.6.sp,
            )
          }

          Text(
            text = "READY",
            color = warmTitanium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 8.6.sp,
            letterSpacing = 0.5.sp,
          )
        }

        // Symmetrical 2-Chamber Tactile Launch Buttons: [+ SINGLE ITEM] & [+ CREATE MEAL]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          // Chamber 01: + SINGLE ITEM (Retains primary testTag for "+" button compatibility)
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(11.dp))
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(Color(0xFF15151C), Color(0xFF0C0C10)),
                ),
              )
              .border(1.dp, Color(0xFF2E2E3A), RoundedCornerShape(11.dp))
              .clickable(role = Role.Button) {
                haptics.confirm()
                popupPreselectedPair = null
                popupInitialIsMealTab = false
                showQuickActionPopup = true
              }
              .padding(11.dp)
              .testTag("quick_actions_plus_button"),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(PureWhite)
                  .border(1.dp, PureWhite, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "+",
                  color = ObsidianBlack,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp,
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF070709))
                  .border(1.dp, Color(0xFF272730), RoundedCornerShape(5.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "01 • ITEM",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 7.8.sp,
                  letterSpacing = 0.4.sp,
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Log Single Item",
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
              Text(
                text = "Pin individual food with custom color ID",
                color = Zinc300,
                fontFamily = InterFontFamily,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
              )
            }
          }

          // Chamber 02: + CREATE MEAL
          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(11.dp))
              .background(
                brush = Brush.verticalGradient(
                  colors = listOf(Color(0xFF15151C), Color(0xFF0C0C10)),
                ),
              )
              .border(1.dp, Color(0xFF2E2E3A), RoundedCornerShape(11.dp))
              .clickable(role = Role.Button) {
                haptics.confirm()
                popupPreselectedPair = null
                popupInitialIsMealTab = true
                showQuickActionPopup = true
              }
              .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(warmTitanium)
                  .border(1.dp, PureWhite, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "+",
                  color = ObsidianBlack,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp,
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF070709))
                  .border(1.dp, Color(0xFF272730), RoundedCornerShape(5.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp),
              ) {
                Text(
                  text = "02 • MEAL",
                  color = warmTitanium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 7.8.sp,
                  letterSpacing = 0.4.sp,
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Create Meal",
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
              )
              Text(
                text = "Combine single items & fuse colors",
                color = Zinc300,
                fontFamily = InterFontFamily,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
              )
            }
          }
        }

        // Bottom 3-Pillar Architecture Specification Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060608))
            .border(1.dp, Color(0xFF1D1D24), RoundedCornerShape(8.dp))
            .padding(vertical = 7.dp, horizontal = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "CATEGORIZED VAULT",
            color = Zinc200,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(10.dp)
              .background(Zinc800),
          )
          Text(
            text = "2.0S HOLD OVERLAP",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 8.0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(10.dp)
              .background(Zinc800),
          )
          Text(
            text = "COLOR MIX ALCHEMY",
            color = warmTitanium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
        }
      }
    } else {
      // Dynamic Categorized Control Bar when user has saved items
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        QuickActionPlusActuatorButton(
          compact = true,
          onClick = {
            haptics.confirm()
            popupPreselectedPair = null
            popupInitialIsMealTab = activeCategory == QuickShelfCategoryFilter.MEALS
            showQuickActionPopup = true
          },
        )

        // Dynamic Category Selector Tabs (ALL / MEALS / SINGLE ITEMS)
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF07070A))
            .border(1.dp, Color(0xFF25252E), RoundedCornerShape(11.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val categoriesToShow = buildList {
            add(QuickShelfCategoryFilter.ALL to "ALL (${savedQuickItems.size})")
            if (savedMeals.isNotEmpty()) {
              add(QuickShelfCategoryFilter.MEALS to "MEALS (${savedMeals.size})")
            }
            if (savedSingles.isNotEmpty()) {
              add(QuickShelfCategoryFilter.SINGLE_ITEMS to "ITEMS (${savedSingles.size})")
            }
          }

          categoriesToShow.forEach { (cat, label) ->
            val selected = activeCategory == cat
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) PureWhite else Color.Transparent)
                .clickable(role = Role.Tab) {
                  haptics.selection()
                  activeCategory = cat
                  if (cat == QuickShelfCategoryFilter.MEALS) {
                    isMealsExpanded = true
                  } else if (cat == QuickShelfCategoryFilter.SINGLE_ITEMS) {
                    isSinglesExpanded = true
                  }
                }
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .testTag("quick_actions_cat_${cat.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = label,
                color = if (selected) ObsidianBlack else PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.8.sp,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // =========================================================================
      // SEPARATE MANUAL COLLAPSE / OPEN COMMAND BAR (Collapsed by Default)
      // Allows opening or collapsing all Meals and all Single Items separately.
      // =========================================================================
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF07070A))
          .border(1.dp, Color(0xFF23232D), RoundedCornerShape(10.dp))
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // 1. Separate Manual Open / Collapse Button for ALL MEALS
        if (savedMeals.isNotEmpty()) {
          Row(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isMealsExpanded) PureWhite else Color(0xFF13131A))
              .border(
                width = 1.dp,
                color = if (isMealsExpanded) PureWhite else Color(0xFF2D2D3A),
                shape = RoundedCornerShape(6.dp),
              )
              .clickable(role = Role.Button) {
                haptics.selection()
                isMealsExpanded = !isMealsExpanded
              }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("quick_actions_toggle_meals_collapse"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (isMealsExpanded) "MEALS (${savedMeals.size}): OPEN ▲" else "MEALS (${savedMeals.size}): COLLAPSED ▼",
              color = if (isMealsExpanded) ObsidianBlack else PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.0.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        // 2. Separate Manual Open / Collapse Button for ALL SINGLE ITEMS
        if (savedSingles.isNotEmpty()) {
          Row(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSinglesExpanded) PureWhite else Color(0xFF13131A))
              .border(
                width = 1.dp,
                color = if (isSinglesExpanded) PureWhite else Color(0xFF2D2D3A),
                shape = RoundedCornerShape(6.dp),
              )
              .clickable(role = Role.Button) {
                haptics.selection()
                isSinglesExpanded = !isSinglesExpanded
              }
              .padding(horizontal = 8.dp, vertical = 5.dp)
              .testTag("quick_actions_toggle_singles_collapse"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (isSinglesExpanded) "ITEMS (${savedSingles.size}): OPEN ▲" else "ITEMS (${savedSingles.size}): COLLAPSED ▼",
              color = if (isSinglesExpanded) ObsidianBlack else PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.0.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Instant Search & Macro Sort Bar (Directly toggled by FIND / SORT button)
      AnimatedVisibility(
        visible = showSearchAndSortBar,
        enter = expandVertically(tween(durationMillis = 220, easing = SmoothOutEasing)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(durationMillis = 200, easing = SmoothOutEasing)) + fadeOut(tween(150)),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xFF08080C))
            .border(1.dp, Color(0xFF282834), RoundedCornerShape(11.dp))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Search Input Box
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(ObsidianBlack)
              .border(
                width = 1.dp,
                color = if (searchQuery.isNotEmpty()) PureWhite else Zinc700,
                shape = RoundedCornerShape(8.dp),
              )
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Box(
              modifier = Modifier.weight(1f),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (searchQuery.isEmpty()) {
                Text(
                  text = "Search ${savedQuickItems.size} saved items or meals by name...",
                  color = Zinc400,
                  fontFamily = InterFontFamily,
                  fontSize = 11.5.sp,
                )
              }
              BasicTextField(
                value = searchQuery,
                onValueChange = { nextQuery ->
                  searchQuery = nextQuery.take(32)
                  // Automatically expand sections when typing so matching results are visible
                  if (savedMeals.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.SINGLE_ITEMS) {
                    isMealsExpanded = true
                  }
                  if (savedSingles.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.MEALS) {
                    isSinglesExpanded = true
                  }
                },
                singleLine = true,
                textStyle = TextStyle(
                  color = PureWhite,
                  fontFamily = InterFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                ),
                cursorBrush = SolidColor(PureWhite),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("quick_actions_search_input"),
              )
            }
            if (searchQuery.isNotEmpty()) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(5.dp))
                  .background(Color(0xFF1A1A24))
                  .border(1.dp, Zinc700, RoundedCornerShape(5.dp))
                  .clickable {
                    haptics.tick()
                    searchQuery = ""
                  }
                  .padding(horizontal = 6.dp, vertical = 2.5.dp),
              ) {
                Text(
                  text = "CLEAR ×",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.0.sp,
                )
              }
            }
          }

          // Smart Sort Pills Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "SORT BY:",
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.2.sp,
            )
            QuickMacroSmartSort.entries.forEach { sortMode ->
              val isSortActive = activeSmartSort == sortMode
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSortActive) PureWhite else Color(0xFF14141B))
                  .border(
                    width = 1.dp,
                    color = if (isSortActive) PureWhite else Zinc700,
                    shape = RoundedCornerShape(6.dp),
                  )
                  .clickable(role = Role.Button) {
                    haptics.selection()
                    activeSmartSort = sortMode
                    // Automatically open visible section(s) so user sees the sorted items immediately
                    if (savedMeals.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.SINGLE_ITEMS) {
                      isMealsExpanded = true
                    }
                    if (savedSingles.isNotEmpty() && activeCategory != QuickShelfCategoryFilter.MEALS) {
                      isSinglesExpanded = true
                    }
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .testTag("quick_actions_sort_${sortMode.name.lowercase()}"),
              ) {
                Text(
                  text = sortMode.label,
                  color = if (isSortActive) ObsidianBlack else PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.0.sp,
                )
              }
            }
          }
        }
      }

      // Live 2.0-Second Hold Progress / Armed Fusion Dock Telemetry Banner
      val currentHoldingItem = remember(holdingItemId, savedSingles) {
        savedSingles.firstOrNull { it.id == holdingItemId }
      }
      val currentDraggingItem = remember(draggingItemId, savedSingles) {
        savedSingles.firstOrNull { it.id == draggingItemId }
      }
      val currentHoveredItem = remember(hoveredTargetItemId, savedSingles) {
        savedSingles.firstOrNull { it.id == hoveredTargetItemId && it.id != draggingItemId }
      }

      AnimatedVisibility(
        visible = currentHoldingItem != null || currentDraggingItem != null || armedFusionSlotItem != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        val activeSource = currentDraggingItem ?: currentHoldingItem ?: armedFusionSlotItem
        if (activeSource != null) {
          val sourceColor = QuickItemColorAlchemy.argbToColor(activeSource.colorArgb)
          val previewMixedArgb = if (currentHoveredItem != null) {
            QuickItemColorAlchemy.mixItemColors(listOf(activeSource.colorArgb, currentHoveredItem.colorArgb))
          } else {
            activeSource.colorArgb
          }
          val previewMixedColor = QuickItemColorAlchemy.argbToColor(previewMixedArgb)

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp)
              .clip(RoundedCornerShape(11.dp))
              .background(Color(0xFF07070A))
              .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(11.dp))
              .padding(horizontal = 11.dp, vertical = 8.5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
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
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(sourceColor)
                      .border(1.dp, PureWhite, CircleShape),
                  )
                  if (currentHoveredItem != null) {
                    Text(
                      text = "+",
                      color = PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 9.sp,
                    )
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(QuickItemColorAlchemy.argbToColor(currentHoveredItem.colorArgb))
                        .border(1.dp, PureWhite, CircleShape),
                    )
                    Text(
                      text = "➔",
                      color = PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 9.sp,
                    )
                    Box(
                      modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(previewMixedColor)
                        .border(1.dp, PureWhite, CircleShape),
                    )
                  }
                }

                Text(
                  text = when {
                    currentHoveredItem != null -> {
                      "RELEASE TO FUSE: ${activeSource.name.uppercase()} + ${currentHoveredItem.name.uppercase()} (MIXED COLOR)"
                    }
                    currentDraggingItem != null -> {
                      "2.0S LOCKED! OVERLAP ONTO ANY OTHER SINGLE ITEM TO FUSE"
                    }
                    currentHoldingItem != null -> {
                      val secs = String.format(Locale.US, "%.1f", holdProgress * 2.0f)
                      "HOLDING ${activeSource.name.uppercase()} • ${secs}S / 2.0S TO ARM OVERLAP..."
                    }
                    else -> {
                      "FUSION SLOT ARMED: ${activeSource.name.uppercase()} • TAP ANY 2ND SINGLE ITEM TO COMBINE"
                    }
                  },
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.4.sp,
                  letterSpacing = 0.3.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              if (armedFusionSlotItem != null && currentHoldingItem == null && currentDraggingItem == null) {
                Text(
                  text = "CANCEL ×",
                  color = SolidDangerRedBorder,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.2.sp,
                  modifier = Modifier
                    .padding(start = 6.dp)
                    .clickable {
                      haptics.tick()
                      armedFusionSlotItem = null
                    },
                )
              }
            }

            // 2.0s Hold Progress Bar
            if (currentHoldingItem != null || currentDraggingItem != null) {
              val barFill = if (currentDraggingItem != null) 1f else holdProgress.coerceIn(0.04f, 1f)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(CircleShape)
                  .background(Zinc900),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(barFill)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(PureWhite),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // =========================================================================
      // DYNAMICALLY CATEGORIZED BLOCKS (1. SAVED MEALS • 2. SINGLE ITEMS VAULT)
      // Each block independently senses viewport visibility (auto open on seen /
      // auto collapse on unseen) AND supports manual collapse / open up separately!
      // =========================================================================
      val showMealsSection = (activeCategory == QuickShelfCategoryFilter.ALL ||
        activeCategory == QuickShelfCategoryFilter.MEALS) &&
        filteredMeals.isNotEmpty()

      val showSinglesSection = (activeCategory == QuickShelfCategoryFilter.ALL ||
        activeCategory == QuickShelfCategoryFilter.SINGLE_ITEMS) &&
        filteredSingles.isNotEmpty()

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("saved_quick_actions_shelf"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!showMealsSection && !showSinglesSection && searchQuery.isNotBlank()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(11.dp))
              .background(Color(0xFF0A0A0E))
              .border(1.dp, Color(0xFF282834), RoundedCornerShape(11.dp))
              .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "NO MATCHES FOR \"${searchQuery.uppercase()}\"",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 9.0.sp,
              modifier = Modifier.weight(1f),
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PureWhite)
                .clickable(role = Role.Button) {
                  haptics.selection()
                  searchQuery = ""
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text(
                text = "RESET SEARCH",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.0.sp,
              )
            }
          }
        }

        // ─── CATEGORY BLOCK A: SAVED MEAL PROTOCOLS (Dynamic + Manual Collapse) ─
        if (showMealsSection) {
          val totalMealsKcal = remember(filteredMeals) { filteredMeals.sumOf { it.caloriesKcal } }
          val totalMealsProtein = remember(filteredMeals) { filteredMeals.sumOf { it.proteinGrams } }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(13.dp))
              .background(Color(0xFF08080B))
              .border(1.dp, Color(0xFF25252E), RoundedCornerShape(13.dp))
              .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            // Section A Header Row (Clickable Manual Toggle)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                  haptics.selection()
                  isMealsExpanded = !isMealsExpanded
                },
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .width(3.5.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(warmTitanium),
                )
                Text(
                  text = "SAVED MEALS • COMBINED & CUSTOM",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.2.sp,
                  letterSpacing = 0.6.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF14141B))
                    .border(1.dp, Color(0xFF2C2C38), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                  Text(
                    text = "${filteredMeals.size} MEAL${if (filteredMeals.size == 1) "" else "S"}",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.0.sp,
                  )
                }

                // Dedicated Manual Collapse / Open Button for Meals
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isMealsExpanded) PureWhite else Color(0xFF171720))
                    .border(
                      width = 1.dp,
                      color = if (isMealsExpanded) PureWhite else Color(0xFF343444),
                      shape = RoundedCornerShape(6.dp),
                    )
                    .clickable(role = Role.Button) {
                      haptics.selection()
                      isMealsExpanded = !isMealsExpanded
                    }
                    .padding(horizontal = 7.5.dp, vertical = 3.dp)
                    .testTag("meals_section_collapse_button"),
                ) {
                  Text(
                    text = if (isMealsExpanded) "COLLAPSE ▲" else "OPEN ▼",
                    color = if (isMealsExpanded) ObsidianBlack else PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 7.8.sp,
                  )
                }
              }
            }

            // Collapsed Summary Strip when Meals section is collapsed (either by unseen viewport or manual toggle)
            AnimatedVisibility(
              visible = !isMealsExpanded,
              enter = expandVertically(tween(durationMillis = 220, easing = SmoothOutEasing)) + fadeIn(tween(180)),
              exit = shrinkVertically(tween(durationMillis = 200, easing = SmoothOutEasing)) + fadeOut(tween(150)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(9.dp))
                  .background(Color(0xFF0E0E14))
                  .border(1.dp, Color(0xFF272733), RoundedCornerShape(9.dp))
                  .clickable(role = Role.Button) {
                    haptics.selection()
                    isMealsExpanded = true
                  }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
                    filteredMeals.take(4).forEach { m ->
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(QuickItemColorAlchemy.argbToColor(m.colorArgb))
                          .border(1.dp, PureWhite, CircleShape),
                      )
                    }
                  }
                  Text(
                    text = "${filteredMeals.size} Meal${if (filteredMeals.size == 1) "" else "s"} Folded • ${numberFormat.format(totalMealsKcal)} kcal • ${totalMealsProtein}g P",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Text(
                  text = "OPEN MEALS ▼",
                  color = warmTitanium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.0.sp,
                )
              }
            }

            // Expanded Meals Content
            AnimatedVisibility(
              visible = isMealsExpanded,
              enter = expandVertically(tween(durationMillis = 260, easing = SmoothOutEasing)) + fadeIn(tween(200)),
              exit = shrinkVertically(tween(durationMillis = 240, easing = SmoothOutEasing)) + fadeOut(tween(180)),
            ) {
              if (isVaultGridMode) {
                // Full-Width Executive Meal Cards (Zero dead 50% empty space on the right!)
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  filteredMeals.forEach { mealItem ->
                    SavedQuickActionItemCard(
                      item = mealItem,
                      isHoldingThis = false,
                      holdProgress = 0f,
                      isDraggingThis = false,
                      isHoveredTarget = false,
                      isArmedInFusionSlot = false,
                      numberFormat = numberFormat,
                      onTap = {
                        haptics.confirm()
                        onQuickTapSavedItem(mealItem)
                      },
                      onDelete = {
                        haptics.tick()
                        onDeleteSavedQuickItem(mealItem.id)
                      },
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }
              } else {
                // Compact Horizontal Dock Mode
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  filteredMeals.forEach { mealItem ->
                    SavedQuickActionItemCard(
                      item = mealItem,
                      isHoldingThis = false,
                      holdProgress = 0f,
                      isDraggingThis = false,
                      isHoveredTarget = false,
                      isArmedInFusionSlot = false,
                      numberFormat = numberFormat,
                      onTap = {
                        haptics.confirm()
                        onQuickTapSavedItem(mealItem)
                      },
                      onDelete = {
                        haptics.tick()
                        onDeleteSavedQuickItem(mealItem.id)
                      },
                      modifier = Modifier.width(268.dp),
                    )
                  }
                }
              }
            }
          }
        }

        // ─── CATEGORY BLOCK B: SINGLE ITEMS VAULT (Dynamic + Auto/Manual Collapse + 2s Hold) ─
        if (showSinglesSection) {
          val totalSinglesKcal = remember(filteredSingles) { filteredSingles.sumOf { it.caloriesKcal } }
          val totalSinglesProtein = remember(filteredSingles) { filteredSingles.sumOf { it.proteinGrams } }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(13.dp))
              .background(Color(0xFF08080B))
              .border(1.dp, Color(0xFF25252E), RoundedCornerShape(13.dp))
              .padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
          ) {
            // Section B Header Row (Clickable Manual Toggle)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                  haptics.selection()
                  isSinglesExpanded = !isSinglesExpanded
                },
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .width(3.5.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PureWhite),
                )
                Text(
                  text = "SINGLE ITEMS VAULT",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.2.sp,
                  letterSpacing = 0.6.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF14141B))
                    .border(1.dp, Color(0xFF2C2C38), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                  Text(
                    text = if (filteredSingles.size >= 2) {
                      "HOLD 2S FUSE • ${filteredSingles.size}"
                    } else {
                      "${filteredSingles.size} ITEM${if (filteredSingles.size == 1) "" else "S"}"
                    },
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.0.sp,
                  )
                }

                // Dedicated Manual Collapse / Open Button for Single Items
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSinglesExpanded) PureWhite else Color(0xFF171720))
                    .border(
                      width = 1.dp,
                      color = if (isSinglesExpanded) PureWhite else Color(0xFF343444),
                      shape = RoundedCornerShape(6.dp),
                    )
                    .clickable(role = Role.Button) {
                      haptics.selection()
                      isSinglesExpanded = !isSinglesExpanded
                    }
                    .padding(horizontal = 7.5.dp, vertical = 3.dp)
                    .testTag("singles_section_collapse_button"),
                ) {
                  Text(
                    text = if (isSinglesExpanded) "COLLAPSE ▲" else "OPEN ▼",
                    color = if (isSinglesExpanded) ObsidianBlack else PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 7.8.sp,
                  )
                }
              }
            }

            // Collapsed Summary Strip when Single Items section is collapsed
            AnimatedVisibility(
              visible = !isSinglesExpanded,
              enter = expandVertically(tween(durationMillis = 220, easing = SmoothOutEasing)) + fadeIn(tween(180)),
              exit = shrinkVertically(tween(durationMillis = 200, easing = SmoothOutEasing)) + fadeOut(tween(150)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(9.dp))
                  .background(Color(0xFF0E0E14))
                  .border(1.dp, Color(0xFF272733), RoundedCornerShape(9.dp))
                  .clickable(role = Role.Button) {
                    haptics.selection()
                    isSinglesExpanded = true
                  }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
                    filteredSingles.take(5).forEach { s ->
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(QuickItemColorAlchemy.argbToColor(s.colorArgb))
                          .border(1.dp, PureWhite, CircleShape),
                      )
                    }
                  }
                  Text(
                    text = "${filteredSingles.size} Item${if (filteredSingles.size == 1) "" else "s"} Folded • ${numberFormat.format(totalSinglesKcal)} kcal • ${totalSinglesProtein}g P",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Text(
                  text = "OPEN ITEMS ▼",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.0.sp,
                )
              }
            }

            // Expanded Single Items Content
            AnimatedVisibility(
              visible = isSinglesExpanded,
              enter = expandVertically(tween(durationMillis = 260, easing = SmoothOutEasing)) + fadeIn(tween(200)),
              exit = shrinkVertically(tween(durationMillis = 240, easing = SmoothOutEasing)) + fadeOut(tween(180)),
            ) {
              if (isVaultGridMode) {
                // Adaptive Multi-Row Bento Vault Grid:
                // - Pairs items 2 per row when 2+ items exist
                // - Any single or odd remaining item automatically spans 100% full width (no dead half-width void!)
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  filteredSingles.chunked(2).forEach { rowSingles ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      rowSingles.forEach { singleItem ->
                        val isHoldingThis = holdingItemId == singleItem.id
                        val isDraggingThis = draggingItemId == singleItem.id
                        val isHoveredTarget = hoveredTargetItemId == singleItem.id &&
                          draggingItemId != null &&
                          draggingItemId != singleItem.id
                        val isArmedInFusionSlot = armedFusionSlotItem?.id == singleItem.id

                        SavedQuickActionItemCard(
                          item = singleItem,
                          isHoldingThis = isHoldingThis,
                          holdProgress = if (isHoldingThis) holdProgress else 0f,
                          isDraggingThis = isDraggingThis,
                          isHoveredTarget = isHoveredTarget,
                          isArmedInFusionSlot = isArmedInFusionSlot,
                          numberFormat = numberFormat,
                          onTap = {
                            val armed = armedFusionSlotItem
                            if (armed != null && armed.id != singleItem.id) {
                              haptics.confirm()
                              armedFusionSlotItem = null
                              popupPreselectedPair = armed to singleItem
                              popupInitialIsMealTab = true
                              showQuickActionPopup = true
                            } else {
                              haptics.confirm()
                              onQuickTapSavedItem(singleItem)
                            }
                          },
                          onDelete = {
                            haptics.tick()
                            onDeleteSavedQuickItem(singleItem.id)
                          },
                          modifier = Modifier
                            .weight(1f)
                            .zIndex(if (isDraggingThis) 25f else 1f)
                            .offset {
                              if (isDraggingThis) {
                                IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                              } else {
                                IntOffset.Zero
                              }
                            }
                            .onGloballyPositioned { coords ->
                              if (!isDraggingThis) {
                                singleItemBounds[singleItem.id] = coords.boundsInRoot()
                              }
                            }
                            .twoSecondHoldToOverlapGesture(
                              item = singleItem,
                              savedSingles = savedSingles,
                              singleItemBounds = singleItemBounds,
                              coroutineScope = coroutineScope,
                              haptics = haptics,
                              onHoldProgress = { id, prog ->
                                holdingItemId = id
                                holdProgress = prog
                              },
                              onDragArmed = { id ->
                                holdingItemId = null
                                holdProgress = 1f
                                draggingItemId = id
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                hoveredTargetItemId = null
                              },
                              onDragMove = { dx, dy, hitTargetId ->
                                dragOffsetX = dx
                                dragOffsetY = dy
                                hoveredTargetItemId = hitTargetId
                              },
                              onDragFinish = { sourceItem, targetItem ->
                                holdingItemId = null
                                holdProgress = 0f
                                draggingItemId = null
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                hoveredTargetItemId = null
                                if (targetItem != null && targetItem.id != sourceItem.id) {
                                  haptics.confirm()
                                  armedFusionSlotItem = null
                                  popupPreselectedPair = sourceItem to targetItem
                                  popupInitialIsMealTab = true
                                  showQuickActionPopup = true
                                } else {
                                  armedFusionSlotItem = if (armedFusionSlotItem?.id == sourceItem.id) {
                                    null
                                  } else {
                                    sourceItem
                                  }
                                }
                              },
                              onShortTap = {
                                val armed = armedFusionSlotItem
                                if (armed != null && armed.id != singleItem.id) {
                                  haptics.confirm()
                                  armedFusionSlotItem = null
                                  popupPreselectedPair = armed to singleItem
                                  popupInitialIsMealTab = true
                                  showQuickActionPopup = true
                                } else {
                                  haptics.confirm()
                                  onQuickTapSavedItem(singleItem)
                                }
                              },
                            ),
                        )
                      }
                    }
                  }
                }
              } else {
                // Compact Horizontal Dock Mode (Also supports 2.0s Hold-to-Overlap)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  filteredSingles.forEach { singleItem ->
                    val isHoldingThis = holdingItemId == singleItem.id
                    val isDraggingThis = draggingItemId == singleItem.id
                    val isHoveredTarget = hoveredTargetItemId == singleItem.id &&
                      draggingItemId != null &&
                      draggingItemId != singleItem.id
                    val isArmedInFusionSlot = armedFusionSlotItem?.id == singleItem.id

                    SavedQuickActionItemCard(
                      item = singleItem,
                      isHoldingThis = isHoldingThis,
                      holdProgress = if (isHoldingThis) holdProgress else 0f,
                      isDraggingThis = isDraggingThis,
                      isHoveredTarget = isHoveredTarget,
                      isArmedInFusionSlot = isArmedInFusionSlot,
                      numberFormat = numberFormat,
                      onTap = {
                        val armed = armedFusionSlotItem
                        if (armed != null && armed.id != singleItem.id) {
                          haptics.confirm()
                          armedFusionSlotItem = null
                          popupPreselectedPair = armed to singleItem
                          popupInitialIsMealTab = true
                          showQuickActionPopup = true
                        } else {
                          haptics.confirm()
                          onQuickTapSavedItem(singleItem)
                        }
                      },
                      onDelete = {
                        haptics.tick()
                        onDeleteSavedQuickItem(singleItem.id)
                      },
                      modifier = Modifier
                        .width(248.dp)
                        .zIndex(if (isDraggingThis) 25f else 1f)
                        .offset {
                          if (isDraggingThis) {
                            IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                          } else {
                            IntOffset.Zero
                          }
                        }
                        .onGloballyPositioned { coords ->
                          if (!isDraggingThis) {
                            singleItemBounds[singleItem.id] = coords.boundsInRoot()
                          }
                        }
                        .twoSecondHoldToOverlapGesture(
                          item = singleItem,
                          savedSingles = savedSingles,
                          singleItemBounds = singleItemBounds,
                          coroutineScope = coroutineScope,
                          haptics = haptics,
                          onHoldProgress = { id, prog ->
                            holdingItemId = id
                            holdProgress = prog
                          },
                          onDragArmed = { id ->
                            holdingItemId = null
                            holdProgress = 1f
                            draggingItemId = id
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            hoveredTargetItemId = null
                          },
                          onDragMove = { dx, dy, hitTargetId ->
                            dragOffsetX = dx
                            dragOffsetY = dy
                            hoveredTargetItemId = hitTargetId
                          },
                          onDragFinish = { sourceItem, targetItem ->
                            holdingItemId = null
                            holdProgress = 0f
                            draggingItemId = null
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            hoveredTargetItemId = null
                            if (targetItem != null && targetItem.id != sourceItem.id) {
                              haptics.confirm()
                              armedFusionSlotItem = null
                              popupPreselectedPair = sourceItem to targetItem
                              popupInitialIsMealTab = true
                              showQuickActionPopup = true
                            } else {
                              armedFusionSlotItem = if (armedFusionSlotItem?.id == sourceItem.id) {
                                null
                              } else {
                                sourceItem
                              }
                            }
                          },
                          onShortTap = {
                            val armed = armedFusionSlotItem
                            if (armed != null && armed.id != singleItem.id) {
                              haptics.confirm()
                              armedFusionSlotItem = null
                              popupPreselectedPair = armed to singleItem
                              popupInitialIsMealTab = true
                              showQuickActionPopup = true
                            } else {
                              haptics.confirm()
                              onQuickTapSavedItem(singleItem)
                            }
                          },
                        ),
                    )
                  }
                }
              }
            }
          }
        }

        if (!showMealsSection && !showSinglesSection) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(11.dp))
              .background(Color(0xFF09090D))
              .border(1.dp, Zinc800, RoundedCornerShape(11.dp))
              .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "No saved items match \"$searchQuery\" in this category.",
              color = Zinc300,
              fontFamily = InterFontFamily,
              fontSize = 11.5.sp,
            )
          }
        }
      }
    }
  }

  // POPUP MODAL WHEN USER CLICKS "+" OR OVERLAPS / FUSES TWO SINGLE ITEMS AFTER 2S HOLD
  if (showQuickActionPopup) {
    QuickActionPopupDialog(
      availableSingleItems = availableSingleItems,
      initialIsCreateMealTab = popupInitialIsMealTab,
      preselectedDragPair = popupPreselectedPair,
      targetCalories = targetCalories,
      targetProtein = targetProtein,
      targetCarbs = targetCarbs,
      targetFats = targetFats,
      mealCount = mealCount,
      haptics = haptics,
      numberFormat = numberFormat,
      onDismiss = {
        showQuickActionPopup = false
        popupPreselectedPair = null
      },
      onSubmit = { loggedItem, shouldSave ->
        onLogNutritionItem(loggedItem, shouldSave)
        showQuickActionPopup = false
        popupPreselectedPair = null
      },
    )
  }
}

/**
 * Precision 2.0-Second Hold-to-Overlap & Combine Gesture Modifier:
 * - Short tap (< 2.0s without scrolling): triggers `onShortTap`
 * - Scroll movement before 2.0s: cancels hold cleanly so scrolling is never blocked
 * - Hold steady for 2.0s (2000ms): arms overlap-fusion mode so the user can drag the Single Item
 *   directly onto any other Single Item to overlap & combine them!
 */
private fun Modifier.twoSecondHoldToOverlapGesture(
  item: SavedQuickNutritionItem,
  savedSingles: List<SavedQuickNutritionItem>,
  singleItemBounds: Map<String, Rect>,
  coroutineScope: kotlinx.coroutines.CoroutineScope,
  haptics: BioSyncHapticController,
  onHoldProgress: (String?, Float) -> Unit,
  onDragArmed: (String) -> Unit,
  onDragMove: (Float, Float, String?) -> Unit,
  onDragFinish: (SavedQuickNutritionItem, SavedQuickNutritionItem?) -> Unit,
  onShortTap: () -> Unit,
): Modifier = this.pointerInput(item.id, savedSingles) {
  val touchSlopPx = viewConfiguration.touchSlop
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var isArmedAfterTwoSec = false
    var isCancelledBeforeArm = false
    var totalPreArmMoveX = 0f
    var totalPreArmMoveY = 0f

    val holdJob: Job = coroutineScope.launch {
      val startMs = System.currentTimeMillis()
      val holdDurationMs = 2000L
      onHoldProgress(item.id, 0.02f)
      var midTickSent = false
      while (true) {
        delay(40L)
        if (isCancelledBeforeArm) break
        val elapsed = System.currentTimeMillis() - startMs
        val frac = (elapsed.toFloat() / holdDurationMs.toFloat()).coerceIn(0f, 1f)
        onHoldProgress(item.id, frac)
        if (!midTickSent && frac >= 0.5f) {
          midTickSent = true
          haptics.tick()
        }
        if (elapsed >= holdDurationMs) {
          isArmedAfterTwoSec = true
          haptics.confirm()
          onDragArmed(item.id)
          break
        }
      }
    }

    var dragDx = 0f
    var dragDy = 0f
    var currentHitTargetId: String? = null
    var releasedUp = false

    while (true) {
      val event = awaitPointerEvent()
      val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
      if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed) {
        releasedUp = true
        break
      }
      val delta = change.positionChange()
      if (!isArmedAfterTwoSec) {
        totalPreArmMoveX += delta.x
        totalPreArmMoveY += delta.y
        if (hypot(totalPreArmMoveX, totalPreArmMoveY) > touchSlopPx * 1.35f) {
          isCancelledBeforeArm = true
          holdJob.cancel()
          onHoldProgress(null, 0f)
          break
        }
      } else {
        change.consume()
        dragDx += delta.x
        dragDy += delta.y
        val startRect = singleItemBounds[item.id]
        if (startRect != null) {
          val currentCenter = Offset(
            x = startRect.center.x + dragDx,
            y = startRect.center.y + dragDy,
          )
          val hitEntry = singleItemBounds.entries.firstOrNull { (otherId, rect) ->
            otherId != item.id && rect.inflate(10f).contains(currentCenter)
          }
          if (hitEntry?.key != currentHitTargetId && hitEntry?.key != null) {
            haptics.selection()
          }
          currentHitTargetId = hitEntry?.key
        }
        onDragMove(dragDx, dragDy, currentHitTargetId)
      }
    }

    holdJob.cancel()
    if (isArmedAfterTwoSec) {
      val targetItem = savedSingles.firstOrNull { it.id == currentHitTargetId }
      onDragFinish(item, targetItem)
    } else {
      onHoldProgress(null, 0f)
      if (releasedUp && !isCancelledBeforeArm) {
        onShortTap()
      }
    }
  }
}

@Composable
private fun QuickActionPlusActuatorButton(
  compact: Boolean = false,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .width(if (compact) 72.dp else 78.dp)
      .height(if (compact) 44.dp else 68.dp)
      .clip(RoundedCornerShape(11.dp))
      .background(PureWhite)
      .border(1.dp, PureWhite, RoundedCornerShape(11.dp))
      .clickable(role = Role.Button, onClick = onClick)
      .testTag("quick_actions_plus_button"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text(
        text = "+",
        color = ObsidianBlack,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = if (compact) 16.sp else 20.sp,
      )
      Text(
        text = "ADD",
        color = ObsidianBlack,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.5.sp,
      )
    }
  }
}

/**
 * Bespoke Solid-Obsidian Hardware Card for a Saved Single Item or Combined/Custom Meal:
 * - ZERO reflected/transparent background color wash (pure opaque `#0C0C11` -> `#07070A` surface)
 * - Sculpted Left Anodized Color Blade (solid bar for Single Items; multi-segment color prism for Combined Meals)
 * - Crystal-clear 4-Pillar Pure-White Macro Telemetry Chamber (`KCAL | PRO | CARB | FAT`)
 * - Solid Component Hardware Chips with individual color dots for combined meals
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedQuickActionItemCard(
  item: SavedQuickNutritionItem,
  isHoldingThis: Boolean,
  holdProgress: Float,
  isDraggingThis: Boolean,
  isHoveredTarget: Boolean,
  isArmedInFusionSlot: Boolean,
  numberFormat: NumberFormat,
  onTap: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = remember(item.colorArgb) {
    QuickItemColorAlchemy.argbToColor(item.colorArgb)
  }
  val mixedColors = remember(item.mixedFromColorArgbs) {
    if (item.mixedFromColorArgbs.size >= 2) {
      item.mixedFromColorArgbs.map { QuickItemColorAlchemy.argbToColor(it) }
    } else {
      emptyList()
    }
  }
  val hasMixedAlchemy = mixedColors.size >= 2
  val highlightActive = isDraggingThis || isHoveredTarget || isArmedInFusionSlot || isHoldingThis

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .drawWithCache {
        val cornerPx = 12.dp.toPx()
        val bladeWidthPx = 5.5.dp.toPx()
        // Pure opaque Carbon-Obsidian background — ZERO transparent colored wash!
        val solidSurfaceBrush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF13131A),
            Color(0xFF0A0A0E),
          ),
        )
        val structuralBorderColor = when {
          isHoveredTarget || isDraggingThis || isArmedInFusionSlot -> PureWhite
          isHoldingThis -> PureWhite.copy(alpha = 0.85f)
          else -> Color(0xFF2B2B36)
        }
        onDrawBehind {
          // 1. Solid Opaque Obsidian Card Chassis
          drawRoundRect(
            brush = solidSurfaceBrush,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
          )

          // 2. Left Machined Anodized Color Blade (Segmented solid blocks if Combined Meal, solid if Single Item)
          if (hasMixedAlchemy) {
            val segCount = mixedColors.size
            val segHeight = size.height / segCount.toFloat()
            mixedColors.forEachIndexed { idx, segColor ->
              drawRect(
                color = segColor,
                topLeft = Offset(0f, idx * segHeight),
                size = Size(bladeWidthPx, segHeight + 1f),
              )
            }
          } else {
            drawRect(
              color = primaryColor,
              topLeft = Offset.Zero,
              size = Size(bladeWidthPx, size.height),
            )
          }

          // Crisp vertical divider line separating the Color Blade from the Obsidian Chassis
          drawLine(
            color = PureWhite.copy(alpha = 0.25f),
            start = Offset(bladeWidthPx, 0f),
            end = Offset(bladeWidthPx, size.height),
            strokeWidth = 1.dp.toPx(),
          )

          // 3. Outer Crisp Structural Border
          drawRoundRect(
            color = structuralBorderColor,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = if (highlightActive) 1.5.dp.toPx() else 1.dp.toPx()),
          )
        }
      }
      .clickable(role = Role.Button, onClick = onTap)
      .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
      .testTag("saved_quick_item_${item.id}"),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // 1. Top Header Row: Solid Color Swatch/Cluster + Pure-White Title + Solid High-Contrast Badge + Delete
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Precision Solid Color Swatch (Overlapping Solid Color Nodes for Combined Meal)
        if (hasMixedAlchemy) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-3).dp),
          ) {
            mixedColors.take(3).forEach { c ->
              Box(
                modifier = Modifier
                  .size(11.dp)
                  .clip(CircleShape)
                  .background(c)
                  .border(1.dp, PureWhite, CircleShape),
              )
            }
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.5.dp))
                .background(primaryColor)
                .border(1.dp, PureWhite, RoundedCornerShape(3.5.dp)),
            )
          }
        } else {
          Box(
            modifier = Modifier
              .size(11.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(primaryColor)
              .border(1.dp, PureWhite, RoundedCornerShape(3.dp)),
          )
        }

        Text(
          text = item.name,
          color = PureWhite,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 13.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Solid High-Contrast Status Pill (PureWhite text on solid #16161F — 100% legible!)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF171720))
            .border(1.dp, Color(0xFF323242), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier = Modifier
              .size(5.5.dp)
              .clip(CircleShape)
              .background(primaryColor),
          )
          Text(
            text = when {
              isArmedInFusionSlot -> "FUSE READY"
              hasMixedAlchemy -> "MIXED MEAL"
              item.isMeal -> "MEAL"
              else -> "ITEM"
            },
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 7.6.sp,
            letterSpacing = 0.3.sp,
          )
        }

        // Tactile Crimson Delete Button
        Box(
          modifier = Modifier
            .size(23.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SolidDangerRed)
            .border(1.dp, SolidDangerRedBorder, RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onDelete)
            .testTag("delete_saved_quick_item_${item.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "×",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            lineHeight = 13.sp,
          )
        }
      }
    }

    // 2. High-Contrast 4-Pillar Macro Telemetry Chamber (Every Value in Bold PureWhite!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF060609))
        .border(1.dp, Color(0xFF22222C), RoundedCornerShape(8.dp))
        .padding(vertical = 6.5.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // Pillar 1: CALORIES
      Column(
        modifier = Modifier.weight(1.25f),
        horizontalAlignment = Alignment.Start,
      ) {
        Text(
          text = "ENERGY",
          color = Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 7.2.sp,
          letterSpacing = 0.4.sp,
        )
        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = numberFormat.format(item.caloriesKcal),
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.8.sp,
            maxLines = 1,
          )
          Text(
            text = "kcal",
            color = Color(0xFFE5DEC9),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.2.sp,
            modifier = Modifier.padding(bottom = 0.5.dp),
          )
        }
      }

      Box(
        modifier = Modifier
          .width(1.dp)
          .height(20.dp)
          .background(Color(0xFF22222C)),
      )

      // Pillar 2: PROTEIN
      Column(
        modifier = Modifier.weight(0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "PRO",
          color = ProteinStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 7.2.sp,
          letterSpacing = 0.4.sp,
        )
        Text(
          text = "${item.proteinGrams}g",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.2.sp,
          maxLines = 1,
        )
      }

      Box(
        modifier = Modifier
          .width(1.dp)
          .height(20.dp)
          .background(Color(0xFF22222C)),
      )

      // Pillar 3: CARBS
      Column(
        modifier = Modifier.weight(0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "CARB",
          color = CarbsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 7.2.sp,
          letterSpacing = 0.4.sp,
        )
        Text(
          text = "${item.carbsGrams}g",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.2.sp,
          maxLines = 1,
        )
      }

      Box(
        modifier = Modifier
          .width(1.dp)
          .height(20.dp)
          .background(Color(0xFF22222C)),
      )

      // Pillar 4: FATS
      Column(
        modifier = Modifier.weight(0.92f),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "FAT",
          color = FatsStart,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 7.2.sp,
          letterSpacing = 0.4.sp,
        )
        Text(
          text = "${item.fatsGrams}g",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 11.2.sp,
          maxLines = 1,
        )
      }
    }

    // 3. Component Breakdown Chips + Solid Color-Mix Alchemy Strip (For Combined Meals)
    if (item.componentNames.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        item.componentNames.forEachIndexed { idx, compName ->
          val compColor = if (mixedColors.isNotEmpty()) {
            mixedColors[idx % mixedColors.size]
          } else {
            primaryColor
          }
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF121218))
              .border(1.dp, Color(0xFF2A2A36), RoundedCornerShape(5.dp))
              .padding(horizontal = 6.5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(
              modifier = Modifier
                .size(6.5.dp)
                .clip(CircleShape)
                .background(compColor)
                .border(0.7.dp, PureWhite, CircleShape),
            )
            Text(
              text = compName,
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 8.6.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (hasMixedAlchemy) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(5.dp))
              .background(Color(0xFF171720))
              .border(1.dp, PureWhite.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
              .padding(horizontal = 6.5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "➔",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.4.sp,
            )
            Box(
              modifier = Modifier
                .size(7.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(primaryColor)
                .border(0.8.dp, PureWhite, RoundedCornerShape(2.dp)),
            )
            Text(
              text = "BLENDED",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 8.0.sp,
            )
          }
        }
      }
    }

    // 4. Live 2.0-Second Hold Progress Bar (Shown when user holds a Single Item to overlap)
    if (isHoldingThis || isDraggingThis) {
      val holdBarFill = if (isDraggingThis) 1f else holdProgress.coerceIn(0.06f, 1f)
      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isDraggingThis) "OVERLAP READY" else "HOLD 2.0S TO OVERLAP",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 7.6.sp,
          )
          Text(
            text = "${(holdBarFill * 100).roundToInt()}%",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 7.6.sp,
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(Color(0xFF050507)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(holdBarFill)
              .height(4.dp)
              .clip(CircleShape)
              .background(PureWhite),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionPopupDialog(
  availableSingleItems: List<SavedQuickNutritionItem>,
  initialIsCreateMealTab: Boolean,
  preselectedDragPair: Pair<SavedQuickNutritionItem, SavedQuickNutritionItem>?,
  targetCalories: Int,
  targetProtein: Int,
  targetCarbs: Int,
  targetFats: Int,
  mealCount: Int,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  onDismiss: () -> Unit,
  onSubmit: (SavedQuickNutritionItem, Boolean) -> Unit,
) {
  val warmTitanium = Color(0xFFE5DEC9)
  var isCreateMealTab by remember(initialIsCreateMealTab) {
    mutableStateOf(initialIsCreateMealTab)
  }

  var itemNameText by remember(preselectedDragPair) {
    mutableStateOf(
      if (preselectedDragPair != null) {
        "${preselectedDragPair.first.name} + ${preselectedDragPair.second.name}".take(36)
      } else {
        ""
      },
    )
  }
  var rawCaloriesText by remember(preselectedDragPair) {
    mutableStateOf(
      if (preselectedDragPair != null) {
        (preselectedDragPair.first.caloriesKcal + preselectedDragPair.second.caloriesKcal).toString()
      } else {
        ""
      },
    )
  }
  var rawProteinText by remember(preselectedDragPair) {
    mutableStateOf(
      if (preselectedDragPair != null) {
        (preselectedDragPair.first.proteinGrams + preselectedDragPair.second.proteinGrams).toString()
      } else {
        ""
      },
    )
  }
  var rawCarbsText by remember(preselectedDragPair) {
    mutableStateOf(
      if (preselectedDragPair != null) {
        (preselectedDragPair.first.carbsGrams + preselectedDragPair.second.carbsGrams).toString()
      } else {
        ""
      },
    )
  }
  var rawFatsText by remember(preselectedDragPair) {
    mutableStateOf(
      if (preselectedDragPair != null) {
        (preselectedDragPair.first.fatsGrams + preselectedDragPair.second.fatsGrams).toString()
      } else {
        ""
      },
    )
  }

  var caloriesDontKnow by remember { mutableStateOf(false) }
  var proteinDontKnow by remember { mutableStateOf(false) }
  var carbsDontKnow by remember { mutableStateOf(false) }
  var fatsDontKnow by remember { mutableStateOf(false) }

  // Default OFF unless opened via 2s Hold-to-Overlap Meal Fusion
  var saveToQuickActions by remember(preselectedDragPair) {
    mutableStateOf(preselectedDragPair != null)
  }

  // Search filter inside Create Meal when user has many Single Items
  var singlePickerSearchQuery by remember { mutableStateOf("") }

  val selectedSingleItemCounts = remember(preselectedDragPair) {
    mutableStateMapOf<String, Int>().apply {
      if (preselectedDragPair != null) {
        put(preselectedDragPair.first.id, 1)
        put(preselectedDragPair.second.id, (get(preselectedDragPair.second.id) ?: 0) + 1)
      }
    }
  }

  // Prune any selected single item that is no longer in availableSingleItems
  LaunchedEffect(availableSingleItems) {
    val validIds = availableSingleItems.map { it.id }.toSet()
    val staleKeys = selectedSingleItemCounts.keys.filter { it !in validIds }
    staleKeys.forEach { selectedSingleItemCounts.remove(it) }
  }

  // Color Selection & Color Mixing Alchemy State
  val selectedComponentColors: List<Long> = remember(
    isCreateMealTab,
    selectedSingleItemCounts.toMap(),
    availableSingleItems,
  ) {
    if (!isCreateMealTab) {
      emptyList()
    } else {
      val list = mutableListOf<Long>()
      for ((id, qty) in selectedSingleItemCounts) {
        if (qty > 0) {
          val found = availableSingleItems.firstOrNull { it.id == id }
          if (found != null) {
            list.add(found.colorArgb)
          }
        }
      }
      list
    }
  }

  val autoBlendedMealColorArgb: Long = remember(selectedComponentColors) {
    if (selectedComponentColors.size >= 2) {
      QuickItemColorAlchemy.mixItemColors(selectedComponentColors)
    } else if (selectedComponentColors.size == 1) {
      selectedComponentColors.first()
    } else {
      0xFFA855F7L
    }
  }

  var useMixedColorForMeal by remember(preselectedDragPair) {
    mutableStateOf(true)
  }
  var selectedColorArgb by remember(preselectedDragPair) {
    mutableLongStateOf(
      if (preselectedDragPair != null) {
        QuickItemColorAlchemy.mixItemColors(
          listOf(preselectedDragPair.first.colorArgb, preselectedDragPair.second.colorArgb),
        )
      } else {
        QuickItemColorAlchemy.presets.first().argb
      },
    )
  }
  var showCustomColorStudio by remember { mutableStateOf(false) }
  var customHue by remember { mutableFloatStateOf(280f) }
  var customSat by remember { mutableFloatStateOf(0.82f) }
  var customVal by remember { mutableFloatStateOf(0.95f) }
  var customHexInput by remember { mutableStateOf("") }

  // Keep meal color synced with mixed component colors when `useMixedColorForMeal` is active
  LaunchedEffect(autoBlendedMealColorArgb, useMixedColorForMeal, isCreateMealTab, selectedComponentColors.size) {
    if (isCreateMealTab && useMixedColorForMeal && selectedComponentColors.isNotEmpty()) {
      selectedColorArgb = autoBlendedMealColorArgb
    }
  }

  val effectiveColorArgb = if (isCreateMealTab && useMixedColorForMeal && selectedComponentColors.size >= 2) {
    autoBlendedMealColorArgb
  } else {
    selectedColorArgb
  }
  val effectiveAccentColor = QuickItemColorAlchemy.argbToColor(effectiveColorArgb)

  fun syncMealFieldsFromSelectedSingles() {
    if (selectedSingleItemCounts.isEmpty()) return
    var sumCal = 0
    var sumP = 0
    var sumC = 0
    var sumF = 0
    val chosenNames = mutableListOf<String>()
    for ((id, qty) in selectedSingleItemCounts) {
      val found = availableSingleItems.firstOrNull { it.id == id }
      if (found != null && qty > 0) {
        sumCal += found.caloriesKcal * qty
        sumP += found.proteinGrams * qty
        sumC += found.carbsGrams * qty
        sumF += found.fatsGrams * qty
        chosenNames.add(if (qty > 1) "${found.name} ×$qty" else found.name)
      }
    }
    rawCaloriesText = sumCal.toString()
    rawProteinText = sumP.toString()
    rawCarbsText = sumC.toString()
    rawFatsText = sumF.toString()
    caloriesDontKnow = false
    proteinDontKnow = false
    carbsDontKnow = false
    fatsDontKnow = false
    if (chosenNames.isNotEmpty()) {
      itemNameText = chosenNames.joinToString(" + ").take(36)
    }
  }

  val resolvedDraft = remember(
    rawCaloriesText,
    rawProteinText,
    rawCarbsText,
    rawFatsText,
    caloriesDontKnow,
    proteinDontKnow,
    carbsDontKnow,
    fatsDontKnow,
    targetCalories,
    targetProtein,
    targetCarbs,
    targetFats,
    mealCount,
    isCreateMealTab,
  ) {
    FairNutrientEstimator.resolveDraft(
      rawCaloriesText = rawCaloriesText,
      rawProteinText = rawProteinText,
      rawCarbsText = rawCarbsText,
      rawFatsText = rawFatsText,
      caloriesDontKnow = caloriesDontKnow,
      proteinDontKnow = proteinDontKnow,
      carbsDontKnow = carbsDontKnow,
      fatsDontKnow = fatsDontKnow,
      targetCalories = targetCalories,
      targetProtein = targetProtein,
      targetCarbs = targetCarbs,
      targetFats = targetFats,
      mealCount = mealCount,
      isMealMode = isCreateMealTab,
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      val scrollState = rememberScrollState()
      Column(
        modifier = Modifier
          .widthIn(max = 500.dp)
          .fillMaxWidth()
          .floating3DProtocolCard(
            cornerRadiusDp = 18.dp,
            topRimTint = PureWhite,
          )
          .verticalScroll(scrollState)
          .padding(16.dp)
          .testTag("quick_action_popup_modal"),
      ) {
        // Modal Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 11.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            DashboardSyncedHeaderDot(
              startColor = PureWhite,
              endColor = Zinc300,
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
              text = if (preselectedDragPair != null) "2.0S FUSION MEAL COMPOSER" else "QUICK ACTION COMPOSER",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.2.sp,
              letterSpacing = 1.0.sp,
            )
          }

          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(SolidDangerRed)
              .border(1.dp, SolidDangerRedBorder, RoundedCornerShape(8.dp))
              .clickable(role = Role.Button, onClick = onDismiss)
              .testTag("quick_action_modal_close"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "×",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 15.sp,
              lineHeight = 15.sp,
            )
          }
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = Zinc800.copy(alpha = 0.85f),
        )

        Spacer(modifier = Modifier.height(13.dp))

        // Segmented Switcher: SINGLE ITEM vs CREATE MEAL
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBlack)
            .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
            .padding(3.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val singleSelected = !isCreateMealTab
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(7.dp))
              .background(if (singleSelected) PureWhite else Color.Transparent)
              .clickable(role = Role.Tab) {
                if (isCreateMealTab) {
                  haptics.selection()
                  isCreateMealTab = false
                }
              }
              .testTag("tab_single_item"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "SINGLE ITEM",
              color = if (singleSelected) ObsidianBlack else Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.8.sp,
              letterSpacing = 0.8.sp,
            )
          }

          val mealSelected = isCreateMealTab
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(7.dp))
              .background(if (mealSelected) PureWhite else Color.Transparent)
              .clickable(role = Role.Tab) {
                if (!isCreateMealTab) {
                  haptics.selection()
                  isCreateMealTab = true
                }
              }
              .testTag("tab_create_meal"),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = "CREATE MEAL",
              color = if (mealSelected) ObsidianBlack else Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 10.8.sp,
              letterSpacing = 0.8.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // In CREATE MEAL mode: Active Saved Single-Items Multi-Selector (Zero deleted items!)
        if (isCreateMealTab) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .floating3DInnerTile(
                cornerRadiusDp = 12.dp,
                accentColor = PureWhite,
              )
              .padding(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .width(3.dp)
                    .height(11.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PureWhite),
                )
                Text(
                  text = "COMBINE SAVED SINGLE ITEMS (${availableSingleItems.size})",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.6.sp,
                  letterSpacing = 0.6.sp,
                )
              }

              if (selectedSingleItemCounts.isNotEmpty()) {
                Text(
                  text = "CLEAR SELECTION",
                  color = warmTitanium,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 8.8.sp,
                  modifier = Modifier.clickable {
                    haptics.tick()
                    selectedSingleItemCounts.clear()
                  },
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (availableSingleItems.isEmpty()) {
                "No saved Single Items in your Quick Actions Vault — enter Meal values below or save a Single Item first."
              } else {
                "Select active Single Items from your Vault to combine their macros & blend their colors."
              },
              color = Zinc300,
              fontFamily = InterFontFamily,
              fontSize = 11.sp,
            )

            if (availableSingleItems.isNotEmpty()) {
              if (availableSingleItems.size >= 5) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBlack)
                    .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                  contentAlignment = Alignment.CenterStart,
                ) {
                  if (singlePickerSearchQuery.isEmpty()) {
                    Text(
                      text = "Search ${availableSingleItems.size} saved single items...",
                      color = Zinc400,
                      fontFamily = InterFontFamily,
                      fontSize = 11.5.sp,
                    )
                  }
                  BasicTextField(
                    value = singlePickerSearchQuery,
                    onValueChange = { singlePickerSearchQuery = it.take(30) },
                    singleLine = true,
                    textStyle = TextStyle(
                      color = PureWhite,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 12.sp,
                    ),
                    cursorBrush = SolidColor(PureWhite),
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              val filteredPickerSingles = remember(availableSingleItems, singlePickerSearchQuery) {
                val q = singlePickerSearchQuery.trim().lowercase(Locale.US)
                if (q.isEmpty()) {
                  availableSingleItems
                } else {
                  availableSingleItems.filter { it.name.lowercase(Locale.US).contains(q) }
                }
              }

              Spacer(modifier = Modifier.height(9.dp))
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                filteredPickerSingles.chunked(2).forEach { rowItems ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    rowItems.forEach { single ->
                      val count = selectedSingleItemCounts[single.id] ?: 0
                      val isSelected = count > 0
                      val singleColor = QuickItemColorAlchemy.argbToColor(single.colorArgb)

                      // Solid Obsidian Selector Chip (Zero transparent background wash!)
                      Row(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(9.dp))
                          .background(if (isSelected) Color(0xFF171721) else Color(0xFF09090D))
                          .border(
                            width = if (isSelected) 1.4.dp else 1.dp,
                            color = if (isSelected) PureWhite else Color(0xFF282834),
                            shape = RoundedCornerShape(9.dp),
                          )
                          .clickable {
                            haptics.selection()
                            if (count == 0) {
                              selectedSingleItemCounts[single.id] = 1
                            } else {
                              selectedSingleItemCounts.remove(single.id)
                            }
                            useMixedColorForMeal = true
                            syncMealFieldsFromSelectedSingles()
                          }
                          .padding(horizontal = 9.dp, vertical = 8.dp)
                          .testTag("meal_select_single_${single.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Row(
                          modifier = Modifier.weight(1f),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                          Box(
                            modifier = Modifier
                              .width(4.dp)
                              .height(22.dp)
                              .clip(RoundedCornerShape(2.dp))
                              .background(singleColor),
                          )
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = single.name,
                              color = PureWhite,
                              fontFamily = InterFontFamily,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 12.sp,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                              text = "${numberFormat.format(single.caloriesKcal)} kcal • ${single.proteinGrams}P",
                              color = Zinc200,
                              fontFamily = JetBrainsMonoFontFamily,
                              fontWeight = FontWeight.Bold,
                              fontSize = 9.2.sp,
                              maxLines = 1,
                            )
                          }
                        }

                        if (isSelected) {
                          Box(
                            modifier = Modifier
                              .clip(RoundedCornerShape(5.dp))
                              .background(PureWhite)
                              .clickable {
                                haptics.step()
                                selectedSingleItemCounts[single.id] = (count + 1).coerceAtMost(9)
                                syncMealFieldsFromSelectedSingles()
                              }
                              .padding(horizontal = 6.dp, vertical = 2.5.dp),
                          ) {
                            Text(
                              text = "×$count",
                              color = ObsidianBlack,
                              fontFamily = JetBrainsMonoFontFamily,
                              fontWeight = FontWeight.ExtraBold,
                              fontSize = 9.5.sp,
                            )
                          }
                        } else {
                          Text(
                            text = "+ ADD",
                            color = Zinc300,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.4.sp,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Inner Floating Card for Name + 2x2 Nutrient Boxes (Pure neutral rim — no colored wash!)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .floating3DInnerTile(
              cornerRadiusDp = 12.dp,
              accentColor = PureWhite,
            )
            .padding(12.dp),
        ) {
          Text(
            text = if (isCreateMealTab) "MEAL NAME" else "ITEM NAME",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
          )
          Spacer(modifier = Modifier.height(5.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 44.dp)
              .clip(RoundedCornerShape(9.dp))
              .background(ObsidianBlack)
              .border(1.dp, Color(0xFF323240), RoundedCornerShape(9.dp))
              .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (itemNameText.isEmpty()) {
              Text(
                text = if (isCreateMealTab) "Enter meal name (e.g. Post-Workout Bowl)..." else "Enter item name (e.g. Greek Yogurt, Eggs)...",
                color = Zinc400,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
              )
            }
            BasicTextField(
              value = itemNameText,
              onValueChange = { itemNameText = it.take(44) },
              singleLine = true,
              textStyle = TextStyle(
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
              ),
              cursorBrush = SolidColor(PureWhite),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_action_name_input"),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // 2x2 Bento Grid of Nutrient Input Boxes
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ObsidianNutrientInputBox(
              label = "Calories",
              unit = "kcal",
              rawText = rawCaloriesText,
              onRawTextChange = {
                rawCaloriesText = it
                if (it.isNotEmpty()) caloriesDontKnow = false
              },
              isDontKnowActive = caloriesDontKnow,
              onToggleDontKnow = {
                haptics.selection()
                caloriesDontKnow = !caloriesDontKnow
                if (caloriesDontKnow) rawCaloriesText = ""
              },
              fairResolvedValue = resolvedDraft.caloriesKcal,
              accentColor = CaloriesStart,
              testTagPrefix = "input_calories",
              modifier = Modifier.weight(1f),
            )

            ObsidianNutrientInputBox(
              label = "Protein",
              unit = "g",
              rawText = rawProteinText,
              onRawTextChange = {
                rawProteinText = it
                if (it.isNotEmpty()) proteinDontKnow = false
              },
              isDontKnowActive = proteinDontKnow,
              onToggleDontKnow = {
                haptics.selection()
                proteinDontKnow = !proteinDontKnow
                if (proteinDontKnow) rawProteinText = ""
              },
              fairResolvedValue = resolvedDraft.proteinGrams,
              accentColor = ProteinStart,
              testTagPrefix = "input_protein",
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ObsidianNutrientInputBox(
              label = "Carbs",
              unit = "g",
              rawText = rawCarbsText,
              onRawTextChange = {
                rawCarbsText = it
                if (it.isNotEmpty()) carbsDontKnow = false
              },
              isDontKnowActive = carbsDontKnow,
              onToggleDontKnow = {
                haptics.selection()
                carbsDontKnow = !carbsDontKnow
                if (carbsDontKnow) rawCarbsText = ""
              },
              fairResolvedValue = resolvedDraft.carbsGrams,
              accentColor = CarbsStart,
              testTagPrefix = "input_carbs",
              modifier = Modifier.weight(1f),
            )

            ObsidianNutrientInputBox(
              label = "Fats",
              unit = "g",
              rawText = rawFatsText,
              onRawTextChange = {
                rawFatsText = it
                if (it.isNotEmpty()) fatsDontKnow = false
              },
              isDontKnowActive = fatsDontKnow,
              onToggleDontKnow = {
                haptics.selection()
                fatsDontKnow = !fatsDontKnow
                if (fatsDontKnow) rawFatsText = ""
              },
              fairResolvedValue = resolvedDraft.fatsGrams,
              accentColor = FatsStart,
              testTagPrefix = "input_fats",
              modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =========================================================================
        // SIGNATURE COLOR SELECTION + COLOR MIXING ALCHEMY + CUSTOM COLOR STUDIO
        // =========================================================================
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .floating3DInnerTile(
              cornerRadiusDp = 12.dp,
              accentColor = PureWhite,
            )
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(14.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(effectiveAccentColor)
                  .border(1.dp, PureWhite, RoundedCornerShape(4.dp)),
              )
              Column {
                Text(
                  text = if (isCreateMealTab && selectedComponentColors.size >= 2) {
                    "MEAL COLOR ALCHEMY & CUSTOM PALETTE"
                  } else {
                    "QUICK-BAR COLOR & CUSTOM STUDIO"
                  },
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.6.sp,
                  letterSpacing = 0.6.sp,
                )
                Text(
                  text = QuickItemColorAlchemy.argbToHexCode(effectiveColorArgb),
                  color = Zinc200,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.4.sp,
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(if (showCustomColorStudio) PureWhite else Color(0xFF15151D))
                .border(
                  width = 1.dp,
                  color = if (showCustomColorStudio) PureWhite else Zinc700,
                  shape = RoundedCornerShape(7.dp),
                )
                .clickable(role = Role.Button) {
                  haptics.selection()
                  showCustomColorStudio = !showCustomColorStudio
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("toggle_custom_color_studio"),
            ) {
              Text(
                text = if (showCustomColorStudio) "CLOSE STUDIO" else "+ CUSTOM COLOR",
                color = if (showCustomColorStudio) ObsidianBlack else PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.2.sp,
              )
            }
          }

          // Live Color Mixing Visualization when 2+ Single Items are combined into a Meal
          if (isCreateMealTab && selectedComponentColors.size >= 2) {
            val mixedResultColor = QuickItemColorAlchemy.argbToColor(autoBlendedMealColorArgb)
            val sourceColors = selectedComponentColors.map { QuickItemColorAlchemy.argbToColor(it) }

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(ObsidianBlack)
                .border(1.dp, Color(0xFF2D2D3A), RoundedCornerShape(9.dp))
                .clickable {
                  haptics.selection()
                  useMixedColorForMeal = true
                  selectedColorArgb = autoBlendedMealColorArgb
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
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
                  sourceColors.take(4).forEachIndexed { idx, col ->
                    if (idx > 0) {
                      Text(
                        text = "+",
                        color = PureWhite,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                      )
                    }
                    Box(
                      modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(col)
                        .border(1.dp, PureWhite, RoundedCornerShape(3.dp)),
                    )
                  }
                  Text(
                    text = "➔",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                  )
                  Box(
                    modifier = Modifier
                      .size(15.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(mixedResultColor)
                      .border(1.dp, PureWhite, RoundedCornerShape(4.dp)),
                  )
                  Text(
                    text = "MIXED (${QuickItemColorAlchemy.argbToHexCode(autoBlendedMealColorArgb)})",
                    color = PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 8.6.sp,
                  )
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (useMixedColorForMeal) PureWhite else Zinc900)
                    .padding(horizontal = 6.5.dp, vertical = 2.5.dp),
                ) {
                  Text(
                    text = if (useMixedColorForMeal) "ACTIVE MIX" else "USE MIX",
                    color = if (useMixedColorForMeal) ObsidianBlack else PureWhite,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 7.6.sp,
                  )
                }
              }

              // Solid Segmented Color-Mix Bar
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(CircleShape)
                  .border(0.6.dp, PureWhite.copy(alpha = 0.4f), CircleShape),
              ) {
                sourceColors.forEach { segCol ->
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .background(segCol),
                  )
                }
                Box(
                  modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .background(mixedResultColor),
                )
              }
            }
          }

          // 12 Curated Signature Swatches (2 rows x 6 swatches)
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            QuickItemColorAlchemy.presets.chunked(6).forEach { rowPresets ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                rowPresets.forEach { preset ->
                  val swatchColor = QuickItemColorAlchemy.argbToColor(preset.argb)
                  val isSelectedSwatch = (!useMixedColorForMeal || selectedComponentColors.size < 2) &&
                    effectiveColorArgb == preset.argb
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(28.dp)
                      .clip(RoundedCornerShape(7.dp))
                      .background(swatchColor)
                      .border(
                        width = if (isSelectedSwatch) 2.dp else 0.8.dp,
                        color = if (isSelectedSwatch) PureWhite else ObsidianBlack.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(7.dp),
                      )
                      .clickable {
                        haptics.selection()
                        useMixedColorForMeal = false
                        selectedColorArgb = preset.argb
                        customHexInput = QuickItemColorAlchemy.argbToHexCode(preset.argb)
                      },
                    contentAlignment = Alignment.Center,
                  ) {
                    if (isSelectedSwatch) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(ObsidianBlack)
                          .border(1.dp, PureWhite, CircleShape),
                      )
                    }
                  }
                }
              }
            }
          }

          // Expandable Custom Color Studio (360° Hue Spectrum + Saturation/Brightness + Hex Input)
          AnimatedVisibility(
            visible = showCustomColorStudio,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ObsidianBlack)
                .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
                .padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "CUSTOM COLOR SPECTRUM • TAP OR DRAG HUE BAR",
                color = Zinc300,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 8.4.sp,
                letterSpacing = 0.5.sp,
              )

              // 360° Rainbow Hue Spectrum Bar
              val rainbowColors = remember {
                listOf(
                  Color(0xFFFF0040),
                  Color(0xFFFF8000),
                  Color(0xFFFFEA00),
                  Color(0xFF10B981),
                  Color(0xFF06B6D4),
                  Color(0xFF6366F1),
                  Color(0xFFA855F7),
                  Color(0xFFEC4899),
                  Color(0xFFFF0040),
                )
              }
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(24.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Brush.horizontalGradient(rainbowColors))
                  .border(1.dp, PureWhite.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                  .pointerInput(Unit) {
                    detectTapGestures { offset ->
                      val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                      customHue = frac * 360f
                      useMixedColorForMeal = false
                      selectedColorArgb = QuickItemColorAlchemy.hsvToArgbLong(customHue, customSat, customVal)
                      customHexInput = QuickItemColorAlchemy.argbToHexCode(selectedColorArgb)
                      haptics.tick()
                    }
                  }
                  .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                      change.consume()
                      val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                      customHue = frac * 360f
                      useMixedColorForMeal = false
                      selectedColorArgb = QuickItemColorAlchemy.hsvToArgbLong(customHue, customSat, customVal)
                      customHexInput = QuickItemColorAlchemy.argbToHexCode(selectedColorArgb)
                    }
                  },
              )

              // Saturation & Brightness Quick Presets + Custom Hex Code Field
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(effectiveAccentColor)
                    .border(1.2.dp, PureWhite, RoundedCornerShape(8.dp)),
                )

                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF09090D))
                    .border(1.dp, Zinc700, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                  contentAlignment = Alignment.CenterStart,
                ) {
                  if (customHexInput.isEmpty()) {
                    Text(
                      text = QuickItemColorAlchemy.argbToHexCode(effectiveColorArgb) + " (Type Hex e.g. #FF6B4A)",
                      color = Zinc500,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 10.2.sp,
                    )
                  }
                  BasicTextField(
                    value = customHexInput,
                    onValueChange = { raw ->
                      val filtered = raw.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == '#' }.take(7)
                      customHexInput = filtered
                      val parsed = QuickItemColorAlchemy.parseHexToArgbOrNull(filtered)
                      if (parsed != null) {
                        useMixedColorForMeal = false
                        selectedColorArgb = parsed
                      }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                      color = PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.5.sp,
                    ),
                    cursorBrush = SolidColor(PureWhite),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("custom_color_hex_input"),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Save to Quick Actions" Toggle Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .floating3DInnerTile(
              cornerRadiusDp = 11.dp,
              accentColor = if (saveToQuickActions) effectiveAccentColor else Zinc700,
              isHighlighted = saveToQuickActions,
            )
            .clickable(role = Role.Switch) {
              haptics.selection()
              saveToQuickActions = !saveToQuickActions
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("save_to_quick_actions_toggle"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isCreateMealTab) "SAVE MEAL TO QUICK ACTIONS VAULT" else "SAVE ITEM TO QUICK ACTIONS VAULT",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.2.sp,
              letterSpacing = 0.6.sp,
            )
            Text(
              text = "Pins to your categorized Meal / Single-Item vault with its signature color",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontSize = 11.sp,
            )
          }

          Box(
            modifier = Modifier
              .width(44.dp)
              .height(24.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (saveToQuickActions) effectiveAccentColor else ObsidianBlack)
              .border(1.dp, if (saveToQuickActions) PureWhite else Zinc700, RoundedCornerShape(12.dp))
              .padding(3.dp),
            contentAlignment = if (saveToQuickActions) Alignment.CenterEnd else Alignment.CenterStart,
          ) {
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (saveToQuickActions) ObsidianBlack else Zinc400),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary Action Button
        val hasValidNutrients = resolvedDraft.caloriesKcal > 0 ||
          resolvedDraft.proteinGrams > 0 ||
          resolvedDraft.carbsGrams > 0 ||
          resolvedDraft.fatsGrams > 0
        val effectiveCalories = if (resolvedDraft.caloriesKcal > 0) {
          resolvedDraft.caloriesKcal
        } else {
          4 * resolvedDraft.proteinGrams + 4 * resolvedDraft.carbsGrams + 9 * resolvedDraft.fatsGrams
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
              brush = if (hasValidNutrients) {
                Brush.horizontalGradient(colors = listOf(PureWhite, warmTitanium))
              } else {
                Brush.horizontalGradient(colors = listOf(Zinc900, Zinc900))
              },
            )
            .border(1.dp, if (hasValidNutrients) PureWhite else Zinc700, RoundedCornerShape(11.dp))
            .clickable(enabled = hasValidNutrients, role = Role.Button) {
              if (hasValidNutrients) {
                haptics.confirm()
                val defaultTitle = if (isCreateMealTab) "Custom Meal" else "Single Item"
                val finalTitle = itemNameText.trim().ifEmpty { defaultTitle }
                val componentNamesList = if (isCreateMealTab && selectedSingleItemCounts.isNotEmpty()) {
                  selectedSingleItemCounts.mapNotNull { (id, qty) ->
                    val found = availableSingleItems.firstOrNull { it.id == id }
                    found?.let { if (qty > 1) "${it.name} ×$qty" else it.name }
                  }
                } else {
                  emptyList()
                }
                val newItem = SavedQuickNutritionItem(
                  name = finalTitle,
                  isMeal = isCreateMealTab,
                  caloriesKcal = effectiveCalories,
                  proteinGrams = resolvedDraft.proteinGrams,
                  carbsGrams = resolvedDraft.carbsGrams,
                  fatsGrams = resolvedDraft.fatsGrams,
                  componentNames = componentNamesList,
                  isSavedToQuickActions = saveToQuickActions,
                  colorArgb = effectiveColorArgb,
                  mixedFromColorArgbs = if (isCreateMealTab && selectedComponentColors.size >= 2) {
                    selectedComponentColors
                  } else {
                    emptyList()
                  },
                )
                onSubmit(newItem, saveToQuickActions)
              }
            }
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .testTag("quick_action_log_submit_button"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(effectiveAccentColor)
                  .border(0.8.dp, ObsidianBlack, CircleShape),
              )
              Text(
                text = if (isCreateMealTab) "Log Meal" else "Log Single Item",
                color = if (hasValidNutrients) ObsidianBlack else Zinc500,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                letterSpacing = (-0.2).sp,
              )
            }

            Text(
              text = "${numberFormat.format(effectiveCalories)} kcal • ${resolvedDraft.proteinGrams}P ${resolvedDraft.carbsGrams}C ${resolvedDraft.fatsGrams}F",
              color = if (hasValidNutrients) ObsidianBlack else Zinc500,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.5.sp,
            )
          }
        }
      }
    }
  }
}

/**
 * Sleek Obsidian Nutrient Input Box with a `3.5.dp x 10.dp` vertical accent pill,
 * plain colored unit text, and an inline dynamic `? FAIR` ("Don't Know") option inside the box.
 */
@Composable
private fun ObsidianNutrientInputBox(
  label: String,
  unit: String,
  rawText: String,
  onRawTextChange: (String) -> Unit,
  isDontKnowActive: Boolean,
  onToggleDontKnow: () -> Unit,
  fairResolvedValue: Int,
  accentColor: Color,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
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
            .height(10.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(accentColor),
        )
        Text(
          text = label,
          color = Zinc200,
          fontFamily = InterFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
        )
      }
      Text(
        text = unit.uppercase(Locale.US),
        color = accentColor,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 9.2.sp,
      )
    }

    Spacer(modifier = Modifier.height(5.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 44.dp)
        .clip(RoundedCornerShape(9.dp))
        .background(ObsidianBlack)
        .border(
          width = 1.dp,
          color = if (isDontKnowActive) accentColor else Zinc800,
          shape = RoundedCornerShape(9.dp),
        )
        .padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (isDontKnowActive) {
          Column {
            Text(
              text = "~$fairResolvedValue $unit",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            )
            Text(
              text = "FAIR EST.",
              color = accentColor,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 8.sp,
            )
          }
        } else if (rawText.isEmpty()) {
          Text(
            text = "0",
            color = Zinc600,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
          )
        }

        BasicTextField(
          value = rawText,
          onValueChange = { raw ->
            onRawTextChange(raw.filter { it.isDigit() }.take(4))
          },
          singleLine = true,
          textStyle = TextStyle(
            color = if (isDontKnowActive) Color.Transparent else PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
          ),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          cursorBrush = SolidColor(PureWhite),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_field"),
        )
      }

      // Dynamic Solid "? FAIR" ("Don't Know") pill inside the input box
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(if (isDontKnowActive) PureWhite else Zinc900)
          .border(
            width = 1.dp,
            color = if (isDontKnowActive) PureWhite else Zinc700,
            shape = RoundedCornerShape(6.dp),
          )
          .clickable(role = Role.Button, onClick = onToggleDontKnow)
          .padding(horizontal = 7.dp, vertical = 4.dp)
          .testTag("${testTagPrefix}_dont_know_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (isDontKnowActive) "AUTO" else "? FAIR",
          color = if (isDontKnowActive) ObsidianBlack else PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 8.8.sp,
        )
      }
    }
  }
}

// ============================================================================
// 2. FLOATING DEDICATED HYDRATION (H2O) COMMAND CARD (SEPARATE SECTION)
// ============================================================================

@Composable
fun DashboardHydrationCommandCard(
  loggedWaterMl: Int,
  targetWaterMl: Int,
  streakThresholdPercent: Int,
  borderFlashProgress: Float = 0f,
  onLogWaterMl: (Int) -> Unit,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  modifier: Modifier = Modifier,
) {
  val safeTarget = targetWaterMl.coerceAtLeast(500)
  val rawFraction = loggedWaterMl.toFloat() / safeTarget.toFloat()
  val barFraction = rawFraction.coerceIn(0f, 1f)
  val pctAchieved = (rawFraction * 100f).roundToInt()
  val isStreakThresholdMet = pctAchieved >= streakThresholdPercent
  val remainingMl = (safeTarget - loggedWaterMl).coerceAtLeast(0)
  val glassesDone = loggedWaterMl / 250f
  val glassesTarget = safeTarget / 250f

  var showCustomInput by remember { mutableStateOf(false) }
  var customMlText by remember { mutableStateOf("") }

  val animatedBarFill by animateFloatAsState(
    targetValue = barFraction,
    animationSpec = tween(durationMillis = 480, easing = SmoothOutEasing),
    label = "hydration_bar_fill",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "hydration_wave")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "liquid_wave_phase",
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = HydrationStart,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_hydration_section"),
  ) {
    // 1. Clean Ecosystem Header Row (Matches Step 04 — NO bulky colored badge block!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = HydrationStart,
          endColor = ProteinStart,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "HYDRATION ENGINE • H₂O",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Text(
        text = "H₂O • $pctAchieved%",
        color = HydrationStart,
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

    // 2. Floating Inner Hydration Telemetry Module
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .floating3DInnerTile(
          cornerRadiusDp = 12.dp,
          accentColor = HydrationStart,
          isHighlighted = isStreakThresholdMet,
        )
        .padding(13.dp),
    ) {
      // Dual-Telemetry Split Readout (No clunky right-side box!)
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
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(HydrationStart, HydrationMid))),
            )
            Text(
              text = "CELLULAR FLUID INTAKE",
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
              text = numberFormat.format(loggedWaterMl),
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 31.sp,
              lineHeight = 31.sp,
              letterSpacing = (-1.0).sp,
              modifier = Modifier.testTag("hydration_current_ml_text"),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "/ ${numberFormat.format(safeTarget)} ml",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 12.2.sp,
              modifier = Modifier.padding(bottom = 3.dp),
            )
          }
        }

        // Sleek Vertical Telemetry Divider
        Box(
          modifier = Modifier
            .padding(horizontal = 12.dp)
            .width(1.dp)
            .height(38.dp)
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Transparent,
                  Zinc700,
                  Color.Transparent,
                ),
              ),
            ),
        )

        // Unboxed Right Telemetry Readout (Glasses Equivalent)
        Column(
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
                .background(HydrationStart),
            )
            Text(
              text = "GLASSES (250ML)",
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 8.8.sp,
              letterSpacing = 0.5.sp,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = String.format(Locale.US, "%.1f", glassesDone),
              color = HydrationStart,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 17.5.sp,
              lineHeight = 17.5.sp,
            )
            Text(
              text = String.format(Locale.US, " / %.1f", glassesTarget),
              color = Zinc400,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
              modifier = Modifier.padding(bottom = 1.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(11.dp))

      // Animated Liquid-Wave Spectrum Gauge Bar with Luminous Reticle
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(9.5.dp)
          .drawWithCache {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(h / 2f, h / 2f)
            val auraPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(
                  115,
                  (HydrationStart.red * 255).roundToInt(),
                  (HydrationStart.green * 255).roundToInt(),
                  (HydrationStart.blue * 255).roundToInt(),
                )
                maskFilter = BlurMaskFilter(safeDashboardBlur(h * 1.15f), BlurMaskFilter.Blur.NORMAL)
              }
            }
            val reticlePaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(
                  180,
                  (HydrationStart.red * 255).roundToInt(),
                  (HydrationStart.green * 255).roundToInt(),
                  (HydrationStart.blue * 255).roundToInt(),
                )
                maskFilter = BlurMaskFilter(safeDashboardBlur(h * 0.95f), BlurMaskFilter.Blur.NORMAL)
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
                style = Stroke(width = 1.dp.toPx()),
              )

              val visualFill = max(animatedBarFill, 0.028f)
              val fillW = (w * visualFill).coerceIn(h, w)

              drawIntoCanvas { canvas ->
                canvas.drawRoundRect(0f, 0f, fillW, h, h / 2f, h / 2f, auraPaint)
              }

              drawRoundRect(
                brush = Brush.horizontalGradient(
                  colors = listOf(HydrationEnd, HydrationMid, HydrationStart, ProteinStart, PureWhite),
                  startX = 0f,
                  endX = fillW.coerceAtLeast(h * 2f),
                ),
                topLeft = Offset.Zero,
                size = Size(fillW, h),
                cornerRadius = corner,
              )

              if (animatedBarFill > 0.01f) {
                val wavePath = Path()
                val steps = 20
                val stepX = fillW / steps.toFloat()
                wavePath.moveTo(0f, h * 0.5f)
                for (i in 0..steps) {
                  val x = i * stepX
                  val y = (h * 0.5f) + sin(wavePhase + i * 0.5f) * (h * 0.18f)
                  wavePath.lineTo(x, y)
                }
                drawPath(
                  path = wavePath,
                  color = PureWhite.copy(alpha = 0.32f),
                  style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                )
              }

              val gateX = (w * (streakThresholdPercent / 100f)).coerceIn(6f, w - 6f)
              drawLine(
                color = PureWhite.copy(alpha = 0.80f),
                start = Offset(gateX, 0f),
                end = Offset(gateX, h),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
              )

              val tipX = (fillW - h * 0.45f).coerceIn(h * 0.5f, w - h * 0.5f)
              val tipCenter = Offset(tipX, h / 2f)
              drawIntoCanvas { canvas ->
                canvas.drawCircle(tipCenter, h * 0.72f, reticlePaint)
              }
              drawCircle(
                color = PureWhite,
                radius = (h * 0.28f).coerceAtLeast(2.2f),
                center = tipCenter,
              )
            }
          }
          .testTag("hydration_progress_bar"),
      )

      Spacer(modifier = Modifier.height(7.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (remainingMl > 0) {
            "${numberFormat.format(remainingMl)} ml remaining to daily hydration goal"
          } else {
            "Daily hydration target achieved"
          },
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = 11.sp,
        )
        Text(
          text = "Streak Gate ≥$streakThresholdPercent%",
          color = if (isStreakThresholdMet) HydrationStart else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.5.sp,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. 5-Column Floating Quick-Log Row: +50ml, +100ml, +250ml, +500ml, Custom
    val presets = remember { listOf(50, 100, 250, 500) }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(7.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      presets.forEach { ml ->
        Column(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .floating3DInnerTile(
              cornerRadiusDp = 10.dp,
              accentColor = HydrationStart,
            )
            .clickable(role = Role.Button) {
              haptics.confirm()
              onLogWaterMl(ml)
            }
            .padding(vertical = 8.dp)
            .testTag("hydration_quick_${ml}ml"),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = "+$ml",
            color = PureWhite,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          )
          Text(
            text = "ml",
            color = HydrationStart,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.2.sp,
          )
        }
      }

      val customActive = showCustomInput
      Column(
        modifier = Modifier
          .weight(1.08f)
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(if (customActive) PureWhite else Zinc900)
          .border(
            width = 1.dp,
            color = if (customActive) PureWhite else Zinc700,
            shape = RoundedCornerShape(10.dp),
          )
          .clickable(role = Role.Button) {
            haptics.selection()
            showCustomInput = !showCustomInput
          }
          .padding(vertical = 8.dp, horizontal = 4.dp)
          .testTag("hydration_custom_toggle_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "CUSTOM",
          color = if (customActive) ObsidianBlack else PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 0.5.sp,
        )
        Text(
          text = if (customActive) "CLOSE" else "+ ML",
          color = if (customActive) ObsidianBlack else Zinc400,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 9.sp,
        )
      }
    }

    AnimatedVisibility(
      visible = showCustomInput,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
          .floating3DInnerTile(
            cornerRadiusDp = 10.dp,
            accentColor = HydrationStart,
          )
          .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 40.dp),
          contentAlignment = Alignment.CenterStart,
        ) {
          if (customMlText.isEmpty()) {
            Text(
              text = "Custom amount in ml (e.g. 350)...",
              color = Zinc500,
              fontFamily = InterFontFamily,
              fontSize = 12.5.sp,
            )
          }
          BasicTextField(
            value = customMlText,
            onValueChange = { raw ->
              customMlText = raw.filter { it.isDigit() }.take(4)
            },
            singleLine = true,
            textStyle = TextStyle(
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(PureWhite),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("hydration_custom_ml_input"),
          )
        }

        val parsedCustomMl = customMlText.toIntOrNull() ?: 0
        val canLogCustom = parsedCustomMl in 1..5000
        Box(
          modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (canLogCustom) PureWhite else ObsidianBlack)
            .border(1.dp, if (canLogCustom) PureWhite else Zinc700, RoundedCornerShape(8.dp))
            .clickable(enabled = canLogCustom, role = Role.Button) {
              if (canLogCustom) {
                haptics.confirm()
                onLogWaterMl(parsedCustomMl)
                customMlText = ""
                showCustomInput = false
              }
            }
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .testTag("hydration_custom_log_submit"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "LOG ML",
            color = if (canLogCustom) ObsidianBlack else Zinc500,
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
// 3. FLOATING TODAY'S TIMELINE SECTION (AM/PM LOGS + DELETE + 3-SECOND UNDO)
// ============================================================================

@Composable
fun DashboardTodayTimelineSection(
  timelineEntries: List<DashboardTimelineEntry>,
  pendingDeletedItem: PendingDeletedTimelineItem?,
  borderFlashProgress: Float = 0f,
  onDeleteEntry: (DashboardTimelineEntry) -> Unit,
  onUndoDelete: () -> Unit,
  haptics: BioSyncHapticController,
  numberFormat: NumberFormat,
  modifier: Modifier = Modifier,
) {
  var undoRemainingMs by remember(pendingDeletedItem?.entry?.id) {
    mutableStateOf(3000L)
  }

  LaunchedEffect(pendingDeletedItem) {
    val pending = pendingDeletedItem ?: return@LaunchedEffect
    while (true) {
      val rem = (pending.expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
      undoRemainingMs = rem
      if (rem <= 0L) break
      delay(60L)
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .floating3DProtocolCard(
        cornerRadiusDp = 18.dp,
        topRimTint = PureWhite,
        borderFlashProgress = borderFlashProgress,
      )
      .padding(16.dp)
      .testTag("dashboard_todays_timeline_section"),
  ) {
    // Default top latest logs limit (6 logs, right in the 5-7 sweet spot)
    val defaultVisibleLogsLimit = 6
    var showAllTimelineLogs by remember { mutableStateOf(false) }

    // timelineEntries is already prepended newest-first as logs are added
    val orderedEntries = timelineEntries
    val hasMoreTimelineLogs = orderedEntries.size > defaultVisibleLogsLimit
    val displayedTimelineEntries = remember(orderedEntries, showAllTimelineLogs) {
      if (showAllTimelineLogs || orderedEntries.size <= defaultVisibleLogsLimit) {
        orderedEntries
      } else {
        orderedEntries.take(defaultVisibleLogsLimit)
      }
    }

    // 1. Clean Ecosystem Header Row (NO bulky colored block!)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 11.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        DashboardSyncedHeaderDot(
          startColor = PureWhite,
          endColor = Zinc300,
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
          text = "TODAY'S TIMELINE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Text(
        text = if (hasMoreTimelineLogs && !showAllTimelineLogs) {
          "LATEST $defaultVisibleLogsLimit / ${orderedEntries.size} LOGGED"
        } else {
          "${orderedEntries.size} LOGGED"
        },
        color = if (orderedEntries.isNotEmpty()) StepsStart else Zinc400,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.8.sp,
        letterSpacing = 0.5.sp,
      )
    }

    HorizontalDivider(
      thickness = 1.dp,
      color = Zinc800.copy(alpha = 0.85f),
    )

    Spacer(modifier = Modifier.height(13.dp))

    // 2. 3-Second High-Contrast Monochrome Undo Banner
    AnimatedVisibility(
      visible = pendingDeletedItem != null && undoRemainingMs > 0L,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      val pending = pendingDeletedItem
      if (pending != null) {
        val secondsLeft = (undoRemainingMs / 1000f).coerceIn(0f, 3f)
        val fractionLeft = (undoRemainingMs / 3000f).coerceIn(0f, 1f)

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .floating3DInnerTile(
              cornerRadiusDp = 11.dp,
              accentColor = PureWhite,
              isHighlighted = true,
            )
            .padding(12.dp)
            .testTag("timeline_undo_banner"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Removed \"${pending.entry.title}\"",
                color = PureWhite,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = String.format(Locale.US, "Restore entry within %.1fs", secondsLeft),
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
              )
            }

            Box(
              modifier = Modifier
                .heightIn(min = 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PureWhite)
                .clickable(role = Role.Button) {
                  haptics.confirm()
                  onUndoDelete()
                }
                .padding(horizontal = 13.dp, vertical = 7.dp)
                .testTag("timeline_undo_button"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "UNDO (${secondsLeft.roundToInt().coerceAtLeast(1)}s)",
                color = ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .clip(RoundedCornerShape(1.5.dp))
              .background(ObsidianBlack),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(fractionLeft)
                .height(3.dp)
                .background(PureWhite),
            )
          }
        }
      }
    }

    if (orderedEntries.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .floating3DInnerTile(
            cornerRadiusDp = 12.dp,
            accentColor = Zinc700,
          )
          .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "NO ENTRIES LOGGED TODAY",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 10.5.sp,
          letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Every Single Item, Meal, and H₂O intake you log will appear here with its AM/PM timestamp.",
          color = Zinc400,
          fontFamily = InterFontFamily,
          fontSize = 11.2.sp,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        displayedTimelineEntries.forEach { entry ->
          val entryAccent = when {
            entry.colorArgb != 0L -> QuickItemColorAlchemy.argbToColor(entry.colorArgb)
            entry.type == DashboardEntryType.HYDRATION -> HydrationStart
            entry.type == DashboardEntryType.MEAL -> StepsStart
            else -> CaloriesStart
          }
          val entryMixedColors = if (entry.mixedFromColorArgbs.size >= 2) {
            entry.mixedFromColorArgbs.map { QuickItemColorAlchemy.argbToColor(it) }
          } else {
            emptyList()
          }
          val compactTypeTag = when {
            entry.type == DashboardEntryType.HYDRATION -> "H₂O"
            entry.type == DashboardEntryType.MEAL && entryMixedColors.size >= 2 -> "MIXED MEAL"
            entry.type == DashboardEntryType.MEAL -> "MEAL"
            else -> "ITEM"
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .floating3DInnerTile(
                cornerRadiusDp = 11.dp,
                accentColor = PureWhite,
              )
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("timeline_entry_${entry.id}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Left: Chronological Node + Vertical Accent Spine + Title/Telemetry
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              // Solid Vertical Timeline Node Indicator (Multi-segment solid color bar for combined meals)
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(entryAccent)
                    .border(0.8.dp, PureWhite, RoundedCornerShape(2.5.dp)),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                  modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                      brush = if (entryMixedColors.size >= 2) {
                        Brush.verticalGradient(colors = entryMixedColors + listOf(entryAccent))
                      } else {
                        Brush.verticalGradient(
                          colors = listOf(entryAccent, entryAccent),
                        )
                      },
                    ),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                  Text(
                    text = entry.title,
                    color = PureWhite,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )

                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.5.dp))
                      .background(Color(0xFF171720))
                      .border(1.dp, Color(0xFF30303E), RoundedCornerShape(4.5.dp))
                      .padding(horizontal = 5.5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(entryAccent),
                    )
                    Text(
                      text = compactTypeTag,
                      color = PureWhite,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 7.8.sp,
                      letterSpacing = 0.4.sp,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(3.dp))

                val subLine = if (entry.type == DashboardEntryType.HYDRATION) {
                  "+${numberFormat.format(entry.hydrationMl)} ml cellular hydration"
                } else {
                  val macroSummary = "+${numberFormat.format(entry.caloriesKcal)} kcal • ${entry.proteinGrams}g P • ${entry.carbsGrams}g C • ${entry.fatsGrams}g F"
                  if (entry.subtitle.isNotBlank()) "$macroSummary (${entry.subtitle})" else macroSummary
                }
                Text(
                  text = subLine,
                  color = Zinc200,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 10.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Clean Monospace Timestamp + Solid Red Delete Button
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
              Text(
                text = entry.formattedTimeAmPm,
                color = Zinc400,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.8.sp,
                letterSpacing = 0.3.sp,
              )

              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(SolidDangerRed)
                  .border(1.dp, SolidDangerRedBorder, RoundedCornerShape(8.dp))
                  .clickable(role = Role.Button) {
                    haptics.warning()
                    onDeleteEntry(entry)
                  }
                  .testTag("timeline_delete_${entry.id}"),
                contentAlignment = Alignment.Center,
              ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                  val w = size.width
                  val h = size.height
                  val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                  drawLine(
                    color = PureWhite,
                    start = Offset(w * 0.18f, h * 0.24f),
                    end = Offset(w * 0.82f, h * 0.24f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                  )
                  drawLine(
                    color = PureWhite,
                    start = Offset(w * 0.38f, h * 0.14f),
                    end = Offset(w * 0.62f, h * 0.14f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                  )
                  val body = Path().apply {
                    moveTo(w * 0.26f, h * 0.28f)
                    lineTo(w * 0.32f, h * 0.86f)
                    lineTo(w * 0.68f, h * 0.86f)
                    lineTo(w * 0.74f, h * 0.28f)
                  }
                  drawPath(path = body, color = PureWhite, style = stroke)
                }
              }
            }
          }
        }

        // View More / Show Less Bar when more than 6 logs exist
        if (hasMoreTimelineLogs) {
          val remainingCount = orderedEntries.size - defaultVisibleLogsLimit
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF0B0B10))
              .border(1.dp, Color(0xFF282834), RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                haptics.selection()
                showAllTimelineLogs = !showAllTimelineLogs
              }
              .padding(horizontal = 12.dp, vertical = 9.5.dp)
              .testTag("timeline_view_more_toggle"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (showAllTimelineLogs) {
                "SHOWING ALL ${orderedEntries.size} LOGS TODAY"
              } else {
                "SHOWING LATEST $defaultVisibleLogsLimit OF ${orderedEntries.size} LOGS"
              },
              color = Zinc300,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 8.8.sp,
              letterSpacing = 0.4.sp,
            )

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (showAllTimelineLogs) Color(0xFF171720) else PureWhite)
                .border(
                  width = 1.dp,
                  color = if (showAllTimelineLogs) Color(0xFF343444) else PureWhite,
                  shape = RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
              Text(
                text = if (showAllTimelineLogs) {
                  "SHOW LESS ▲"
                } else {
                  "VIEW MORE (+$remainingCount) ▼"
                },
                color = if (showAllTimelineLogs) PureWhite else ObsidianBlack,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.2.sp,
                letterSpacing = 0.3.sp,
              )
            }
          }
        }
      }
    }
  }
}
