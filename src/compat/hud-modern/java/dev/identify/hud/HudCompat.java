package dev.identify.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class HudCompat {
    private HudCompat() {
    }

    static boolean isGuiHidden(Minecraft mc) {
        return mc.gui.hud.isHidden();
    }

    static int bossBarCount(Minecraft mc) {
        return BossBars.count(mc.gui.hud.getBossOverlay());
    }

    static boolean isScreenOpen(Minecraft mc) {
        return mc.gui.screen() != null;
    }

    static void showNotice(Minecraft mc, Component message) {
        mc.gui.hud.setOverlayMessage(message, false);
    }
}
