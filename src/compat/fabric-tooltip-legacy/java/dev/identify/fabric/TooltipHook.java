package dev.identify.fabric;

import dev.identify.client.IdentifyClient;
import dev.identify.look.ItemDetails;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

final class TooltipHook {
    private TooltipHook() {
    }

    static void register() {
        ItemTooltipCallback.EVENT.register((stack, flag, lines) -> {
            if (IdentifyClient.config().enabled() && IdentifyClient.config().itemTooltips()) {
                ItemDetails.append(stack, lines, flag.isAdvanced());
            }
        });
    }
}
