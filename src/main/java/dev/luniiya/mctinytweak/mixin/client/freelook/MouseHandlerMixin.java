package dev.luniiya.mctinytweak.mixin.client.freelook;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.luniiya.mctinytweak.client.freelook.Freelook;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While freelook is held, mouse movement turns the freelook camera instead of the player. */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void mc_tiny_tweak$turnFreelook(LocalPlayer player, double dx, double dy, Operation<Void> original) {
		if (Freelook.camera.getActive()) Freelook.camera.turn(dx, dy);
		else original.call(player, dx, dy);
	}
}
