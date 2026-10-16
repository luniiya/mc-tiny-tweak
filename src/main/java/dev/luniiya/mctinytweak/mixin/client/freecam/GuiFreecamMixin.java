package dev.luniiya.mctinytweak.mixin.client.freecam;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luniiya.mctinytweak.client.freecam.Freecam;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The crosshair aims where the player looks, not the freecam, so it is hidden while flying. */
@Mixin(Gui.class)
abstract class GuiFreecamMixin {
	@ModifyExpressionValue(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
	private boolean mc_tiny_tweak$hideCrosshairInFreecam(boolean firstPerson) {
		return firstPerson && !Freecam.shown();
	}
}
