package dev.luniiya.mctinytweak

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object McTinyTweak : ModInitializer {
    const val MOD_ID = "mc_tiny_tweak"

    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        logger.info("MC Tiny Tweak loaded")
    }
}
