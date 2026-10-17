package dev.luniiya.mctinytweak.client.damagenumbers

import net.minecraft.client.Minecraft
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

/** Damage number sounds, which only this player hears. */
object DamageSounds {
    /** Plays [sound] right at the player, so it hits just as hard wherever the mob is. */
    fun play(sound: SoundEvent, volume: Float, pitch: Float) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val level = minecraft.level ?: return
        level.playLocalSound(player.x, player.eyeY, player.z, sound, SoundSource.PLAYERS, volume, pitch, false)
    }
}
