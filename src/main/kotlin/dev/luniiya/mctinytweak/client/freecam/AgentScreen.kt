package dev.luniiya.mctinytweak.client.freecam

import com.mojang.blaze3d.platform.InputConstants
import dev.luniiya.mctinytweak.client.McTinyTweakClient
import dev.luniiya.mctinytweak.client.ModMenuScreen
import dev.luniiya.mctinytweak.client.mcp.AgentControl
import dev.luniiya.mctinytweak.freecam.PanelLayout
import dev.luniiya.mctinytweak.mixin.client.freecam.AbstractContainerScreenAccessor
import dev.luniiya.mctinytweak.mixin.client.freecam.MouseHandlerAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

/**
 * Containers the agent opens while it plays. They never take the player's mouse or keys: in freecam they are
 * drawn small under the status card, and in agent view full size, both without the player's cursor.
 */
object AgentScreen {
    /** Mouse position given to agent screens, so nothing looks hovered by the player's hidden cursor. */
    const val NO_MOUSE = -10_000

    /** True while a panel is being drawn, so the screen skips its darkened full-window background. */
    @JvmStatic
    var drawingPanel = false
        private set

    /** True when the open screen is the agent's: a container while the agent is in control. */
    @JvmStatic
    fun open(): Boolean = open(Minecraft.getInstance().screen)

    @JvmStatic
    fun open(screen: Screen?): Boolean = AgentControl.agentInControl() && screen is AbstractContainerScreen<*>

    /** Draws the agent's screen, as a panel in freecam or full size in agent view. */
    @JvmStatic
    fun render(screen: Screen, graphics: GuiGraphics, partialTick: Float, draw: Runnable) {
        if (!Freecam.shown()) return draw.run()
        val accessor = screen as AbstractContainerScreenAccessor
        val panel = PanelLayout.place(
            graphics.guiWidth(), AgentStatusCard.bottom + 8,
            accessor.`mc_tiny_tweak$getImageWidth`(), accessor.`mc_tiny_tweak$getImageHeight`(),
        )
        TooltipRenderUtil.renderTooltipBackground(graphics, panel.x, panel.y, panel.width, panel.height, null)
        graphics.enableScissor(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height)
        graphics.pose().pushMatrix()
        graphics.pose().translate(panel.mapX(0, accessor.`mc_tiny_tweak$getLeftPos`()), panel.mapY(0, accessor.`mc_tiny_tweak$getTopPos`()))
        graphics.pose().scale(panel.scale, panel.scale)
        drawingPanel = true
        try {
            draw.run()
        } finally {
            drawingPanel = false
            graphics.pose().popMatrix()
            graphics.disableScissor()
        }
    }

    /**
     * A key pressed while the agent's screen is open. The player's keys never reach it; only the mod menu,
     * freecam and agent view keys do something. Returns true to swallow the key.
     */
    @JvmStatic
    fun keyPressed(event: KeyEvent): Boolean {
        if (!open()) return false
        val minecraft = Minecraft.getInstance()
        when {
            McTinyTweakClient.openMenuKey.matches(event) -> minecraft.setScreen(ModMenuScreen(null))
            McTinyTweakClient.agentViewKey.matches(event) -> Freecam.toggleAgentView()
            McTinyTweakClient.freecamKey.matches(event) -> Freecam.toggle()
        }
        return true
    }

    /** Keeps the mouse captured for the freecam while the agent's screen is open. Client thread, every tick. */
    fun tick(minecraft: Minecraft) {
        if (!open(minecraft.screen)) return
        val mouse = minecraft.mouseHandler
        if (mouse.isMouseGrabbed || !minecraft.isWindowActive) return
        (mouse as MouseHandlerAccessor).`mc_tiny_tweak$setMouseGrabbed`(true)
        val window = minecraft.window
        InputConstants.grabOrReleaseMouse(window, GLFW.GLFW_CURSOR_DISABLED, window.screenWidth / 2.0, window.screenHeight / 2.0)
    }
}
