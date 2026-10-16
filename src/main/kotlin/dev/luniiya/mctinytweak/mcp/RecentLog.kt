package dev.luniiya.mctinytweak.mcp

/** The newest [capacity] entries with the time each arrived, oldest first. Safe from any thread. */
class RecentLog<T>(private val capacity: Int) {
    data class Entry<T>(val timeMillis: Long, val value: T)

    private val entries = ArrayDeque<Entry<T>>()

    /** Goes up on every change, so a screen can tell when to redraw its lines. */
    @Volatile
    var version = 0L
        private set

    @Synchronized
    fun add(value: T, timeMillis: Long = System.currentTimeMillis()) {
        entries.addLast(Entry(timeMillis, value))
        while (entries.size > capacity) entries.removeFirst()
        version++
    }

    @Synchronized
    fun snapshot(): List<Entry<T>> = entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
        version++
    }
}
