package dev.identify.hud;

import com.mojang.blaze3d.platform.InputConstants;
import dev.identify.Texts;
import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;

public final class IdentifyKeys {
    private static KeyMapping panel;
    private static KeyMapping blocks;
    private static KeyMapping entities;

    private IdentifyKeys() {
    }

    public static List<KeyMapping> create() {
        if (panel == null) {
            int unbound = InputConstants.UNKNOWN.getValue();
            panel = KeyFactory.create("key.identify.toggle", unbound);
            blocks = KeyFactory.create("key.identify.blocks", unbound);
            entities = KeyFactory.create("key.identify.entities", unbound);
        }
        return Arrays.asList(panel, blocks, entities);
    }

    public static void tick(Minecraft mc) {
        if (panel == null) {
            return;
        }
        IdentifyConfig config = IdentifyClient.config();
        while (panel.consumeClick()) {
            notice(mc, "panel", config.togglePanel());
        }
        while (blocks.consumeClick()) {
            notice(mc, "blocks", config.toggleBlocks());
        }
        while (entities.consumeClick()) {
            notice(mc, "entities", config.toggleEntities());
        }
    }

    private static void notice(Minecraft mc, String what, boolean on) {
        IdentifyClient.saveConfig();
        if (mc.gui != null) {
            HudCompat.showNotice(mc, Texts.translatable("identify.notice." + what + (on ? ".on" : ".off")));
        }
    }
}
