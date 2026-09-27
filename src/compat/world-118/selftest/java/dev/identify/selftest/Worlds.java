package dev.identify.selftest;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;

final class Worlds {
    private Worlds() {
    }

    static void createFlat(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess registries = RegistryAccess.BUILTIN.get();
        WorldGenSettings generation = WorldGenSettings.create(registries,
                new DedicatedServerProperties.WorldGenProperties("0", new JsonObject(), false, "flat"));
        mc.createLevel(name, settings, registries, generation);
    }

    static void runCommand(IntegratedServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack(), command);
    }
}
