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

    // jitter vị trí — phá pattern
    private fun jx(v: Float): Float {
        val j = Config.jitterPx
        if (j <= 0f) return v
        return v + rnd.nextFloat() * j - j / 2f
    }

    // jitter thời gian — tránh timing cố định
    private fun dur(base: Long, spread: Int): Long {
        return base + rnd.nextInt(spread)
    }

    fun snapHead(x: Float, y: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val sx = jx(x)
        val sy = jx(y)
        val ey = sy - Config.snapPx
        val path = Path()
        path.moveTo(sx, sy)
        // đường cong nhẹ — trông tự nhiên hơn đường thẳng
        val midY = sy - Config.snapPx * 0.55f
        val midX = sx + (rnd.nextFloat() * 4f - 2f)
        path.quadTo(midX, midY, sx, ey)
        dispatch(path, dur(42L, 18))
    }

    fun dragStep(px: Float, py: Float, x: Float, y: Float, dy: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        var ey = y
        if (dy < 0) {
            // kéo lên → bù recoil mạnh
            ey = y + dy * (Config.pullUp - 1f)
            ey -= Config.recoilComp
        }
        val path = Path()
        path.moveTo(px, py)
        path.lineTo(x, ey)
        dispatch(path, dur(10L, 8))
    }

    fun holdAt(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x + 0.3f, y + 0.3f)
        dispatch(path, dur(10L, 6))
    }

    private fun dispatch(path: Path, durMs: Long) {
        val stroke = GestureDescription.StrokeDescription(path, 0L, durMs)
        val b = GestureDescription.Builder()
        b.addStroke(stroke)
        dispatchGesture(b.build(), null, null)
    }
}
