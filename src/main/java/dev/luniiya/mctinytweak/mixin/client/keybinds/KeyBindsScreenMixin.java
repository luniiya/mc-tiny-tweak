package dev.luniiya.mctinytweak.mixin.client.keybinds;

import dev.luniiya.mctinytweak.client.keybinds.KeyBindsTweaks;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds search, input mode and default-bind tools to the Key Binds screen. Logic lives in {@link KeyBindsTweaks}. */
@Mixin(KeyBindsScreen.class)
abstract class KeyBindsScreenMixin extends OptionsSubScreen {
	private KeyBindsScreenMixin(Screen parent, Options options, Component title) {
		super(parent, options, title);
	}

	@Unique
	private KeyBindsTweaks mc_tiny_tweak$tweaks;

	@Unique
	private KeyBindsTweaks mc_tiny_tweak$tweaks() {
		if (mc_tiny_tweak$tweaks == null) mc_tiny_tweak$tweaks = new KeyBindsTweaks((KeyBindsScreen) (Object) this);
		return mc_tiny_tweak$tweaks;
	}

	/** KeyBindsScreen inherits addTitle, so the mixin adds an override instead of injecting. */
	@Override
	protected void addTitle() {
		mc_tiny_tweak$tweaks().addHeader();
	}

	@Inject(method = "addContents", at = @At("TAIL"))
	private void mc_tiny_tweak$captureEntries(CallbackInfo ci) {
		mc_tiny_tweak$tweaks().captureEntries();
	}

	@Inject(method = "addFooter", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$addFooter(CallbackInfo ci) {
		mc_tiny_tweak$tweaks().addFooter();
		ci.cancel();
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$captureKeyFilter(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (mc_tiny_tweak$tweaks().onKeyPressed(event)) cir.setReturnValue(true);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void mc_tiny_tweak$captureMouseFilter(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (mc_tiny_tweak$tweaks().onMouseClicked(event)) cir.setReturnValue(true);
	}
}
