package dev.luniiya.mctinytweak

import net.minecraft.SharedConstants
import net.minecraft.core.HolderLookup
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.server.Bootstrap

/** Bootstraps vanilla registries once for tests that touch items, components or text. */
object MinecraftTestSupport {
    val registries: HolderLookup.Provider by lazy {
        bootstrap()
        VanillaRegistries.createLookup()
    }

    fun bootstrap() {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }
}
