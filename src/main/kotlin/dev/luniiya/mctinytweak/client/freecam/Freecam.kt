package dev.luniiya.mctinytweak.client.freecam

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.mcp.AgentControl
import dev.luniiya.mctinytweak.freecam.FreecamCamera
import dev.luniiya.mctinytweak.mixin.client.mcp.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Flies the view away from the player. It is on while the player toggles it on or an agent has taken over,
 * so the player can watch the agent from anywhere. The agent view key jumps to the agent's eyes and back.
 * Movement reads the physical keys, because key mappings are blocked while freecam or the agent is in charge.
 */
object Freecam {
    @JvmField
    val camera = FreecamCamera()

    /** The player's own switch, from the freecam key or the mod menu. */
    var toggled = false
        private set

    /** While an agent plays, true shows the agent's eyes instead of the freecam. */
    var watchingAgent = false
        private set

    /** Set while the agent takes a screenshot, so it sees through its own eyes. */
    @Volatile
    @JvmStatic
    var screenshotView = false

    /** Counts rendered frames, so a screenshot can wait until the agent's view is on screen. */
    @Volatile
    @JvmStatic
    var frames = 0L
        private set

    fun toggleKey(category: KeyMapping.Category) =
        KeyMapping("key.${McTinyTweak.MOD_ID}.freecam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, category)

    fun agentViewKey(category: KeyMapping.Category) =
        KeyMapping("key.${McTinyTweak.MOD_ID}.agent_view", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category)

    /** True when the view comes from the freecam this frame. */
    @JvmStatic
    fun shown(): Boolean = camera.started && !screenshotView &&
        FreecamCamera.shows(toggled, AgentControl.agentInControl(), watchingAgent)

    /** True when the player's own freecam holds their movement keys, with no agent playing. */
    @JvmStatic
    fun holdsPlayerInput(): Boolean = camera.started && toggled && !AgentControl.agentInControl()

    @JvmStatic
    fun renderedFrame() {
        frames++
    }

    fun toggle() {
        toggled = !toggled
        Minecraft.getInstance().player?.displayClientMessage(
            Component.translatable(if (toggled) "notice.mc_tiny_tweak.freecam.on" else "notice.mc_tiny_tweak.freecam.off"),
            true,
        )
    }

    fun toggleAgentView() {
        if (AgentControl.agentInControl()) watchingAgent = !watchingAgent
    }

    /** Client thread, once per tick. */
    fun tick(minecraft: Minecraft) {
        val player = minecraft.player
        val agent = AgentControl.agentInControl()
        if (!agent) watchingAgent = false
        val on = player != null && FreecamCamera.isOn(toggled, agent)
        if (!on) {
            if (camera.started) {
                camera.stop()
                if (!agent) KeyMapping.setAll()
            }
            if (player == null) toggled = false
            return
        }
        if (!camera.started) {
            val eyes = player!!.getEyePosition(1f)
            camera.startBehind(eyes.x, eyes.y, eyes.z, player.yRot, player.xRot)
            if (!agent) releasePlayerKeys(minecraft)
        }
        if (!shown()) return
        if (minecraft.screen != null) return camera.tick(0f, 0f, 0f, false)
        val options = minecraft.options
        fun down(mapping: KeyMapping) = isPhysicallyDown(minecraft, mapping)
        camera.tick(
            forward = axis(down(options.keyUp), down(options.keyDown)),
            left = axis(down(options.keyLeft), down(options.keyRight)),
            up = axis(down(options.keyJump), down(options.keyShift)),
            fast = down(options.keySprint),
        )
    }

    /** The mappings freecam takes away from the player, so the body stands still while the camera flies. */
    @JvmStatic
    fun heldMappings(minecraft: Minecraft): List<KeyMapping> = with(minecraft.options) {
        listOf(keyUp, keyDown, keyLeft, keyRight, keyJump, keyShift, keySprint, keyAttack, keyUse, keyPickItem, McTinyTweakClient.freelookKey)
    }

    private fun releasePlayerKeys(minecraft: Minecraft) {
        for (mapping in heldMappings(minecraft)) mapping.isDown = false
    }

    private fun axis(positive: Boolean, negative: Boolean) = (if (positive) 1f else 0f) - (if (negative) 1f else 0f)

    private fun isPhysicallyDown(minecraft: Minecraft, mapping: KeyMapping): Boolean {
        val key = (mapping as KeyMappingAccessor).`mc_tiny_tweak$getKey`()
        if (key == InputConstants.UNKNOWN) return false
        val window = minecraft.window
        return when (key.type) {
            InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(window, key.value)
            InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(window.handle(), key.value) == GLFW.GLFW_PRESS
            else -> false
        }
    }
}
