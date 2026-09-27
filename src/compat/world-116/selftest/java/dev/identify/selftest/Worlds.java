package dev.identify.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;

import java.util.Properties;

final class Worlds {
    private Worlds() {
    }

    static void createFlat(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
        Properties properties = new Properties();
        properties.setProperty("level-type", "flat");
        mc.createLevel(name, settings, registries, WorldGenSettings.create(registries, properties));
    }

    static void runCommand(IntegratedServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack(), legacy(command));
    }

    private static String legacy(String command) {
        if (command.startsWith("item replace ")) {
            return "replaceitem " + command.substring("item replace ".length()).replace(" with ", " ");
        }
        return command;
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height,
                mc.getMainRenderTarget(), message -> {
                });
    }
}
