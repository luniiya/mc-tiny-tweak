package dev.luniiya.mctinytweak.mcp

import dev.luniiya.mctinytweak.mcp.MiningTools.Candidate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MiningToolsTest {
    @Test
    fun `an empty selected hand switches to the available pickaxe`() {
        assertEquals(7, MiningTools.best(listOf(Candidate(0, 1f, false), Candidate(7, 4f, true, 100)), 0, true)?.slot)
    }

    @Test
    fun `fast tools that cannot harvest ore are never chosen over a correct tool`() {
        val gold = Candidate(0, 12f, false, 20)
        val iron = Candidate(12, 6f, true, 200)
        assertEquals(iron, MiningTools.best(listOf(gold, iron), 0, true))
        assertNull(MiningTools.best(listOf(gold), 0, true))
    }

    @Test
    fun `axes and shovels can be selected from main inventory`() {
        val hand = Candidate(0, 1f, true)
        val fast = Candidate(20, 8f, true, 100)
        assertEquals(fast, MiningTools.best(listOf(hand, fast), 0, false))
    }

    @Test
    fun `avoid needless wear and hotbar swaps when mining speed is tied`() {
        val tool = Candidate(0, 1f, true, 80)
        val hand = Candidate(1, 1f, true)
        assertEquals(hand, MiningTools.best(listOf(tool, hand), 0, false))
        assertEquals(3, MiningTools.best(listOf(Candidate(3, 6f, true, 60), Candidate(15, 6f, true, 100)), 3, true)?.slot)
    }

    @Test
    fun `broken tools invalid slots and invalid speeds cannot be selected`() {
        assertNull(MiningTools.best(listOf(Candidate(0, 5f, true, 0), Candidate(40, 10f, true), Candidate(1, Float.NaN, true)), 0, true))
    }
}
