package dev.luniiya.mctinytweak.mixin.client.visuals;

import dev.luniiya.mctinytweak.visuals.FireOverlay;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Lowers the burning overlay. */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
	@ModifyArg(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
	private static float mc_tiny_tweak$lowerFire(float y) {
		return FireOverlay.lowered(y);
	}
}
