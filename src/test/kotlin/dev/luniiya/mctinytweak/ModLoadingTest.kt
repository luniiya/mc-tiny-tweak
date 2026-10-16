package dev.luniiya.mctinytweak

import com.google.gson.JsonParser
import net.fabricmc.api.ModInitializer
import net.fabricmc.language.kotlin.KotlinAdapter
import net.fabricmc.loader.api.FabricLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ModLoadingTest {
    private val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()

    private fun resource(name: String) = Files.newBufferedReader(container.findPath(name).orElseThrow {
        IllegalStateException("Missing packaged resource: $name")
    }).use { JsonParser.parseReader(it).asJsonObject }

    @Test
    fun `packaged metadata declares the current game and mod versions`() {
        val metadata = resource("fabric.mod.json")
        assertEquals(McTinyTweak.MOD_ID, metadata["id"].asString)
        assertEquals(System.getProperty("expected.mod.version"), metadata["version"].asString)
        val dependencies = metadata.getAsJsonObject("depends")
        assertEquals(System.getProperty("expected.minecraft.version"), dependencies["minecraft"].asString)
        assertTrue(dependencies.has("fabricloader"))
        assertTrue(dependencies.has("fabric-api"))
        assertTrue(dependencies.has("fabric-language-kotlin"))
    }

    @Test
    fun `Kotlin adapter resolves the declared entrypoint`() {
        val entrypoint = resource("fabric.mod.json").getAsJsonObject("entrypoints")
            .getAsJsonArray("main").single().asJsonObject
        assertEquals("kotlin", entrypoint["adapter"].asString)
        val instance = KotlinAdapter().create(container, entrypoint["value"].asString, ModInitializer::class.java)
        assertSame(McTinyTweak, instance)
        assertDoesNotThrow { instance.onInitialize() }
    }

    @Test
    fun `every declared mixin configuration is packaged and its classes exist`() {
        val metadata = resource("fabric.mod.json")
        for (entry in metadata.getAsJsonArray("mixins")) {
            val config = resource(entry.asString)
            assertTrue(config["required"].asBoolean)
            assertEquals(1, config.getAsJsonObject("injectors")["defaultRequire"].asInt)
            for (side in listOf("mixins", "client", "server")) {
                for (mixin in config.getAsJsonArray(side) ?: continue) {
                    Class.forName("${config["package"].asString}.${mixin.asString}", false, javaClass.classLoader)
                }
            }
        }
    }
}
