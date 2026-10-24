package com.minepiece.essentials.mixin;

import com.minepiece.essentials.network.BackgroundGuiRefresh;
import com.minepiece.essentials.network.ServerGuiInterceptor;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paquets sortants : pendant qu'un relevé de fond attend son écran, une action du joueur
 * susceptible d'ouvrir un écran (bloc, entité, objet, commande tapée) fait abandonner
 * l'interception, pour ne jamais capturer l'écran du joueur. Lecture seule, rien n'est
 * modifié ni bloqué.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void minepiece$onSend(Packet<?> packet, CallbackInfo ci) {
        if (!ServerGuiInterceptor.isIntercepting()) return;
        boolean mayOpenScreen = packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundInteractPacket
                || ((packet instanceof ServerboundChatCommandPacket || packet instanceof ServerboundChatCommandSignedPacket)
                    && !BackgroundGuiRefresh.isSendingOwnCommand());
        if (mayOpenScreen) ServerGuiInterceptor.onPlayerInteraction();
    }
}
