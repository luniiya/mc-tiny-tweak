package dev.luniiya.mctinytweak

import dev.luniiya.mctinytweak.defaults.PersonalDefaults
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import org.slf4j.LoggerFactory

/** Runs before the game reads options.txt, so a fresh game starts with the personal defaults. */
object McTinyTweakPreLaunch : PreLaunchEntrypoint {
    override fun onPreLaunch() {
        val loader = FabricLoader.getInstance()
        if (loader.environmentType != EnvType.CLIENT) return
        val optionsFile = loader.gameDir.resolve("options.txt")
        if (PersonalDefaults.writeIfMissing(optionsFile)) {
            LoggerFactory.getLogger(McTinyTweak.MOD_ID).info("Wrote personal default options to {}", optionsFile)
        }
    }
}
