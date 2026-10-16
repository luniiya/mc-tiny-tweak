package dev.luniiya.mctinytweak.client.mcp

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.luniiya.mctinytweak.mcp.AgentControls
import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import dev.luniiya.mctinytweak.mcp.McpTool
import dev.luniiya.mctinytweak.mcp.Schema
import dev.luniiya.mctinytweak.mcp.Schema.optDouble
import dev.luniiya.mctinytweak.mcp.Schema.optInt
import dev.luniiya.mctinytweak.mcp.Schema.requireDouble
import dev.luniiya.mctinytweak.mcp.Schema.requireString
import dev.luniiya.mctinytweak.mcp.Schema.stringList
import dev.luniiya.mctinytweak.mcp.ToolError
import dev.luniiya.mctinytweak.mcp.ToolResult
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

/**
 * The tools an agent can call. Each one does its game work on the client thread through [onClient];
 * waiting (for chat feedback or held keys) happens on the request thread so the game keeps running.
 */
object GameTools {
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    val all: List<McpTool> by lazy {
        listOf(
            McpTool("get_state", "The player's position, rotation, health, hunger, game mode, held item, open screen, what the crosshair targets, and nearby players.", Feature.STATE) { getState() },
            McpTool("get_inventory", "Every non-empty inventory slot. Slots 0-8 are the hotbar, 9-35 the main inventory, 36-39 armor (feet to head) and 40 the offhand.", Feature.STATE) { getInventory() },
            McpTool(
                "screenshot", "A PNG of what the game shows right now.", Feature.SCREENSHOT,
                Schema.obj(Schema.integer("downscale", "Divide the window size by this to save tokens (default 2).", min = 1, max = 4)),
            ) { screenshot(it.optInt("downscale", 2, 1..4)) },
            McpTool(
                "read_chat", "Chat lines with their ids, oldest first, including command feedback and Baritone messages. Pass the last id you saw to get only newer lines.", Feature.CHAT_READ,
                Schema.obj(
                    Schema.integer("after_id", "Only lines with a larger id (default 0, all kept lines).", min = 0),
                    Schema.integer("limit", "At most this many of the newest matching lines (default 50).", min = 1, max = 500),
                ),
            ) { readChat(it.optInt("after_id", 0, 0..Int.MAX_VALUE).toLong(), it.optInt("limit", 50, 1..500)) },
            McpTool(
                "send_chat", "Sends a chat message as the player. Use run_command for commands.", Feature.CHAT_SEND,
                Schema.obj(Schema.string("message", "The message, without a leading slash.", required = true)),
            ) { sendChat(it.requireString("message")) },
            McpTool(
                "run_command", "Runs a command as the player, like typing it in chat, and returns the chat lines that arrive shortly after.", Feature.COMMANDS,
                Schema.obj(
                    Schema.string("command", "The command, with or without the leading slash, for example `time set day`.", required = true),
                    Schema.integer("wait_ms", "How long to collect feedback (default 1000).", min = 0, max = 10_000),
                ),
            ) { runCommand(it.requireString("command"), it.optInt("wait_ms", 1000, 0..10_000)) },
            McpTool(
                "hold_keys", "Holds keys for a number of ticks (20 ticks = 1 second), then releases them. Names: ${AgentControls.KEY_ALIASES.keys.joinToString()}, or any key mapping id like key.hotbar.1. Holding attack mines the targeted block.", Feature.INPUT,
                Schema.obj(
                    Schema.stringList("keys", "The keys to hold together.", required = true),
                    Schema.integer("ticks", "How long to hold them.", required = true, min = 1, max = AgentControls.MAX_HOLD_TICKS),
                    Schema.Property("wait", JsonObject().apply { addProperty("type", "boolean"); addProperty("description", "Wait until they are released before answering (default true).") }, false),
                ),
            ) { holdKeys(it.stringList("keys"), it.optInt("ticks", 20, 1..AgentControls.MAX_HOLD_TICKS), it.get("wait")?.takeIf { w -> w.isJsonPrimitive }?.asBoolean ?: true) },
            McpTool(
                "press_key", "Presses a key once, like a tap: open the inventory, drop an item, attack once.", Feature.INPUT,
                Schema.obj(Schema.string("key", "A name from hold_keys or a key mapping id.", required = true)),
            ) { pressKey(it.requireString("key")) },
            McpTool("release_keys", "Releases every key the agent holds.", Feature.INPUT) { onClient { AgentInput.releaseAll() }; ToolResult.text("Released") },
            McpTool(
                "look", "Turns the player to an absolute rotation. Yaw 0 faces south (+Z), 90 west, 180 north, -90 east; pitch -90 is straight up, 90 straight down.", Feature.INPUT,
                Schema.obj(Schema.number("yaw", "Degrees.", required = true), Schema.number("pitch", "Degrees, -90 to 90.", required = true)),
            ) { look(it.requireDouble("yaw").toFloat(), it.requireDouble("pitch").toFloat()) },
            McpTool(
                "look_at", "Turns the player's eyes toward a point, for example the center of a block at x+0.5, y+0.5, z+0.5.", Feature.INPUT,
                Schema.obj(Schema.number("x", "", required = true), Schema.number("y", "", required = true), Schema.number("z", "", required = true)),
            ) { lookAt(it.requireDouble("x"), it.requireDouble("y"), it.requireDouble("z")) },
            McpTool(
                "select_slot", "Selects a hotbar slot.", Feature.INPUT,
                Schema.obj(Schema.integer("slot", "1 to 9.", required = true, min = 1, max = 9)),
            ) { selectSlot(it.optInt("slot", 1, 1..9)) },
            McpTool("close_screen", "Closes the open screen or menu, such as the pause menu, an inventory or a chest.", Feature.INPUT) { closeScreen() },
            McpTool(
                "baritone", "Runs a Baritone command (without #), for example `mine diamond_ore`, `goto 100 64 -200`, `follow player Name`, `stop`. Baritone's replies are returned from chat.", Feature.BARITONE,
                Schema.obj(
                    Schema.string("command", "The Baritone command.", required = true),
                    Schema.integer("wait_ms", "How long to collect Baritone's replies (default 1000).", min = 0, max = 10_000),
                ),
            ) { baritone(it.requireString("command"), it.optInt("wait_ms", 1000, 0..10_000)) },
            McpTool("baritone_status", "Whether Baritone is pathing, which process is in control (mining, following...) and its goal.", Feature.BARITONE) { baritoneStatus() },
        )
    }

    private fun getState(): ToolResult = onClient { minecraft ->
        val player = minecraft.requirePlayer()
        val level = player.level()
        val state = JsonObject().apply {
            addProperty("name", player.name.string)
            addProperty("server", minecraft.currentServer?.ip ?: if (minecraft.isLocalServer) "singleplayer" else "unknown")
            addProperty("dimension", level.dimension().identifier().toString())
            add("position", JsonObject().apply {
                addProperty("x", round(player.x)); addProperty("y", round(player.y)); addProperty("z", round(player.z))
            })
            add("block_position", JsonObject().apply {
                addProperty("x", player.blockPosition().x); addProperty("y", player.blockPosition().y); addProperty("z", player.blockPosition().z)
            })
            addProperty("yaw", round(player.yRot.toDouble()))
            addProperty("pitch", round(player.xRot.toDouble()))
            addProperty("health", player.health)
            addProperty("max_health", player.maxHealth)
            addProperty("food", player.foodData.foodLevel)
            addProperty("xp_level", player.experienceLevel)
            addProperty("game_mode", minecraft.gameMode?.playerMode?.serializedName ?: "unknown")
            addProperty("on_ground", player.onGround())
            addProperty("in_water", player.isInWater)
            addProperty("selected_slot", player.inventory.selectedSlot + 1)
            addProperty("held_item", describe(player.mainHandItem))
            addProperty("offhand_item", describe(player.offhandItem))
            addProperty("day_time", level.dayTime % 24000)
            addProperty("screen", minecraft.screen?.title?.string?.ifEmpty { minecraft.screen?.javaClass?.simpleName })
            addProperty("target", describeTarget(minecraft, minecraft.hitResult))
            add("nearby_players", JsonArray().apply {
                for (other in level.players()) {
                    if (other === player) continue
                    add(JsonObject().apply {
                        addProperty("name", other.name.string)
                        addProperty("distance", round(other.distanceTo(player).toDouble()))
                    })
                }
            })
            addProperty("agent_keys_held", AgentInput.heldCount)
            addProperty("baritone_installed", BaritoneBridge.installed)
        }
        ToolResult.text(gson.toJson(state))
    }

    private fun getInventory(): ToolResult = onClient { minecraft ->
        val inventory = minecraft.requirePlayer().inventory
        val slots = JsonArray()
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.isEmpty) continue
            slots.add(JsonObject().apply {
                addProperty("slot", slot)
                addProperty("item", BuiltInRegistries.ITEM.getKey(stack.item).toString())
                addProperty("count", stack.count)
                if (stack.isDamageableItem) addProperty("durability_left", stack.maxDamage - stack.damageValue)
                stack.customName?.let { addProperty("name", it.string) }
            })
        }
        ToolResult.text(gson.toJson(JsonObject().apply {
            addProperty("selected_hotbar_slot", inventory.selectedSlot + 1)
            add("slots", slots)
        }))
    }

    private fun screenshot(downscale: Int): ToolResult {
        val png = CompletableFuture<ByteArray>()
        val size = onClient { minecraft ->
            val target = minecraft.mainRenderTarget
            val scale = if (target.width % downscale == 0 && target.height % downscale == 0) downscale else 1
            Screenshot.takeScreenshot(target, scale) { image ->
                try {
                    val file = Files.createTempFile("mc-tiny-tweak-mcp", ".png")
                    try {
                        image.writeToFile(file)
                        png.complete(Files.readAllBytes(file))
                    } finally {
                        Files.deleteIfExists(file)
                    }
                } catch (e: Exception) {
                    png.completeExceptionally(e)
                } finally {
                    image.close()
                }
            }
            "${target.width / scale}x${target.height / scale}"
        }
        val bytes = try {
            png.get(10, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            throw ToolError("The screenshot did not finish in time")
        } catch (e: ExecutionException) {
            throw ToolError("Could not take the screenshot: ${e.cause?.message}")
        }
        return ToolResult(listOf(ToolResult.Content.Image(Base64.getEncoder().encodeToString(bytes), "image/png"), ToolResult.Content.Text(size)))
    }

    private fun readChat(afterId: Long, limit: Int): ToolResult {
        val lines = Mcp.chat.after(afterId, limit)
        if (lines.isEmpty()) return ToolResult.text("No new chat lines (newest id ${Mcp.chat.lastId()})")
        return ToolResult.text(lines.joinToString("\n") { "[${it.id}] ${it.text}" })
    }

    private fun sendChat(message: String): ToolResult {
        val text = message.trim()
        if (text.isEmpty()) throw ToolError("The message is empty")
        if (text.startsWith("/")) throw ToolError("Use run_command for commands")
        if (text.length > CHAT_LIMIT) throw ToolError("Chat messages are at most $CHAT_LIMIT characters")
        onClient { it.requirePlayer().connection.sendChat(text) }
        return ToolResult.text("Sent")
    }

    private fun runCommand(command: String, waitMillis: Int): ToolResult {
        val text = command.trim().removePrefix("/")
        if (text.isEmpty()) throw ToolError("The command is empty")
        val before = Mcp.chat.lastId()
        onClient { it.requirePlayer().connection.sendCommand(text) }
        return ToolResult.text("Ran /$text" + feedbackSince(before, waitMillis))
    }

    private fun baritone(command: String, waitMillis: Int): ToolResult {
        if (!BaritoneBridge.installed) throw ToolError("Baritone is not installed in this game")
        val text = command.trim().removePrefix("#")
        val before = Mcp.chat.lastId()
        val known = onClient { it.requirePlayer(); BaritoneBridge.execute(text) }
        if (!known) throw ToolError("Baritone does not know the command `$text`. Try `help`." + feedbackSince(before, 0))
        return ToolResult.text("Ran #$text" + feedbackSince(before, waitMillis))
    }

    private fun baritoneStatus(): ToolResult {
        if (!BaritoneBridge.installed) throw ToolError("Baritone is not installed in this game")
        return ToolResult.text(onClient { it.requirePlayer(); BaritoneBridge.status() })
    }

    /** Waits, then returns the chat lines that arrived after [lastId], when reading chat is allowed. */
    private fun feedbackSince(lastId: Long, waitMillis: Int): String {
        if (!Mcp.settings.allows(Feature.CHAT_READ)) return ""
        if (waitMillis > 0) Thread.sleep(waitMillis.toLong())
        val lines = Mcp.chat.after(lastId, 100)
        return if (lines.isEmpty()) "\nNo chat feedback." else "\nChat:\n" + lines.joinToString("\n") { "[${it.id}] ${it.text}" }
    }

    private fun holdKeys(keys: List<String>, ticks: Int, wait: Boolean): ToolResult {
        if (keys.isEmpty()) throw ToolError("Give at least one key")
        onClient { minecraft ->
            minecraft.requirePlayer()
            val mappings = keys.map { mapping(minecraft, it) }
            for (mapping in mappings) AgentInput.hold(mapping, ticks)
        }
        if (!wait) return ToolResult.text("Holding ${keys.joinToString()} for $ticks ticks")
        val deadline = System.currentTimeMillis() + ticks * 50L + 5_000
        Thread.sleep(ticks * 50L)
        while (AgentInput.heldCount > 0 && System.currentTimeMillis() < deadline) Thread.sleep(25)
        return ToolResult.text("Held ${keys.joinToString()} for $ticks ticks")
    }

    private fun pressKey(key: String): ToolResult {
        onClient { minecraft -> AgentInput.click(mapping(minecraft, key)) }
        return ToolResult.text("Pressed $key")
    }

    private fun look(yaw: Float, pitch: Float): ToolResult {
        if (!yaw.isFinite() || !pitch.isFinite()) throw ToolError("Angles must be numbers")
        onClient { minecraft -> turn(minecraft.requirePlayer(), yaw, pitch.coerceIn(-90f, 90f)) }
        return ToolResult.text("Looking at yaw $yaw, pitch ${pitch.coerceIn(-90f, 90f)}")
    }

    private fun lookAt(x: Double, y: Double, z: Double): ToolResult {
        val angles = onClient { minecraft ->
            val player = minecraft.requirePlayer()
            val eyes = player.eyePosition
            AgentControls.anglesTo(x - eyes.x, y - eyes.y, z - eyes.z).also { turn(player, it.yaw, it.pitch) }
        }
        return ToolResult.text("Looking at yaw ${round(angles.yaw.toDouble())}, pitch ${round(angles.pitch.toDouble())}")
    }

    private fun turn(player: LocalPlayer, yaw: Float, pitch: Float) {
        AgentInput.touch()
        player.yRot = yaw
        player.xRot = pitch
        player.yRotO = yaw
        player.xRotO = pitch
        player.yHeadRot = yaw
    }

    private fun selectSlot(slot: Int): ToolResult {
        onClient { minecraft ->
            AgentInput.touch()
            minecraft.requirePlayer().inventory.selectedSlot = slot - 1
        }
        return ToolResult.text("Selected slot $slot")
    }

    private fun closeScreen(): ToolResult {
        val closed = onClient { minecraft ->
            AgentInput.touch()
            val screen = minecraft.screen ?: return@onClient null
            screen.onClose()
            screen.title.string.ifEmpty { screen.javaClass.simpleName }
        }
        return ToolResult.text(if (closed == null) "No screen was open" else "Closed $closed")
    }

    private fun mapping(minecraft: Minecraft, key: String): KeyMapping {
        val name = AgentControls.mappingName(key)
            ?: throw ToolError("Unknown key `$key`. Use one of ${AgentControls.KEY_ALIASES.keys.joinToString()} or a key mapping id")
        return minecraft.options.keyMappings.firstOrNull { it.name == name } ?: throw ToolError("No key mapping named $name")
    }

    private fun describe(stack: ItemStack): String =
        if (stack.isEmpty) "empty" else "${stack.count}x ${BuiltInRegistries.ITEM.getKey(stack.item)}" + (stack.customName?.let { " \"${it.string}\"" } ?: "")

    private fun describeTarget(minecraft: Minecraft, hit: HitResult?): String = when (hit) {
        is BlockHitResult if hit.type == HitResult.Type.BLOCK -> {
            val pos = hit.blockPos
            val block = minecraft.level?.getBlockState(pos)?.block?.let { BuiltInRegistries.BLOCK.getKey(it).toString() } ?: "unknown"
            "block $block at ${pos.x} ${pos.y} ${pos.z}, face ${hit.direction.serializedName}"
        }
        is EntityHitResult -> "entity ${BuiltInRegistries.ENTITY_TYPE.getKey(hit.entity.type)} ${hit.entity.name.string}"
        else -> "nothing"
    }

    private fun round(value: Double) = Math.round(value * 100) / 100.0

    private fun Minecraft.requirePlayer(): LocalPlayer = player ?: throw ToolError("Not in a world")

    /** Runs [block] on the client thread and waits for it, passing [ToolError]s through. */
    private fun <T> onClient(block: (Minecraft) -> T): T {
        val minecraft = Minecraft.getInstance()
        try {
            return CompletableFuture.supplyAsync(Supplier { block(minecraft) }, minecraft).get(10, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        } catch (e: TimeoutException) {
            throw ToolError("The game did not answer in time")
        }
    }

    private const val CHAT_LIMIT = 256
}
