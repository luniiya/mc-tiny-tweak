package dev.luniiya.mctinytweak.damagenumbers

import com.google.gson.JsonParser
import dev.luniiya.mctinytweak.McTinyTweak
import net.fabricmc.loader.api.FabricLoader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.nameWithoutExtension

class DamageSoundsTest {
    private val container = FabricLoader.getInstance().getModContainer(McTinyTweak.MOD_ID).orElseThrow()

    @Test
    fun `every damage sound event has its ogg file and every file is used`() {
        val sounds = Files.newBufferedReader(container.findPath("assets/mc_tiny_tweak/sounds.json").orElseThrow()).use { JsonParser.parseReader(it).asJsonObject }
        val files = Files.list(container.findPath("assets/mc_tiny_tweak/sounds/damage").orElseThrow()).use { list ->
            list.filter { it.extension == "ogg" }.toList()
        }
        files.forEach { assertTrue(it.fileSize() > 1000, "${it.fileName} looks empty") }
        val referenced = sounds.entrySet().filter { it.key.startsWith("damage.") }.flatMap { (_, event) ->
            event.asJsonObject.getAsJsonArray("sounds").map { it.asString.removePrefix("${McTinyTweak.MOD_ID}:damage/") }
        }
        assertEquals(files.map { it.nameWithoutExtension }.sorted(), referenced.sorted())
        for ((event, value) in sounds.entrySet()) {
            assertEquals(listOf("${McTinyTweak.MOD_ID}:damage/${event.removePrefix("damage.")}"), value.asJsonObject.getAsJsonArray("sounds").map { it.asString }, event)
        }
    }
}
