package dev.identify.fabric;

import dev.identify.client.IdentifyClient;
import dev.identify.hud.IdentifyKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public final class IdentifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IdentifyClient.init(FabricLoader.getInstance().getConfigDir());
        IdentifyClient.setModNames(namespace -> FabricLoader.getInstance().getModContainer(namespace)
                .map(container -> container.getMetadata().getName())
                .orElse(null));
        for (KeyMapping key : IdentifyKeys.create()) {
            KeyRegistrar.register(key);
        }
        ClientTickEvents.END_CLIENT_TICK.register(IdentifyKeys::tick);
        HudHook.register();
        TooltipHook.register();
    }
}
