package com.dj.systool

import android.content.Context
import android.content.SharedPreferences

object Config {
    var snapPx = 180f
    var pullUp = 0.85f
    var horizHug = 0.20f
    var jitterPx = 2.0f
    var microAmp = 4f
    var microMs = 80L
    var padSize = 700
    var padX = 40
    var padY = 90
    var aimlock = true
    var boostRam = true
    var headTrack = true
    var antiban = true
    var gameIndex = 0
    private const val P = "st"
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val s = sp!!
        aimlock = s.getBoolean("al", true)
        boostRam = s.getBoolean("br", true)
        headTrack = s.getBoolean("ht", true)
        antiban = s.getBoolean("ab", true)
        gameIndex = s.getInt("gi", 0)
    }

    fun save() {
        sp?.edit()?.putBoolean("al", aimlock)?.putBoolean("br", boostRam)
            ?.putBoolean("ht", headTrack)?.putBoolean("ab", antiban)
            ?.putInt("gi", gameIndex)?.apply()
    }

    fun activePackages(): List<String> = when (gameIndex) {
        0 -> listOf("com.dts.freefireth")
        1 -> listOf("com.dts.freefiremax")
        2 -> listOf("com.dts.freefireth", "com.dts.freefiremax")
        else -> listOf("com.dts.freefireth", "com.dts.freefiremax")
    }
}
