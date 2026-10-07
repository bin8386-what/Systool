package com.dj.systool

import android.content.Context
import android.content.SharedPreferences

object Config {
    var snapPx = 320f
    var pullUp = 2.8f
    var horizHug = 0.15f
    var jitterPx = 1.5f
    var microAmp = 8f
    var microMs = 60L
    var padSize = 700
    var padX = 40
    var padY = 90
    var aimlock = true
    var headTrack = true
    var boostRam = true
    var fixRung = true
    var optimize = true
    var antiban = true
    var gameIndex = 0
    private const val P = "st"
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val s = sp!!
        snapPx = s.getFloat("sp", 320f)
        pullUp = s.getFloat("pu", 2.8f)
        jitterPx = s.getFloat("jp", 1.5f)
        microAmp = s.getFloat("ma", 8f)
        aimlock = s.getBoolean("al", true)
        headTrack = s.getBoolean("ht", true)
        boostRam = s.getBoolean("br", true)
        fixRung = s.getBoolean("fr", true)
        optimize = s.getBoolean("op", true)
        antiban = s.getBoolean("ab", true)
        gameIndex = s.getInt("gi", 0)
    }

    fun save() {
        sp?.edit()?.putFloat("sp", snapPx)?.putFloat("pu", pullUp)
            ?.putFloat("jp", jitterPx)?.putFloat("ma", microAmp)
            ?.putBoolean("al", aimlock)?.putBoolean("ht", headTrack)
            ?.putBoolean("br", boostRam)?.putBoolean("fr", fixRung)
            ?.putBoolean("op", optimize)?.putBoolean("ab", antiban)
            ?.putInt("gi", gameIndex)?.apply()
    }

    fun activePackages(): List<String> = when (gameIndex) {
        0 -> listOf("com.dts.freefireth")
        1 -> listOf("com.dts.freefiremax")
        2 -> listOf("com.dts.freefireth", "com.dts.freefiremax")
        else -> listOf("com.dts.freefireth", "com.dts.freefiremax")
    }
}
