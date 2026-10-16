package dev.luniiya.mctinytweak.client.attributes

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.attributes.AttributeAutoApply
import dev.luniiya.mctinytweak.attributes.AttributeAutoApply.Decision
import dev.luniiya.mctinytweak.client.HiddenCommandFeedback
import dev.luniiya.mctinytweak.client.itemeditor.ItemEditorSession
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path

/**
 * Applies the auto preset once per world, as soon as you are an operator there: on joining, or
 * later when you get OP. See [AttributeAutoApply] for how it decides.
 */
object AttributeAutoApplier {
    /** Ticks to wait after joining, so the server has sent your real attribute values. */
    private const val SETTLE_TICKS = 40

    private val file: Path get() = FabricLoader.getInstance().configDir.resolve(AttributeAutoApply.FILE_NAME)

    private var loaded: AttributeAutoApply? = null

    val state: AttributeAutoApply
        get() = loaded ?: read().also { loaded = it }

    /** The connection that was checked, so each join is checked once. */
    private var connection: ClientPacketListener? = null
    private var ticks = 0
    private var checked = false

    fun update(change: (AttributeAutoApply) -> AttributeAutoApply) {
        val next = change(state)
        if (next == state) return
        loaded = next
        try {
            Files.createDirectories(file.parent)
            Files.writeString(file, next.serialize())
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", file, e)
        }
    }

    /** Called when you load a preset or edit an attribute yourself, so the auto preset leaves this world alone. */
    fun remember(minecraft: Minecraft, what: String) {
        val world = worldKey(minecraft) ?: return
        update { it.remember(world, what) }
    }

    fun tick(minecraft: Minecraft) {
        val current = minecraft.connection
        if (current !== connection) {
            connection = current
            ticks = 0
            checked = false
            HiddenCommandFeedback.clear()
        }
        val player = minecraft.player ?: return
        if (checked || ++ticks < SETTLE_TICKS) return
        // Not an operator yet: keep waiting, in case OP is given later.
        if (!ItemEditorSession.canApplyAsOperator(minecraft)) return
        checked = true
        val world = worldKey(minecraft) ?: return
        val synced = PlayerAttributes.holders(player).filter { it.value().isClientSyncable }
        val values = synced.associate { it.registeredName to player.attributes.getBaseValue(it) }
        val defaults = synced.associate { it.registeredName to PlayerAttributes.default(it) }
        when (val decision = state.decide(world, values, defaults, PlayerAttributes.presets())) {
            Decision.Nothing -> Unit
            is Decision.Remember -> update { it.remember(world, decision.preset) }
            is Decision.Apply -> {
                PlayerAttributes.load(player, decision.preset, quiet = true)
                update { it.remember(world, decision.preset) }
                McTinyTweak.logger.info("Applied attribute preset {} in {}", decision.preset, world)
            }
        }
    }

    /**
     * One key per world and account. Singleplayer worlds use their folder and seed, so a new
     * world reusing a deleted world's folder name still counts as new.
     */
    private fun worldKey(minecraft: Minecraft): String? {
        val player = minecraft.player ?: return null
        val server = minecraft.singleplayerServer
        val world = if (server != null) {
            val folder = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().fileName
            "world:$folder:${server.worldData.worldGenOptions().seed()}"
        } else {
            "server:${minecraft.currentServer?.ip?.lowercase() ?: return null}"
        }
        return "$world/${player.uuid}"
    }

    private fun read(): AttributeAutoApply = try {
        if (Files.exists(file)) AttributeAutoApply.parse(Files.readString(file)) else AttributeAutoApply()
    } catch (e: Exception) {
        McTinyTweak.logger.warn("Could not read {}, using the default auto preset", file, e)
        AttributeAutoApply()
    }
}
