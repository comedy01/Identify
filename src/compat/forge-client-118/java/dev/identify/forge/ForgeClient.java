package dev.identify.forge;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus) {
        modBus.addListener(ForgeClient::setup);
        context.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(
                        (client, parent) -> new IdentifySettingsScreen(parent, client.options)));
    }

    private static void setup(FMLClientSetupEvent event) {
        OverlayRegistry.registerOverlayTop("Identify", (gui, poseStack, partialTick, width, height) ->
                IdentifyHudRenderer.render(new Canvas(poseStack), partialTick));
    }
}
