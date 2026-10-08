package dev.identify.hud;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class IdentifyKeys {
    private static final String CATEGORY = "key.category." + IdentifyClient.MOD_ID + ".main";

    private static KeyBinding panel;
    private static KeyBinding blocks;
    private static KeyBinding entities;

    private IdentifyKeys() {
    }

    public static void register() {
        panel = create("key.identify.toggle");
        blocks = create("key.identify.blocks");
        entities = create("key.identify.entities");
    }

    private static KeyBinding create(String name) {
        KeyBinding key = new KeyBinding(name, Keyboard.KEY_NONE, CATEGORY);
        ClientRegistry.registerKeyBinding(key);
        return key;
    }

    public static void tick(Minecraft mc) {
        if (panel == null) {
            return;
        }
        IdentifyConfig config = IdentifyClient.config();
        while (panel.isPressed()) {
            notice(mc, "panel", config.togglePanel());
        }
        while (blocks.isPressed()) {
            notice(mc, "blocks", config.toggleBlocks());
        }
        while (entities.isPressed()) {
            notice(mc, "entities", config.toggleEntities());
        }
    }

    private static void notice(Minecraft mc, String what, boolean on) {
        IdentifyClient.saveConfig();
        if (mc.ingameGUI != null) {
            mc.ingameGUI.setOverlayMessage(new TextComponentTranslation("identify.notice." + what + (on ? ".on" : ".off")), false);
        }
    }
}
