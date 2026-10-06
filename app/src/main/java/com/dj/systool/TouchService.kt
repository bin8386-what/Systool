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
        var instance: TouchService? = null
        var gameActive: Boolean = false
    }

    private val h = Handler(Looper.getMainLooper())
    private var micro: Runnable? = null

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null) return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName
        if (pkg == null) return
        val name = pkg.toString()
        var active = false
        val list = Config.activePackages()
        for (item in list) {
            if (item == name) active = true
        }
        gameActive = active
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        stopMicro()
        instance = null
        super.onDestroy()
    }

    private fun jit(v: Float): Float {
        val jp = Config.jitterPx
        val r = Random.nextFloat() * jp
        return v + r - jp / 2
    }fun snapHead(x: Float, y: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val sx = jit(x)
        val sy = jit(y)
        val ex = jit(x)
        val ey = jit(y) - Config.snapPx
        val path = Path()
        path.moveTo(sx, sy)
        val mid = (sx + ex) / 2
        val factor = 0.62.toFloat()
        val cy = sy - Config.snapPx * factor
        path.quadTo(mid, cy, ex, ey)
        dispatch(path, 55L, 95L)
    }

    fun dragAssist(sx: Float, sy: Float, dx: Float, dy: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        if (dy >= 0) return
        val ex = jit(sx + dx) - dx * Config.horizHug
        val ey = jit(sy + dy) - dy * Config.pullUp
        val path = Path()
        path.moveTo(jit(sx), jit(sy))
        val mid = (sx + ex) / 2
        val factor = 0.55.toFloat()
        val cy = sy - abs(ey - sy) * factor
        path.quadTo(mid, cy, ex, ey)
        dispatch(path, 45L, 85L)
    }fun startMicro(x: Float, y: Float) {
        if (!Config.headTrack) return
        if (!gameActive) return
        stopMicro()
        val r = object : Runnable {
            var n = 0
            override fun run() {
                n = n + 1
                if (n > 20) {
                    stopMicro()
                    return
                }
                if (!gameActive) {
                    stopMicro()
                    return
                }
                val path = Path()
                path.moveTo(x, y)
                val f1 = 1.5.toFloat()
                val f2 = 0.75.toFloat()
                val f3 = 0.5.toFloat()
                val nx = x + Random.nextFloat() * f1 - f2
                val ny = y - Config.microAmp + Random.nextFloat() - f3
                path.lineTo(nx, ny)
                dispatch(path, 20L, 26L)
                h.postDelayed(this, Config.microMs)
            }
        }
        micro = r
        h.post(r)
    }

    fun stopMicro() {
        val m = micro
        if (m != null) {
            h.removeCallbacks(m)
        }
        micro = null
    }

    private fun dispatch(path: Path, a: Long, b: Long) {
        val dur = Random.nextLong(a, b)
        val stroke = GestureDescription.StrokeDescription(path, 0L, dur)
        val builder = GestureDescription.Builder()
        builder.addStroke(stroke)
        val g = builder.build()
        dispatchGesture(g, null, null)
    }
}
