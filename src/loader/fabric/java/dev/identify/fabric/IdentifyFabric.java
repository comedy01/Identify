package dev.identify.fabric;

import dev.identify.client.IdentifyClient;
import dev.identify.look.ItemDetails;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.loader.api.FabricLoader;

public final class IdentifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IdentifyClient.init(FabricLoader.getInstance().getConfigDir());
        HudHook.register();
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
                ItemDetails.append(stack, lines);
            }
        });
    }
}
