package dev.luniiya.mctinytweak.itemeditor

/** Converts between a multi-line text area and lore lines. */
object LoreLines {
    /** One lore line per text line. Trailing blank lines are dropped; blank lines in between are kept as spacers. */
    fun split(text: String): List<String> = text.split('\n').dropLastWhile { it.isBlank() }

    fun join(lines: List<String>): String = lines.joinToString("\n")
}
