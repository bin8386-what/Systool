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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.util.Random

class CoreService : Service() {

    private lateinit var wm: WindowManager
    private var pad: View? = null
    private var lp: WindowManager.LayoutParams? = null
    private var padActive = true

    private var rawX = 0f
    private var rawY = 0f
    private var smoothX = 0f
    private var smoothY = 0f
    private var sentX = 0f
    private var sentY = 0f
    private var isDown = false
    private var downTime = 0L
    private var movedDist = 0f

    private val h = Handler(Looper.getMainLooper())
    private val rnd = Random()

    private fun sm(prev: Float, cur: Float): Float {
        val a = Config.smoothAlpha
        return a * cur + (1f - a) * prev
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!isDown) return
            val s = TouchService.instance ?: return
            smoothX = sm(smoothX, rawX)
            smoothY = sm(smoothY, rawY)
            var dx = smoothX - sentX
            var dy = smoothY - sentY
            val dz = Config.deadZone
            if (Math.abs(dx) < dz) dx = 0f
            if (Math.abs(dy) < dz) dy = 0f
            val d2 = dx * dx + dy * dy
            if (d2 > 0.25f) {
                s.dragStep(sentX, sentY, smoothX, smoothY, dy)
                sentX = smoothX
                sentY = smoothY
            } else {
                s.holdAt(smoothX, smoothY)
            }
            val next = 8L + rnd.nextInt(8)
            h.postDelayed(this, next)
        }
    }

    private val watch = object : Runnable {
        override fun run() {
            val a = TouchService.gameActive
            if (a != padActive) setPad(a)
            h.postDelayed(this, 250L)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (TouchService.gameActive && Config.boostRam) killBg()
            h.postDelayed(this, 3000L)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ch()
        startForeground(1, noti())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addPad()
        setPad(false)
        h.post(watch)
        h.post(tick)
    }

    private fun otype() = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    private fun flags(): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREENif (!padActive) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    private fun setPad(a: Boolean) {
        padActive = a
        val v = pad ?: return
        val p = lp ?: return
        p.flags = flags()
        try { wm.updateViewLayout(v, p) } catch (e: Exception) {}
    }

    private fun addPad() {
        val padView = View(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            otype(),
            flags(),
            PixelFormat.TRANSLUCENT
        )
        padView.setOnTouchListener { view: View, ev: MotionEvent ->
            val s = TouchService.instance
            if (s == null) return@setOnTouchListener false
            if (!TouchService.gameActive) return@setOnTouchListener false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    rawX = ev.rawX
                    rawY = ev.rawY
                    smoothX = ev.rawX
                    smoothY = ev.rawY
                    sentX = ev.rawX
                    sentY = ev.rawY
                    isDown = true
                    movedDist = 0f
                    downTime = System.currentTimeMillis()
                    h.postDelayed(loop, 20L)
                }
                MotionEvent.ACTION_MOVE -> {
                    movedDist += Math.abs(ev.rawX - rawX) + Math.abs(ev.rawY - rawY)
                    rawX = ev.rawX
                    rawY = ev.rawY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dur = System.currentTimeMillis() - downTime
                    if (movedDist < Config.tapMaxDist && dur < Config.tapMaxMs) {
                        s.snapHead(ev.rawX, ev.rawY)
                    }
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

    private val apps = listOf(
        "com.facebook.katana", "com.facebook.orca", "com.instagram.android",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.google.android.youtube", "com.spotify.music", "com.discord",
        "com.telegram.messenger", "org.telegram.messenger", "com.whatsapp",
        "com.zing.zalo", "com.android.chrome", "com.sec.android.app.sbrowser",
        "org.mozilla.firefox", "com.netflix.mediaclient",
        "com.snapchat.android", "com.twitter.android",
        "com.google.android.apps.photos", "com.google.android.apps.maps",
        "com.google.android.gm", "com.samsung.android.game.gamehome",
        "com.sec.android.app.shealth", "com.samsung.android.app.spage",
        "com.samsung.android.bixby.agent"
    )

    private fun killBg() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (p in apps) {try { am.killBackgroundProcesses(p) } catch (e: Exception) {}
        }
        try {
            val m = ActivityManager::class.java
                .getMethod("killAllBackgroundProcesses")
            m.invoke(am)
        } catch (e: Exception) {}
    }

    private fun ch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val c = NotificationChannel("sys", "System Service",
            NotificationManager.IMPORTANCE_MIN)
        c.setShowBadge(false)
        c.setSound(null, null)
        nm.createNotificationChannel(c)
    }

    private fun noti(): Notification {
        val b = Notification.Builder(this, "sys")
        return b.setContentTitle("System Service")
            .setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        h.removeCallbacks(loop)
        h.removeCallbacks(watch)
        val v = pad
        if (v != null) wm.removeView(v)
        super.onDestroy()
    }
}
