package dev.luniiya.mctinytweak.mixin.client.hotbar;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface GuiInvoker {
	@Invoker("getCameraPlayer")
	@Nullable
	Player mc_tiny_tweak$getCameraPlayer();

	@Invoker("renderSlot")
	void mc_tiny_tweak$renderSlot(GuiGraphics graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed);
}
