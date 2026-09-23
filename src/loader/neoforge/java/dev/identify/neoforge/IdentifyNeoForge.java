package dev.identify.neoforge;

import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.look.ItemDetails;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = IdentifyNeoForge.MOD_ID, dist = Dist.CLIENT)
public final class IdentifyNeoForge {
    public static final String MOD_ID = "identify";

    public IdentifyNeoForge(IEventBus modBus, ModContainer container) {
        IdentifyClient.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(IdentifyNeoForge::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(IdentifyNeoForge::onTooltip);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new IdentifySettingsScreen(parent, Minecraft.getInstance().options));
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(MOD_ID, "look_info"),
                IdentifyHudRenderer::render);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
            ItemDetails.append(event.getItemStack(), event.getToolTip());
        }
    }
}
