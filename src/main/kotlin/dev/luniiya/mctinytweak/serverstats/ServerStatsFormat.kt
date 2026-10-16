package dev.luniiya.mctinytweak.serverstats

/** Colors for the pause menu's TPS and latency: green when fine, yellow when a bit off, red when bad. */
object ServerStatsFormat {
    const val GOOD = 0xFF55FF55.toInt()
    const val OKAY = 0xFFFFFF55.toInt()
    const val BAD = 0xFFFF5555.toInt()

    fun tpsColor(tps: Double, tickRate: Float) = when {
        tps >= tickRate * 0.9 -> GOOD
        tps >= tickRate * 0.75 -> OKAY
        else -> BAD
    }

    fun latencyColor(millis: Int) = when {
        millis < 100 -> GOOD
        millis < 250 -> OKAY
        else -> BAD
    }

    fun tps(tps: Double) = "%.1f".format(java.util.Locale.ROOT, tps)
}
