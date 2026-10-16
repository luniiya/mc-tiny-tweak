package dev.luniiya.mctinytweak.attributes

import java.math.BigDecimal

/** Builds `/attribute` commands that change the player's own attributes. */
object AttributeCommands {
    /** Returns the command without its leading slash, as `ClientPacketListener.sendCommand` expects. */
    fun setBase(attribute: String, value: Double): String {
        require(value.isFinite()) { "Value must be a finite number" }
        return "attribute @s $attribute base set ${format(value)}"
    }

    fun resetBase(attribute: String): String = "attribute @s $attribute base reset"

    /**
     * Writes a value without an exponent, because Brigadier's number reader stops at `E`
     * and some defaults are huge, like the waypoint ranges' 6.0E7.
     */
    fun format(value: Double): String {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    /** Reads typed text as a base value inside the attribute's range, or null if it is not one. */
    fun parse(text: String, min: Double, max: Double): Double? {
        val value = text.trim().toDoubleOrNull() ?: return null
        return value.takeIf { it.isFinite() && it in min..max }
    }
}
