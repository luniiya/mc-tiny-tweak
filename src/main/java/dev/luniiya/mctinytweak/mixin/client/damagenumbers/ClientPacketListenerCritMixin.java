package dev.luniiya.mctinytweak.mixin.client.damagenumbers;

import dev.luniiya.mctinytweak.client.damagenumbers.DamageNumberPopups;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the damage numbers which entity just took a critical hit. TAIL only runs on the client thread. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerCritMixin {
	@Inject(method = "handleAnimate", at = @At("TAIL"))
	private void mc_tiny_tweak$recordCrit(ClientboundAnimatePacket packet, CallbackInfo ci) {
		if (packet.getAction() == ClientboundAnimatePacket.CRITICAL_HIT) DamageNumberPopups.critical(packet.getId());
	}
}
