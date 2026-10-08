package com.dj.systool

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import java.util.Random

class CoreService : Service() {

    var wm: WindowManager? = null
    var pad: View? = null
    var lp: LayoutParams? = null
    var on = false

    var rx = 0f
    var ry = 0f
    var sx = 0f
    var sy = 0f
    var px = 0f
    var py = 0f
    var down = false
    var t0 = 0L
    var mv = 0f

    var h: Handler? = null
    var rnd = Random()

    val loop = object : Runnable {
        override fun run() {
            if (!down) return
            val ts = TouchService.instance
            if (ts == null) return
            val a = Config.smoothAlpha
            sx = a * rx + (1f - a) * sx
            sy = a * ry + (1f - a) * sy
            var dx = sx - px
            var dy = sy - py
            val dz = Config.deadZone
            if (Math.abs(dx) < dz) dx = 0f
            if (Math.abs(dy) < dz) dy = 0f
            val d2 = dx * dx + dy * dy
            if (d2 > 0.25f) {
                ts.dragStep(px, py, sx, sy, dy)
                px = sx
                py = sy
            } else {
                ts.holdAt(sx, sy)
            }
            val n = 8L + rnd.nextInt(8)
            h?.postDelayed(this, n)
        }
    }

    val watch = object : Runnable {
        override fun run() {
            val a = TouchService.gameActive
            if (a != on) setPad(a)
            h?.postDelayed(this, 300L)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mkChannel()
        startForeground(1, mkNoti())
        h = Handler(Looper.getMainLooper())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mkPad()
        setPad(false)
        h?.post(watch)
    }

    fun mkFlags(): Int {
        var f = LayoutParams.FLAG_NOT_FOCUSABLE
        f = f or LayoutParams.FLAG_NOT_TOUCH_MODAL
        f = f or LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (on) return f
        return f or LayoutParams.FLAG_NOT_TOUCHABLE
    }

    fun setPad(a: Boolean) {
        on = a
        val v = pad ?: return
        val p = lp ?: return
        p.flags = mkFlags()
        try { wm?.updateViewLayout(v, p) } catch (e: Exception) {}
    }

    fun mkPad() {
        val v = View(this)
        val p = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            2038,
            mkFlags(),
            PixelFormat.TRANSLUCENT
        )
        v.setOnTouchListener { _: View, ev: MotionEvent ->
            val ts = TouchService.instanceif (ts == null) return@setOnTouchListener false
            if (!TouchService.gameActive) {
                return@setOnTouchListener false
            }
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    rx = ev.rawX
                    ry = ev.rawY
                    sx = ev.rawX
                    sy = ev.rawY
                    px = ev.rawX
                    py = ev.rawY
                    down = true
                    mv = 0f
                    t0 = System.currentTimeMillis()
                    h?.postDelayed(loop, 20L)
                }
                MotionEvent.ACTION_MOVE -> {
                    val m1 = Math.abs(ev.rawX - rx)
                    val m2 = Math.abs(ev.rawY - ry)
                    mv = mv + m1 + m2
                    rx = ev.rawX
                    ry = ev.rawY
                }
                MotionEvent.ACTION_UP -> {
                    up(ts, ev.rawX, ev.rawY)
                }
                MotionEvent.ACTION_CANCEL -> {
                    down = false
                    h?.removeCallbacks(loop)
                }
            }
            true
        }
        pad = v
        lp = p
        wm?.addView(v, p)
    }

    fun up(ts: TouchService, x: Float, y: Float) {
        val dur = System.currentTimeMillis() - t0
        if (mv < Config.tapMaxDist && dur < Config.tapMaxMs) {
            ts.snapHead(x, y)
        }
        down = false
        h?.removeCallbacks(loop)
    }

    fun mkChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE)
            as NotificationManager
        val c = NotificationChannel(
            "sys", "System Service", 1)
        c.setSound(null, null)
        nm.createNotificationChannel(c)
    }

    fun mkNoti(): Notification {
        return Notification.Builder(this, "sys")
            .setContentTitle("System Service")
            .setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        h?.removeCallbacks(loop)
        h?.removeCallbacks(watch)
        val v = pad
        if (v != null) wm?.removeView(v)
        super.onDestroy()
    }
}
