package dev.luniiya.mctinytweak.mixin.client.armorbar;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.luniiya.mctinytweak.armorbar.ArmorBar;
import dev.luniiya.mctinytweak.client.tweaks.Tweaks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While armor is hidden, shows the armor bar one line above the player's name. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
	/** One line of name tag text, in the text pixels the name tag offset uses; negative moves up. */
	private static final int LINE_HEIGHT = 10;

	@Inject(
		method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")
	)
	private void mc_tiny_tweak$submitArmorBar(
		AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci
	) {
		if (Tweaks.showArmor() || state.nameTag == null) return;
		// Same offset vanilla gives the name, which moves up for players showing extra ears.
		int nameOffset = state.showExtraEars ? -10 : 0;
		var segments = ArmorBar.INSTANCE.segments(state.headEquipment, state.chestEquipment, state.legsEquipment, state.feetEquipment);
		collector.submitNameTag(
			poseStack, state.nameTagAttachment, nameOffset - LINE_HEIGHT, ArmorBar.INSTANCE.component(segments),
			!state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera
		);
	}
}
