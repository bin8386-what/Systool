package com.dj.systool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

class TouchService : AccessibilityService() {
    companion object {
        var instance: TouchService? = null
        var gameActive: Boolean = false
    }

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
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x, y - Config.snapPx)
        dispatch(path, 90L)
    }

    fun dragStep(px: Float, py: Float, x: Float, y: Float, dy: Float) {
        if (!Config.aimlock) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path()
        path.moveTo(px, py)
        val ey = y + dy * Config.pullUp
        path.lineTo(x, ey)
        dispatch(path, 30L)
    }

    fun holdAt(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x, y - 1)
        dispatch(path, 30L)
    }

    private fun dispatch(path: Path, durMs: Long) {
        val d = durMs + Random.nextLong(0L, 10L)
        val stroke = GestureDescription.StrokeDescription(path, 0L, d)
        val b = GestureDescription.Builder()
        b.addStroke(stroke)
        dispatchGesture(b.build(), null, null)
    }
}
