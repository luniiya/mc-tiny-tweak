package dev.luniiya.mctinytweak.mcp

/** Validate packet arguments before touching a menu; slot ids belong to that menu only. */
object ContainerClicks {
    fun validate(expectedId: Int, actualId: Int, slot: Int, size: Int, action: String, button: Int) {
        if (expectedId != actualId) throw ToolError("The menu changed; call get_container again")
        if (slot !in 0 until size && !(slot == -999 && action == "pickup")) throw ToolError("Invalid menu slot: $slot")
        val validButton = when (action) {
            "pickup", "quick_move", "throw" -> button in 0..1
            "swap" -> button in 0..8 || button == 40
            else -> throw ToolError("Unknown slot action: $action")
        }
        if (!validButton) throw ToolError("Invalid button $button for $action")
    }
}
