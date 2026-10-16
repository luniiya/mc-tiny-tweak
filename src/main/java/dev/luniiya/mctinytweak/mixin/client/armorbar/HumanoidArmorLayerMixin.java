package dev.luniiya.mctinytweak.mixin.client.armorbar;

import com.llamalad7.mixinextras.sugar.Local;
import dev.luniiya.mctinytweak.client.tweaks.Tweaks;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides armor on players (and mannequins) while Show armor is off. Mobs keep their armor. */
@Mixin(HumanoidArmorLayer.class)
abstract class HumanoidArmorLayerMixin {
	@Inject(
		method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void mc_tiny_tweak$hidePlayerArmor(CallbackInfo ci, @Local(argsOnly = true) HumanoidRenderState state) {
		if (state instanceof AvatarRenderState && !Tweaks.showArmor()) ci.cancel();
	}
}
