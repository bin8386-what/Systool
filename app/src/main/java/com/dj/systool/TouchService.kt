package com.dj.systool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs
import kotlin.random.Random

class TouchService : AccessibilityService() {
    companion object {
        @Volatile var instance: TouchService? = null
        @Volatile var gameActive = false
    }
    private val h = Handler(Looper.getMainLooper())
    private var micro: Runnable? = null

    override fun onServiceConnected() { instance = this }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val p = e.packageName?.toString() ?: return
            gameActive = p in Config.activePackages()
        }
    }

    override fun onInterrupt() {}
    override fun onDestroy() { stopMicro(); instance = null; super.onDestroy() }

    private fun jit(v: Float) = v + Random.nextFloat() * Config.jitterPx - Config.jitterPx / 2f

    fun snapHead(x: Float, y: Float) {
        if (!Config.aimlock || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val sx = jit(x); val sy = jit(y)
        val ex = jit(x); val ey = jit(y) - Config.snapPx
        val path = Path().apply {
            moveTo(sx, sy)
            quadTo((sx + ex) / 2f, sy - Config.snapPx * 0.62f, ex, ey)
        }
        dispatch(path, 55L, 95L)
    }

    fun dragAssist(sx: Float, sy: Float, dx: Float, dy: Float) {
        if (!Config.aimlock || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (dy >= 0f) return
        val ex = jit(sx + dx) - dx * Config.horizHug
        val ey = jit(sy + dy) - dy * Config.pullUp
        val path = Path().apply {
            moveTo(jit(sx), jit(sy))
            quadTo((sx + ex) / 2f, sy - abs(ey - sy) * 0.55f, ex, ey)
        }
        dispatch(path, 45L, 85L)
    }

    fun startMicro(x: Float, y: Float) {
        if (!Config.headTrack || !gameActive) return
        stopMicro()
        micro = object : Runnable {
            var n = 0
            override fun run() {
                if (n++ > 20 || !gameActive) { stopMicro(); return }
                val path = Path().apply {
                    moveTo(x, y)
                    lineTo(x + Random.nextFloat() * 1.5f - 0.75f,
                           y - Config.microAmp + Random.nextFloat() - 0.5f)
                }
                dispatch(path, 20L, 26L)
                h.postDelayed(this, Config.microMs)
            }
        }.also { h.post(it) }
    }

    fun stopMicro() { micro?.let { h.removeCallbacks(it) }; micro = null }

    private fun dispatch(path: Path, a: Long, b: Long) {
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(
                path, 0L, Random.nextLong(a, b))).build()dispatchGesture(g, null, null)
    }
}
