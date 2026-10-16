package dev.luniiya.mctinytweak.mixin.client.mcp;

import com.mojang.blaze3d.platform.InputConstants;
import dev.luniiya.mctinytweak.client.mcp.AgentControl;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the agent has taken over, real key and mouse button events stop reaching key mappings. */
@Mixin(KeyMapping.class)
abstract class KeyMappingControlMixin {
	@Inject(method = "set", at = @At("HEAD"), cancellable = true)
	private static void mc_tiny_tweak$blockSet(InputConstants.Key key, boolean down, CallbackInfo ci) {
		if (AgentControl.blocksKey(key)) ci.cancel();
	}

	@Inject(method = "click", at = @At("HEAD"), cancellable = true)
	private static void mc_tiny_tweak$blockClick(InputConstants.Key key, CallbackInfo ci) {
		if (AgentControl.blocksKey(key)) ci.cancel();
	}

	/** Re-reads every key from the keyboard, for example when a screen closes. */
	@Inject(method = "setAll", at = @At("HEAD"), cancellable = true)
	private static void mc_tiny_tweak$blockSetAll(CallbackInfo ci) {
		if (AgentControl.agentInControl()) ci.cancel();
	}
}
