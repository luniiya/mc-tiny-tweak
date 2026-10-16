package dev.luniiya.mctinytweak.client.mcp

import com.mojang.blaze3d.platform.NativeImage
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dev.luniiya.mctinytweak.baritone.BaritoneDefaults
import dev.luniiya.mctinytweak.client.freecam.Freecam
import dev.luniiya.mctinytweak.mcp.AgentControls
import dev.luniiya.mctinytweak.mcp.BlockInspection
import dev.luniiya.mctinytweak.mcp.ContainerClicks
import dev.luniiya.mctinytweak.mcp.CraftingFit
import dev.luniiya.mctinytweak.mcp.McpSettings.Feature
import dev.luniiya.mctinytweak.mcp.McpTool
import dev.luniiya.mctinytweak.mcp.Schema
import dev.luniiya.mctinytweak.mcp.ScreenshotSize
import dev.luniiya.mctinytweak.mcp.Schema.optDouble
import dev.luniiya.mctinytweak.mcp.Schema.optInt
import dev.luniiya.mctinytweak.mcp.Schema.optString
import dev.luniiya.mctinytweak.mcp.Schema.requireDouble
import dev.luniiya.mctinytweak.mcp.Schema.requireString
import dev.luniiya.mctinytweak.mcp.Schema.stringList
import dev.luniiya.mctinytweak.mcp.ToolError
import dev.luniiya.mctinytweak.mcp.ToolResult
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.StackedItemContents
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.RecipeDisplayId
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplayContext
import net.minecraft.world.level.chunk.status.ChunkStatus
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
            McpTool("get_state", "Connection and screen state, plus player position, rotation, health, hunger, game mode, held items, death state, crosshair target and nearby players. Also works on the title or disconnect screen.", Feature.STATE) { getState() },
            McpTool("get_inventory", "Every non-empty inventory slot. Slots 0-8 are the hotbar, 9-35 the main inventory, 36-39 armor (feet to head) and 40 the offhand.", Feature.STATE) { getInventory() },
            McpTool(
                "inspect_blocks", "Inspect a small cube of client-loaded blocks and fluids before mining or overriding a refused route. Coordinates default to your block position; radius defaults to 1 (maximum 4). Unknown/unloaded or out-of-height cells have loaded=false, never safe air. Air is omitted unless include_air=true. Does not load chunks.", Feature.STATE,
                Schema.obj(
                    Schema.integer("x", "Center block X.", min = -30_000_000, max = 30_000_000),
                    Schema.integer("y", "Center block Y.", min = -30_000_000, max = 30_000_000),
                    Schema.integer("z", "Center block Z.", min = -30_000_000, max = 30_000_000),
                    Schema.integer("radius", "Cube radius in blocks.", min = 0, max = 4),
                    Schema.Property("include_air", JsonObject().apply { addProperty("type", "boolean") }, false),
                ),
            ) { inspectBlocks(it) },
            McpTool("get_container", "Open menu slots, including empty slots, player inventory mapping and cursor stack. Use these menu slot ids with click_slot; they differ from get_inventory ids.", Feature.STATE) { getContainer() },
            McpTool(
                "click_slot", "Needs take_over. Click a menu slot: pickup (left/right), quick_move (shift-click), swap (button 0-8 hotbar or 40 offhand), throw (button 0 one, 1 stack). slot -999 clicks outside for pickup. Pass container_id from get_container to refuse stale menus.", Feature.INPUT,
                Schema.obj(
                    Schema.integer("container_id", "Current menu id from get_container.", required = true, min = 0),
                    Schema.integer("slot", "Menu slot id.", required = true, min = -999),
                    Schema.string("action", "pickup, quick_move, swap or throw (default pickup)."),
                    Schema.integer("button", "Mouse button or swap target (default 0).", min = 0, max = 40),
                ),
            ) { clickSlot(it.optInt("container_id", -1, 0..Int.MAX_VALUE), it.optInt("slot", -1, -999..Int.MAX_VALUE), it.optString("action") ?: "pickup", it.optInt("button", 0, 0..40)) },
            McpTool(
                "screenshot", "A PNG of what the game shows right now.", Feature.SCREENSHOT,
                Schema.obj(
                    Schema.integer("downscale", "Divide the window size by this to save tokens (default 2).", min = 1, max = 4),
                    Schema.string("view", "\"agent\" (default) shows your own eyes. \"player\" shows what the person sees, which is their freecam while you have taken over."),
                ),
            ) {
                val view = it.optString("view") ?: "agent"
                if (view != "agent" && view != "player") throw ToolError("view must be agent or player")
                screenshot(it.optInt("downscale", 2, 1..4), playerView = view == "player")
            },
            McpTool(
                "read_chat", "Chat lines with their ids, oldest first, including command feedback and Baritone messages. Pass the last id you saw to get only newer lines.", Feature.CHAT_READ,
                Schema.obj(
                    Schema.integer("after_id", "Only lines with a larger id (default 0, all kept lines).", min = 0),
                    Schema.integer("limit", "At most this many of the newest matching lines (default 50).", min = 1, max = 500),
                ),
            ) { readChat(it.optInt("after_id", 0, 0..Int.MAX_VALUE).toLong(), it.optInt("limit", 50, 1..500)) },
            McpTool(
                "wait_for_chat", "Waits for incoming player chat through this MCP tool call, then returns it. A pending tool result lets an agent resume without prompt injection. Needs an initialized HTTP session; chat_notifications can mute it. Own messages and tool feedback do not wake it.", Feature.CHAT_READ,
                Schema.obj(
                    Schema.integer("after_id", "Only newer incoming lines (default: start waiting from now).", min = 0),
                    Schema.integer("timeout_ms", "Maximum wait (default 30000).", min = 0, max = 60000),
                ),
            ) {
                val newest = Mcp.chat.lastId()
                val requested = it.optInt("after_id", newest.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), 0..Int.MAX_VALUE).toLong()
                // A game restart resets line ids; an old cursor must not hide every new incoming message.
                val after = if (requested > newest) 0L else requested
                val deadline = System.nanoTime() + it.optInt("timeout_ms", 30000, 0..60000) * 1_000_000L
                var lines: List<dev.luniiya.mctinytweak.mcp.ChatLog.Line>
                while (true) {
                    if (Mcp.events.muted()) return@McpTool ToolResult.text("Chat wake is muted")
                    lines = Mcp.chat.incomingAfter(after, 100)
                    if (lines.isNotEmpty() || System.nanoTime() >= deadline) break
                    Thread.sleep(100)
                }
                ToolResult.text(if (lines.isEmpty()) "No incoming chat (newest id ${Mcp.chat.lastId()})" else lines.joinToString("\n") { line -> "[${line.id}] ${line.text}" })
            },
            McpTool(
                "chat_notifications", "Enables or mutes incoming-chat MCP resource notifications for your initialized HTTP session. Subscribe to minecraft://chat and open the authenticated GET event stream to receive them. Your host decides whether they wake an agent; this never injects prompts.", Feature.CHAT_READ,
                Schema.obj(Schema.Property("enabled", JsonObject().apply { addProperty("type", "boolean") }, true)),
            ) {
                Mcp.events.wake(it["enabled"].asBoolean)
                ToolResult.text(if (it["enabled"].asBoolean) "Chat notifications enabled for this session" else "Chat notifications muted for this session")
            },
            McpTool(
                "send_chat", "Sends a chat message as the player. Needs take_over. Use run_command for commands.", Feature.CHAT_SEND,
                Schema.obj(Schema.string("message", "The message, without a leading slash.", required = true)),
            ) { sendChat(it.requireString("message")) },
            McpTool(
                "run_command", "Runs a command as the player, like typing it in chat, and returns the chat lines that arrive shortly after. Works with or without take_over.", Feature.COMMANDS,
                Schema.obj(
                    Schema.string("command", "The command, with or without the leading slash, for example `time set day`.", required = true),
                    Schema.integer("wait_ms", "How long to collect feedback (default 1000).", min = 0, max = 10_000),
                ),
            ) { runCommand(it.requireString("command"), it.optInt("wait_ms", 1000, 0..10_000)) },
            McpTool(
                "hold_keys", "Holds keys for a number of ticks (20 ticks = 1 second), then releases them. Names: ${AgentControls.KEY_ALIASES.keys.joinToString()}, or any key mapping id like key.hotbar.1. Holding attack mines the targeted block, automatically selecting an available harvesting tool.", Feature.INPUT,
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
            McpTool(
                "craft", "Needs take_over. Crafts an item through the recipe book, like clicking the recipe and shift-clicking the result. Works in the inventory's 2x2 grid; 3x3 recipes (tools, armor) need an open crafting table: look at one and press_key use first. The recipe must be unlocked in the recipe book.", Feature.INPUT,
                Schema.obj(
                    Schema.string("item", "The item to make, for example `oak_planks` or `minecraft:stick`.", required = true),
                    Schema.integer("times", "How many times to craft the recipe (default 1). One craft of planks makes 4.", min = 1, max = 64),
                ),
            ) { craft(it.requireString("item"), it.optInt("times", 1, 1..64)) },
            McpTool("close_screen", "Closes the open screen or menu, such as the pause menu, an inventory or a chest.", Feature.INPUT) { closeScreen() },
            McpTool(
                "baritone", "Needs take_over. Runs a Baritone command (without #), for example `mine diamond_ore`, `goto 100 64 -200`, `follow player Name`, `stop`. Baritone's replies are returned (they stay out of the player's chat). A refused route may imply danger or an obstacle: inspect before overriding it.", Feature.BARITONE,
                Schema.obj(
                    Schema.string("command", "The Baritone command.", required = true),
                    Schema.integer("wait_ms", "How long to collect Baritone's replies (default 1000).", min = 0, max = 10_000),
                ),
            ) { baritone(it.requireString("command"), it.optInt("wait_ms", 1000, 0..10_000)) },
            McpTool("baritone_status", "Whether Baritone is pathing, which process is in control (mining, following...) and its goal.", Feature.BARITONE) { baritoneStatus() },
            McpTool(
                "baritone_settings", "Lists Baritone settings as `name (type) = value [default]`. Without a query, lists only the ones changed from the mod's defaults. Works without take_over.", Feature.BARITONE,
                Schema.obj(Schema.string("query", "Words that must all appear in the setting name, for example `parkour` or `allow break`.")),
            ) { baritoneSettings(it.optString("query")) },
            McpTool(
                "baritone_set_setting", "Changes one Baritone setting and saves it, for example name `allowParkour` value `true`, or `blocksToAvoid` `stone,dirt`. Works without take_over.", Feature.BARITONE,
                Schema.obj(Schema.string("name", "The setting name.", required = true), Schema.string("value", "The new value as text.", required = true)),
            ) { baritoneSetSetting(it.requireString("name"), it.requireString("value")) },
            McpTool(
                "baritone_reset_settings", "Puts every Baritone setting back to MC Tiny Tweak's Baritone defaults (Baritone's own where the mod has none).", Feature.BARITONE,
            ) {
                requireBaritone()
                onClient { BaritoneSettings.resetToModDefaults() }
                ToolResult.text("Baritone settings are back to the mod's defaults")
            },
            McpTool(
                "take_over", "Takes sole control of the player: the person's keyboard and mouse stop working in game until release_control, or until they take control back from the mod menu. Needed for send_chat, Baritone and joining or leaving worlds.", Feature.TAKE_OVER,
            ) { onClient { AgentControl.takeOver() }; ToolResult.text("You are in control") },
            McpTool("respawn", "Needs take_over. Requests respawn after death (spectating in hardcore), like the death screen button. Check get_state afterwards.", Feature.INPUT) {
                requireTakeOver("respawn")
                onClient { minecraft ->
                    val player = minecraft.requirePlayer()
                    if (!player.isDeadOrDying) throw ToolError("The player is still alive")
                    AgentInput.releaseAll()
                    if (BaritoneBridge.installed) BaritoneBridge.stop()
                    player.respawn()
                }
                ToolResult.text("Respawn requested; check get_state in a moment")
            },
            McpTool("release_control", "Gives control back to the person and stops held keys and Baritone.", Feature.TAKE_OVER) {
                onClient { AgentControl.release(byPlayer = false) }; ToolResult.text("The person is in control")
            },
            McpTool("list_servers", "The servers saved in the multiplayer list, with their addresses.", Feature.WORLDS) { listServers() },
            McpTool(
                "join_server", "Needs take_over and no world open. Connects to a server; check get_state a few seconds later.", Feature.WORLDS,
                Schema.obj(Schema.string("address", "For example `localhost` or `play.example.net:25565`.", required = true)),
            ) { joinServer(it.requireString("address")) },
            McpTool("list_worlds", "The singleplayer worlds, newest played first, with the id open_world takes.", Feature.WORLDS) { listWorlds() },
            McpTool(
                "open_world", "Needs take_over and no world open. Opens a singleplayer world by its id from list_worlds.", Feature.WORLDS,
                Schema.obj(Schema.string("id", "The world's folder id.", required = true)),
            ) { openWorld(it.requireString("id")) },
            McpTool("leave_world", "Needs take_over. Leaves the current world or server, like Disconnect or Save and Quit.", Feature.WORLDS) { leaveWorld() },
        )
    }

    private fun getState(): ToolResult = onClient { minecraft ->
        val player = minecraft.player
        if (player == null) return@onClient ToolResult.text(gson.toJson(JsonObject().apply {
            addProperty("connected", false)
            addProperty("screen", minecraft.screen?.javaClass?.simpleName)
            addProperty("screen_title", minecraft.screen?.title?.string)
            addProperty("control", if (AgentControl.agentInControl()) "agent" else "player")
        }))
        val level = minecraft.level ?: throw ToolError("Not in a world")
        val state = JsonObject().apply {
            addProperty("connected", true)
            addProperty("dead", player.isDeadOrDying)
            addProperty("air", player.airSupply)
            addProperty("max_air", player.maxAirSupply)
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
            addProperty("in_lava", player.isInLava)
            addProperty("on_fire", player.isOnFire)
            addProperty("fire_ticks", player.remainingFireTicks)
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
            addProperty("control", if (AgentControl.agentInControl()) "agent" else "player")
            addProperty("agent_keys_held", AgentInput.heldCount)
            addProperty("agent_mining_error", AgentInput.miningError)
            addProperty("baritone_installed", BaritoneBridge.installed)
        }
        ToolResult.text(gson.toJson(state))
    }

    private fun inspectBlocks(args: JsonObject): ToolResult = onClient { minecraft ->
        val player = minecraft.requirePlayer()
        val level = minecraft.level ?: throw ToolError("Not in a world")
        val origin = player.blockPosition()
        val center = BlockPos(args.optInt("x", origin.x, -30_000_000..30_000_000), args.optInt("y", origin.y, -30_000_000..30_000_000), args.optInt("z", origin.z, -30_000_000..30_000_000))
        val cells = BlockInspection.inspect(center, args.optInt("radius", 1, 0..4), args.get("include_air")?.asBoolean ?: false) { pos ->
            // ClientLevel.hasChunk always returns true; only the cache can distinguish unloaded cells.
            val chunk = if (level.isOutsideBuildHeight(pos)) null else level.chunkSource.getChunk(pos.x shr 4, pos.z shr 4, ChunkStatus.FULL, false)
            if (chunk == null) null else {
                val state = level.getBlockState(pos)
                val fluid = state.fluidState
                BlockInspection.Block(BuiltInRegistries.BLOCK.getKey(state.block).toString(), state.isAir,
                    if (fluid.isEmpty) null else BuiltInRegistries.FLUID.getKey(fluid.type).toString(), fluid.isSource, state.toString())
            }
        }
        ToolResult.text(gson.toJson(JsonObject().apply {
            addProperty("dimension", level.dimension().identifier().toString())
            add("blocks", JsonArray().apply {
                for (cell in cells) add(JsonObject().apply {
                    addProperty("x", cell.position.x); addProperty("y", cell.position.y); addProperty("z", cell.position.z)
                    addProperty("loaded", cell.block != null)
                    cell.block?.let { block ->
                        addProperty("block", block.id); addProperty("state", block.state)
                        addProperty("fluid", block.fluid); addProperty("fluid_source", block.fluidSource)
                    }
                })
            })
        }))
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

    private fun getContainer(): ToolResult = onClient { minecraft ->
        val player = minecraft.requirePlayer()
        val menu = player.containerMenu
        ToolResult.text(gson.toJson(JsonObject().apply {
            addProperty("container_id", menu.containerId)
            addProperty("menu", menu.javaClass.simpleName)
            addProperty("cursor", describe(menu.carried))
            add("slots", JsonArray().apply {
                for (slot in menu.slots) add(JsonObject().apply {
                    addProperty("slot", slot.index)
                    addProperty("item", describe(slot.item))
                    addProperty("can_place", slot.mayPlace(menu.carried))
                    addProperty("can_pickup", slot.mayPickup(player))
                    if (slot.container === player.inventory) addProperty("inventory_slot", slot.containerSlot)
                })
            })
        }))
    }

    private fun clickSlot(containerId: Int, slot: Int, action: String, button: Int): ToolResult {
        requireTakeOver("click_slot")
        onClient { minecraft ->
            val player = minecraft.requirePlayer()
            val menu = player.containerMenu
            ContainerClicks.validate(containerId, menu.containerId, slot, menu.slots.size, action, button)
            AgentInput.touch()
            minecraft.gameMode!!.handleInventoryMouseClick(menu.containerId, slot, button, ClickType.valueOf(action.uppercase()), player)
        }
        // Allow the server to acknowledge the click before reading slots again.
        Thread.sleep(CRAFT_WAIT_MILLIS)
        return getContainer()
    }

    private fun screenshot(downscale: Int, playerView: Boolean): ToolResult {
        // The agent sees through its own eyes, not the player's freecam, unless it asks for the player's view.
        if (!playerView && onClient { Freecam.shown() }) {
            val start = Freecam.frames
            Freecam.screenshotView = true
            val deadline = System.currentTimeMillis() + 1000
            while (Freecam.frames < start + 2 && System.currentTimeMillis() < deadline) Thread.sleep(5)
        }
        try {
            return captureScreenshot(downscale)
        } finally {
            Freecam.screenshotView = false
        }
    }

    private fun captureScreenshot(downscale: Int): ToolResult {
        val png = CompletableFuture<ByteArray>()
        val size = onClient { minecraft ->
            val target = minecraft.mainRenderTarget
            val size = ScreenshotSize.plan(target.width, target.height, downscale)
            Screenshot.takeScreenshot(target, size.captureScale) { image ->
                try {
                    val file = Files.createTempFile("mc-tiny-tweak-mcp", ".png")
                    try {
                        if (size.captureScale == downscale) {
                            image.writeToFile(file)
                        } else {
                            NativeImage(size.width, size.height, false).use { resized ->
                                image.resizeSubRectTo(0, 0, image.width, image.height, resized)
                                resized.writeToFile(file)
                            }
                        }
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
            "${size.width}x${size.height}"
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
        requireTakeOver("send_chat")
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
        requireTakeOver("baritone")
        val text = command.trim().removePrefix("#")
        val before = Mcp.chat.lastId()
        val known = onClient { it.requirePlayer(); BaritoneBridge.execute(text) }
        if (!known) throw ToolError("Baritone does not know the command `$text`. Try `help`." + feedbackSince(before, 0))
        return ToolResult.text("Ran #$text" + feedbackSince(before, waitMillis))
    }

    private fun requireBaritone() {
        if (!BaritoneBridge.installed) throw ToolError("Baritone is not installed in this game")
    }

    private fun baritoneSettings(query: String?): ToolResult {
        requireBaritone()
        val entries = onClient { BaritoneSettings.entries() }
            .filter { if (query.isNullOrBlank()) it.modified else BaritoneDefaults.matches(it.name, query) }
        if (entries.isEmpty()) return ToolResult.text(if (query.isNullOrBlank()) "Every setting has the mod's default" else "No setting matches `$query`")
        val shown = entries.take(MAX_SETTINGS)
        val more = if (entries.size > shown.size) "\n... ${entries.size - shown.size} more, narrow the query" else ""
        return ToolResult.text(shown.joinToString("\n") { "${it.name} (${it.type}) = ${it.value} [${it.default}]" } + more)
    }

    private fun baritoneSetSetting(name: String, value: String): ToolResult {
        requireBaritone()
        val entry = onClient {
            try {
                BaritoneSettings.set(name, value)
            } catch (e: IllegalArgumentException) {
                throw ToolError(e.message ?: "Invalid setting")
            }
            BaritoneSettings.entry(name)
        } ?: throw ToolError("Unknown Baritone setting: $name")
        return ToolResult.text("${entry.name} = ${entry.value}")
    }

    private const val MAX_SETTINGS = 60

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
            if (minecraft.options.keyAttack in mappings) {
                AgentMiningTools.prepare(minecraft)
                AgentInput.miningError = null
            }
            for (mapping in mappings) AgentInput.hold(mapping, ticks)
        }
        if (!wait) return ToolResult.text("Holding ${keys.joinToString()} for $ticks ticks")
        val deadline = System.currentTimeMillis() + ticks * 50L + 5_000
        Thread.sleep(ticks * 50L)
        while (AgentInput.heldCount > 0 && System.currentTimeMillis() < deadline) Thread.sleep(25)
        if (keys.any { AgentControls.mappingName(it) == "key.attack" }) {
            onClient { AgentInput.miningError }?.let { throw ToolError(it) }
        }
        return ToolResult.text("Held ${keys.joinToString()} for $ticks ticks")
    }

    private fun pressKey(key: String): ToolResult {
        onClient { minecraft ->
            val mapping = mapping(minecraft, key)
            if (mapping === minecraft.options.keyAttack) AgentMiningTools.prepare(minecraft)
            AgentInput.click(mapping)
        }
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

    private fun listServers(): ToolResult = onClient { minecraft ->
        val servers = ServerList(minecraft).apply { load() }
        if (servers.size() == 0) return@onClient ToolResult.text("No saved servers")
        ToolResult.text((0 until servers.size()).joinToString("\n") { servers.get(it).let { s -> "${s.name}: ${s.ip}" } })
    }

    private fun joinServer(address: String): ToolResult {
        val target = address.trim()
        if (!ServerAddress.isValidAddress(target)) throw ToolError("Not a server address: $target")
        requireTakeOver("join_server")
        onClient { minecraft ->
            if (minecraft.level != null) throw ToolError("Leave the current world first with leave_world")
            val name = ServerList(minecraft).apply { load() }.let { list -> (0 until list.size()).map(list::get).firstOrNull { it.ip == target }?.name } ?: target
            ConnectScreen.startConnecting(TitleScreen(), minecraft, ServerAddress.parseString(target), ServerData(name, target, ServerData.Type.OTHER), false, null)
        }
        return ToolResult.text("Connecting to $target. Check get_state in a few seconds.")
    }

    private fun listWorlds(): ToolResult {
        val source = Minecraft.getInstance().levelSource
        val worlds = try {
            source.loadLevelSummaries(source.findLevelCandidates()).get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw ToolError("Could not read the worlds: ${e.message}")
        }
        if (worlds.isEmpty()) return ToolResult.text("No singleplayer worlds")
        return ToolResult.text(worlds.sortedByDescending { it.lastPlayed }.joinToString("\n") {
            "${it.levelId}: ${it.levelName}" + if (it.isLocked) " (open elsewhere)" else ""
        })
    }

    private fun openWorld(id: String): ToolResult {
        requireTakeOver("open_world")
        onClient { minecraft ->
            if (minecraft.level != null) throw ToolError("Leave the current world first with leave_world")
            if (!minecraft.levelSource.levelExists(id)) throw ToolError("No world with the id $id. See list_worlds.")
            minecraft.createWorldOpenFlows().openWorld(id) { minecraft.setScreen(TitleScreen()) }
        }
        return ToolResult.text("Opening $id. Check get_state in a few seconds.")
    }

    private fun leaveWorld(): ToolResult {
        requireTakeOver("leave_world")
        onClient { minecraft ->
            if (minecraft.level == null) throw ToolError("Not in a world")
            minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE)
        }
        return ToolResult.text("Left the world")
    }

    private fun craft(item: String, times: Int): ToolResult {
        requireTakeOver("craft")
        val id = CraftingFit.itemId(item)
        val recipe = onClient { minecraft -> findRecipe(minecraft, id) }
        var crafted = 0
        var made = 0
        repeat(times) {
            onClient { minecraft ->
                val menu = minecraft.requirePlayer().containerMenu
                minecraft.gameMode!!.handlePlaceRecipe(menu.containerId, recipe, false)
            }
            Thread.sleep(CRAFT_WAIT_MILLIS)
            val count = onClient { minecraft ->
                val player = minecraft.requirePlayer()
                val menu = player.containerMenu as? AbstractCraftingMenu ?: return@onClient 0
                val result = menu.resultSlot.item
                if (result.isEmpty) return@onClient 0
                val count = result.count
                minecraft.gameMode!!.handleInventoryMouseClick(menu.containerId, menu.resultSlot.index, 0, ClickType.QUICK_MOVE, player)
                count
            }
            if (count == 0) {
                if (crafted == 0) throw ToolError("Could not craft $id: missing ingredients, or the server refused the recipe")
                return ToolResult.text("Crafted $crafted times ($made $id), then ran out of ingredients")
            }
            crafted++
            made += count
            Thread.sleep(CRAFT_WAIT_MILLIS)
        }
        return ToolResult.text("Crafted $crafted times ($made $id)")
    }

    /** A known, craftable recipe for [id] that fits the open crafting grid. Client thread. */
    private fun findRecipe(minecraft: Minecraft, id: String): RecipeDisplayId {
        val player = minecraft.requirePlayer()
        val menu = player.containerMenu as? AbstractCraftingMenu
            ?: throw ToolError("Close the open screen, or open a crafting table, before crafting")
        val context = SlotDisplayContext.fromLevel(player.level())
        val contents = StackedItemContents()
        player.inventory.fillStackedContents(contents)
        menu.fillCraftSlotsStackedContents(contents)
        val matching = player.recipeBook.collections.flatMap { it.recipes }
            .filter { entry -> entry.resultItems(context).any { BuiltInRegistries.ITEM.getKey(it.item).toString() == id } }
        if (matching.isEmpty()) throw ToolError("No unlocked recipe makes $id")
        val fitting = matching.filter { fits(it.display(), menu) }
        if (fitting.isEmpty()) throw ToolError("$id needs a 3x3 grid: open a crafting table first")
        return (fitting.firstOrNull { it.canCraft(contents) } ?: throw ToolError("Missing ingredients for $id")).id()
    }

    private fun fits(display: RecipeDisplay, menu: AbstractCraftingMenu) = when (display) {
        is ShapedCraftingRecipeDisplay -> CraftingFit.shaped(display.width(), display.height(), menu.gridWidth, menu.gridHeight)
        is ShapelessCraftingRecipeDisplay -> CraftingFit.shapeless(display.ingredients().size, menu.gridWidth, menu.gridHeight)
        else -> false
    }

    private const val CRAFT_WAIT_MILLIS = 150L

    private fun requireTakeOver(tool: String) {
        if (!AgentControl.agentInControl()) throw ToolError("$tool needs take_over first, so the person is not playing at the same time")
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
