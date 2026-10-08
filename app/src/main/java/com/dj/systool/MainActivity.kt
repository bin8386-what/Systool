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
    private lateinit var rootLayout: View

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
        setContentView(R.layout.activity_main)
        Config.init(this)

        rootLayout = findViewById(R.id.rootLayout)
        tvCpu = findViewById(R.id.tvCpu)
        tvRam = findViewById(R.id.tvRam)
        tvNet = findViewById(R.id.tvNet)

        loadBg()

        val swAim = findViewById<Switch>(R.id.swAimlock)
        val swHead = findViewById<Switch>(R.id.swHead)
        val swAnti = findViewById<Switch>(R.id.swAntiShake)
        val swFps = findViewById<Switch>(R.id.swBoostFps)
        val swBoost = findViewById<Switch>(R.id.swBoost)
        val swRung = findViewById<Switch>(R.id.swFixRung)
        val swOpt = findViewById<Switch>(R.id.swOptimize)
        val swBan = findViewById<Switch>(R.id.swAntiban)

        swAim.isChecked = Config.aimlock
        swHead.isChecked = Config.headTrack
        swAnti.isChecked = Config.antiShake
        swFps.isChecked = Config.boostFps
        swBoost.isChecked = Config.boostRam
        swRung.isChecked = Config.fixRung
        swOpt.isChecked = Config.optimize
        swBan.isChecked = Config.antiban

        swAim.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.aimlock = v; Config.save() }
        swHead.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.headTrack = v; Config.save() }
        swAnti.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.antiShake = v; Config.save() }
        swFps.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.boostFps = v; Config.save() }
        swBoost.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.boostRam = v; Config.save() }swRung.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.fixRung = v; Config.save() }
        swOpt.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.optimize = v; Config.save() }
        swBan.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.antiban = v; Config.save() }

        val sp = findViewById<Spinner>(R.id.spGame)
        val games = arrayOf("Free Fire", "Free Fire MAX", "Ca hai")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        sp.setSelection(Config.gameIndex)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                Config.gameIndex = pos
                Config.save()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnPickBg).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.btnResetBg).setOnClickListener {
            Config.bgUri = ""
            Config.save()
            loadBg()
            Toast.makeText(this, "Da reset", Toast.LENGTH_SHORT).show()
        }

        val btnOn = findViewById<Button>(R.id.btnOn)
        val btnOff = findViewById<Button>(R.id.btnOff)
        paintBtns(btnOn, btnOff)

        btnOn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Cap quyen Overlay truoc", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (TouchService.instance == null) {
                Toast.makeText(this, "Bat Accessibility truoc", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Cap quyen Write Settings", Toast.LENGTH_LONG).show()
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

    private fun pickImage() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        startActivityForResult(i, PICK_IMG)
    }override fun onActivityResult(req: Int, res: Int, data: Intent?) {
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
            Toast.makeText(this, "Da doi anh nen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBg() {
        if (Config.bgUri.isEmpty()) {
            rootLayout.setBackgroundResource(R.drawable.bg_main)
            return
        }
        try {
            val uri = Uri.parse(Config.bgUri)
            val input = contentResolver.openInputStream(uri)
            val d: Drawable? = Drawable.createFromStream(input, "bg")
            input?.close()
            if (d != null) rootLayout.background = d
            else rootLayout.setBackgroundResource(R.drawable.bg_main)
        } catch (e: Exception) {
            rootLayout.setBackgroundResource(R.drawable.bg_main)
        }
    }

    override fun onResume() {
        super.onResume()
        h.post(tick)
        val tv = findViewById<TextView>(R.id.tvStatus)
        val acc = if (TouchService.instance != null) "OK" else "X"
        val ov = if (Settings.canDrawOverlays(this)) "OK" else "X"
        val ws = if (Settings.System.canWrite(this)) "OK" else "X"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val dnd = if (nm.isNotificationPolicyAccessGranted) "OK" else "X"
        tv.text = "Acc:$acc Overlay:$ov Write:$ws DND:$dnd"
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(tick)
    }

    private fun paintBtns(on: Button, off: Button) {
        if (running) {
            on.background = ContextCompat.getDrawable(this, R.drawable.btn_green)
            off.background = ContextCompat.getDrawable(this, R.drawable.btn_gray)
            on.setTextColor(Color.WHITE)
            off.setTextColor(Color.parseColor("#777777"))
        } else {
            on.background = ContextCompat.getDrawable(this, R.drawable.btn_gray)
            off.background = ContextCompat.getDrawable(this, R.drawable.btn_red)
            on.setTextColor(Color.parseColor("#777777"))
            off.setTextColor(Color.WHITE)
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
            lastCpuIdle = idleif (dT <= 0) return 0
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
