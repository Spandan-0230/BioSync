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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

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

// ============================================================================
// 1. FLOATING QUICK ACTIONS (+) BAR WITH SAVED ITEMS BESIDE IT + POPUP MODAL
// ============================================================================

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
  var showQuickActionPopup by remember { mutableStateOf(false) }
  var popupInitialIsMealTab by remember { mutableStateOf(false) }
  var popupPreselectedPair by remember {
    mutableStateOf<Pair<SavedQuickNutritionItem, SavedQuickNutritionItem>?>(null)
  }

  // Drag-and-drop state for combining two Single Items beside the "+" button
  var draggingItemId by remember { mutableStateOf<String?>(null) }
  var dragOffsetX by remember { mutableFloatStateOf(0f) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }
  var hoveredTargetItemId by remember { mutableStateOf<String?>(null) }
  val singleItemBounds = remember { mutableStateMapOf<String, Rect>() }

  // Pool of all created Single Items for Create Meal & Drag-to-Meal
  val availableSingleItems = remember(allCreatedSingleItems, savedQuickItems) {
    val map = LinkedHashMap<String, SavedQuickNutritionItem>()
    for (item in savedQuickItems) {
      if (!item.isMeal) map[item.id] = item
    }
    for (item in allCreatedSingleItems) {
      if (!item.isMeal && item.id !in map) map[item.id] = item
    }
    map.values.toList()
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
          text = "QUICK ACTIONS ENGINE",
          color = PureWhite,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.2.sp,
          letterSpacing = 1.0.sp,
        )
      }

      Text(
        text = if (savedQuickItems.count { !it.isMeal } >= 2) {
          "DRAG ITEM → MEAL • ${savedQuickItems.size} SAVED"
        } else {
          "${savedQuickItems.size} SAVED"
        },
        color = if (savedQuickItems.isNotEmpty()) ProteinStart else Zinc400,
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

    // 2. Main Row: [ + ADD Button ] on the left, and [ User Saved Quick Actions ] right beside it!
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Primary Floating "+" Quick Action Trigger Button (opens the Popup Modal)
      Column(
        modifier = Modifier
          .width(76.dp)
          .height(68.dp)
          .drawWithCache {
            val cPx = 12.dp.toPx()
            val glowPaint = Paint().apply {
              isAntiAlias = true
              asFrameworkPaint().apply {
                color = android.graphics.Color.argb(65, 255, 255, 255)
                maskFilter = BlurMaskFilter(safeDashboardBlur(12.dp.toPx()), BlurMaskFilter.Blur.NORMAL)
              }
            }
            onDrawBehind {
              drawIntoCanvas { canvas ->
                canvas.drawRoundRect(
                  2.dp.toPx(),
                  4.dp.toPx(),
                  size.width - 2.dp.toPx(),
                  size.height + 4.dp.toPx(),
                  cPx,
                  cPx,
                  glowPaint,
                )
              }
            }
          }
          .clip(RoundedCornerShape(12.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(PureWhite, Zinc200),
            ),
          )
          .border(1.dp, PureWhite, RoundedCornerShape(12.dp))
          .clickable(role = Role.Button) {
            haptics.confirm()
            popupPreselectedPair = null
            popupInitialIsMealTab = false
            showQuickActionPopup = true
          }
          .testTag("quick_actions_plus_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "+",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 24.sp,
          lineHeight = 24.sp,
        )
        Text(
          text = "ADD",
          color = ObsidianBlack,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 9.5.sp,
          letterSpacing = 0.8.sp,
        )
      }

      // BESIDE IT: User's Saved Quick Actions (Meals & Single Items with Drag-and-Drop)
      if (savedQuickItems.isEmpty()) {
        Row(
          modifier = Modifier
            .weight(1f)
            .height(68.dp)
            .floating3DInnerTile(
              cornerRadiusDp = 12.dp,
              accentColor = Zinc600,
            )
            .clickable(role = Role.Button) {
              haptics.selection()
              popupPreselectedPair = null
              popupInitialIsMealTab = false
              showQuickActionPopup = true
            }
            .padding(horizontal = 13.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(10.dp)
                  .clip(RoundedCornerShape(1.5.dp))
                  .background(PureWhite),
              )
              Text(
                text = "SAVED QUICK ACTIONS SHELF",
                color = PureWhite,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.7.sp,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Tap + to log a Single Item or Meal. Toggle Save to pin it right here.",
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontSize = 10.8.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      } else {
        val horizontalScroll = rememberScrollState()
        Row(
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(horizontalScroll)
            .testTag("saved_quick_actions_shelf"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          savedQuickItems.forEach { item ->
            val isDraggingThis = draggingItemId == item.id
            val isHoveredTarget = hoveredTargetItemId == item.id &&
              draggingItemId != null &&
              draggingItemId != item.id
            val itemAccent = if (item.isMeal) ProteinStart else CaloriesStart

            Row(
              modifier = Modifier
                .zIndex(if (isDraggingThis) 10f else 1f)
                .height(68.dp)
                .offset {
                  if (isDraggingThis) {
                    IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                  } else {
                    IntOffset.Zero
                  }
                }
                .onGloballyPositioned { coords ->
                  if (!item.isMeal && !isDraggingThis) {
                    singleItemBounds[item.id] = coords.boundsInRoot()
                  }
                }
                .pointerInput(item.id, item.isMeal, savedQuickItems) {
                  if (!item.isMeal) {
                    detectDragGestures(
                      onDragStart = {
                        haptics.selection()
                        draggingItemId = item.id
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        hoveredTargetItemId = null
                      },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                        dragOffsetY += dragAmount.y
                        val startRect = singleItemBounds[item.id]
                        if (startRect != null) {
                          val currentCenter = Offset(
                            x = startRect.center.x + dragOffsetX,
                            y = startRect.center.y + dragOffsetY,
                          )
                          val hitEntry = singleItemBounds.entries.firstOrNull { (otherId, rect) ->
                            otherId != item.id && rect.contains(currentCenter)
                          }
                          hoveredTargetItemId = hitEntry?.key
                        }
                      },
                      onDragEnd = {
                        val targetId = hoveredTargetItemId
                        val sourceItem = savedQuickItems.firstOrNull { it.id == item.id && !it.isMeal }
                        val targetItem = savedQuickItems.firstOrNull { it.id == targetId && !it.isMeal }
                        draggingItemId = null
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        hoveredTargetItemId = null
                        if (sourceItem != null && targetItem != null) {
                          haptics.confirm()
                          popupPreselectedPair = sourceItem to targetItem
                          popupInitialIsMealTab = true
                          showQuickActionPopup = true
                        }
                      },
                      onDragCancel = {
                        draggingItemId = null
                        dragOffsetX = 0f
                        dragOffsetY = 0f
                        hoveredTargetItemId = null
                      },
                    )
                  }
                }
                .floating3DInnerTile(
                  cornerRadiusDp = 12.dp,
                  accentColor = if (isHoveredTarget || isDraggingThis) PureWhite else itemAccent,
                  isHighlighted = isHoveredTarget || isDraggingThis,
                )
                .clickable(role = Role.Button) {
                  haptics.confirm()
                  onQuickTapSavedItem(item)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("saved_quick_item_${item.id}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Column(verticalArrangement = Arrangement.Center) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier = Modifier
                      .width(3.dp)
                      .height(11.dp)
                      .clip(RoundedCornerShape(1.5.dp))
                      .background(itemAccent),
                  )
                  Text(
                    text = item.name,
                    color = PureWhite,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = if (item.isMeal) "MEAL" else "ITEM",
                    color = itemAccent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.8.sp,
                    letterSpacing = 0.5.sp,
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "${item.caloriesKcal} kcal • ${item.proteinGrams}P ${item.carbsGrams}C ${item.fatsGrams}F",
                  color = Zinc300,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 10.sp,
                )
              }

              Box(
                modifier = Modifier
                  .size(25.dp)
                  .clip(RoundedCornerShape(7.dp))
                  .background(SolidDangerRed)
                  .border(1.dp, SolidDangerRedBorder, RoundedCornerShape(7.dp))
                  .clickable(role = Role.Button) {
                    haptics.tick()
                    onDeleteSavedQuickItem(item.id)
                  }
                  .testTag("delete_saved_quick_item_${item.id}"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "×",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 14.sp,
                  lineHeight = 14.sp,
                )
              }
            }
          }
        }
      }
    }
  }

  // POPUP MODAL WHEN USER CLICKS "+" OR DRAGS ONE SINGLE ITEM ONTO ANOTHER
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

  // Default OFF as requested
  var saveToQuickActions by remember { mutableStateOf(false) }

  val selectedSingleItemCounts = remember(preselectedDragPair) {
    mutableStateMapOf<String, Int>().apply {
      if (preselectedDragPair != null) {
        put(preselectedDragPair.first.id, 1)
        put(preselectedDragPair.second.id, (get(preselectedDragPair.second.id) ?: 0) + 1)
      }
    }
  }

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
    if (itemNameText.isBlank() && chosenNames.isNotEmpty()) {
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
              text = "QUICK ACTION COMPOSER",
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

        // Segmented Switcher: SINGLE ITEM vs CREATE MEAL (Solid PureWhite vs ObsidianBlack)
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

        // In CREATE MEAL mode: Show all created Single Items for multi-select combination
        if (isCreateMealTab) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .floating3DInnerTile(
                cornerRadiusDp = 12.dp,
                accentColor = ProteinStart,
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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Box(
                  modifier = Modifier
                    .width(3.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(ProteinStart),
                )
                Text(
                  text = "SELECT CREATED SINGLE ITEMS (OPTIONAL)",
                  color = PureWhite,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.8.sp,
                  letterSpacing = 0.7.sp,
                )
              }

              if (selectedSingleItemCounts.isNotEmpty()) {
                Text(
                  text = "CLEAR",
                  color = ProteinStart,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.5.sp,
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
                "No Single Items created yet — enter Meal values directly below."
              } else {
                "Select Single Items to sum their macros automatically, or leave unselected to enter Meal values directly."
              },
              color = Zinc400,
              fontFamily = InterFontFamily,
              fontSize = 11.sp,
            )

            if (availableSingleItems.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
              ) {
                availableSingleItems.forEach { single ->
                  val count = selectedSingleItemCounts[single.id] ?: 0
                  val isSelected = count > 0
                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) PureWhite else Zinc950)
                      .border(
                        width = 1.dp,
                        color = if (isSelected) PureWhite else Zinc700,
                        shape = RoundedCornerShape(8.dp),
                      )
                      .clickable {
                        haptics.selection()
                        if (count == 0) {
                          selectedSingleItemCounts[single.id] = 1
                        } else {
                          selectedSingleItemCounts.remove(single.id)
                        }
                        syncMealFieldsFromSelectedSingles()
                      }
                      .padding(horizontal = 9.dp, vertical = 6.dp)
                      .testTag("meal_select_single_${single.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = single.name,
                      color = if (isSelected) ObsidianBlack else PureWhite,
                      fontFamily = InterFontFamily,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 11.5.sp,
                    )
                    Text(
                      text = "${single.caloriesKcal} kcal",
                      color = if (isSelected) ObsidianBlack else Zinc400,
                      fontFamily = JetBrainsMonoFontFamily,
                      fontSize = 9.5.sp,
                    )
                    if (isSelected) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(ObsidianBlack)
                          .clickable {
                            haptics.step()
                            selectedSingleItemCounts[single.id] = (count + 1).coerceAtMost(9)
                            syncMealFieldsFromSelectedSingles()
                          }
                          .padding(horizontal = 5.dp, vertical = 1.dp),
                      ) {
                        Text(
                          text = "×$count",
                          color = PureWhite,
                          fontFamily = JetBrainsMonoFontFamily,
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.5.sp,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
        }

        // Inner Floating Card for Name + 2x2 Nutrient Boxes
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .floating3DInnerTile(
              cornerRadiusDp = 12.dp,
              accentColor = Zinc600,
            )
            .padding(12.dp),
        ) {
          Text(
            text = if (isCreateMealTab) "MEAL NAME" else "ITEM NAME",
            color = Zinc300,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
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
              .border(1.dp, Zinc800, RoundedCornerShape(9.dp))
              .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (itemNameText.isEmpty()) {
              Text(
                text = if (isCreateMealTab) "Enter meal name (e.g. Post-Workout Bowl)..." else "Enter item name (e.g. Greek Yogurt, Eggs)...",
                color = Zinc500,
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

          // 2x2 Bento Grid of Nutrient Input Boxes (Clean vertical accent bar + inline "? FAIR" toggle)
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

        // "Save to Quick Actions" Toggle Row (Default OFF)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .floating3DInnerTile(
              cornerRadiusDp = 11.dp,
              accentColor = if (saveToQuickActions) PureWhite else Zinc700,
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
              text = if (isCreateMealTab) "SAVE MEAL TO QUICK ACTIONS" else "SAVE ITEM TO QUICK ACTIONS",
              color = PureWhite,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 10.2.sp,
              letterSpacing = 0.6.sp,
            )
            Text(
              text = "Default OFF • Turn ON to pin beside the + button",
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
              .background(if (saveToQuickActions) PureWhite else ObsidianBlack)
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

        // Primary PureWhite Action Button
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
            .background(if (hasValidNutrients) PureWhite else Zinc900)
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
            Text(
              text = if (isCreateMealTab) "Log Meal" else "Log Single Item",
              color = if (hasValidNutrients) ObsidianBlack else Zinc500,
              fontFamily = InterFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.5.sp,
              letterSpacing = (-0.2).sp,
            )

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
        text = "${timelineEntries.size} LOGGED",
        color = if (timelineEntries.isNotEmpty()) StepsStart else Zinc400,
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

    if (timelineEntries.isEmpty()) {
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
        timelineEntries.forEach { entry ->
          val entryAccent = when (entry.type) {
            DashboardEntryType.HYDRATION -> HydrationStart
            DashboardEntryType.MEAL -> StepsStart
            DashboardEntryType.SINGLE_ITEM -> CaloriesStart
          }
          val compactTypeTag = when (entry.type) {
            DashboardEntryType.HYDRATION -> "H₂O"
            DashboardEntryType.MEAL -> "MEAL"
            DashboardEntryType.SINGLE_ITEM -> "ITEM"
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .floating3DInnerTile(
                cornerRadiusDp = 11.dp,
                accentColor = entryAccent,
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
              // Sleek Vertical Timeline Node Indicator
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(entryAccent),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                  modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                      Brush.verticalGradient(
                        colors = listOf(entryAccent.copy(alpha = 0.85f), entryAccent.copy(alpha = 0.15f)),
                      ),
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )

                  Text(
                    text = "• $compactTypeTag",
                    color = entryAccent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.2.sp,
                    letterSpacing = 0.5.sp,
                  )
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
                  color = Zinc300,
                  fontFamily = JetBrainsMonoFontFamily,
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
      }
    }
  }
}
