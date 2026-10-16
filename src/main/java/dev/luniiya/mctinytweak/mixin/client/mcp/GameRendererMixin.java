package dev.luniiya.mctinytweak.mixin.client.mcp;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.luniiya.mctinytweak.client.mcp.AgentInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While an agent is playing, the unfocused window does not open the pause menu. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z"))
	private boolean mc_tiny_tweak$stayActiveForAgent(boolean active) {
		return active || AgentInput.isPlaying();
	}
}
