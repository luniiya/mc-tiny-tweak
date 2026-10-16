package dev.luniiya.mctinytweak.mixin.client.visuals;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.luniiya.mctinytweak.visuals.ShieldOpacity;
import java.util.function.Function;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fades the base and pattern layers of a held shield; banners themselves never set the flag. */
@Mixin(BannerRenderer.class)
abstract class BannerRendererMixin {
	/** Material caches its first render type, so the call is replaced rather than given another factory. */
	@WrapOperation(
		method = "submitPatterns",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/Material;renderType(Ljava/util/function/Function;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
	)
	private static RenderType mc_tiny_tweak$translucentShieldBase(
		Material material, Function<Identifier, RenderType> factory, Operation<RenderType> original
	) {
		return ShieldOpacity.getRenderingHeld() ? RenderTypes.entityTranslucent(material.atlasLocation()) : original.call(material, factory);
	}

	@ModifyArg(
		method = {"submitPatterns", "submitPatternLayer"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel"),
		index = 6
	)
	private static int mc_tiny_tweak$fadeShieldLayers(int color) {
		return ShieldOpacity.tint(color);
	}
}
