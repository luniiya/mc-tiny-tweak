package dev.luniiya.mctinytweak.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import dev.luniiya.mctinytweak.client.PersonalKeyDefaults;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Every key mapping's default key comes from the personal defaults when one is listed. */
@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
	@ModifyExpressionValue(
		method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;I)V",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants$Type;getOrCreate(I)Lcom/mojang/blaze3d/platform/InputConstants$Key;")
	)
	private InputConstants.Key mc_tiny_tweak$usePersonalDefault(InputConstants.Key original, @Local(argsOnly = true) String name) {
		return PersonalKeyDefaults.defaultFor(name, original);
	}
}
