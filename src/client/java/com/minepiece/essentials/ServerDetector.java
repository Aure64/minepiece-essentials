package com.minepiece.essentials;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class ServerDetector {
    private static boolean onMinePiece = false;
    private static String lastCheckedAddress = "";

    private ServerDetector() {}

    public static boolean isOnMinePiece() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo serverInfo = client.getCurrentServerEntry();

        if (serverInfo == null) {
            if (onMinePiece) {
                onMinePiece = false;
                lastCheckedAddress = "";
                MinepieceEssentialsClient.LOGGER.info("[ServerDetector] Disconnected from MinePiece");
            }
            return false;
        }

        String address = serverInfo.address;
        if (address == null) {
            onMinePiece = false;
            return false;
        }

        if (!address.equals(lastCheckedAddress)) {
            lastCheckedAddress = address;
            String host = address.contains(":") ? address.substring(0, address.indexOf(':')) : address;
            onMinePiece = host.toLowerCase().contains("minepiece");

            if (onMinePiece) {
                MinepieceEssentialsClient.LOGGER.info("[ServerDetector] Connected to MinePiece ({})", address);
            } else {
                MinepieceEssentialsClient.LOGGER.info("[ServerDetector] Not on MinePiece ({})", address);
            }
        }

        return onMinePiece;
    }
}
