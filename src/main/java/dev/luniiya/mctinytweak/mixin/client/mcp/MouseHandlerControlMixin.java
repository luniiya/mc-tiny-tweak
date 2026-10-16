package dev.luniiya.mctinytweak.mixin.client.mcp;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.luniiya.mctinytweak.client.freecam.AgentScreen;
import dev.luniiya.mctinytweak.client.freecam.Freecam;
import dev.luniiya.mctinytweak.client.mcp.AgentControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the agent has taken over, moving the mouse does not turn the player and scrolling does not change slots.
 * In freecam the mouse turns the freecam instead.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerControlMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void mc_tiny_tweak$blockTurn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
		if (Freecam.shown()) Freecam.camera.turn(dx, dy);
		else if (!AgentControl.agentInControl()) original.call(player, dx, dy);
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$blockScroll(long window, double x, double y, CallbackInfo ci) {
		if (AgentControl.agentInControl() && (minecraft.screen == null || AgentScreen.open())) ci.cancel();
	}
}
