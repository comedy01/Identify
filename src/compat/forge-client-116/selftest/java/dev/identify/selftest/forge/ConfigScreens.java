package dev.identify.selftest.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;

final class ConfigScreens {
    private ConfigScreens() {
    }

    static Screen open() {
        return ModList.get()
                .getModContainerById("identify")
                .flatMap(container -> container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY))
                .map(factory -> factory.apply(Minecraft.getInstance(), null))
                .orElseThrow(() -> new AssertionError("no config screen registered"));
    }
}
