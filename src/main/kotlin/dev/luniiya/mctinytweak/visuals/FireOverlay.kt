package dev.luniiya.mctinytweak.visuals

/** The burning overlay is lowered so it covers less of the screen. */
object FireOverlay {
    /** How far below vanilla's spot (y -0.3) the two fire sprites sit. Larger values show less fire. */
    const val EXTRA_DROP = 0.3f

    @JvmStatic
    fun lowered(y: Float) = y - EXTRA_DROP
}
