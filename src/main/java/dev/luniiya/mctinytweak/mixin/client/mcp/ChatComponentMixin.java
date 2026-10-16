package dev.luniiya.mctinytweak.mixin.client.mcp;

import dev.luniiya.mctinytweak.client.Notices;
import dev.luniiya.mctinytweak.mcp.ChatWake;
import net.minecraft.client.Minecraft;
import dev.luniiya.mctinytweak.client.mcp.Mcp;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copies every line shown in chat, including command feedback, into the agent's chat log. The mod's own notices are left out. */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
	@Inject(
		method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD")
	)
	private void mc_tiny_tweak$logForAgent(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag, CallbackInfo ci) {
		if (!Notices.getShowing()) {
            var player = Minecraft.getInstance().player;
            Mcp.getChat().add(message.getString(), System.currentTimeMillis(), ChatWake.isIncoming(message.getString(), player == null ? null : player.getName().getString()));
        }
	}
}
