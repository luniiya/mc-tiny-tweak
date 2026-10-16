package dev.luniiya.mctinytweak.mixin.client.hotbar;

import dev.luniiya.mctinytweak.client.hotbar.SplitHotbar;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the vanilla hotbar with the split one. */
@Mixin(Gui.class)
abstract class GuiMixin {
	@Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$renderSplitHotbar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		SplitHotbar.render((Gui) (Object) this, graphics, deltaTracker);
		ci.cancel();
	}
}
