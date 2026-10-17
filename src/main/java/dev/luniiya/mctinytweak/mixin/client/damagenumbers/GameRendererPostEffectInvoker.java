package dev.luniiya.mctinytweak.mixin.client.damagenumbers;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the impact frame of a MAX hit invert the world for an instant, like spectating an enderman does. */
@Mixin(GameRenderer.class)
public interface GameRendererPostEffectInvoker {
	@Invoker("setPostEffect")
	void mc_tiny_tweak$setPostEffect(Identifier id);
}
