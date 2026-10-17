package dev.luniiya.mctinytweak.client.damagenumbers

import dev.luniiya.mctinytweak.McTinyTweak
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

/**
 * The mod's own damage number sounds, synthesized by `tools/damage_sounds.py`. Only this client plays them,
 * so they need no registering: `sounds.json` is enough.
 */
object DamageSounds {
    val HEAVY = event("heavy")
    val BRUTAL = event("brutal")
    val DEVASTATING = event("devastating")
    val MAX = event("max")
    val SHATTER = event("shatter")
    val SLASH = event("slash")
    val IMPACT = event("impact")
    val MAX_TAG = event("max_tag")
    val NULL = event("null")
    val CRIT = event("crit")
    val COMBO_TICK = event("combo_tick")
    val COMBO_MILESTONE = event("combo_milestone")
    val COMBO_RANK = event("combo_rank")
    val COMBO_BREAK = event("combo_break")
    val HEAL = event("heal")
    val HEAL_BIG = event("heal_big")

    private fun event(name: String) = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(McTinyTweak.MOD_ID, "damage.$name"))

    /** Plays [sound] right at the player, so it hits just as hard wherever the mob is. */
    fun play(sound: SoundEvent, volume: Float, pitch: Float) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return
        level.playLocalSound(player.x, player.eyeY, player.z, sound, SoundSource.PLAYERS, volume, pitch, false)
    }
}
