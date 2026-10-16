package dev.luniiya.mctinytweak.mixin.client.mcp;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the agent queue a press on one mapping without pressing every mapping that shares its key. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	int mc_tiny_tweak$getClickCount();

	@Accessor("clickCount")
	void mc_tiny_tweak$setClickCount(int clickCount);
}
