package com.minepiece.essentials.mixin;

import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.network.ServerGuiInterceptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    // Les handle* vanilla commencent par ensureRunningOnSameThread : une injection
    // en HEAD s'exécute d'abord sur le thread Netty, puis à nouveau sur le thread
    // client. On ne traite que la seconde passe (état non synchronisé côté mod).
    private static boolean minepiece$offThread() {
        return !Minecraft.getInstance().isSameThread();
    }

    // Le pied de page du tab-list ("PLAY.MINEPIECE.NET") est visible partout,
    // y compris sur l'île perso (/is) où la boss bar disparaît : c'est un
    // signal fiable de détection du serveur (lecture seule, aucun envoi).
    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void onPlayerListHeader(ClientboundTabListPacket packet, CallbackInfo ci) {
        if (minepiece$offThread()) return;
        ServerDetector.onTabListHeaderFooter(
            packet.header() != null ? packet.header().getString() : null,
            packet.footer() != null ? packet.footer().getString() : null);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"))
    private void onOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (minepiece$offThread()) return;
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onScreenOpen(packet.getContainerId());
        }
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (minepiece$offThread()) return;
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onInventoryUpdate(packet.containerId(), packet.items());
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void onSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (minepiece$offThread()) return;
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onSlotUpdate(packet.getContainerId(), packet.getSlot(), packet.getItem());
        }
    }
}
