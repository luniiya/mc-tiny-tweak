package dev.luniiya.mctinytweak.mixin.client.controllayer;

import dev.luniiya.mctinytweak.client.controllayer.ControlActions;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ctrl + a mod shortcut key is taken before vanilla sees it, so the key's own binding never fires. */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerControlLayerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$controlLayer(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (window != this.minecraft.getWindow().handle()) return;
		if (ControlActions.onKey(event.key(), action, event.hasControlDown())) ci.cancel();
	}
}
