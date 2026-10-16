package dev.luniiya.mctinytweak.mixin.client.freecam;

import dev.luniiya.mctinytweak.client.freecam.AgentScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player's keys do not reach the agent's screen; Escape cannot close it. */
@Mixin(KeyboardHandler.class)
abstract class AgentScreenKeyboardMixin {
	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$keepKeysFromAgentScreen(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (!AgentScreen.open()) return;
		if (action == 1) AgentScreen.keyPressed(event);
		ci.cancel();
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$keepTypingFromAgentScreen(long window, CharacterEvent event, CallbackInfo ci) {
		if (AgentScreen.open()) ci.cancel();
	}
}
