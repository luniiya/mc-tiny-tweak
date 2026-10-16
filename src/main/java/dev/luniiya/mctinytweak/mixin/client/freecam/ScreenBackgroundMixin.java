package dev.luniiya.mctinytweak.mixin.client.freecam;

import dev.luniiya.mctinytweak.client.freecam.AgentScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The agent's screen panel keeps the world visible instead of darkening it. */
@Mixin(Screen.class)
abstract class ScreenBackgroundMixin {
	@Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$skipPanelBackground(GuiGraphics graphics, CallbackInfo ci) {
		if (AgentScreen.getDrawingPanel()) ci.cancel();
	}
}
