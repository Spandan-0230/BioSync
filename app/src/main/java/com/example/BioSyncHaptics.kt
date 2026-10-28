package com.example

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

@Immutable
class BioSyncHapticController(
  private val view: View,
  private val composeHaptics: HapticFeedback,
) {
  /**
   * Subtle micro-tick for keystrokes, focus shifts, and fine value adjustments.
   */
  fun tick() {
    val performed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    } else {
      view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    if (!performed) {
      composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  /**
   * Crisp stepper pulse for - / + buttons and numeric increments/decrements.
   */
  fun step() {
    val performed = view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    if (!performed) {
      composeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  /**
   * Tactile detent click for unit swaps (CM <-> INCH, KG <-> LBS), biological sex toggles, and month cells.
   */
  fun selection() {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      HapticFeedbackConstants.CONTEXT_CLICK
    } else {
      HapticFeedbackConstants.VIRTUAL_KEY
    }
    val performed = view.performHapticFeedback(constant)
    if (!performed) {
      composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  /**
   * Firm confirmation pulse for primary CTA buttons (Get Started, Continue) and modal confirmations.
   */
  fun confirm() {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      HapticFeedbackConstants.CONFIRM
    } else {
      HapticFeedbackConstants.VIRTUAL_KEY
    }
    val performed = view.performHapticFeedback(constant)
    if (!performed) {
      composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  /**
   * Distinct double/heavy warning pulse for out-of-range clinical alerts and disclaimer modals.
   */
  fun warning() {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      HapticFeedbackConstants.REJECT
    } else {
      HapticFeedbackConstants.LONG_PRESS
    }
    val performed = view.performHapticFeedback(constant)
    if (!performed) {
      composeHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }
}

@Composable
fun rememberBioSyncHaptics(): BioSyncHapticController {
  val view = LocalView.current
  val composeHaptics = LocalHapticFeedback.current
  return remember(view, composeHaptics) {
    BioSyncHapticController(view = view, composeHaptics = composeHaptics)
  }
}
