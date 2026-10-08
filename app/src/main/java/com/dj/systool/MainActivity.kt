package com.dj.systool

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.TrafficStats
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.RandomAccessFile

class MainActivity : AppCompatActivity() {

    private var running = false
    private val h = Handler(Looper.getMainLooper())

    private var lastCpuTotal = 0L
    private var lastCpuIdle = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastMs = 0L

    private lateinit var tvCpu: TextView
    private lateinit var tvRam: TextView
    private lateinit var tvNet: TextView
    private lateinit var rootLayout: LinearLayout

    private val PICK_IMG = 1001

    private val tick = object : Runnable {
        override fun run() {
            tvCpu.text = "${cpuPct()}%"
            tvRam.text = "${ramPct()}%"
            tvNet.text = "${netPct()}%"
            h.postDelayed(this, 1500L)
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Config.init(this)

        rootLayout = LinearLayout(this)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setPadding(30, 30, 30, 30)
        rootLayout.setBackgroundColor(Color.parseColor("#0a0618"))

        val scroll = ScrollView(this)
        scroll.addView(rootLayout)
        setContentView(scroll)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER

        tvCpu = makeCircle("0%")
        tvRam = makeCircle("0%")
        tvNet = makeCircle("0%")

        row.addView(makeCol("CPU", tvCpu))
        row.addView(makeCol("RAM", tvRam))
        row.addView(makeCol("NET", tvNet))
        rootLayout.addView(row)

        addSpace()

        addSwitch("AIMLOCK", Config.aimlock) { v -> Config.aimlock = v; Config.save() }
        addSwitch("BOOST RAM", Config.boostRam) { v -> Config.boostRam = v; Config.save() }
        addSwitch("SENSITIVITY", Config.antiShake) { v -> Config.antiShake = v; Config.save() }
        addSwitch("FIX RUNG", Config.fixRung) { v -> Config.fixRung = v; Config.save() }
        addSwitch("TOI UU", Config.optimize) { v -> Config.optimize = v; Config.save() }
        addSwitch("BAM DAU", Config.headTrack) { v -> Config.headTrack = v; Config.save() }
        addSwitch("ANTIBAN", Config.antiban) { v -> Config.antiban = v; Config.save() }
        addSwitch("BOOST FPS", Config.boostFps) { v -> Config.boostFps = v; Config.save() }

        addSpace()

        val sp = Spinner(this)
        val games = arrayOf("Free Fire", "Free Fire MAX", "Ca hai")sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        sp.setSelection(Config.gameIndex)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                Config.gameIndex = pos
                Config.save()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        rootLayout.addView(sp)

        addSpace()

        val btnPick = makeBtn("DOI ANH NEN", "#2a2a3a")
        btnPick.setOnClickListener { pickImage() }
        rootLayout.addView(btnPick)

        val btnReset = makeBtn("MAC DINH", "#2a2a3a")
        btnReset.setOnClickListener {
            Config.bgUri = ""
            Config.save()
            loadBg()
        }
        rootLayout.addView(btnReset)

        addSpace()

        val btnOn = makeBtn("ON", "#22C55E")
        val btnOff = makeBtn("OFF", "#EF4444")
        rootLayout.addView(btnOn)
        rootLayout.addView(btnOff)
        paintBtns(btnOn, btnOff)

        loadBg()

        btnOn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Cap quyen Overlay", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (TouchService.instance == null) {
                Toast.makeText(this, "Bat Accessibility", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Cap quyen Write", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, CoreService::class.java))
            running = true
            paintBtns(btnOn, btnOff)
            Toast.makeText(this, "Da BAT v2", Toast.LENGTH_SHORT).show()
        }

        btnOff.setOnClickListener {
            stopService(Intent(this, CoreService::class.java))
            running = false
            paintBtns(btnOn, btnOff)
            Toast.makeText(this, "Da TAT", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCircle(txt: String): TextView {
        val t = TextView(this)
        t.text = txt
        t.setTextColor(Color.WHITE)
        t.textSize = 17f
        t.gravity = Gravity.CENTER
        t.setBackgroundColor(Color.parseColor("#1A0E2A"))
        t.layoutParams = LinearLayout.LayoutParams(200, 200)
        return t
    }

    private fun makeCol(label: String, circle: TextView): LinearLayout {val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.gravity = Gravity.CENTER
        c.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        c.addView(circle)
        val t = TextView(this)
        t.text = label
        t.setTextColor(Color.parseColor("#AAAAAA"))
        t.textSize = 12f
        t.setPadding(0, 10, 0, 0)
        c.addView(t)
        return c
    }

    private fun addSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val sw = Switch(this)
        sw.text = label
        sw.setTextColor(Color.WHITE)
        sw.textSize = 15f
        sw.isChecked = checked
        sw.setPadding(20, 20, 20, 20)
        sw.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> onChange(v) }
        rootLayout.addView(sw)
    }

    private fun makeBtn(text: String, colorHex: String): Button {
        val b = Button(this)
        b.text = text
        b.setTextColor(Color.WHITE)
        b.textSize = 16f
        b.setBackgroundColor(Color.parseColor(colorHex))
        b.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 140)
        return b
    }

    private fun addSpace() {
        val v = View(this)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 30)
        rootLayout.addView(v)
    }

    private fun pickImage() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        startActivityForResult(i, PICK_IMG)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_IMG && res == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            Config.bgUri = uri.toString()
            Config.save()
            loadBg()
        }
    }

    private fun loadBg() {
        if (Config.bgUri.isEmpty()) {
            rootLayout.setBackgroundColor(Color.parseColor("#0a0618"))
            return
        }
        try {
            val uri = Uri.parse(Config.bgUri)
            val input = contentResolver.openInputStream(uri)
            val d: Drawable? = Drawable.createFromStream(input, "bg")
            input?.close()
            if (d != null) rootLayout.background = d
            else rootLayout.setBackgroundColor(Color.parseColor("#0a0618"))
        } catch (e: Exception) {
            rootLayout.setBackgroundColor(Color.parseColor("#0a0618"))
        }
    }

    override fun onResume() {
        super.onResume()
        h.post(tick)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(tick)
    }

    private fun paintBtns(on: Button, off: Button) {
        if (running) {on.setBackgroundColor(Color.parseColor("#22C55E"))
            off.setBackgroundColor(Color.parseColor("#333333"))
        } else {
            on.setBackgroundColor(Color.parseColor("#333333"))
            off.setBackgroundColor(Color.parseColor("#EF4444"))
        }
    }

    private fun cpuPct(): Int {
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
        } catch (e: Exception) { return 0 }
    }

    private fun ramPct(): Int {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        if (mi.totalMem <= 0) return 0
        val used = mi.totalMem - mi.availMem
        return (used * 100L / mi.totalMem).toInt()
    }

    private fun netPct(): Int {
        val now = System.currentTimeMillis()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (lastMs == 0L) {
            lastRx = rx; lastTx = tx; lastMs = now; return 0
        }
        val dt = (now - lastMs).coerceAtLeast(1)
        val db = (rx - lastRx) + (tx - lastTx)
        lastRx = rx; lastTx = tx; lastMs = now
        val kbps = db * 1000L / dt / 1024L
        val v = (kbps * 100L / 500L).toInt()
        return if (v < 0) 0 else if (v > 100) 100 else v
    }
}
