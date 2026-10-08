package dev.identify.forge;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.Canvas;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.hud.IdentifyKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClient {
    private ForgeClient() {
    }

    static void register(ModLoadingContext context, IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                IdentifyKeys.tick(Minecraft.getInstance());
            }
        });
        modBus.addListener(ForgeClient::setup);
        context.registerExtensionPoint(
                ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(
                        (client, parent) -> new IdentifySettingsScreen(parent, client.options)));
    }

    private static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (KeyMapping key : IdentifyKeys.create()) {
                ClientRegistry.registerKeyBinding(key);
            }
        });
        OverlayRegistry.registerOverlayTop("Identify", (gui, poseStack, partialTick, width, height) ->
                IdentifyHudRenderer.render(new Canvas(poseStack), partialTick));
    }
}
