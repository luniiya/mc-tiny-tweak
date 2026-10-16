package dev.luniiya.mctinytweak.mixin.client.freecam;

import dev.luniiya.mctinytweak.client.freecam.Freecam;
import dev.luniiya.mctinytweak.freecam.FreecamCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the view to the freecam after vanilla placed it. Marking the camera detached makes the world render
 * the player's body, like third person.
 */
@Mixin(Camera.class)
abstract class CameraFreecamMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(method = "setup", at = @At("TAIL"))
	private void mc_tiny_tweak$useFreecam(Level level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
		Freecam.renderedFrame();
		if (!Freecam.shown()) return;
		FreecamCamera camera = Freecam.camera;
		this.detached = true;
		setRotation(camera.getYaw(), camera.getPitch());
		setPosition(camera.x(partialTick), camera.y(partialTick), camera.z(partialTick));
	}
}
