package dev.luniiya.mctinytweak.armorbar

import dev.luniiya.mctinytweak.MinecraftTestSupport
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArmorBarTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() = MinecraftTestSupport.bootstrap()
    }

    private val none = ItemStack.EMPTY

    @Test
    fun `no armor leaves every segment empty`() {
        assertEquals(List(4) { ArmorBar.EMPTY_COLOR }, ArmorBar.segments(none, none, none, none))
    }

    @Test
    fun `one netherite piece fills a quarter of the bar from the left`() {
        val segments = ArmorBar.segments(none, none, none, ItemStack(Items.NETHERITE_BOOTS))
        assertEquals(ArmorBar.colorOf(ItemStack(Items.NETHERITE_BOOTS), EquipmentSlot.FEET), segments[0])
        assertEquals(List(3) { ArmorBar.EMPTY_COLOR }, segments.drop(1))
    }

    @Test
    fun `every vanilla armor material has its own color`() {
        val helmets = listOf(
            Items.LEATHER_HELMET, Items.COPPER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET,
            Items.GOLDEN_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET, Items.TURTLE_HELMET,
        )
        val colors = helmets.map { ArmorBar.colorOf(ItemStack(it), EquipmentSlot.HEAD) }
        assertTrue(colors.none { it == null || it == ArmorBar.UNKNOWN_COLOR || it == ArmorBar.EMPTY_COLOR }, "$colors")
        assertEquals(colors.size, colors.toSet().size, "colors must be distinct: $colors")
    }

    @Test
    fun `leather uses its dye color`() {
        val dyed = ItemStack(Items.LEATHER_CHESTPLATE)
        dyed.set(DataComponents.DYED_COLOR, DyedItemColor(0x3366FF))
        assertEquals(0x3366FF, ArmorBar.colorOf(dyed, EquipmentSlot.CHEST))
    }

    @Test
    fun `elytra, pumpkins and misplaced armor do not count`() {
        assertNull(ArmorBar.colorOf(ItemStack(Items.ELYTRA), EquipmentSlot.CHEST))
        assertNull(ArmorBar.colorOf(ItemStack(Items.CARVED_PUMPKIN), EquipmentSlot.HEAD))
        assertNull(ArmorBar.colorOf(ItemStack(Items.DIAMOND_BOOTS), EquipmentSlot.HEAD))
        assertEquals(List(4) { ArmorBar.EMPTY_COLOR }, ArmorBar.segments(ItemStack(Items.CARVED_PUMPKIN), ItemStack(Items.ELYTRA), none, none))
    }

    @Test
    fun `the bar text has one colored part per segment`() {
        val bar = ArmorBar.component(listOf(0x123456, ArmorBar.EMPTY_COLOR, ArmorBar.EMPTY_COLOR, ArmorBar.EMPTY_COLOR))
        assertEquals(ArmorBar.SEGMENT_TEXT.repeat(4), bar.string)
        assertEquals(0x123456, bar.siblings[0].style.color?.value)
    }

    @Test
    fun `armor mixins apply to the vanilla renderers`() {
        assertTrue(HumanoidArmorLayer::class.java.declaredMethods.any { it.name.contains("hidePlayerArmor") })
        assertTrue(AvatarRenderer::class.java.declaredMethods.any { it.name.contains("submitArmorBar") })
    }
}
