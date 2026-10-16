package dev.luniiya.mctinytweak.itemeditor

import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack

/** Reads and writes a whole item stack, including every data component, as editable SNBT. */
object ItemSnbt {
    fun encode(stack: ItemStack, registries: HolderLookup.Provider): String {
        val tag = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
            .getOrThrow { IllegalArgumentException("Cannot encode item: $it") }
        return indent(tag.toString())
    }

    fun decode(text: String, registries: HolderLookup.Provider): ItemDraft.Result {
        val tag = try {
            TagParser.parseCompoundFully(text)
        } catch (e: CommandSyntaxException) {
            return ItemDraft.Result.Failure(listOf(e.message ?: "Invalid SNBT"))
        }
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
            .flatMap(ItemStack::validateStrict)
            .mapOrElse({ ItemDraft.Result.Success(it) }, { ItemDraft.Result.Failure(listOf(it.message())) })
    }

    /** Breaks compact SNBT onto indented lines, leaving quoted strings untouched. */
    fun indent(snbt: String, unit: String = "  "): String {
        val out = StringBuilder()
        var depth = 0
        var quote: Char? = null
        var escaped = false
        fun newline() {
            out.append('\n')
            repeat(depth) { out.append(unit) }
        }
        var i = 0
        while (i < snbt.length) {
            val c = snbt[i++]
            if (quote != null) {
                out.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == quote -> quote = null
                }
                continue
            }
            when (c) {
                '"', '\'' -> { quote = c; out.append(c) }
                '{', '[' -> {
                    out.append(c)
                    // Typed arrays such as [I;1,2] need their prefix directly after the bracket.
                    if (c == '[' && i + 1 < snbt.length && snbt[i + 1] == ';') {
                        out.append(snbt, i, i + 2)
                        i += 2
                    }
                    depth++
                    newline()
                }
                '}', ']' -> { depth--; newline(); out.append(c) }
                ',' -> { out.append(c); newline() }
                ':' -> out.append(": ")
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}
