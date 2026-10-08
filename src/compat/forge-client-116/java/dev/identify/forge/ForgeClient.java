package dev.identify.forge;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.hud.IdentifyKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            for (KeyMapping key : IdentifyKeys.create()) {
                ClientRegistry.registerKeyBinding(key);
            }
        }));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                IdentifyKeys.tick(Minecraft.getInstance());
            }
        });
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
