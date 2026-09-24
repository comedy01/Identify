package dev.identify.look;

import net.minecraft.client.multiplayer.PlayerInfo;

final class Profiles {
    private Profiles() {
    }

    static String name(PlayerInfo info) {
        return info.getProfile().getName();
    }
}
