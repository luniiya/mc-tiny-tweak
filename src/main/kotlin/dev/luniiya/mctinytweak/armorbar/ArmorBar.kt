package dev.luniiya.mctinytweak.armorbar

import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.EquipmentAssets

/**
 * A four segment bar shown above player names while armor is hidden. Each worn piece fills one segment in its
 * material's color; worn pieces fill from the left so one piece reads as a quarter full bar.
 */
object ArmorBar {
    const val SEGMENTS = 4
    const val EMPTY_COLOR = 0x3A3A3A
    /** Shown for armor from data packs or other mods whose material has no color here. */
    const val UNKNOWN_COLOR = 0xFFFFFF
    /** Two full blocks per segment, since the block glyph renders narrow. */
    const val SEGMENT_TEXT = "██"

    private val SLOTS = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)

    private val MATERIAL_COLORS = mapOf(
        EquipmentAssets.CHAINMAIL to 0x8C8C96,
        EquipmentAssets.COPPER to 0xD9804F,
        EquipmentAssets.IRON to 0xE6E6E6,
        EquipmentAssets.GOLD to 0xFAD64A,
        EquipmentAssets.DIAMOND to 0x4AEDD9,
        EquipmentAssets.NETHERITE to 0x5E5458,
        EquipmentAssets.TURTLE_SCUTE to 0x47BF4A,
    )

    /** The segment color for [stack] worn in [slot], or null when it is not armor for that slot. */
    fun colorOf(stack: ItemStack, slot: EquipmentSlot): Int? {
        val equippable = stack.get(DataComponents.EQUIPPABLE) ?: return null
        if (equippable.slot() != slot) return null
        val asset = equippable.assetId().orElse(null) ?: return null
        return when (asset) {
            EquipmentAssets.ELYTRA -> null
            EquipmentAssets.LEATHER -> DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR) and 0xFFFFFF
            else -> MATERIAL_COLORS[asset] ?: UNKNOWN_COLOR
        }
    }

    /** Segment colors from left to right for the given head, chest, legs and feet items. */
    fun segments(head: ItemStack, chest: ItemStack, legs: ItemStack, feet: ItemStack): List<Int> {
        val worn = listOf(head, chest, legs, feet).zip(SLOTS).mapNotNull { (stack, slot) -> colorOf(stack, slot) }
        return worn + List(SEGMENTS - worn.size) { EMPTY_COLOR }
    }

    fun component(segments: List<Int>): Component {
        val bar = Component.empty()
        for (color in segments) bar.append(Component.literal(SEGMENT_TEXT).withStyle(Style.EMPTY.withColor(color)))
        return bar
    }
}
