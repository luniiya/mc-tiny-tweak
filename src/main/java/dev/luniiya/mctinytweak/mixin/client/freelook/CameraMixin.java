package dev.luniiya.mctinytweak.mixin.client.freelook;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.luniiya.mctinytweak.client.freelook.Freelook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Points the camera along the freelook direction. Only the first two rotations in setup copy the entity's view
 * (minecart and normal); later ones derive from them, so third person orbits around the player correctly.
 */
@Mixin(Camera.class)
abstract class CameraMixin {
	@WrapOperation(
		method = "setup",
		at = {
			@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
			@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1),
		}
	)
	private void mc_tiny_tweak$useFreelookRotation(
		Camera camera, float yaw, float pitch, Operation<Void> original, @Local(argsOnly = true) Entity entity
	) {
		if (Freelook.overrides(entity)) original.call(camera, Freelook.camera.getYaw(), Freelook.camera.getPitch());
		else original.call(camera, yaw, pitch);
	}
}
