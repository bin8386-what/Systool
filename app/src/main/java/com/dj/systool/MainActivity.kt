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
        val swBoost = findViewById<Switch>(R.id.swBoost)
        val swHead = findViewById<Switch>(R.id.swHead)
        val swAnti = findViewById<Switch>(R.id.swAnti)

        swAim.isChecked = Config.aimlock
        swBoost.isChecked = Config.boostRam
        swHead.isChecked = Config.headTrack
        swAnti.isChecked = Config.antiban

        swAim.setOnCheckedChangeListener { _, v -> Config.aimlock = v; Config.save() }
        swBoost.setOnCheckedChangeListener { _, v -> Config.boostRam = v; Config.save() }
        swHead.setOnCheckedChangeListener { _, v -> Config.headTrack = v; Config.save() }
        swAnti.setOnCheckedChangeListener { _, v -> Config.antiban = v; Config.save() }

        val sp = findViewById<Spinner>(R.id.spGame)
        val games = arrayOf("Free Fire", "Free Fire MAX", "Ca hai")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, games)
        sp.setSelection(Config.gameIndex)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                Config.gameIndex = pos; Config.save()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnOn).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Can cap quyen Overlay", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (TouchService.instance == null) {
                Toast.makeText(this, "Bat Accessibility truoc", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, CoreService::class.java))
            Toast.makeText(this, "Da BAT", Toast.LENGTH_SHORT).show()
            finish()
        }
        findViewById<Button>(R.id.btnOff).setOnClickListener {
            stopService(Intent(this, CoreService::class.java))
            Toast.makeText(this, "Da TAT", Toast.LENGTH_SHORT).show()
        }
    }
}
