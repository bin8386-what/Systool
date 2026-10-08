package com.dj.systool

import android.content.Context
import android.content.SharedPreferences

object Config {
    var snapPx = 700f
    var pullUp = 6.5f
    var horizHug = 0.05f
    var jitterPx = 0.15f
    var antiShake = true
    var smoothAlpha = 0.72f
    var deadZone = 0.4f
    var tapMaxMs = 270L
    var tapMaxDist = 26f
    var recoilComp = 0.5f

    var aimlock = true
    var headTrack = true
    var boostFps = true
    var boostRam = true
    var fixRung = true
    var optimize = true
    var antiban = true
    var gameIndex = 0

    var bgColor = "#0a0618"
    var bgUri = ""

    private const val P = "hl"
    private var sp: SharedPreferences? = null

    fun init(c: Context) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val s = sp!!
        snapPx = s.getFloat("sp", 700f)
        pullUp = s.getFloat("pu", 6.5f)
        horizHug = s.getFloat("hh", 0.05f)
        jitterPx = s.getFloat("jp", 0.15f)
        antiShake = s.getBoolean("as", true)
        smoothAlpha = s.getFloat("sa", 0.72f)
        deadZone = s.getFloat("dz", 0.4f)
        recoilComp = s.getFloat("rc", 0.5f)
        aimlock = s.getBoolean("al", true)
        headTrack = s.getBoolean("ht", true)
        boostFps = s.getBoolean("bf", true)
        boostRam = s.getBoolean("br", true)
        fixRung = s.getBoolean("fr", true)
        optimize = s.getBoolean("op", true)
        antiban = s.getBoolean("ab", true)
        gameIndex = s.getInt("gi", 0)
        bgColor = s.getString("bgc", "#0a0618") ?: "#0a0618"
        bgUri = s.getString("bgu", "") ?: ""
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
            ?.putString("bgu", bgUri)
            ?.apply()
    }

    fun activePackages(): List<String> = when (gameIndex) {
        0 -> listOf("com.dts.freefireth")
        1 -> listOf("com.dts.freefiremax")
        2 -> listOf("com.dts.freefireth", "com.dts.freefiremax")
        else -> listOf("com.dts.freefireth", "com.dts.freefiremax")
    }
}
