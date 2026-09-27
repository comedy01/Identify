package dev.identify.fabric;

import dev.identify.client.IdentifyClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class IdentifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IdentifyClient.init(FabricLoader.getInstance().getConfigDir());
        HudHook.register();
        TooltipHook.register();
    }
}
