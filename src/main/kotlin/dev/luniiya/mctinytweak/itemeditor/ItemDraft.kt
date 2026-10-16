package dev.luniiya.mctinytweak.itemeditor

import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.util.Unit
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.ItemEnchantments

/** Editable fields of an item stack, kept as plain values so screens can bind to them directly. */
data class ItemDraft(
    val itemId: String,
    val count: Int,
    val damage: Int,
    val unbreakable: Boolean,
    /** `&`-coded custom name; blank removes the custom name. */
    val name: String,
    /** `&`-coded lore lines. */
    val lore: List<String>,
    /** Enchantment id to level, in display order. */
    val enchantments: Map<String, Int>,
) {
    sealed interface Result {
        data class Success(val stack: ItemStack) : Result
        data class Failure(val errors: List<String>) : Result
    }

    /**
     * Applies this draft on top of [base], keeping every component the draft does not edit.
     * [registries] resolves data-driven entries such as enchantments.
     */
    fun build(base: ItemStack, registries: HolderLookup.Provider): Result {
        val errors = mutableListOf<String>()
        val item = Identifier.tryParse(itemId.trim())?.let { BuiltInRegistries.ITEM.getOptional(it).orElse(null) }
        if (item == null || item == Items.AIR) return Result.Failure(listOf("Unknown item: ${itemId.trim()}"))

        val stack = when {
            base.isEmpty -> ItemStack(item)
            base.item == item -> base.copy()
            else -> base.transmuteCopy(item, base.count)
        }

        if (count !in 1..MAX_COUNT) {
            errors += "Count must be between 1 and $MAX_COUNT"
        } else {
            stack.count = count
            if (count > stack.maxStackSize) {
                if (canTakeDamage(stack)) errors += "Damageable items cannot stack above 1"
                else stack.set(DataComponents.MAX_STACK_SIZE, count)
            }
        }

        if (canTakeDamage(stack)) {
            if (damage !in 0..stack.maxDamage) errors += "Damage must be between 0 and ${stack.maxDamage}"
            else stack.damageValue = damage
        } else if (damage != 0) {
            errors += "This item cannot take damage"
        }

        if (unbreakable) stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE)
        else stack.resetToDefault(DataComponents.UNBREAKABLE)

        if (name.isBlank()) stack.resetToDefault(DataComponents.CUSTOM_NAME)
        else stack.set(DataComponents.CUSTOM_NAME, LegacyText.parse(name))

        when {
            lore.size > ItemLore.MAX_LINES -> errors += "Lore can have at most ${ItemLore.MAX_LINES} lines"
            lore.isEmpty() -> stack.resetToDefault(DataComponents.LORE)
            else -> stack.set(DataComponents.LORE, ItemLore(lore.map(LegacyText::parse)))
        }

        val enchantmentLookup = registries.lookupOrThrow(Registries.ENCHANTMENT)
        val enchantmentsOut = ItemEnchantments.Mutable(ItemEnchantments.EMPTY)
        for ((id, level) in enchantments) {
            val key = Identifier.tryParse(id.trim())?.let { ResourceKey.create(Registries.ENCHANTMENT, it) }
            val holder = key?.let { enchantmentLookup.get(it).orElse(null) }
            when {
                holder == null -> errors += "Unknown enchantment: $id"
                level !in 1..MAX_ENCHANTMENT_LEVEL -> errors += "Level of $id must be between 1 and $MAX_ENCHANTMENT_LEVEL"
                else -> enchantmentsOut.set(holder, level)
            }
        }
        stack.set(enchantmentComponent(stack), enchantmentsOut.toImmutable())

        if (errors.isEmpty()) {
            ItemStack.validateStrict(stack).error().ifPresent { errors += it.message() }
        }
        return if (errors.isEmpty()) Result.Success(stack) else Result.Failure(errors)
    }

    companion object {
        /** The largest stack size the max_stack_size component allows. */
        const val MAX_COUNT = 99
        /** The largest level the enchantment codec accepts. */
        const val MAX_ENCHANTMENT_LEVEL = 255

        fun from(stack: ItemStack): ItemDraft {
            val enchantments = linkedMapOf<String, Int>()
            for (entry in stack.getOrDefault(enchantmentComponent(stack), ItemEnchantments.EMPTY).entrySet()) {
                entry.key.unwrapKey().ifPresent { enchantments[it.identifier().toString()] = entry.intValue }
            }
            return ItemDraft(
                itemId = BuiltInRegistries.ITEM.getKey(stack.item).toString(),
                count = stack.count,
                damage = if (canTakeDamage(stack)) stack.damageValue else 0,
                unbreakable = stack.has(DataComponents.UNBREAKABLE),
                name = stack.get(DataComponents.CUSTOM_NAME)?.let(LegacyText::toLegacy).orEmpty(),
                lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().map(LegacyText::toLegacy),
                enchantments = enchantments,
            )
        }

        /** Unlike [ItemStack.isDamageableItem], this stays true for unbreakable items, which still store damage. */
        fun canTakeDamage(stack: ItemStack) = stack.has(DataComponents.MAX_DAMAGE)

        /**
         * Returns a component to the item's default. Plain [ItemStack.remove] would instead record a removal,
         * which hides defaults such as the empty lore every item has.
         */
        private fun <T : Any> ItemStack.resetToDefault(type: DataComponentType<T>) {
            val default = prototype.get(type)
            if (default == null) remove(type) else set(type, default)
        }

        /** Enchanted books keep their enchantments in a separate component. */
        fun enchantmentComponent(stack: ItemStack) =
            if (stack.`is`(Items.ENCHANTED_BOOK)) DataComponents.STORED_ENCHANTMENTS else DataComponents.ENCHANTMENTS
    }
}
