package dev.luniiya.mctinytweak.visuals

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.util.StringUtil
import net.minecraft.util.Util
import net.minecraft.world.entity.player.PlayerSkin
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Supplier

/**
 * Skins on offline-mode servers. Such servers send player profiles without a textures property, so everyone shows
 * a default skin. For those profiles the skin of the real account with the same name is fetched from Mojang instead.
 * Online servers always send textures, so they are never touched.
 */
object OfflineSkins {
    private val realProfiles = ConcurrentHashMap<String, CompletableFuture<Optional<GameProfile>>>()

    fun needsLookup(profile: GameProfile): Boolean =
        profile.properties()["textures"].isEmpty() && StringUtil.isValidPlayerName(profile.name())

    /** The real account named [name], fetched once per name for the whole session; empty when there is none. */
    fun realProfile(name: String, fetch: (String) -> Optional<GameProfile>, executor: Executor): CompletableFuture<Optional<GameProfile>> =
        realProfiles.computeIfAbsent(name.lowercase(Locale.ROOT)) {
            CompletableFuture.supplyAsync({ fetch(name) }, executor).exceptionally { Optional.empty() }
        }

    /** Like vanilla's lookup: the default skin until the real one has loaded, and only signed skins for other players. */
    fun lookup(profile: GameProfile, secureOnly: Boolean): Supplier<PlayerSkin> {
        val minecraft = Minecraft.getInstance()
        val fallback = DefaultPlayerSkin.get(profile)
        val skin = realProfile(profile.name(), minecraft.services().profileResolver()::fetchByName, Util.nonCriticalIoPool())
            .thenCompose { real -> real.map(minecraft.skinManager::get).orElse(CompletableFuture.completedFuture(Optional.empty())) }
        return Supplier { skin.getNow(Optional.empty()).filter { !secureOnly || it.secure() }.orElse(fallback) }
    }

    internal fun clearCache() = realProfiles.clear()
}
