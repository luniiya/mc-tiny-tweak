package dev.luniiya.mctinytweak.mcp

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SchemaTest {
    private val schema = Schema.obj(
        Schema.integer("slot", "", required = true, min = 1, max = 9),
        Schema.stringList("keys", ""),
    )

    @Test
    fun `invalid slot types and bounds cannot silently select the default slot`() {
        for (json in listOf("{}", "{\"slot\":\"2\"}", "{\"slot\":null}", "{\"slot\":1.5}", "{\"slot\":0}", "{\"slot\":10}", "{\"slot\":1e400}")) {
            assertThrows(ToolError::class.java) { Schema.validate(JsonParser.parseString(json).asJsonObject, schema) }
        }
    }

    @Test
    fun `valid boundary slots and optional arrays work but malformed keys fail`() {
        for (json in listOf("{\"slot\":1}", "{\"slot\":9,\"keys\":[\"forward\"]}")) {
            assertDoesNotThrow { Schema.validate(JsonParser.parseString(json).asJsonObject, schema) }
        }
        for (json in listOf("{\"slot\":1,\"keys\":[1]}", "{\"slot\":1,\"keys\":false}")) {
            assertThrows(ToolError::class.java) { Schema.validate(JsonParser.parseString(json).asJsonObject, schema) }
        }
    }
}
