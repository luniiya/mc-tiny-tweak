package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChatLogTest {
    @Test
    fun `lines are numbered and can be read after a known id`() {
        val log = ChatLog()
        assertEquals(0, log.lastId())
        val first = log.add("hello", 1)
        log.add("world", 2)
        assertEquals(1, first.id)
        assertEquals(listOf("world"), log.after(first.id, 50).map { it.text })
        assertEquals(2, log.lastId())
    }

    @Test
    fun `limit keeps the newest lines in order`() {
        val log = ChatLog()
        repeat(5) { log.add("line $it") }
        assertEquals(listOf("line 3", "line 4"), log.after(0, 2).map { it.text })
    }

    @Test
    fun `old lines are dropped past capacity but ids keep counting`() {
        val log = ChatLog(capacity = 3)
        repeat(10) { log.add("line $it") }
        assertEquals(listOf(8L, 9L, 10L), log.after(0, 100).map { it.id })
    }
}
