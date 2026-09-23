package dev.identify.fabric;

import dev.identify.client.IdentifyClient;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.look.ItemDetails;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public final class IdentifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IdentifyClient.init(FabricLoader.getInstance().getConfigDir());
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(IdentifyClient.MOD_ID, "look_info"),
                IdentifyHudRenderer::render);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
                ItemDetails.append(stack, lines);
            }
        });
    }
}
