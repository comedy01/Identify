package dev.identify.client;

import dev.identify.config.IdentifyConfig;

import java.nio.file.Path;

public final class IdentifyClient {
    public static final String MOD_ID = "identify";

    private static IdentifyConfig config = new IdentifyConfig();
    private static Path configPath;

    private IdentifyClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(IdentifyConfig.FILE_NAME);
        config = IdentifyConfig.load(configPath);
    }

    public static IdentifyConfig config() {
        return config;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
