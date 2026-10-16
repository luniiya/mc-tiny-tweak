package dev.luniiya.mctinytweak.visuals

/** Lights the whole world like night vision does, without the effect or its flicker when it runs out. */
object Fullbright {
    /** The night vision strength handed to the lightmap shader: full while on, vanilla's value otherwise. */
    @JvmStatic
    fun nightVision(vanilla: Float, enabled: Boolean) = if (enabled) 1f else vanilla
}
