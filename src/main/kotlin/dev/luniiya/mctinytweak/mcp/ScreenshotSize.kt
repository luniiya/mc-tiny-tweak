package dev.luniiya.mctinytweak.mcp

/** Vanilla capture requires divisible dimensions; otherwise resize the full capture afterwards. */
data class ScreenshotSize(val width: Int, val height: Int, val captureScale: Int) {
    companion object {
        fun plan(width: Int, height: Int, downscale: Int): ScreenshotSize {
            require(width > 0 && height > 0 && downscale in 1..4)
            val captureScale = if (width % downscale == 0 && height % downscale == 0) downscale else 1
            return ScreenshotSize((width / downscale).coerceAtLeast(1), (height / downscale).coerceAtLeast(1), captureScale)
        }
    }
}
