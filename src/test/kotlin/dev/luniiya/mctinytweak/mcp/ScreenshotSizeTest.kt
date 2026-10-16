package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScreenshotSizeTest {
    @Test
    fun `nondivisible windows still produce a downscaled screenshot`() {
        assertEquals(ScreenshotSize(472, 262, 1), ScreenshotSize.plan(1890, 1050, 4))
        assertEquals(ScreenshotSize(426, 240, 1), ScreenshotSize.plan(1280, 720, 3))
    }

    @Test
    fun `divisible windows keep the efficient vanilla capture path`() {
        assertEquals(ScreenshotSize(945, 525, 2), ScreenshotSize.plan(1890, 1050, 2))
        assertEquals(ScreenshotSize(1890, 1050, 1), ScreenshotSize.plan(1890, 1050, 1))
    }

    @Test
    fun `small framebuffers never produce empty images`() {
        assertEquals(ScreenshotSize(1, 1, 1), ScreenshotSize.plan(1, 1, 4))
        assertThrows(IllegalArgumentException::class.java) { ScreenshotSize.plan(100, 100, 0) }
    }
}
