package dev.luniiya.mctinytweak.mixin.client.freecam;

import dev.luniiya.mctinytweak.client.freecam.AgentScreen;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The agent's screen keeps the mouse captured for the freecam and ignores the player's clicks. */
@Mixin(MouseHandler.class)
abstract class AgentScreenInputMixin {
	@Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$keepMouseForFreecam(CallbackInfo ci) {
		if (AgentScreen.open()) ci.cancel();
	}

	@Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$ignoreClicksOnAgentScreen(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if (AgentScreen.open()) ci.cancel();
	}
}
