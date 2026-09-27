package dev.identify.forge;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus) {
        context.registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (client, parent) -> new IdentifySettingsScreen(parent, client.options));
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::onOverlay);
    }

    private static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            IdentifyHudRenderer.render(new Canvas(event.getMatrixStack()), event.getPartialTicks());
        }
    }
}
