package dev.luniiya.mctinytweak.mixin.client.visuals;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.luniiya.mctinytweak.visuals.ShieldOpacity;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Renders first person shields with a translucent render type and a faded tint. */
@Mixin(ShieldSpecialRenderer.class)
abstract class ShieldSpecialRendererMixin {
	@Inject(method = "submit", at = @At("HEAD"))
	private void mc_tiny_tweak$startHeldShield(CallbackInfo ci, @Local(argsOnly = true) ItemDisplayContext context) {
		ShieldOpacity.setRenderingHeld(context.firstPerson());
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void mc_tiny_tweak$endHeldShield(CallbackInfo ci) {
		ShieldOpacity.setRenderingHeld(false);
	}

	@WrapOperation(
		method = "submit",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/object/equipment/ShieldModel;renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
	)
	private RenderType mc_tiny_tweak$translucentShield(ShieldModel model, Identifier texture, Operation<RenderType> original) {
		return ShieldOpacity.getRenderingHeld() ? RenderTypes.entityTranslucent(texture) : original.call(model, texture);
	}

	@ModifyArg(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart"), index = 8)
	private int mc_tiny_tweak$fadeShield(int color) {
		return ShieldOpacity.tint(color);
	}
}
