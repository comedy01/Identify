package dev.identify.forge;

import dev.identify.client.IdentifyClient;
import dev.identify.look.ItemDetails;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(IdentifyClient.MOD_ID)
public final class IdentifyForge {
    public IdentifyForge() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerExtensionPoint(
                ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, fromServer) -> true));
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        IdentifyClient.init(FMLPaths.CONFIGDIR.get());
        IdentifyClient.setModNames(namespace -> ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(null));
        MinecraftForge.EVENT_BUS.addListener(IdentifyForge::onTooltip);
        ForgeClient.register(context, FMLJavaModLoadingContext.get().getModEventBus());
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
            ItemDetails.append(event.getItemStack(), event.getToolTip(), event.getFlags().isAdvanced());
        }
    }
}
