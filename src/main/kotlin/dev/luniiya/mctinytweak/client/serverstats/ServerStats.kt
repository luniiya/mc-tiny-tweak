package dev.luniiya.mctinytweak.client.serverstats

import dev.luniiya.mctinytweak.serverstats.ServerStatsFormat
import dev.luniiya.mctinytweak.serverstats.TpsEstimator
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.network.chat.Component

/**
 * TPS and latency in the top right corner of the pause menu (the music toast uses the top left).
 * Singleplayer reads the built-in server's tick time; multiplayer estimates TPS from time updates
 * and shows the ping the server reports in the tab list.
 */
object ServerStats {
    private val estimator = TpsEstimator()

    /** The connection the estimate belongs to, so a new server starts fresh. */
    private var connection: Any? = null

    @JvmStatic
    fun onTime(gameTime: Long) {
        val current = Minecraft.getInstance().connection
        if (current !== connection) {
            connection = current
            estimator.clear()
        }
        estimator.add(gameTime, System.nanoTime())
    }

    fun register() {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (screen is PauseScreen && screen.showsPauseMenu()) {
                ScreenEvents.afterRender(screen).register { _, graphics, _, _, _ -> render(screen, graphics) }
            }
        }
    }

    private fun tickRate(minecraft: Minecraft) = minecraft.level?.tickRateManager()?.tickrate() ?: 20f

    private fun tps(minecraft: Minecraft): Double? {
        val server = minecraft.singleplayerServer
        if (server != null) {
            val nanos = server.averageTickTimeNanos.takeIf { it > 0 } ?: return null
            return (1e9 / nanos).coerceAtMost(server.tickRateManager().tickrate().toDouble())
        }
        if (minecraft.connection !== connection) return null
        return estimator.tps(tickRate(minecraft))
    }

    private fun render(screen: PauseScreen, graphics: GuiGraphics) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val font = minecraft.font
        val tickRate = tickRate(minecraft)
        val tps = tps(minecraft)
        val tpsLine = Component.translatable("screen.mc_tiny_tweak.server_stats.tps", tps?.let(ServerStatsFormat::tps) ?: "...")
        val tpsColor = tps?.let { ServerStatsFormat.tpsColor(it, tickRate) } ?: 0xFFAAAAAA.toInt()
        val latency = if (minecraft.isLocalServer) null else minecraft.connection?.getPlayerInfo(player.uuid)?.latency
        val latencyLine = latency?.let { Component.translatable("screen.mc_tiny_tweak.server_stats.latency", it) }
            ?: Component.translatable("screen.mc_tiny_tweak.server_stats.local")
        val latencyColor = latency?.let(ServerStatsFormat::latencyColor) ?: 0xFFAAAAAA.toInt()
        val right = screen.width - MARGIN
        graphics.drawString(font, tpsLine, right - font.width(tpsLine), MARGIN, tpsColor)
        graphics.drawString(font, latencyLine, right - font.width(latencyLine), MARGIN + font.lineHeight + 2, latencyColor)
    }

    private const val MARGIN = 6
}
