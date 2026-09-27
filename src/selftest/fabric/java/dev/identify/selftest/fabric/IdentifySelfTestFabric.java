package dev.identify.selftest.fabric;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.selftest.IdentifySelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class IdentifySelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        IdentifySelfTest test = new IdentifySelfTest(FabricLoader.getInstance().getConfigDir(),
                () -> new IdentifySettingsScreen(null, Minecraft.getInstance().options));
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
