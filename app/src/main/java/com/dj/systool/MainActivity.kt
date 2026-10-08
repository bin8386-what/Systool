package com.dj.systool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        Config.init(this)

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
        swBoost.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.boostRam = v; Config.save() }
        swRung.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.fixRung = v; Config.save() }
        swOpt.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.optimize = v; Config.save() }
        swBan.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.antiban = v; Config.save() }

        val sp = findViewById<Spinner>(R.id.spGame)
        val games = arrayOf("Free Fire", "Free Fire MAX", "Ca hai")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        sp.setSelection(Config.gameIndex)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                Config.gameIndex = pos
                Config.save()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val btnOn = findViewById<Button>(R.id.btnOn)
        val btnOff = findViewById<Button>(R.id.btnOff)

        btnOn.setOnClickListener {
            val accOk = TouchService.instance != nullval ovOk = Settings.canDrawOverlays(this)
            val wsOk = Settings.System.canWrite(this)

            if (!ovOk) {
                Toast.makeText(this, "Cap quyen Overlay truoc", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (!accOk) {
                Toast.makeText(this, "Bat Accessibility truoc", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            if (!wsOk) {
                Toast.makeText(this, "Cap quyen Write Settings", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, CoreService::class.java))
            Toast.makeText(this, "Da BAT v2", Toast.LENGTH_SHORT).show()
        }

        btnOff.setOnClickListener {
            stopService(Intent(this, CoreService::class.java))
            Toast.makeText(this, "Da TAT", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val tv = findViewById<TextView>(R.id.tvStatus)
        val acc = if (TouchService.instance != null) "OK" else "X"
        val ov = if (Settings.canDrawOverlays(this)) "OK" else "X"
        val ws = if (Settings.System.canWrite(this)) "OK" else "X"
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        val dnd = if (nm.isNotificationPolicyAccessGranted) "OK" else "X"
        tv.text = "Acc:$acc | Overlay:$ov | Write:$ws | DND:$dnd"
    }
}
