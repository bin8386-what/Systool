package com.dj.systool

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class CoreService : Service() {

    private lateinit var wm: WindowManager
    private var pad: View? = null
    private var lp: WindowManager.LayoutParams? = null
    private var padActive = true
    private var prevX = 0f
    private var prevY = 0f
    private var curX = 0f
    private var curY = 0f
    private var isDown = false
    private val h = Handler(Looper.getMainLooper())

    private val loop = object : Runnable {
        override fun run() {
            if (!isDown) return
            val s = TouchService.instance ?: return
            val dx = curX - prevX
            val dy = curY - prevY
            if (dx * dx + dy * dy > 4) {
                s.dragStep(prevX, prevY, curX, curY, dy)
                prevX = curX
                prevY = curY
            } else {
                s.holdAt(curX, curY)
            }
            h.postDelayed(this, 30L)
        }
    }

    private val gameWatch = object : Runnable {
        override fun run() {
            val active = TouchService.gameActive
            if (active != padActive) setPadActive(active)
            h.postDelayed(this, 400L)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (TouchService.gameActive && Config.boostRam) applyPerf()
            h.postDelayed(this, 8000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        channel()
        startForeground(1, notif())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addPad()
        setPadActive(false)
        h.post(gameWatch)
        h.post(tick)
    }

    private fun otype(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        return WindowManager.LayoutParams.TYPE_PHONE
    }

    private fun baseFlags(): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!padActive) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    private fun setPadActive(active: Boolean) {
        padActive = active
        val view = pad ?: return
        val params = lp ?: return
        params.flags = baseFlags()
        try { wm.updateViewLayout(view, params) } catch (e: Exception) {}
    }private fun addPad() {
        val padView = View(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            otype(),
            baseFlags(),
            PixelFormat.TRANSLUCENT
        )
        padView.setOnTouchListener { view: View, ev: MotionEvent ->
            val s = TouchService.instance
            if (s == null) return@setOnTouchListener false
            if (!TouchService.gameActive) return@setOnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    curX = ev.rawX
                    curY = ev.rawY
                    prevX = ev.rawX
                    prevY = ev.rawY
                    isDown = true
                    s.snapHead(ev.rawX, ev.rawY)
                    h.postDelayed(loop, 100L)
                }
                MotionEvent.ACTION_MOVE -> {
                    curX = ev.rawX
                    curY = ev.rawY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDown = false
                    h.removeCallbacks(loop)
                }
            }
            true
        }
        pad = padView
        lp = params
        wm.addView(padView, params)
    }

    private val heavy = listOf(
        "com.facebook.katana", "com.facebook.orca", "com.instagram.android",
        "com.zhiliaoapp.musically", "com.google.android.youtube",
        "com.spotify.music", "com.discord", "com.whatsapp",
        "com.android.chrome", "com.sec.android.app.sbrowser"
    )

    private fun applyPerf() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (p in heavy) {
            try { am.killBackgroundProcesses(p) } catch (e: Exception) {}
        }
        if (Settings.System.canWrite(this)) {
            try {
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            } catch (e: Exception) {}
        }
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel("st", "System Tool",
            NotificationManager.IMPORTANCE_MIN)
        nm.createNotificationChannel(ch)
    }

    private fun notif(): Notification {
        val b: Notification.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            b = Notification.Builder(this, "st")
        else
            b = Notification.Builder(this)
        return b.setContentTitle("System Tool").setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        h.removeCallbacks(loop)
        h.removeCallbacks(gameWatch)
        val view = pad
        if (view != null) wm.removeView(view)
        if (Settings.System.canWrite(this)) {
            try {
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
            } catch (e: Exception) {}
        }
        super.onDestroy()
    }
}
