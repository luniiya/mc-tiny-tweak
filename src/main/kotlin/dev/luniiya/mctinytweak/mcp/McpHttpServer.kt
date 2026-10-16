package dev.luniiya.mctinytweak.mcp

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A minimal HTTP/1.1 server for MCP's Streamable HTTP transport, answering POST /mcp with plain JSON.
 * It is written on sockets because the JDK's httpserver module is missing from some game runtimes.
 *
 * Only this computer can connect. Every request needs the bearer [token], and requests from web pages
 * (a foreign Origin or Host) are refused, so a website cannot drive the game through the browser.
 */
class McpHttpServer(
    private val token: String,
    private val handle: (String) -> String?,
) {
    private var socket: ServerSocket? = null
    private var workers: ExecutorService? = null

    /** The bound port, or null while stopped. */
    val port: Int? get() = socket?.localPort

    /** Binds the first free port from [firstPort], trying [attempts] ports in a row, and returns it. */
    @Synchronized
    fun start(firstPort: Int, attempts: Int = 10): Int {
        check(socket == null) { "Already running" }
        var lastError: IOException? = null
        for (candidate in firstPort until firstPort + attempts) {
            val server = ServerSocket()
            try {
                server.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), candidate))
            } catch (e: BindException) {
                server.close()
                lastError = e
                continue
            }
            socket = server
            val pool = Executors.newCachedThreadPool { runnable -> Thread(runnable, "MC Tiny Tweak MCP").apply { isDaemon = true } }
            workers = pool
            pool.execute { acceptLoop(server, pool) }
            return server.localPort
        }
        throw lastError ?: IOException("No port to try")
    }

    @Synchronized
    fun stop() {
        socket?.close()
        socket = null
        workers?.shutdownNow()
        workers = null
    }

    private fun acceptLoop(server: ServerSocket, pool: ExecutorService) {
        while (!server.isClosed) {
            val client = try {
                server.accept()
            } catch (e: IOException) {
                return
            }
            pool.execute { client.use(::serve) }
        }
    }

    private fun serve(client: Socket) {
        client.soTimeout = READ_TIMEOUT_MILLIS
        val output = client.getOutputStream()
        val request = try {
            readRequest(BufferedInputStream(client.getInputStream()))
        } catch (e: RequestError) {
            return respond(output, e.status, e.message ?: "")
        } catch (e: IOException) {
            return
        }
        val (status, body) = route(request)
        respond(output, status, body, if (status == 200) "application/json" else "text/plain")
    }

    internal data class Request(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    internal fun route(request: Request): Pair<Int, String> {
        if (request.path.substringBefore('?') != PATH) return 404 to "Not found"
        if (!isLocalOrigin(request.headers["origin"]) || !isLocalHost(request.headers["host"])) return 403 to "Forbidden"
        if (!hasToken(request.headers["authorization"])) return 401 to "Missing or wrong token"
        if (request.method != "POST") return 405 to "Only POST is supported"
        val reply = handle(request.body) ?: return 202 to ""
        return 200 to reply
    }

    private fun hasToken(header: String?): Boolean {
        val given = header?.removePrefix("Bearer ")?.trim() ?: return false
        return MessageDigest.isEqual(given.toByteArray(), token.toByteArray())
    }

    private class RequestError(val status: Int, message: String) : Exception(message)

    private fun readRequest(input: InputStream): Request {
        val head = ByteArrayOutputStream()
        var matched = 0
        while (matched < 4) {
            val b = input.read()
            if (b < 0) throw IOException("Closed")
            head.write(b)
            if (head.size() > MAX_HEADER_BYTES) throw RequestError(431, "Headers too large")
            matched = if (b == HEADER_END[matched].code) matched + 1 else if (b == '\r'.code) 1 else 0
        }
        val lines = head.toString(Charsets.ISO_8859_1).split("\r\n").filter { it.isNotEmpty() }
        val parts = lines.firstOrNull()?.split(' ') ?: throw RequestError(400, "Bad request")
        if (parts.size != 3) throw RequestError(400, "Bad request")
        val headers = lines.drop(1).filter { ':' in it }
            .associate { it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim() }
        if (headers["transfer-encoding"] != null) throw RequestError(411, "Send a Content-Length")
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length !in 0..MAX_BODY_BYTES) throw RequestError(413, "Body too large")
        val body = input.readNBytes(length)
        if (body.size < length) throw IOException("Closed early")
        return Request(parts[0], parts[1], headers, body.toString(Charsets.UTF_8))
    }

    private fun respond(output: OutputStream, status: Int, body: String, contentType: String = "text/plain") {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val head = "HTTP/1.1 $status ${REASONS[status] ?: "Status"}\r\n" +
            "Content-Type: $contentType; charset=utf-8\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        try {
            output.write(head.toByteArray(Charsets.ISO_8859_1))
            output.write(bytes)
            output.flush()
        } catch (_: IOException) {
        }
    }

    companion object {
        const val PATH = "/mcp"
        private const val HEADER_END = "\r\n\r\n"
        private const val MAX_HEADER_BYTES = 16 * 1024
        private const val MAX_BODY_BYTES = 1024 * 1024
        private const val READ_TIMEOUT_MILLIS = 30_000
        private val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "[::1]")
        private val REASONS = mapOf(
            200 to "OK", 202 to "Accepted", 400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden",
            404 to "Not Found", 405 to "Method Not Allowed", 411 to "Length Required", 413 to "Payload Too Large",
            431 to "Request Header Fields Too Large",
        )

        /** Agents send no Origin; a browser always does, and only local pages are let through. */
        fun isLocalOrigin(origin: String?): Boolean {
            if (origin == null) return true
            val host = try {
                URI(origin).host
            } catch (e: Exception) {
                return false
            } ?: return false
            return host in LOCAL_HOSTS || "[$host]" in LOCAL_HOSTS
        }

        /** Guards against DNS rebinding, where a website's name resolves to 127.0.0.1. */
        fun isLocalHost(host: String?): Boolean {
            if (host == null) return false
            val name = if (host.startsWith("[")) host.substringBefore(']') + "]" else host.substringBefore(':')
            return name.lowercase() in LOCAL_HOSTS
        }
    }
}
