package dev.luniiya.mctinytweak.mcp

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Per-client resource subscriptions. Notifications contain a URI, never an injected agent prompt. */
class McpChatEvents(private val enabled: () -> Boolean) {
    private class Session {
        @Volatile var subscribed = false
        @Volatile var wake = true
        @Volatile var touched = System.currentTimeMillis()
        val pending = LinkedBlockingQueue<String>(1) // Coalesce bursts; clients read the current resource.
    }
    private val sessions = ConcurrentHashMap<String, Session>()
    private val context = ThreadLocal<String?>()

    @Synchronized fun create(): String {
        val oldest = System.currentTimeMillis() - 30 * 60_000
        sessions.entries.removeIf { it.value.touched < oldest }
        if (sessions.size >= 64) throw ToolError("Too many MCP sessions; close an old session")
        val id = UUID.randomUUID().toString()
        sessions[id] = Session()
        return id
    }
    fun exists(id: String) = sessions.containsKey(id)
    fun remove(id: String) { sessions.remove(id) }
    fun clear() { sessions.clear() }
    fun clearPending() { sessions.values.forEach { it.pending.clear() } }
    fun <T> withSession(id: String?, block: () -> T): T {
        context.set(id)
        id?.let { sessions[it]?.touched = System.currentTimeMillis() }
        try { return block() } finally { context.remove() }
    }
    private fun current(): Session = context.get()?.let { sessions[it] }
        ?: throw ToolError("Initialize an HTTP MCP session first")
    fun subscribe(on: Boolean) {
        current().apply { subscribed = on; if (!on) pending.clear() }
    }
    fun wake(on: Boolean) {
        current().apply { wake = on; if (!on) pending.clear() }
    }
    fun muted() = !enabled() || !current().wake

    fun publishChat() {
        if (!enabled()) return
        for (session in sessions.values) if (session.subscribed && session.wake) session.pending.offer(UPDATE)
    }
    fun poll(id: String, timeoutMillis: Long): String? {
        val session = sessions[id] ?: return null
        session.touched = System.currentTimeMillis()
        val event = session.pending.poll(timeoutMillis, TimeUnit.MILLISECONDS)
        return event?.takeIf { enabled() && session.subscribed && session.wake && sessions[id] === session }
    }
    companion object {
        const val URI = "minecraft://chat"
        private const val UPDATE = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/resources/updated\",\"params\":{\"uri\":\"minecraft://chat\"}}"
    }
}

/** Track agent echoes separately: a human can type through the same local player. */
class ChatWakeFilter(private val clock: () -> Long = System::currentTimeMillis) {
    private val publicChat = Regex("^<([^>]+)>\\s+(.+)", RegexOption.DOT_MATCHES_ALL)
    private val echoes = ArrayDeque<Pair<String, Long>>()
    @Synchronized fun sentByAgent(text: String) {
        expire()
        if (echoes.size >= 32) echoes.removeFirst()
        echoes.addLast(text to clock())
    }
    private fun expire() { while (echoes.isNotEmpty() && clock() - echoes.first().second >= 30_000) echoes.removeFirst() }
    @Synchronized fun isIncoming(text: String, localName: String?): Boolean {
        if (localName == null) return false
        val match = publicChat.matchEntire(text) ?: return false
        if (match.groupValues[1] != localName) return true
        expire()
        val echo = echoes.indexOfFirst { it.first == match.groupValues[2] }
        if (echo < 0) return true
        echoes.removeAt(echo)
        return false
    }
}

/** Public chat wakes listeners; registered agent echoes, tool feedback and system notices do not. */
object ChatWake {
    private val filter = ChatWakeFilter()
    fun sentByAgent(text: String) = filter.sentByAgent(text)
    @JvmStatic fun isIncoming(text: String, localName: String?) = filter.isIncoming(text, localName)
}
