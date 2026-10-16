package dev.luniiya.mctinytweak.mixin.client.armorbar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.luniiya.mctinytweak.armorbar.ArmorBar;
import dev.luniiya.mctinytweak.armorbar.ArmorBarShape;
import dev.luniiya.mctinytweak.client.tweaks.Tweaks;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While armor is hidden, draws the armor bar just above the player's name, facing the camera like the name. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
	@Inject(
		method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")
	)
	private void mc_tiny_tweak$submitArmorBar(
		AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci
	) {
		if (Tweaks.showArmor() || state.nameTag == null || state.nameTagAttachment == null) return;
		// Same offset vanilla gives the name, which moves up for players showing extra ears.
		int nameOffset = state.showExtraEars ? -10 : 0;
		List<Integer> segments = ArmorBar.INSTANCE.segments(state.headEquipment, state.chestEquipment, state.legsEquipment, state.feetEquipment);
		List<ArmorBarShape.Rect> rects = ArmorBarShape.INSTANCE.rects(segments, ArmorBarShape.INSTANCE.top(nameOffset));

		// The same billboard transform the name tag renderer applies.
		poseStack.pushPose();
		poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5, state.nameTagAttachment.z);
		poseStack.mulPose(camera.orientation);
		poseStack.scale(0.025F, -0.025F, 0.025F);
		collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (pose, buffer) -> {
			Matrix4f matrix = pose.pose();
			for (ArmorBarShape.Rect rect : rects) mc_tiny_tweak$quad(buffer, matrix, rect);
		});
		poseStack.popPose();
	}

	/** Emits both windings so the rectangle shows whichever way the flipped name tag space faces. */
	private static void mc_tiny_tweak$quad(VertexConsumer buffer, Matrix4f matrix, ArmorBarShape.Rect rect) {
		float x0 = rect.getX0(), y0 = rect.getY0(), x1 = rect.getX1(), y1 = rect.getY1();
		int color = rect.getArgb();
		int light = LightTexture.FULL_BRIGHT;
		buffer.addVertex(matrix, x0, y0, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x0, y1, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x1, y1, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x1, y0, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x1, y0, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x1, y1, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x0, y1, 0).setColor(color).setLight(light);
		buffer.addVertex(matrix, x0, y0, 0).setColor(color).setLight(light);
	}
}
