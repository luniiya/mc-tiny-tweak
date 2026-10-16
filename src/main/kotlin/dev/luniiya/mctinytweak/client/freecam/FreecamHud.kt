package dev.luniiya.mctinytweak.client.freecam

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements

/** The hotbar and the player's bars belong to the player's eyes, so freecam hides them; agent view shows them. */
object FreecamHud {
    val HIDDEN = listOf(
        VanillaHudElements.HOTBAR,
        VanillaHudElements.ARMOR_BAR,
        VanillaHudElements.HEALTH_BAR,
        VanillaHudElements.FOOD_BAR,
        VanillaHudElements.AIR_BAR,
        VanillaHudElements.MOUNT_HEALTH,
        VanillaHudElements.INFO_BAR,
        VanillaHudElements.EXPERIENCE_LEVEL,
        VanillaHudElements.HELD_ITEM_TOOLTIP,
    )

    fun register() {
        for (id in HIDDEN) {
            HudElementRegistry.replaceElement(id) { original ->
                HudElement { graphics, delta -> if (!Freecam.shown()) original.render(graphics, delta) }
            }
        }
    }
}
