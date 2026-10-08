package com.dj.systool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.util.Random

class TouchService : AccessibilityService() {
    companion object {
        var instance: TouchService? = null
        var gameActive: Boolean = false
    }

    private val rnd = Random()

    override fun onServiceConnected() { instance = this }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName ?: return
        gameActive = Config.activePackages().contains(pkg.toString())
    }

    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun snapHead(x: Float, y: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val j = Config.jitterPx
        val sx = x + rnd.nextFloat() * j - j / 2f
        val sy = y + rnd.nextFloat() * j - j / 2f
        val ey = sy - Config.snapPx
        val path = Path()
        path.moveTo(sx, sy)
        path.quadTo(sx + rnd.nextFloat() * 3f - 1.5f,
            sy - Config.snapPx * 0.5f, sx, ey)
        dispatch(path, 34L + rnd.nextInt(12))
    }

    fun dragStep(px: Float, py: Float, x: Float, y: Float, dy: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        var ey = y
        if (dy < 0f) {
            ey = y + dy * (Config.pullUp - 1f)
            ey -= Config.recoilComp
        }
        val path = Path()
        path.moveTo(px, py)
        path.lineTo(x, ey)
        dispatch(path, 6L + rnd.nextInt(6))
    }

    fun holdAt(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + 0.2f, y + 0.2f)
        dispatch(path, 8L + rnd.nextInt(4))
    }

    private fun dispatch(path: Path, durMs: Long) {
        val stroke = GestureDescription.StrokeDescription(path, 0L, durMs)
        val b = GestureDescription.Builder()
        b.addStroke(stroke)
        dispatchGesture(b.build(), null, null)
    }
}
