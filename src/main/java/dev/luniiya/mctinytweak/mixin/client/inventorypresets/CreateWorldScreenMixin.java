package dev.luniiya.mctinytweak.mixin.client.inventorypresets;

import dev.luniiya.mctinytweak.client.inventorypresets.StarterInventory;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tells the starter kit about worlds made from the Create World screen. */
@Mixin(CreateWorldScreen.class)
abstract class CreateWorldScreenMixin {
	@Inject(method = "createNewWorld", at = @At("RETURN"))
	private void mc_tiny_tweak$armStarterKit(LayeredRegistryAccess<RegistryLayer> registries, WorldData worldData, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) StarterInventory.worldCreated(worldData.getGameType(), worldData.isAllowCommands());
	}
}
