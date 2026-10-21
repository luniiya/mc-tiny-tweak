package dev.luniiya.mctinytweak.mixin.client.visuals;

import com.mojang.authlib.GameProfile;
import dev.luniiya.mctinytweak.visuals.OfflineSkins;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Real skins on offline-mode servers, see {@link OfflineSkins}. */
@Mixin(PlayerInfo.class)
abstract class PlayerInfoMixin {
	@Inject(method = "createSkinLookup", at = @At("HEAD"), cancellable = true)
	private static void mc_tiny_tweak$offlineSkin(GameProfile profile, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
		if (OfflineSkins.INSTANCE.needsLookup(profile)) {
			cir.setReturnValue(OfflineSkins.INSTANCE.lookup(profile, !Minecraft.getInstance().isLocalPlayer(profile.id())));
		}
	}
}
