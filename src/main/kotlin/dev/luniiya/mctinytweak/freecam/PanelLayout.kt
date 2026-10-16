package dev.luniiya.mctinytweak.freecam

/**
 * Where an agent's container screen goes while the player watches from freecam: shrunk into the top right corner,
 * under the status card, so the player keeps flying and still sees what the agent does in the menu.
 */
data class PanelLayout(val x: Int, val y: Int, val width: Int, val height: Int, val scale: Float) {
    /** Moves a point from the screen's own layout into the panel. */
    fun mapX(screenX: Int, leftPos: Int) = x + (screenX - leftPos) * scale
    fun mapY(screenY: Int, topPos: Int) = y + (screenY - topPos) * scale

    companion object {
        const val SCALE = 0.5f
        const val MARGIN = 6

        fun place(guiWidth: Int, top: Int, imageWidth: Int, imageHeight: Int, scale: Float = SCALE): PanelLayout {
            val width = Math.round(imageWidth * scale)
            val height = Math.round(imageHeight * scale)
            return PanelLayout(guiWidth - width - MARGIN, top, width, height, scale)
        }
    }
}
