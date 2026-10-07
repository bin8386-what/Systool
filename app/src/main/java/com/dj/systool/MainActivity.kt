package com.dj.systool

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var running = false

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        Config.init(this)

        val swAim = findViewById<Switch>(R.id.swAimlock)
        val swHead = findViewById<Switch>(R.id.swHead)
        val swBoost = findViewById<Switch>(R.id.swBoost)
        val swRung = findViewById<Switch>(R.id.swFixRung)
        val swOpt = findViewById<Switch>(R.id.swOptimize)
        val swAnti = findViewById<Switch>(R.id.swAntiban)

        swAim.isChecked = Config.aimlock
        swHead.isChecked = Config.headTrack
        swBoost.isChecked = Config.boostRam
        swRung.isChecked = Config.fixRung
        swOpt.isChecked = Config.optimize
        swAnti.isChecked = Config.antiban

        swAim.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.aimlock = v; Config.save() }
        swHead.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.headTrack = v; Config.save() }
        swBoost.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.boostRam = v; Config.save() }
        swRung.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.fixRung = v; Config.save() }
        swOpt.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.optimize = v; Config.save() }
        swAnti.setOnCheckedChangeListener { _: CompoundButton, v: Boolean -> Config.antiban = v; Config.save() }

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

        updateButtons(btnOn, btnOff)

        btnOn.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Can cap quyen Overlay", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            if (TouchService.instance == null) {Toast.makeText(this, "Bat Accessibility truoc", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, CoreService::class.java))
            running = true
            updateButtons(btnOn, btnOff)
            Toast.makeText(this, "Da BAT", Toast.LENGTH_SHORT).show()
        }

        btnOff.setOnClickListener {
            stopService(Intent(this, CoreService::class.java))
            running = false
            updateButtons(btnOn, btnOff)
            Toast.makeText(this, "Da TAT", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtons(on: Button, off: Button) {
        if (running) {
            on.setBackgroundColor(Color.parseColor("#4CAF50"))
            on.setTextColor(Color.WHITE)
            off.setBackgroundColor(Color.parseColor("#444444"))
            off.setTextColor(Color.parseColor("#888888"))
        } else {
            on.setBackgroundColor(Color.parseColor("#444444"))
            on.setTextColor(Color.parseColor("#888888"))
            off.setBackgroundColor(Color.parseColor("#F44336"))
            off.setTextColor(Color.WHITE)
        }
    }
}
