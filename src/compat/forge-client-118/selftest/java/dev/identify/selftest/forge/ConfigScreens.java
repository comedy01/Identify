package dev.identify.selftest.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModList;

final class ConfigScreens {
    private ConfigScreens() {
    }

    static Screen open() {
        return ModList.get()
                .getModContainerById("identify")
                .flatMap(container -> container.getCustomExtension(ConfigGuiHandler.ConfigGuiFactory.class))
                .map(factory -> factory.screenFunction().apply(Minecraft.getInstance(), null))
                .orElseThrow(() -> new AssertionError("no config screen registered"));
    }
}
