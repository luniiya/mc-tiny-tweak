package dev.luniiya.mctinytweak.visuals

import kotlin.math.roundToInt

/**
 * Held shields render partly see-through in first person. The shield renderer sets [renderingHeld] while it
 * submits a first person shield so the banner pattern code, which shields share with banners, can fade too.
 */
object ShieldOpacity {
    /** 1 is vanilla, 0 is invisible. */
    const val OPACITY = 0.6f

    @JvmStatic
    var renderingHeld = false

    /** Fades an ARGB tint while a held shield is being submitted, and leaves it alone otherwise. */
    @JvmStatic
    fun tint(argb: Int) = if (renderingHeld) withOpacity(argb, OPACITY) else argb

    fun withOpacity(argb: Int, opacity: Float): Int {
        val alpha = ((argb ushr 24) * opacity.coerceIn(0f, 1f)).roundToInt()
        return (alpha shl 24) or (argb and 0xFFFFFF)
    }
}
