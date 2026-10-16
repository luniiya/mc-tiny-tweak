package dev.luniiya.mctinytweak.freecam

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PanelLayoutTest {
    @Test
    fun `a chest shrinks to half size in the top right corner`() {
        val panel = PanelLayout.place(guiWidth = 480, top = 60, imageWidth = 176, imageHeight = 166)
        assertEquals(88, panel.width)
        assertEquals(83, panel.height)
        assertEquals(480 - 88 - PanelLayout.MARGIN, panel.x)
        assertEquals(60, panel.y)
    }

    @Test
    fun `the screen's top left corner lands on the panel's`() {
        val panel = PanelLayout.place(480, 60, 176, 166)
        assertEquals(panel.x.toFloat(), panel.mapX(152, leftPos = 152))
        assertEquals(panel.y.toFloat(), panel.mapY(40, topPos = 40))
        assertEquals(panel.x + 88f, panel.mapX(152 + 176, leftPos = 152))
    }
}
