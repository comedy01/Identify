package dev.identify.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class Worlds {
    private Worlds() {
    }

    static void createFlat(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess registries = RegistryAccess.BUILTIN.get();
        WorldGenSettings generation = registries.registryOrThrow(Registry.WORLD_PRESET_REGISTRY)
                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldGenSettings(0L, false, false);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, registries, generation);
    }

    static void runCommand(IntegratedServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {
        });
    }
}
