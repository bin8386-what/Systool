package com.dj.systool

import android.content.Context
import android.content.SharedPreferences

object Config {
    // AIM — max
    var snapPx = 620f
    var pullUp = 5.5f
    var horizHug = 0.06f
    var jitterPx = 0.2f
    var antiShake = true
    var smoothAlpha = 0.68f
    var deadZone = 0.5f
    var tapMaxMs = 260L
    var tapMaxDist = 24f
    var recoilComp = 0.4f

    // switches
    var aimlock = true
    var headTrack = true
    var boostFps = true
    var boostRam = true
    var fixRung = true
    var optimize = true
    var antiban = true
    var gameIndex = 0

    private const val P = "hl"
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val s = sp!!
        snapPx = s.getFloat("sp", 620f)
        pullUp = s.getFloat("pu", 5.5f)
        horizHug = s.getFloat("hh", 0.06f)
        jitterPx = s.getFloat("jp", 0.2f)
        antiShake = s.getBoolean("as", true)
        smoothAlpha = s.getFloat("sa", 0.68f)
        deadZone = s.getFloat("dz", 0.5f)
        recoilComp = s.getFloat("rc", 0.4f)
        aimlock = s.getBoolean("al", true)
        headTrack = s.getBoolean("ht", true)
        boostFps = s.getBoolean("bf", true)
        boostRam = s.getBoolean("br", true)
        fixRung = s.getBoolean("fr", true)
        optimize = s.getBoolean("op", true)
        antiban = s.getBoolean("ab", true)
        gameIndex = s.getInt("gi", 0)
    }

    fun save() {
        sp?.edit()
            ?.putFloat("sp", snapPx)?.putFloat("pu", pullUp)
            ?.putFloat("hh", horizHug)?.putFloat("jp", jitterPx)
            ?.putBoolean("as", antiShake)
            ?.putFloat("sa", smoothAlpha)?.putFloat("dz", deadZone)
            ?.putFloat("rc", recoilComp)
            ?.putBoolean("al", aimlock)?.putBoolean("ht", headTrack)
            ?.putBoolean("bf", boostFps)?.putBoolean("br", boostRam)
            ?.putBoolean("fr", fixRung)?.putBoolean("op", optimize)
            ?.putBoolean("ab", antiban)?.putInt("gi", gameIndex)?.apply()
    }

    fun activePackages(): List<String> = when (gameIndex) {
        0 -> listOf("com.dts.freefireth")
        1 -> listOf("com.dts.freefiremax")
        2 -> listOf("com.dts.freefireth", "com.dts.freefiremax")
        else -> listOf("com.dts.freefireth", "com.dts.freefiremax")
    }
}
