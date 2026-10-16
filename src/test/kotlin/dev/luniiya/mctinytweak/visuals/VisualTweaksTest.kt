package dev.luniiya.mctinytweak.visuals

import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.ScreenEffectRenderer
import net.minecraft.client.renderer.blockentity.BannerRenderer
import net.minecraft.client.renderer.special.ShieldSpecialRenderer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.lwjgl.glfw.GLFW

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
    fun `fullbright gives the lightmap full night vision only while on`() {
        assertEquals(1f, Fullbright.nightVision(0f, enabled = true))
        assertEquals(1f, Fullbright.nightVision(0.3f, enabled = true), "a fading night vision effect stays full")
        assertEquals(0f, Fullbright.nightVision(0f, enabled = false))
        assertEquals(0.3f, Fullbright.nightVision(0.3f, enabled = false), "vanilla night vision is untouched")
    }

    @Test
    fun `fullbright key defaults to G`() {
        val key = McTinyTweakClient.fullbrightKey
        assertEquals(GLFW.GLFW_KEY_G, key.defaultKey.value)
        assertEquals("key.keyboard.g", PersonalDefaults.keybinds["key.mc_tiny_tweak.fullbright"])
    }

    @Test
    fun `visual mixins apply to the vanilla renderers`() {
        fun has(type: Class<*>, handler: String) = type.declaredMethods.any { it.name.contains(handler) }
        assertTrue(has(ScreenEffectRenderer::class.java, "lowerFire"))
        assertTrue(has(ShieldSpecialRenderer::class.java, "translucentShield"))
        assertTrue(has(ShieldSpecialRenderer::class.java, "fadeShield"))
        assertTrue(has(BannerRenderer::class.java, "translucentShieldBase"))
        assertTrue(has(BannerRenderer::class.java, "fadeShieldLayers"))
        assertTrue(has(LightTexture::class.java, "fullbright"))
    }
}
