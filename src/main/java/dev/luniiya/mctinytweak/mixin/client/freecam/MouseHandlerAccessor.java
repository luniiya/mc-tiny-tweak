package dev.luniiya.mctinytweak.mixin.client.freecam;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Captures the mouse without closing the open screen, which vanilla's grabMouse would do. */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
	@Accessor("mouseGrabbed")
	void mc_tiny_tweak$setMouseGrabbed(boolean grabbed);
}
