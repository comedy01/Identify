package dev.identify.look;

import net.minecraft.client.Minecraft;

final class ShiftKey {
    private ShiftKey() {
    }

    static boolean isDown(Minecraft mc) {
        return mc.hasShiftDown();
    }
}
