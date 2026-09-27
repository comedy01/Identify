package dev.identify.forge;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus) {
        modBus.addListener(ForgeClient::registerOverlays);
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (client, parent) -> new IdentifySettingsScreen(parent, client.options)));
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("look_info", (gui, graphics, partialTick, width, height) ->
                IdentifyHudRenderer.render(new Canvas(graphics), partialTick));
    }
}
