package dev.identify.client;

import net.minecraft.resources.ResourceLocation;

public final class Ids {
    private Ids() {
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(IdentifyClient.MOD_ID, path);
    }
}
