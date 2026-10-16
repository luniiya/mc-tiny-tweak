package dev.luniiya.mctinytweak.visuals

import net.minecraft.client.renderer.ScreenEffectRenderer
import net.minecraft.client.renderer.blockentity.BannerRenderer
import net.minecraft.client.renderer.special.ShieldSpecialRenderer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VisualTweaksTest {
    @AfterEach
    fun reset() {
        ShieldOpacity.renderingHeld = false
    }

    @Test
    fun `fire sits lower than vanilla`() {
        assertTrue(FireOverlay.lowered(-0.3f) < -0.3f)
        assertEquals(-0.3f - FireOverlay.EXTRA_DROP, FireOverlay.lowered(-0.3f))
    }

    @Test
    fun `opacity scales only the alpha channel`() {
        assertEquals(0x99FFFFFF.toInt(), ShieldOpacity.withOpacity(-1, 0.6f))
        assertEquals(0x4D_12_34_56, ShieldOpacity.withOpacity(0x80123456.toInt(), 0.6f))
        assertEquals(0x00FFFFFF, ShieldOpacity.withOpacity(-1, -1f), "opacity is clamped at invisible")
    }

    @Test
    fun `only held shields are faded`() {
        assertEquals(-1, ShieldOpacity.tint(-1), "banners and shields outside first person keep their color")
        ShieldOpacity.renderingHeld = true
        assertEquals(ShieldOpacity.withOpacity(-1, ShieldOpacity.OPACITY), ShieldOpacity.tint(-1))
        assertTrue(ShieldOpacity.OPACITY in 0.3f..0.9f, "partly see-through, not invisible")
    }

    @Test
    fun `visual mixins apply to the vanilla renderers`() {
        fun has(type: Class<*>, handler: String) = type.declaredMethods.any { it.name.contains(handler) }
        assertTrue(has(ScreenEffectRenderer::class.java, "lowerFire"))
        assertTrue(has(ShieldSpecialRenderer::class.java, "translucentShield"))
        assertTrue(has(ShieldSpecialRenderer::class.java, "fadeShield"))
        assertTrue(has(BannerRenderer::class.java, "translucentShieldBase"))
        assertTrue(has(BannerRenderer::class.java, "fadeShieldLayers"))
    }
}
