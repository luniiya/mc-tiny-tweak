package dev.luniiya.mctinytweak.mixin.client.freecam;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luniiya.mctinytweak.client.freecam.Freecam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The first person hand stays with the player, so it is not drawn in front of the freecam. */
@Mixin(GameRenderer.class)
abstract class GameRendererFreecamMixin {
	@ModifyExpressionValue(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
	private boolean mc_tiny_tweak$hideHandInFreecam(boolean firstPerson) {
		return firstPerson && !Freecam.shown();
	}
}
