package dev.luniiya.mctinytweak.mixin.client.keybinds;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBindsScreen.class)
public interface KeyBindsScreenAccessor {
	@Accessor("keyBindsList")
	KeyBindsList mc_tiny_tweak$getKeyBindsList();

	@Accessor("resetButton")
	void mc_tiny_tweak$setResetButton(Button button);
}
