package dev.luniiya.mctinytweak.mixin.client.freecam;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container's window sits, so the agent's screen can be shrunk around it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int mc_tiny_tweak$getLeftPos();

	@Accessor("topPos")
	int mc_tiny_tweak$getTopPos();

	@Accessor("imageWidth")
	int mc_tiny_tweak$getImageWidth();

	@Accessor("imageHeight")
	int mc_tiny_tweak$getImageHeight();
}
