package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RecentLogTest {
    @Test
    fun `keeps only the newest entries, oldest first`() {
        val log = RecentLog<String>(3)
        for ((i, text) in listOf("a", "b", "c", "d").withIndex()) log.add(text, timeMillis = i.toLong())
        assertEquals(listOf("b", "c", "d"), log.snapshot().map { it.value })
        assertEquals(listOf(1L, 2L, 3L), log.snapshot().map { it.timeMillis })
    }

    @Test
    fun `every change bumps the version and clear empties the log`() {
        val log = RecentLog<String>(10)
        val start = log.version
        log.add("a")
        assertTrue(log.version > start)
        val added = log.version
        log.clear()
        assertTrue(log.version > added)
        assertTrue(log.snapshot().isEmpty())
    }
}
