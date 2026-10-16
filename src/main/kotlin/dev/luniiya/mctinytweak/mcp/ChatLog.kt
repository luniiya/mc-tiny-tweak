package dev.luniiya.mctinytweak.mcp

/** The most recent chat lines, numbered so an agent can ask for only what arrived after the last line it saw. */
class ChatLog(private val capacity: Int = DEFAULT_CAPACITY, private val onAdded: (Line) -> Unit = {}) {
    data class Line(val id: Long, val timeMillis: Long, val text: String, val wakeable: Boolean = false)

    private val lines = ArrayDeque<Line>()
    private var nextId = 1L

    @Synchronized
    fun add(text: String, timeMillis: Long = System.currentTimeMillis(), wakeable: Boolean = false): Line {
        val line = Line(nextId++, timeMillis, text, wakeable)
        lines.addLast(line)
        while (lines.size > capacity) lines.removeFirst()
        onAdded(line)
        return line
    }

    /** Lines with an id above [afterId], keeping the newest [limit]. */
    @Synchronized
    fun after(afterId: Long, limit: Int): List<Line> = lines.filter { it.id > afterId }.takeLast(limit)

    @Synchronized
    fun incomingAfter(afterId: Long, limit: Int): List<Line> = lines.filter { it.id > afterId && it.wakeable }.takeLast(limit)

    /** The id of the newest line, or 0 when nothing has arrived yet. */
    @Synchronized
    fun lastId(): Long = lines.lastOrNull()?.id ?: 0L

    companion object {
        const val DEFAULT_CAPACITY = 500
    }
}
