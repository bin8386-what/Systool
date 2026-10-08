package com.dj.systool

import android.content.Context
import android.content.SharedPreferences

object Config {
    var snapPx = 800f
    var pullUp = 7.5f
    var horizHug = 0.04f
    var jitterPx = 0.1f
    var antiShake = true
    var smoothAlpha = 0.75f
    var deadZone = 0.35f
    var tapMaxMs = 280L
    var tapMaxDist = 28f
    var recoilComp = 0.6f

    var aimlock = true
    var headTrack = true
    var boostFps = true
    var boostRam = true
    var fixRung = true
    var optimize = true
    var antiban = true
    var gameIndex = 0

    var bgColor = "#0a0618"

    private const val P = "hl"
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val s = sp!!
        snapPx = s.getFloat("sp", 800f)
        pullUp = s.getFloat("pu", 7.5f)
        horizHug = s.getFloat("hh", 0.04f)
        jitterPx = s.getFloat("jp", 0.1f)
        antiShake = s.getBoolean("as", true)
        smoothAlpha = s.getFloat("sa", 0.75f)
        deadZone = s.getFloat("dz", 0.35f)
        recoilComp = s.getFloat("rc", 0.6f)
        aimlock = s.getBoolean("al", true)
        headTrack = s.getBoolean("ht", true)
        boostFps = s.getBoolean("bf", true)
        boostRam = s.getBoolean("br", true)
        fixRung = s.getBoolean("fr", true)
        optimize = s.getBoolean("op", true)
        antiban = s.getBoolean("ab", true)
        gameIndex = s.getInt("gi", 0)
        bgColor = s.getString("bgc", "#0a0618") ?: "#0a0618"
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
            ?.putBoolean("ab", antiban)?.putInt("gi", gameIndex)
            ?.putString("bgc", bgColor)
            ?.apply()
    }

    fun activePackages(): List<String> = when (gameIndex) {
        0 -> listOf("com.dts.freefireth")
        1 -> listOf("com.dts.freefiremax")
        2 -> listOf("com.dts.freefireth", "com.dts.freefiremax")
        else -> listOf("com.dts.freefireth", "com.dts.freefiremax")
    }
}
