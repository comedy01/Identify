package dev.identify.look;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ShiftKey {
    private ShiftKey() {
    }

    static boolean isDown(Minecraft mc) {
        return Screen.hasShiftDown();
    }
}
