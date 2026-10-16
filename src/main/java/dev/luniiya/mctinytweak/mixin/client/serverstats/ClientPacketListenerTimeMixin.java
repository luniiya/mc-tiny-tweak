package dev.luniiya.mctinytweak.mixin.client.serverstats;

import dev.luniiya.mctinytweak.client.serverstats.ServerStats;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds the server's time updates to the TPS estimate. TAIL only runs on the client thread. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerTimeMixin {
	@Inject(method = "handleSetTime", at = @At("TAIL"))
	private void mc_tiny_tweak$recordTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
		ServerStats.onTime(packet.gameTime());
	}
}
