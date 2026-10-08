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

    var ob = -1
    var or = -1
    var ot = -1
    var oh = -1
    var wl: PowerManager.WakeLock? = null

    val loop = object : Runnable {
        override fun run() {
            if (!down) return
            val ts = TouchService.instance ?: return
            val a = Config.smoothAlpha
            sx = a * rx + (1f - a) * sx
            sy = a * ry + (1f - a) * sy
            var dx = sx - px
            var dy = sy - py
            val dz = Config.deadZone
            if (Math.abs(dx) < dz) dx = 0f
            if (Math.abs(dy) < dz) dy = 0f
            val d2 = dx * dx + dy * dy
            if (d2 > 0.2f) {
                ts.dragStep(px, py, sx, sy, dy)
                px = sx
                py = sy
            } else {
                ts.holdAt(sx, sy)
            }
            h?.postDelayed(this, 7L + rnd.nextInt(6))
        }
    }

    val watch = object : Runnable {
        override fun run() {
            val a = TouchService.gameActive
            if (a != on) {
                setPad(a)
                if (a) enter() else exit()
            }
            h?.postDelayed(this, 200L)
        }
    }

    val tick = object : Runnable {
        override fun run() {
            if (TouchService.gameActive) doAll()
            h?.postDelayed(this, 2500L)
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mkCh()
        startForeground(1, mkNo())
        h = Handler(Looper.getMainLooper())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mkPad()
        setPad(false)
        h?.post(watch)
        h?.post(tick)
    }

    fun enter() {
        if (Config.boostFps) getWl()
    }

    fun exit() {
        rmWl()
        restore()
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
            val ts = TouchService.instance
            if (ts == null) return@setOnTouchListener false
            if (!TouchService.gameActive) {
                return@setOnTouchListener false
            }
            val act = ev.actionMasked
            if (act == MotionEvent.ACTION_DOWN) {
                rx = ev.rawX
                ry = ev.rawY
                sx = ev.rawX
                sy = ev.rawY
                px = ev.rawX
                py = ev.rawY
                down = true
                mv = 0f
                t0 = System.currentTimeMillis()
                h?.postDelayed(loop, 15L)
            } else if (act == MotionEvent.ACTION_MOVE) {
                mv = mv + Math.abs(ev.rawX - rx)
                mv = mv + Math.abs(ev.rawY - ry)
                rx = ev.rawX
                ry = ev.rawY
            } else if (act == MotionEvent.ACTION_UP) {
                val dur = System.currentTimeMillis() - t0
                val small = mv < Config.tapMaxDist
                val quick = dur < Config.tapMaxMs
                if (small && quick) {
                    ts.snapHead(ev.rawX, ev.rawY)
                }
                down = false
                h?.removeCallbacks(loop)
            } else if (act == MotionEvent.ACTION_CANCEL) {
                down = false
                h?.removeCallbacks(loop)
            }
            true
        }
        pad = v
        lp = p
        wm?.addView(v, p)
    }

    val heavy = listOf(
        "com.facebook.katana", "com.facebook.orca",
        "com.facebook.lite", "com.instagram.android",
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.spotify.music", "com.discord",
        "com.telegram.messenger", "org.telegram.messenger",
        "com.whatsapp", "com.zing.zalo", "com.viber.voip",
        "com.linecorp.line", "com.kakao.talk",
        "com.android.chrome", "com.sec.android.app.sbrowser",
        "org.mozilla.firefox", "com.opera.browser",
        "com.netflix.mediaclient", "com.snapchat.android",
        "com.twitter.android",
        "com.google.android.apps.photos",
        "com.google.android.apps.maps",
        "com.google.android.apps.docs",
        "com.google.android.gm",
        "com.google.android.calendar",
        "com.samsung.android.game.gamehome",
        "com.sec.android.app.shealth",
        "com.samsung.android.app.spage",
        "com.samsung.android.bixby.agent",
        "com.samsung.android.visionintelligence",
        "com.samsung.android.messaging",
        "com.samsung.android.email.provider",
        "com.samsung.android.app.notes",
        "com.samsung.android.calendar",
        "com.sec.android.app.myfiles",
        "com.samsung.android.dialer",
        "com.samsung.android.contacts",
        "com.samsung.android.app.reminder",
        "com.samsung.android.app.tips",
        "com.samsung.android.game.gos",
        "com.samsung.android.arzone",
        "com.samsung.android.oneconnect"
    )

    fun doAll() {
        if (Config.boostRam) killBg()
        if (Config.boostFps) boostFps()
        if (Config.fixRung) fixRung()
        if (Config.optimize) opt()
        if (Config.antiban) anti()
    }

    fun killBg() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (p in heavy) {
            try { am.killBackgroundProcesses(p) } catch (e: Exception) {}
        }
        try {
            val m = ActivityManager::class.java
                .getMethod("killAllBackgroundProcesses")
            m.invoke(am)
        } catch (e: Exception) {}
    }

    fun getWl() {
        if (wl != null) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hl:cpu")
            wl?.setReferenceCounted(false)
            wl?.acquire(60 * 60 * 1000L)
        } catch (e: Exception) {}
    }

    fun rmWl() {
        try {
            if (wl?.isHeld == true) wl?.release()
        } catch (e: Exception) {}
        wl = null
    }

    fun boostFps() {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.Global.putFloat(contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
            Settings.Global.putFloat(contentResolver,
                Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            Settings.Global.putInt(contentResolver,
                "sem_perf_level", 1)
            Settings.Global.putInt(contentResolver,
                "sem_enhanced_cpu_responsiveness", 1)
            Settings.Global.putInt(contentResolver,
                "game_auto_temperature_control", 0)
            Settings.Global.putInt(contentResolver,
                "persist.sys.sdhci.max_speed", 1)
        } catch (e: Exception) {}
    }

    fun fixRung() {
        if (!Settings.System.canWrite(this)) return
        try {
            if (oh < 0) oh = Settings.System.getInt(contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
            Settings.System.putInt(contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            Settings.System.putInt(contentResolver,
                Settings.System.SOUND_EFFECTS_ENABLED, 0)
        } catch (e: Exception) {}
    }

    fun opt() {
        if (!Settings.System.canWrite(this)) return
        try {
            Settings.Global.putInt(contentResolver,
                Settings.Global.WIFI_SLEEP_POLICY, 2)
            Settings.Global.putInt(contentResolver,
                Settings.Global.AUTO_TIME_ZONE, 0)
            Settings.Global.putInt(contentResolver,
                Settings.Global.AUTO_TIME, 0)
        } catch (e: Exception) {}
    }

    fun anti() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(2)
            }
        } catch (e: Exception) {}
        if (!Settings.System.canWrite(this)) return
        try {
            if (ob < 0) ob = Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128)
            if (or < 0) or = Settings.System.getInt(contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 1)
            if (ot < 0) ot = Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 70)
            Settings.System.putInt(contentResolver,
                Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT, 1800000)
        } catch (e: Exception) {}
        try {
            ContentResolver::class.java
                .getMethod("setMasterSyncAutomatically",
                    Boolean::class.java)
                .invoke(null, false)
        } catch (e: Exception) {}
    }

    fun restore() {
        if (!Settings.System.canWrite(this)) return
        try {
            if (ob >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, ob)
                ob = -1
            }
            if (or >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION, or)
                or = -1
            }
            if (ot >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT, ot)
                ot = -1
            }
            if (oh >= 0) {
                Settings.System.putInt(contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, oh)
                oh = -1
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
                .getMethod("setMasterSyncAutomatically",
                    Boolean::class.java)
                .invoke(null, true)
        } catch (e: Exception) {}
    }

    fun mkCh() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NOTIFICATION_SERVICE)
            as NotificationManager
        val c = NotificationChannel("sys", "System Service", 1)
        c.setShowBadge(false)
        c.setSound(null, null)
        nm.createNotificationChannel(c)
    }

    fun mkNo(): Notification {
        val b: Notification.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            b = Notification.Builder(this, "sys")
        else
            b = Notification.Builder(this)
        return b.setContentTitle("System Service")
            .setContentText("running")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(Notification.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        h?.removeCallbacks(loop)
        h?.removeCallbacks(watch)
        h?.removeCallbacks(tick)
        val v = pad
        if (v != null) wm?.removeView(v)
        rmWl()
        restore()
        super.onDestroy()
    }
}
