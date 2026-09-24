package dev.identify.client;

import net.minecraft.resources.Identifier;

public final class Ids {
    private Ids() {
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(IdentifyClient.MOD_ID, path);
    }
}
