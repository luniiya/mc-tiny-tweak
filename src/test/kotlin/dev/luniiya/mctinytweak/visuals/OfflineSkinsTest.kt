package dev.luniiya.mctinytweak.visuals

import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.client.multiplayer.PlayerInfo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class OfflineSkinsTest {
    private val direct = java.util.concurrent.Executor { it.run() }

    @AfterEach
    fun reset() = OfflineSkins.clearCache()

    private fun profile(name: String, textures: Boolean = false) = GameProfile(
        UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()),
        name,
        PropertyMap(if (textures) ImmutableMultimap.of("textures", Property("textures", "e30=", "sig")) else ImmutableMultimap.of()),
    )

    @Test
    fun `only profiles without textures are looked up`() {
        assertTrue(OfflineSkins.needsLookup(profile("Narpwa")), "offline-mode servers send no textures")
        assertFalse(OfflineSkins.needsLookup(profile("Narpwa", textures = true)), "online servers keep their own skins")
        assertFalse(OfflineSkins.needsLookup(profile("not a name!")), "invalid names can't be real accounts")
    }

    @Test
    fun `each name is fetched once whatever its case`() {
        var fetches = 0
        val real = profile("Narpwa", textures = true)
        val fetch = { _: String -> fetches++; Optional.of(real) }
        assertEquals(real, OfflineSkins.realProfile("Narpwa", fetch, direct).join().get())
        assertEquals(real, OfflineSkins.realProfile("narpwa", fetch, direct).join().get())
        assertEquals(1, fetches)
    }

    @Test
    fun `a failed fetch means no real account`() {
        val result = OfflineSkins.realProfile("Ghost", { throw IllegalStateException("Mojang is down") }, direct).join()
        assertTrue(result.isEmpty)
    }

    @Test
    fun `the mixin target exists`() {
        assertNotNull(PlayerInfo::class.java.declaredMethods.find { it.name == "createSkinLookup" })
    }
}
