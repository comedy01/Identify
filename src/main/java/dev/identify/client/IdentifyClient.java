package dev.identify.client;

import dev.identify.config.IdentifyConfig;
import dev.identify.info.ModNames;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class IdentifyClient {
    public static final String MOD_ID = "identify";

    private static IdentifyConfig config = new IdentifyConfig();
    private static Path configPath;
    private static Function<String, String> modNames = namespace -> null;
    private static final Map<String, String> MOD_LABELS = new ConcurrentHashMap<>();

    private IdentifyClient() {
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve(IdentifyConfig.FILE_NAME);
        config = IdentifyConfig.load(configPath);
    }

    public static void setModNames(Function<String, String> lookup) {
        modNames = lookup;
        MOD_LABELS.clear();
    }

    public static String modLabel(String namespace) {
        if (namespace == null) {
            return "";
        }
        return MOD_LABELS.computeIfAbsent(namespace, key -> ModNames.label(key, modNames));
    }

    public static IdentifyConfig config() {
        return config;
    }

    public static void saveConfig() {
        config.saveQuietly(configPath);
    }
}
