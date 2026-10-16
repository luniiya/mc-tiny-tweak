package dev.luniiya.mctinytweak.mixin.client.healthbar;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.luniiya.mctinytweak.client.healthbar.CompactHealth;
import dev.luniiya.mctinytweak.client.tweaks.Tweaks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Past 20 hearts, draws the compact health bars instead of rows of hearts, and keeps armor right above them. */
@Mixin(Gui.class)
abstract class GuiHealthMixin {
	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$compactHearts(
		GuiGraphics graphics, Player player, int x, int y, int rowHeight, int regenIndex, float maxHealth,
		int health, int displayHealth, int absorption, boolean blink, CallbackInfo ci
	) {
		if (CompactHealth.active(maxHealth, absorption)) {
			CompactHealth.render(graphics, x, y, maxHealth, health, absorption, blink);
			ci.cancel();
		}
	}

	/** Vanilla lifts armor by one row per row of hearts; the bars take one row. */
	@WrapOperation(
		method = "renderPlayerHealth",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V")
	)
	private void mc_tiny_tweak$armorAboveBars(GuiGraphics graphics, Player player, int y, int rows, int rowHeight, int x, Operation<Void> original) {
		// More than two rows means more than 40 points of health and absorption, the same test as above.
		boolean compact = rows > 2 && CompactHealth.active(41f, 0);
		original.call(graphics, player, y, compact ? 1 : rows, rowHeight, x);
	}
}
