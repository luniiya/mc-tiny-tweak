package dev.luniiya.mctinytweak.serverstats

/**
 * Estimates a server's ticks per second from its time updates, which vanilla servers send every
 * 20 ticks with the world's game time. Servers send nothing else about TPS, and the mod is client only.
 */
class TpsEstimator(private val windowNanos: Long = 10_000_000_000L) {
    private class Sample(val gameTime: Long, val nanos: Long)

    private val samples = ArrayDeque<Sample>()

    fun add(gameTime: Long, nanos: Long) {
        val last = samples.lastOrNull()
        // Time going back or jumping far (`/time`, a new world) breaks the history.
        if (last != null && (gameTime < last.gameTime || gameTime - last.gameTime > MAX_STEP || nanos <= last.nanos)) samples.clear()
        samples.addLast(Sample(gameTime, nanos))
        while (samples.size > 2 && nanos - samples.first().nanos > windowNanos) samples.removeFirst()
    }

    fun clear() = samples.clear()

    /** Ticks per second, at most [tickRate], or null until two updates came in. */
    fun tps(tickRate: Float): Double? {
        if (samples.size < 2) return null
        val ticks = samples.last().gameTime - samples.first().gameTime
        val seconds = (samples.last().nanos - samples.first().nanos) / 1e9
        if (seconds <= 0) return null
        // Updates arrive a little unevenly, so a healthy server can look slightly above its rate.
        return (ticks / seconds).coerceAtMost(tickRate.toDouble())
    }

    companion object {
        /** More than this many ticks between two updates is a time change, not ticking. */
        const val MAX_STEP = 200L
    }
}
