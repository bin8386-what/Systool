package com.dj.systool

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
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

    private var oldBrightness = -1
    private var oldRotation = -1
    private var oldScreenTimeout = -1
    private var oldHaptic = -1
    private var wakeLock: PowerManager.WakeLock? = null

    private fun smooth(prev: Float, cur: Float): Float {
        val a = Config.smoothAlpha
        return a * cur + (1f - a) * prev
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!isDown) return
            val s = TouchService.instance ?: return
            smoothX = smooth(smoothX, rawX)
            smoothY = smooth(smoothY, rawY)
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

    private val gameWatch = object : Runnable {
        override fun run() {
            val active = TouchService.gameActive
            if (active != padActive) {
                setPadActive(active)
                if (active) onGameEnter() else onGameExit()
            }
            h.postDelayed(this, 250L)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (TouchService.gameActive) applyAll()
            h.postDelayed(this, 3000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        channel()
        startForeground(1, notif())wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addPad()
        setPadActive(false)
        h.post(gameWatch)
        h.post(tick)
    }

    private fun onGameEnter() {
        if (Config.boostFps) acquireWake()
    }

    private fun onGameExit() {
        releaseWake()
        restoreSystem()
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
    }

    private fun addPad() {
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

    private val heavy = listOf("com.facebook.katana", "com.facebook.orca", "com.facebook.lite",
        "com.instagram.android", "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill", "com.google.android.youtube",
        "com.google.android.apps.youtube.music", "com.spotify.music",
        "com.discord", "com.telegram.messenger", "org.telegram.messenger",
        "com.whatsapp", "com.zing.zalo", "com.viber.voip", "com.linecorp.line",
        "com.kakao.talk", "com.android.chrome", "com.sec.android.app.sbrowser",
        "org.mozilla.firefox", "com.opera.browser",
        "com.netflix.mediaclient", "com.snapchat.android",
        "com.twitter.android", "com.google.android.apps.photos",
        "com.google.android.apps.maps", "com.google.android.apps.docs",
        "com.google.android.gm", "com.google.android.calendar",
        "com.samsung.android.game.gamehome", "com.sec.android.app.shealth",
        "com.samsung.android.app.spage", "com.samsung.android.bixby.agent",
        "com.samsung.android.visionintelligence",
        "com.samsung.android.messaging", "com.samsung.android.email.provider",
        "com.samsung.android.app.notes", "com.samsung.android.calendar",
        "com.sec.android.app.myfiles", "com.samsung.android.dialer",
        "com.samsung.android.contacts", "com.samsung.android.app.reminder",
        "com.samsung.android.app.tips", "com.samsung.android.game.gos",
        "com.samsung.android.arzone", "com.samsung.android.oneconnect"
    )

    private fun applyAll() {
        if (Config.boostRam) killHeavy()
        if (Config.boostFps) boostFps()
        if (Config.fixRung) fixRung()
        if (Config.optimize) optimize()
        if (Config.antiban) antiban()
    }

    private fun killHeavy() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (p in heavy) {
            try { am.killBackgroundProcesses(p) } catch (e: Exception) {}
        }
        try {
            val m = ActivityManager::class.java.getMethod("killAllBackgroundProcesses")
            m.invoke(am)
        } catch (e: Exception) {}
    }

    private fun acquireWake() {
        if (wakeLock != null) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hl:cpu")
            wakeLock?.setReferenceCounted(false)
            wakeLock?.acquire(60 * 60 * 1000L)
        } catch (e: Exception) {}
    }

    private fun releaseWake() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {}
        wakeLock = null
    }

    private fun boostFps() {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.Global.putFloat(contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)Settings.Global.putFloat(contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            Settings.Global.putInt(contentResolver, "sem_perf_level", 1)
            Settings.Global.putInt(contentResolver,
                "sem_enhanced_cpu_responsiveness", 1)
            Settings.Global.putInt(contentResolver,
                "game_auto_temperature_control", 0)
            Settings.Global.putInt(contentResolver,
                "persist.sys.sdhci.max_speed", 1)
            Settings.Global.putInt(contentResolver,
                "persist.sys.NV_FPSLIMIT", 0)
            Settings.Global.putInt(contentResolver,
                "persist.sys.NV_POWERMODE", 1)
        } catch (e: Exception) {}
    }

    private fun fixRung() {
        if (!Settings.System.canWrite(this)) return
        try {
            if (oldHaptic < 0) {
                oldHaptic = Settings.System.getInt(contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
            }
            Settings.System.putInt(contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            Settings.System.putInt(contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0)
        } catch (e: Exception) {}
    }

    private fun optimize() {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.Global.putInt(contentResolver,
                Settings.Global.WIFI_SLEEP_POLICY,
                Settings.Global.WIFI_SLEEP_POLICY_NEVER)
            Settings.Global.putInt(contentResolver,
                Settings.Global.AUTO_TIME_ZONE, 0)
            Settings.Global.putInt(contentResolver,
                Settings.Global.AUTO_TIME, 0)
        } catch (e: Exception) {}
    }

    private fun antiban() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (e: Exception) {}
        if (!Settings.System.canWrite(this)) return
        try {
            if (oldBrightness < 0) oldBrightness = Settings.System.getInt(
                contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            if (oldRotation < 0) oldRotation = Settings.System.getInt(
                contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
            if (oldScreenTimeout < 0) oldScreenTimeout = Settings.System.getInt(
                contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 80)
            Settings.System.putInt(contentResolver,Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, 30 * 60 * 1000)
        } catch (e: Exception) {}
        try {
            ContentResolver::class.java
                .getMethod("setMasterSyncAutomatically", Boolean::class.java)
                .invoke(null, false)
        } catch (e: Exception) {}
    }

    private fun restoreSystem() {
        if (!Settings.System.canWrite(this)) return
        try {
            if (oldBrightness >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, oldBrightness)
                oldBrightness = -1
            }
            if (oldRotation >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, oldRotation)
                oldRotation = -1
            }
            if (oldScreenTimeout >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT, oldScreenTimeout)
                oldScreenTimeout = -1
            }
            if (oldHaptic >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, oldHaptic)
                oldHaptic = -1
            }
            Settings.System.putInt(contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 1)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
        } catch (e: Exception) {}
        try {
            ContentResolver::class.java
                .getMethod("setMasterSyncAutomatically", Boolean::class.java)
                .invoke(null, true)
        } catch (e: Exception) {}
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel("sys", "System Service",
            NotificationManager.IMPORTANCE_MIN)
        ch.setShowBadge(false)
        ch.enableLights(false)
        ch.enableVibration(false)
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
    }

    private fun notif(): Notification {
        val b: Notification.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            b = Notification.Builder(this, "sys")
        else
            b = Notification.Builder(this)
        return b.setContentTitle("System Service")
            .setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN)
            .setOngoing(true).build()
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        h.removeCallbacks(loop)
        h.removeCallbacks(gameWatch)
        val view = pad
        if (view != null) wm.removeView(view)
        releaseWake()
        restoreSystem()
        super.onDestroy()
    }
}
