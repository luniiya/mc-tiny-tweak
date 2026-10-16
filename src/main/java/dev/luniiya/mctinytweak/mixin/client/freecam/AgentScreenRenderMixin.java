package dev.luniiya.mctinytweak.mixin.client.freecam;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.luniiya.mctinytweak.client.freecam.AgentScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Draws the agent's screen as a small panel in freecam, without the player's cursor hovering it. */
@Mixin(GameRenderer.class)
abstract class AgentScreenRenderMixin {
	@WrapOperation(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V")
	)
	private void mc_tiny_tweak$drawAgentScreen(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
		if (!AgentScreen.open(screen)) {
			original.call(screen, graphics, mouseX, mouseY, partialTick);
			return;
		}
		AgentScreen.render(screen, graphics, partialTick, () -> original.call(screen, graphics, AgentScreen.NO_MOUSE, AgentScreen.NO_MOUSE, partialTick));
	}
}
