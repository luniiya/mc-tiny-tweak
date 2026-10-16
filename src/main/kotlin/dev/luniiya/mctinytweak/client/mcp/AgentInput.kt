package dev.luniiya.mctinytweak.client.mcp

import dev.luniiya.mctinytweak.mixin.client.mcp.KeyMappingAccessor
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft

/**
 * Keys held by the agent. They are pressed again at the start of every tick, so screens closing or the real
 * keyboard cannot release them early, and released once their ticks run out. Touch only on the client thread.
 */
object AgentInput {
    private val held = linkedMapOf<KeyMapping, Int>()

    /** Agent input keeps the unfocused game from pausing for this long. */
    private const val PLAYING_MILLIS = 5 * 60 * 1000L

    @Volatile
    private var lastInputMillis = 0L

    /** True while the agent holds keys or sent input recently. */
    @JvmStatic
    fun isPlaying() = AgentControl.agentInControl() || heldCount > 0 || System.currentTimeMillis() - lastInputMillis < PLAYING_MILLIS

    @Volatile
    var heldCount = 0
        private set

    @Volatile
    var miningError: String? = null

    @Volatile
    var useError: String? = null

    fun touch() {
        lastInputMillis = System.currentTimeMillis()
    }

    fun hold(mapping: KeyMapping, ticks: Int) {
        touch()
        if (mapping !in held) click(mapping)
        held[mapping] = ticks
        mapping.isDown = true
        heldCount = held.size
    }

    /** Queues one press, as a tap of the key would. */
    fun click(mapping: KeyMapping) {
        touch()
        val accessor = mapping as KeyMappingAccessor
        accessor.`mc_tiny_tweak$setClickCount`(accessor.`mc_tiny_tweak$getClickCount`() + 1)
    }

    fun releaseAll() {
        for (mapping in held.keys) mapping.isDown = false
        held.clear()
        heldCount = 0
    }

    fun tick(minecraft: Minecraft) {
        if (held.isEmpty()) return
        if (minecraft.player == null) return releaseAll()
        val iterator = held.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value <= 0) {
                entry.key.isDown = false
                iterator.remove()
            } else {
                if (entry.key === minecraft.options.keyUse) {
                    try {
                        AgentUseSafety.prepare(minecraft)
                    } catch (error: dev.luniiya.mctinytweak.mcp.ToolError) {
                        useError = error.message
                        entry.key.isDown = false
                        iterator.remove()
                        continue
                    }
                }
                if (entry.key === minecraft.options.keyAttack) {
                    try {
                        AgentMiningTools.prepare(minecraft)
                    } catch (error: dev.luniiya.mctinytweak.mcp.ToolError) {
                        miningError = error.message
                        entry.key.isDown = false
                        iterator.remove()
                        continue
                    }
                }
                entry.key.isDown = true
                entry.setValue(entry.value - 1)
            }
        }
        heldCount = held.size
    }
}
