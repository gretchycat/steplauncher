package com.steplauncher.core.renderer

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

object ForgivingTouchHelper {

    // Forgiving movement slop: 48dp allowance for finger micro-wobble/jitter
    private const val FORGIVING_SLOP_DP = 48f
    private const val LONG_PRESS_TIMEOUT_MS = 400L

    /**
     * Binds a forgiving click and long-press listener to any View.
     * Allows up to 48dp of movement jitter while holding down a finger before cancelling the long press.
     */
    fun bind(
        view: View,
        onClick: (() -> Unit)? = null,
        onLongClick: ((View) -> Boolean)? = null
    ) {
        val handler = Handler(Looper.getMainLooper())
        val density = view.resources.displayMetrics.density
        val forgivingSlopPx = FORGIVING_SLOP_DP * density

        var startX = 0f
        var startY = 0f
        var isLongPressed = false
        var isCancelled = false

        val longPressRunnable = Runnable {
            isLongPressed = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onLongClick?.invoke(view)
        }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isLongPressed = false
                    isCancelled = false
                    if (onLongClick != null) {
                        handler.removeCallbacks(longPressRunnable)
                        handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isCancelled && !isLongPressed) {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                        // Only cancel if movement exceeds forgiving 48dp threshold
                        if (distance > forgivingSlopPx) {
                            isCancelled = true
                            handler.removeCallbacks(longPressRunnable)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (!isLongPressed && !isCancelled) {
                        val dx = event.x - startX
                        val dy = event.y - startY
                        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (distance <= forgivingSlopPx) {
                            v.performClick()
                            onClick?.invoke()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    isCancelled = true
                    true
                }
                else -> false
            }
        }
    }
}
