package dev.identify.client;

import net.minecraft.resources.ResourceLocation;

public final class Ids {
    private Ids() {
    }

    public static ResourceLocation of(String path) {
        return new ResourceLocation(IdentifyClient.MOD_ID, path);
    }
}
