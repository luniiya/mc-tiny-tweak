package dev.luniiya.mctinytweak.mcp

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.Socket

class McpHttpServerTest {
    private val token = "a".repeat(64)
    private val server = McpHttpServer(token) { body -> if (body.contains("\"id\"")) """{"echo":$body}""" else null }

    @AfterEach
    fun stop() = server.stop()

    /** Sends a raw request, so headers such as Host and Origin are exactly what a browser or agent would send. */
    private fun send(port: Int, method: String = "POST", path: String = "/mcp", body: String = """{"id":1}""", headers: Map<String, String> = mapOf()): Pair<Int, String> {
        val all = mapOf("Host" to "127.0.0.1:$port", "Authorization" to "Bearer $token") + headers
        Socket("127.0.0.1", port).use { socket ->
            val bytes = body.toByteArray()
            val head = "$method $path HTTP/1.1\r\n" + all.filterValues { it.isNotEmpty() }.entries.joinToString("") { "${it.key}: ${it.value}\r\n" } +
                "Content-Length: ${bytes.size}\r\n\r\n"
            socket.getOutputStream().write(head.toByteArray() + bytes)
            val response = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            return response.substringAfter(' ').substringBefore(' ').toInt() to response.substringAfter("\r\n\r\n")
        }
    }

    @Test
    fun `an authorized POST reaches the protocol`() {
        val port = server.start(freePort())
        assertEquals(200 to """{"echo":{"id":1}}""", send(port))
        assertEquals(202, send(port, body = "{}").first, "notifications are accepted without a body")
    }

    @Test
    fun `requests without the right token are refused`() {
        val port = server.start(freePort())
        assertEquals(401, send(port, headers = mapOf("Authorization" to "Bearer wrong")).first)
        assertEquals(401, send(port, headers = mapOf("Authorization" to "")).first)
    }

    @Test
    fun `web pages cannot reach the server`() {
        val port = server.start(freePort())
        assertEquals(403, send(port, headers = mapOf("Origin" to "https://evil.example")).first)
        assertEquals(403, send(port, headers = mapOf("Host" to "evil.example:$port")).first, "DNS rebinding")
        assertEquals(200, send(port, headers = mapOf("Origin" to "http://localhost:3000")).first)
    }

    @Test
    fun `only POST on the MCP path is served`() {
        val port = server.start(freePort())
        assertEquals(404, send(port, path = "/other").first)
        assertEquals(405, send(port, method = "GET", body = "").first)
    }

    @Test
    fun `a taken port makes the server use the next one`() {
        ServerSocket(0, 50, java.net.InetAddress.getLoopbackAddress()).use { taken ->
            val port = server.start(taken.localPort, attempts = 20)
            assertNotEquals(taken.localPort, port)
            assertTrue(port in taken.localPort + 1..taken.localPort + 20)
            assertEquals(port, server.port)
        }
    }

    @Test
    fun `host and origin checks accept only this computer`() {
        assertTrue(McpHttpServer.isLocalHost("localhost:25599"))
        assertTrue(McpHttpServer.isLocalHost("[::1]:25599"))
        assertFalse(McpHttpServer.isLocalHost(null))
        assertFalse(McpHttpServer.isLocalHost("127.0.0.1.evil.example"))
        assertTrue(McpHttpServer.isLocalOrigin(null))
        assertFalse(McpHttpServer.isLocalOrigin("null"))
        assertFalse(McpHttpServer.isLocalOrigin("http://localhost.evil.example"))
    }

    @Test
    fun `games starting from a taken port each get the next free one`() {
        val first = freePort()
        val others = List(3) { McpHttpServer(token) { null } }
        try {
            val ports = listOf(server.start(first, McpSettings.PORT_ATTEMPTS)) + others.map { it.start(first, McpSettings.PORT_ATTEMPTS) }
            assertEquals(4, ports.toSet().size, "every game has its own port: $ports")
            assertTrue(ports.all { it in first until first + McpSettings.PORT_ATTEMPTS })
        } finally {
            others.forEach { it.stop() }
        }
    }

    @Test
    fun `port searches stop at 65535 without overflow or invalid socket addresses`() {
        assertEquals(65530..65535, McpHttpServer.portsToTry(65530, 100))
        assertEquals(65535..65535, McpHttpServer.portsToTry(65535, Int.MAX_VALUE))
        assertEquals(25599..25698, McpHttpServer.portsToTry(25599, 100))
        for (port in listOf(-1, 65536)) assertThrows(IllegalArgumentException::class.java) { McpHttpServer.portsToTry(port, 1) }
        assertThrows(IllegalArgumentException::class.java) { McpHttpServer.portsToTry(25599, 0) }
    }

    private fun freePort() = ServerSocket(0).use { it.localPort }
}
