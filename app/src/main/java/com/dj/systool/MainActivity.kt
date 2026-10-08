package com.dj.systool

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.GradientDrawable
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.RandomAccessFile

class MainActivity : AppCompatActivity() {

    var running = false
    var h: Handler? = null

    var lastCpuTotal = 0L
    var lastCpuIdle = 0L
    var lastRx = 0L
    var lastTx = 0L
    var lastMs = 0L

    lateinit var tvCpu: TextView
    lateinit var tvRam: TextView
    lateinit var tvNet: TextView
    lateinit var root: LinearLayout

    var anim: ValueAnimator? = null
    var animDraw: AnimatedImageDrawable? = null

    val PICK_GIF = 2002

    val colors = arrayOf(
        "#0a0618", "#000000", "#0d1b2a",
        "#1a0a2e", "#2a0a1a", "#0a1a0a"
    )

    val tick = object : Runnable {
        override fun run() {
            tvCpu.text = cpuPct().toString() + "%"
            tvRam.text = ramPct().toString() + "%"
            tvNet.text = netPct().toString() + "%"
            h?.postDelayed(this, 1500L)
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Config.init(this)
        h = Handler(Looper.getMainLooper())

        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(30, 30, 30, 30)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)

        addTitle()
        addCircles()
        addSpace()
        addAllSwitches()
        addSpace()
        addSpinner()
        addSpace()
        addColorPicker()
        addSpace()
        addOnOff()

        if (Config.bgUri.isNotEmpty()) {
            loadGif()
        } else {
            applyBg()
        }
    }

    fun addTitle() {
        val t = TextView(this)
        t.text = "HEADLOCK PLUS"
        t.setTextColor(Color.parseColor("#22C55E"))
        t.textSize = 26f
        t.gravity = Gravity.CENTER
        t.setPadding(0, 20, 0, 10)
        root.addView(t)

        val v = TextView(this)
        v.text = "v2"
        v.setTextColor(Color.parseColor("#666666"))
        v.textSize = 14f
        v.gravity = Gravity.CENTER
        v.setPadding(0, 0, 0, 10)
        root.addView(v)

        val m = TextView(this)
        m.text = "Made by Nguyen"
        m.setTextColor(Color.parseColor("#888888"))
        m.textSize = 12f
        m.gravity = Gravity.CENTER
        m.setPadding(0, 0, 0, 30)
        root.addView(m)
    }

    fun addCircles() {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        tvCpu = mkCircle("0%")
        tvRam = mkCircle("0%")
        tvNet = mkCircle("0%")
        row.addView(mkCol("CPU", tvCpu))
        row.addView(mkCol("RAM", tvRam))
        row.addView(mkCol("NET", tvNet))
        root.addView(row)
    }

    fun mkCircle(txt: String): TextView {
        val t = TextView(this)
        t.text = txt
        t.setTextColor(Color.WHITE)
        t.textSize = 16f
        t.gravity = Gravity.CENTER
        t.setBackgroundColor(Color.parseColor("#1A0E2A"))
        t.layoutParams = LinearLayout.LayoutParams(180, 180)
        return t
    }

    fun mkCol(label: String, circle: TextView): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(0, -2, 1f)
        c.layoutParams = lp
        c.addView(circle)
        val t = TextView(this)
        t.text = label
        t.setTextColor(Color.parseColor("#AAAAAA"))
        t.textSize = 11f
        t.setPadding(0, 10, 0, 0)
        c.addView(t)
        return c
    }

    fun addAllSwitches() {
        mkSwitch("AIMLOCK PLUS", Config.aimlock, 0)
        mkSwitch("BOOST RAM", Config.boostRam, 1)
        mkSwitch("SENSITIVITY", Config.antiShake, 2)
        mkSwitch("FIX RUNG", Config.fixRung, 3)
        mkSwitch("TOI UU THIET BI", Config.optimize, 4)
        mkSwitch("BAM DAU", Config.headTrack, 5)
        mkSwitch("ANTIBAN", Config.antiban, 6)
        mkSwitch("BOOST FPS", Config.boostFps, 7)
    }

    fun mkSwitch(label: String, checked: Boolean, id: Int) {
        val sw = Switch(this)
        sw.text = label
        sw.setTextColor(Color.WHITE)
        sw.textSize = 15f
        sw.isChecked = checked
        sw.setPadding(20, 20, 20, 20)
        sw.setOnCheckedChangeListener { _: CompoundButton, v: Boolean ->
            when (id) {
                0 -> Config.aimlock = v
                1 -> Config.boostRam = v
                2 -> Config.antiShake = v
                3 -> Config.fixRung = v
                4 -> Config.optimize = v
                5 -> Config.headTrack = v
                6 -> Config.antiban = v
                7 -> Config.boostFps = v
            }
            Config.save()
        }
        root.addView(sw)
    }

    fun addSpinner() {
        val sp = Spinner(this)
        val games = arrayOf("Free Fire", "Free Fire MAX", "Ca hai")
        sp.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, games)
        sp.setSelection(Config.gameIndex)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?,
                view: View?, pos: Int, id: Long) {
                Config.gameIndex = pos
                Config.save()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        root.addView(sp)
    }

    fun addColorPicker() {
        val label = TextView(this)
        label.text = "MAU NEN"
        label.setTextColor(Color.parseColor("#AAAAAA"))
        label.textSize = 12f
        label.setPadding(0, 0, 0, 12)
        root.addView(label)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        for (c in colors) {
            val b = View(this)
            val lp = LinearLayout.LayoutParams(0, 120, 1f)
            lp.setMargins(6, 6, 6, 6)
            b.layoutParams = lp
            val gd = GradientDrawable()
            gd.setColor(Color.parseColor(c))
            gd.cornerRadius = 20f
            gd.setStroke(3, Color.parseColor("#444444"))
            b.background = gd
            b.setOnClickListener {
                stopGif()
                Config.bgColor = c
                Config.bgUri = ""
                Config.save()
                applyBg()
            }
            row.addView(b)
        }
        root.addView(row)

        val rb = Button(this)
        rb.text = "RAINBOW"
        rb.setTextColor(Color.WHITE)
        rb.textSize = 14f
        rb.setBackgroundColor(Color.parseColor("#333333"))
        rb.layoutParams = LinearLayout.LayoutParams(-1, 100)
        rb.setOnClickListener {
            stopGif()
            Config.bgColor = "rainbow"
            Config.bgUri = ""
            Config.save()
            startRainbow()
        }
        root.addView(rb)

        val gb = Button(this)
        gb.text = "CHON ANH DONG (GIF)"
        gb.setTextColor(Color.WHITE)
        gb.textSize = 14f
        gb.setBackgroundColor(Color.parseColor("#333333"))
        gb.layoutParams = LinearLayout.LayoutParams(-1, 100)
        gb.setOnClickListener { pickGif() }
        root.addView(gb)
    }

    fun pickGif() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        startActivityForResult(i, PICK_GIF)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_GIF && res == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            Config.bgUri = uri.toString()
            Config.save()
            loadGif()
        }
    }

    fun loadGif() {
        if (Config.bgUri.isEmpty()) {
            applyBg()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            applyBg()
            return
        }
        try {
            stopRainbow()
            stopGif()
            val uri = Uri.parse(Config.bgUri)
            val src = ImageDecoder.createSource(contentResolver, uri)
            val dr = ImageDecoder.decodeDrawable(src)
            if (dr is AnimatedImageDrawable) {
                dr.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                animDraw = dr
                root.background = dr
                dr.start()
            } else {
                root.background = dr
            }
        } catch (e: Exception) {
            applyBg()
        }
    }

    fun stopGif() {
        animDraw?.stop()
        animDraw = null
    }

    fun startRainbow() {
        stopRainbow()
        val r = root
        anim = ValueAnimator.ofFloat(0f, 360f)
        anim?.duration = 6000L
        anim?.repeatCount = ValueAnimator.INFINITE
        anim?.addUpdateListener { a ->
            val deg = a.animatedValue as Float
            val hue = deg % 360f
            val c1 = Color.HSVToColor(
                floatArrayOf(hue, 0.6f, 0.12f))
            val c2 = Color.HSVToColor(
                floatArrayOf((hue + 60f) % 360f, 0.6f, 0.08f))
            val c3 = Color.HSVToColor(
                floatArrayOf((hue + 120f) % 360f, 0.6f, 0.12f))
            val gd = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c1, c2, c3)
            )
            r.background = gd
        }
        anim?.start()
    }

    fun stopRainbow() {
        anim?.cancel()
        anim = null
    }

    fun applyBg() {
        stopRainbow()
        stopGif()
        if (Config.bgColor == "rainbow") {
            startRainbow()
            return
        }
        try {
            root.setBackgroundColor(Color.parseColor(Config.bgColor))
        } catch (e: Exception) {
            root.setBackgroundColor(Color.parseColor("#0a0618"))
        }
    }

    fun addOnOff() {
        val btnOn = Button(this)
        btnOn.text = "ON"
        btnOn.setTextColor(Color.WHITE)
        btnOn.textSize = 18f
        btnOn.setBackgroundColor(Color.parseColor("#22C55E"))
        btnOn.layoutParams = LinearLayout.LayoutParams(-1, 160)
        root.addView(btnOn)

        val btnOff = Button(this)
        btnOff.text = "OFF"
        btnOff.setTextColor(Color.WHITE)
        btnOff.textSize = 18f
        btnOff.setBackgroundColor(Color.parseColor("#333333"))
        btnOff.layoutParams = LinearLayout.LayoutParams(-1, 160)
        root.addView(btnOff)

        btnOn.setOnClickListener { startOn(btnOn, btnOff) }
        btnOff.setOnClickListener {
            stopService(Intent(this, CoreService::class.java))
            running = false
            paint(btnOn, btnOff)
            Toast.makeText(this, "Da TAT", Toast.LENGTH_SHORT).show()
        }
    }

    fun startOn(btnOn: Button, btnOff: Button) {
        val ov = Settings.canDrawOverlays(this)
        if (!ov) {
            Toast.makeText(this, "Cap quyen Overlay",
                Toast.LENGTH_LONG).show()
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            i.data = Uri.parse("package:" + packageName)
            startActivity(i)
            return
        }
        if (TouchService.instance == null) {
            Toast.makeText(this, "Bat Accessibility",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val ws = Settings.System.canWrite(this)
        if (!ws) {
            Toast.makeText(this, "Cap quyen Write",
                Toast.LENGTH_LONG).show()
            val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            i.data = Uri.parse("package:" + packageName)
            startActivity(i)
            return
        }
        ContextCompat.startForegroundService(this,
            Intent(this, CoreService::class.java))
        running = true
        paint(btnOn, btnOff)
        Toast.makeText(this, "Da BAT v2", Toast.LENGTH_SHORT).show()
    }

    fun paint(on: Button, off: Button) {
        if (running) {
            on.setBackgroundColor(Color.parseColor("#22C55E"))
            off.setBackgroundColor(Color.parseColor("#333333"))
        } else {
            on.setBackgroundColor(Color.parseColor("#333333"))
            off.setBackgroundColor(Color.parseColor("#EF4444"))
        }
    }

    fun addSpace() {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(-1, 30)
        root.addView(v)
    }

    override fun onResume() {
        super.onResume()
        h?.post(tick)
        if (Config.bgUri.isNotEmpty()) {
            loadGif()
        } else if (Config.bgColor == "rainbow") {
            startRainbow()
        }
    }

    override fun onPause() {
        super.onPause()
        h?.removeCallbacks(tick)
        stopRainbow()
        animDraw?.stop()
    }

    fun cpuPct(): Int {
        try {
            val r = RandomAccessFile("/proc/stat", "r")
            val line = r.readLine() ?: return 0
            r.close()
            val p = line.split("\\s+".toRegex())
            if (p.size < 8) return 0
            val idle = p[4].toLong()
            var total = 0L
            for (i in 1..7) total += p[i].toLong()
            val dT = total - lastCpuTotal
            val dI = idle - lastCpuIdle
            lastCpuTotal = total
            lastCpuIdle = idle
            if (dT <= 0) return 0
            val v = ((dT - dI) * 100L / dT).toInt()
            return if (v < 0) 0 else if (v > 100) 100 else v
        } catch (e: Exception) {
            return 0
        }
    }

    fun ramPct(): Int {
        val am = getSystemService(Context.ACTIVITY_SERVICE)
            as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        if (mi.totalMem <= 0) return 0
        val used = mi.totalMem - mi.availMem
        return (used * 100L / mi.totalMem).toInt()
    }

    fun netPct(): Int {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (lastMs == 0L) {
            lastRx = rx
            lastTx = tx
            lastMs = now
            return 0
        }
        val dt = (now - lastMs).coerceAtLeast(1)
        val db = (rx - lastRx) + (tx - lastTx)
        lastRx = rx
        lastTx = tx
        lastMs = now
        val kb = db * 1000L / dt / 1024L
        val v = (kb * 100L / 500L).toInt()
        return if (v < 0) 0 else if (v > 100) 100 else v
    }
}
