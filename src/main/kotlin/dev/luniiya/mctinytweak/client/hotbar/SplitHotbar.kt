package dev.luniiya.mctinytweak.client.hotbar

import dev.luniiya.mctinytweak.hotbar.HotbarHints
import dev.luniiya.mctinytweak.hotbar.HotbarLayout
import dev.luniiya.mctinytweak.mixin.client.hotbar.GuiInvoker
import net.minecraft.client.AttackIndicatorStatus
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.HumanoidArm

/**
 * Draws the hotbar in two groups, with the first four slots set apart and a shadow of their purpose in each
 * empty one. Replaces vanilla's hotbar.
 */
object SplitHotbar {
    private val HOTBAR = Identifier.withDefaultNamespace("hud/hotbar")
    private val SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection")
    private val OFFHAND_LEFT = Identifier.withDefaultNamespace("hud/hotbar_offhand_left")
    private val OFFHAND_RIGHT = Identifier.withDefaultNamespace("hud/hotbar_offhand_right")
    private val ATTACK_BACKGROUND = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background")
    private val ATTACK_PROGRESS = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress")

    @JvmStatic
    fun render(gui: Gui, graphics: GuiGraphics, deltaTracker: DeltaTracker) {
        val invoker = gui as GuiInvoker
        val player = invoker.`mc_tiny_tweak$getCameraPlayer`() ?: return
        val center = graphics.guiWidth() / 2
        val top = graphics.guiHeight() - HotbarLayout.HEIGHT
        val left = HotbarLayout.left(center)
        val right = HotbarLayout.right(center)
        val w = HotbarLayout.SPRITE_WIDTH
        val h = HotbarLayout.HEIGHT

        // First group: left border and slots 1-4, then the sprite's right border column to close it.
        val firstSlots = HotbarLayout.FIRST_GROUP_U_END
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, w, h, 0, 0, left, top, firstSlots, h)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, w, h, w - 1, 0, left + firstSlots, top, 1, h)
        // Second group: the sprite's left border column, then slots 5-9 with the real right border.
        val second = HotbarLayout.secondPieceX(center)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, w, h, 0, 0, second, top, 1, h)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, w, h, firstSlots, 0, second + 1, top, w - firstSlots, h)

        val selected = player.inventory.selectedSlot
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION, HotbarLayout.selectionX(selected, center), top - 1, 24, 23)

        val offhand = player.offhandItem
        val offhandSide = player.mainArm.opposite
        if (!offhand.isEmpty) {
            if (offhandSide == HumanoidArm.LEFT) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OFFHAND_LEFT, left - 29, graphics.guiHeight() - 23, 29, 24)
            else graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OFFHAND_RIGHT, right, graphics.guiHeight() - 23, 29, 24)
        }

        val itemY = graphics.guiHeight() - 16 - 3
        for (slot in 0 until HotbarLayout.GROUP_SIZE) {
            if (player.inventory.getItem(slot).isEmpty) drawHint(graphics, slot, HotbarLayout.itemX(slot, center), itemY)
        }
        var seed = 1
        for (slot in 0 until HotbarLayout.SLOTS) {
            invoker.`mc_tiny_tweak$renderSlot`(graphics, HotbarLayout.itemX(slot, center), itemY, deltaTracker, player, player.inventory.getItem(slot), seed++)
        }
        if (!offhand.isEmpty) {
            val x = if (offhandSide == HumanoidArm.LEFT) left - 26 else right + 10
            invoker.`mc_tiny_tweak$renderSlot`(graphics, x, itemY, deltaTracker, player, offhand, seed++)
        }

        val minecraft = Minecraft.getInstance()
        if (minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            val strength = minecraft.player?.getAttackStrengthScale(0f) ?: 1f
            if (strength < 1f) {
                val y = graphics.guiHeight() - 20
                val x = if (offhandSide == HumanoidArm.RIGHT) left - 22 else right + 6
                val filled = (strength * 19f).toInt()
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ATTACK_BACKGROUND, x, y, 18, 18)
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ATTACK_PROGRESS, 18, 18, 0, 18 - filled, x, y + 18 - filled, 18, filled)
            }
        }
    }

    private fun drawHint(graphics: GuiGraphics, slot: Int, x: Int, y: Int) {
        val hint = HotbarHints.forSlot(slot) ?: return
        val texture = hint.texture
        if (texture == null) {
            for (run in HotbarHints.cubeRuns(x, y)) graphics.fill(run.x0, run.y, run.x1, run.y + 1, run.argb)
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(texture), x, y, 0f, 0f, 16, 16, 16, 16, HotbarHints.SHADOW_COLOR)
        }
    }
}
