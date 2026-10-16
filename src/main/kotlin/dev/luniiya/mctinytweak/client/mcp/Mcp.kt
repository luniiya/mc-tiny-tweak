package dev.luniiya.mctinytweak.client.mcp

import dev.luniiya.mctinytweak.McTinyTweak
import dev.luniiya.mctinytweak.client.Notices
import dev.luniiya.mctinytweak.client.freecam.AgentStatusCard
import dev.luniiya.mctinytweak.mcp.ChatResources
import dev.luniiya.mctinytweak.mcp.McpChatEvents
import dev.luniiya.mctinytweak.mcp.ChatLog
import dev.luniiya.mctinytweak.mcp.McpHttpServer
import dev.luniiya.mctinytweak.mcp.McpProtocol
import dev.luniiya.mctinytweak.mcp.McpSettings
import dev.luniiya.mctinytweak.mcp.ToolCallSummary
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom

/**
 * The MCP server that lets an agent play this game instance. Settings and the access token live in the
 * instance's config folder, so every instance has its own switch and token.
 */
object Mcp {
    private val configDir: Path get() = FabricLoader.getInstance().configDir
    private val settingsFile: Path get() = configDir.resolve(McpSettings.FILE_NAME)
    private val tokenFile: Path get() = configDir.resolve(TOKEN_FILE_NAME)

    const val TOKEN_FILE_NAME = "mc_tiny_tweak_mcp_token.txt"

    @JvmStatic
    val chat = ChatLog(onAdded = { if (it.wakeable) events.publishChat() })

    val events = McpChatEvents { settings.enabled && settings.wakeOnChat && settings.allows(McpSettings.Feature.CHAT_READ) }

    @Volatile
    var settings = McpSettings()
        private set

    private var token = ""
    private var server: McpHttpServer? = null

    /** The port the server listens on, or null while it is off or failed to start. */
    val port: Int? get() = server?.port

    /** Why the server is not running although it is switched on. */
    var startError: String? = null
        private set

    fun load() {
        settings = try {
            if (Files.exists(settingsFile)) McpSettings.parse(Files.readString(settingsFile)) else McpSettings()
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not read {}, using defaults", settingsFile, e)
            McpSettings()
        }
        token = readOrCreateToken()
        applyRunning()
    }

    fun update(change: (McpSettings) -> McpSettings) {
        val before = settings
        settings = change(settings)
        save()
        if (!settings.wakeOnChat || !settings.allows(McpSettings.Feature.CHAT_READ)) events.clearPending()
        if (!settings.enabled || !settings.allows(McpSettings.Feature.TAKE_OVER)) {
            Minecraft.getInstance().execute { AgentControl.release(byPlayer = true) }
        }
        if (before.enabled != settings.enabled || before.port != settings.port) {
            stop()
            applyRunning()
        }
    }

    /** Replaces the token, which disconnects every agent set up with the old one. */
    fun regenerateToken() {
        token = newToken()
        writeToken()
        stop()
        applyRunning()
    }

    /** A Claude Code command that connects an agent to this instance, or null while the server is off. */
    fun setupCommand(): String? {
        val bound = port ?: return null
        return "claude mcp add --transport http ${McpSettings.serverName(bound)} http://127.0.0.1:$bound${McpHttpServer.PATH} " +
            "--header \"Authorization: Bearer $token\""
    }

    private fun applyRunning() {
        startError = null
        if (!settings.enabled || server != null) return
        val protocol = McpProtocol(
            serverName = "MC Tiny Tweak",
            serverVersion = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).map { it.metadata.version.friendlyString }.orElse("dev"),
            tools = GameTools::all,
            chatResources = ChatResources(chat, events) { settings.allows(McpSettings.Feature.CHAT_READ) },
            allowed = { settings.allows(it) },
            onError = { tool, e -> McTinyTweak.logger.warn("MCP tool {} failed", tool, e) },
            onCall = { tool, arguments ->
                val summary = ToolCallSummary.describe(tool.name, arguments)
                AgentStatusCard.lastAction = summary
                if (settings.showActions) Notices.show(Component.translatable("notice.mc_tiny_tweak.mcp.action", summary))
            },
        )
        val created = McpHttpServer(token, protocol::handle).apply { chatEvents = events }
        try {
            val bound = created.start(settings.port, McpSettings.PORT_ATTEMPTS)
            server = created
            if (bound != settings.port) {
                settings = settings.rememberingPort(bound)
                save()
            }
            McTinyTweak.logger.info("MCP server listening on 127.0.0.1:{}", bound)
        } catch (e: Exception) {
            startError = e.message ?: e.javaClass.simpleName
            McTinyTweak.logger.warn("Could not start the MCP server", e)
        }
    }

    private fun stop() {
        server?.stop()
        server = null
        AgentInput.releaseAll()
    }

    private fun save() {
        try {
            Files.createDirectories(settingsFile.parent)
            Files.writeString(settingsFile, settings.serialize())
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", settingsFile, e)
        }
    }

    private fun readOrCreateToken(): String {
        try {
            if (Files.exists(tokenFile)) Files.readString(tokenFile).trim().takeIf { it.length >= 32 }?.let { return it }
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not read {}, making a new token", tokenFile, e)
        }
        token = newToken()
        writeToken()
        return token
    }

    private fun writeToken() {
        try {
            Files.createDirectories(tokenFile.parent)
            Files.writeString(tokenFile, token + "\n")
        } catch (e: Exception) {
            McTinyTweak.logger.warn("Could not save {}", tokenFile, e)
        }
    }

    private fun newToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
