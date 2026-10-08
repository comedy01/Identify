package dev.identify.forge;

import dev.identify.client.IdentifyClient;
import dev.identify.hud.IdentifyHudRenderer;
import dev.identify.hud.IdentifyKeys;
import net.minecraft.client.Minecraft;
import dev.identify.look.ItemDetails;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
        IdentifyClient.setModNames(namespace -> {
            ModContainer container = Loader.instance().getIndexedModList().get(namespace);
            return container == null ? null : container.getName();
        });
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        IdentifyKeys.register();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            IdentifyKeys.tick(Minecraft.getMinecraft());
        }
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
            ItemDetails.append(event.getItemStack(), event.getToolTip(), event.getFlags().isAdvanced());
        }
    }
}
