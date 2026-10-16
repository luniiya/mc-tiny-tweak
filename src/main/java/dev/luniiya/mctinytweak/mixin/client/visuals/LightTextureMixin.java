package dev.luniiya.mctinytweak.mixin.client.visuals;

import dev.luniiya.mctinytweak.client.tweaks.Tweaks;
import dev.luniiya.mctinytweak.visuals.Fullbright;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fullbright: the lightmap's fourth value is the night vision strength. */
@Mixin(LightTexture.class)
abstract class LightTextureMixin {
	@ModifyArg(
		method = "updateLightTexture",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3)
	)
	private float mc_tiny_tweak$fullbright(float nightVision) {
		return Fullbright.nightVision(nightVision, Tweaks.fullbright());
	}
}
