package com.dj.systool

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*

class CoreService : Service() {
    private lateinit var wm: WindowManager
    private var pad: View? = null
    private var lastX = 0f
    private var lastY = 0f
    private val h = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            if (TouchService.gameActive && Config.boostRam) applyPerf()
            h.postDelayed(this, 8000L)
        }
    }

    override fun onBind(i: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        channel()
        startForeground(1, notif())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addPad()
        h.post(tick)
    }

    private fun otype() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    private fun addPad() {
        val v = View(this)
        val lp = WindowManager.LayoutParams(
            Config.padSize, Config.padSize, otype(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = Config.padX; y = Config.padY
        }
        v.setOnTouchListener { _, ev ->
            val s = TouchService.instance ?: return@setOnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = ev.rawX; lastY = ev.rawY
                    s.snapHead(ev.rawX, ev.rawY)
                    s.startMicro(ev.rawX, ev.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - lastX
                    val dy = ev.rawY - lastY
                    if (dx * dx + dy * dy > 16f) {
                        s.dragAssist(ev.rawX, ev.rawY, dx, dy)
                        lastX = ev.rawX; lastY = ev.rawY
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> s.stopMicro()
            }
            false
        }
        pad = v
        wm.addView(v, lp)
    }

    private val heavy = listOf(
        "com.facebook.katana", "com.facebook.orca", "com.instagram.android",
        "com.zhiliaoapp.musically", "com.google.android.youtube",
        "com.spotify.music", "com.discord", "com.whatsapp",
        "com.android.chrome", "com.sec.android.app.sbrowser"
    )

    private fun applyPerf() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        heavy.forEach { try { am.killBackgroundProcesses(it) } catch (_: Exception) {} }
        if (Settings.System.canWrite(this)) {
            try {Settings.Global.putFloat(contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            } catch (_: Exception) {}
        }
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("st", "System Tool",
                NotificationManager.IMPORTANCE_MIN))
    }

    private fun notif(): Notification {
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, "st")
        else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("System Tool").setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN).build()
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        pad?.let { wm.removeView(it) }
        if (Settings.System.canWrite(this)) {
            try {
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
                Settings.Global.putFloat(contentResolver,
                    Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
            } catch (_: Exception) {}
        }
        super.onDestroy()
    }
}
