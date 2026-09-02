package com.minepiece.essentials.mixin;

import com.minepiece.essentials.ServerDetector;
import com.minepiece.essentials.network.ServerGuiInterceptor;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    // Le pied de page du tab-list ("PLAY.MINEPIECE.NET") est visible partout,
    // y compris sur l'île perso (/is) où la boss bar disparaît : c'est un
    // signal fiable de détection du serveur (lecture seule, aucun envoi).
    @Inject(method = "onPlayerListHeader", at = @At("HEAD"))
    private void onPlayerListHeader(PlayerListHeaderS2CPacket packet, CallbackInfo ci) {
        ServerDetector.onTabListHeaderFooter(
            packet.header() != null ? packet.header().getString() : null,
            packet.footer() != null ? packet.footer().getString() : null);
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onScreenOpen(packet.getSyncId());
        }
    }

    @Inject(method = "onInventory", at = @At("HEAD"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci) {
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onInventoryUpdate(packet.syncId(), packet.contents());
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"))
    private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (ServerGuiInterceptor.isIntercepting()) {
            ServerGuiInterceptor.onSlotUpdate(packet.getSyncId(), packet.getSlot(), packet.getStack());
        }
    }
}
