package dev.identify.forge;

import dev.identify.client.IdentifyClient;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.look.ItemDetails;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(
        modid = IdentifyClient.MOD_ID,
        useMetadata = true,
        clientSideOnly = true,
        acceptableRemoteVersions = "*",
        acceptedMinecraftVersions = "[1.12.2]",
        guiFactory = "dev.identify.forge.IdentifyGuiFactory")
public final class IdentifyForge {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        IdentifyClient.init(event.getModConfigurationDirectory().toPath());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            IdentifyHudRenderer.render(event.getResolution(), event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
            ItemDetails.append(event.getItemStack(), event.getToolTip());
        }
    }
}
