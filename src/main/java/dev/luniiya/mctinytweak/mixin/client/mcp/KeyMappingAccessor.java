package dev.luniiya.mctinytweak.mixin.client.mcp;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the agent queue a press on one mapping without pressing every mapping that shares its key, and read bound keys. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	int mc_tiny_tweak$getClickCount();

	@Accessor("clickCount")
	void mc_tiny_tweak$setClickCount(int clickCount);

	@Accessor("key")
	InputConstants.Key mc_tiny_tweak$getKey();
}
