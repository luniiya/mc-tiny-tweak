package dev.luniiya.mctinytweak.client.freelook

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.freelook.FreelookCamera
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft

/** Hold the freelook key to look around without turning the player. Mixins read [camera] to redirect the view. */
object Freelook {
    @JvmField
    val camera = FreelookCamera()

    fun keyMapping(category: KeyMapping.Category) = KeyMapping(
        "key.${McTinyTweak.MOD_ID}.freelook",
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_MIDDLE,
        category,
    )

    fun tick(minecraft: Minecraft, key: KeyMapping) {
        val player = minecraft.player
        val held = key.isDown && player != null
        if (held && !camera.active) {
            minecraft.options.cameraType = camera.start(player.yRot, player.xRot, minecraft.options.cameraType)
        } else if (!held && camera.active) {
            camera.stop()?.let { minecraft.options.cameraType = it }
        }
    }

    /** True when the camera should follow freelook instead of the given entity's rotation. */
    @JvmStatic
    fun overrides(entity: Any?) = camera.active && entity === Minecraft.getInstance().player
}
