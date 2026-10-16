package dev.luniiya.mctinytweak.serverstats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TpsEstimatorTest {
    private val second = 1_000_000_000L

    @Test
    fun `a healthy server reads 20, never more`() {
        val tps = TpsEstimator()
        assertNull(tps.tps(20f))
        tps.add(1000, 0)
        tps.add(1020, second)
        assertEquals(20.0, tps.tps(20f)!!, 0.001)
        // An update that arrives early must not show 25 TPS.
        tps.add(1040, second + second * 6 / 10)
        assertEquals(20.0, tps.tps(20f)!!, 0.001)
    }

    @Test
    fun `a lagging server reads its real rate`() {
        val tps = TpsEstimator()
        for (i in 0..5) tps.add(i * 20L, i * 2 * second)
        assertEquals(10.0, tps.tps(20f)!!, 0.001)
    }

    @Test
    fun `old updates leave the window`() {
        val tps = TpsEstimator(windowNanos = 5 * second)
        for (i in 0..5) tps.add(i * 20L, i * 2 * second) // 10 TPS for 10 seconds
        for (i in 1..6) tps.add(100 + i * 20L, 10 * second + i * second) // then 20 TPS
        assertEquals(20.0, tps.tps(20f)!!, 0.001)
    }

    @Test
    fun `setting the time does not count as ticking`() {
        val tps = TpsEstimator()
        tps.add(1000, 0)
        tps.add(1020, second)
        tps.add(50_000, 2 * second)
        assertNull(tps.tps(20f))
        tps.add(50_020, 3 * second)
        assertEquals(20.0, tps.tps(20f)!!, 0.001)
    }

    @Test
    fun `colors follow the tick rate and ping`() {
        assertEquals(ServerStatsFormat.GOOD, ServerStatsFormat.tpsColor(19.5, 20f))
        assertEquals(ServerStatsFormat.OKAY, ServerStatsFormat.tpsColor(16.0, 20f))
        assertEquals(ServerStatsFormat.BAD, ServerStatsFormat.tpsColor(8.0, 20f))
        assertEquals(ServerStatsFormat.GOOD, ServerStatsFormat.latencyColor(40))
        assertEquals(ServerStatsFormat.BAD, ServerStatsFormat.latencyColor(400))
        assertEquals("19.5", ServerStatsFormat.tps(19.46))
    }
}
