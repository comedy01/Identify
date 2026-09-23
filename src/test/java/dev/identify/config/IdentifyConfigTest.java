package dev.identify.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IdentifyConfigTest {
    @TempDir
    Path dir;

    @Test
    void missingFileCreatesDefaults() {
        Path file = dir.resolve(IdentifyConfig.FILE_NAME);
        IdentifyConfig config = IdentifyConfig.load(file);
        assertTrue(Files.isRegularFile(file));
        assertTrue(config.enabled());
        assertEquals(IdentifyPolicy.DEFAULT_RANGE, config.range());
        assertEquals(IdentifyPolicy.DEFAULT_Y_OFFSET, config.yOffset());
    }

    @Test
    void roundTripKeepsValues() throws IOException {
        Path file = dir.resolve(IdentifyConfig.FILE_NAME);
        IdentifyConfig config = new IdentifyConfig();
        config.setShowEntities(false);
        config.setItemTooltips(false);
        config.setRange(20.0);
        config.setYOffset(40);
        config.save(file);

        IdentifyConfig loaded = IdentifyConfig.load(file);
        assertFalse(loaded.showEntities());
        assertFalse(loaded.itemTooltips());
        assertEquals(20.0, loaded.range());
        assertEquals(40, loaded.yOffset());
    }

    @Test
    void outOfRangeValuesAreClamped() throws IOException {
        Path file = dir.resolve(IdentifyConfig.FILE_NAME);
        Files.writeString(file, "{\"range\": 9999, \"yOffset\": -50}", StandardCharsets.UTF_8);
        IdentifyConfig loaded = IdentifyConfig.load(file);
        assertEquals(IdentifyPolicy.MAX_RANGE, loaded.range());
        assertEquals(IdentifyPolicy.MIN_Y_OFFSET, loaded.yOffset());
    }

    @Test
    void brokenFileFallsBackToDefaultsAndKeepsBackup() throws IOException {
        Path file = dir.resolve(IdentifyConfig.FILE_NAME);
        Files.writeString(file, "{not json", StandardCharsets.UTF_8);
        IdentifyConfig loaded = IdentifyConfig.load(file);
        assertTrue(loaded.enabled());
        assertTrue(Files.isRegularFile(dir.resolve(IdentifyConfig.FILE_NAME + ".broken")));
    }

    @Test
    void wrongTypedValueFallsBackToDefaults() throws IOException {
        Path file = dir.resolve(IdentifyConfig.FILE_NAME);
        Files.writeString(file, "{\"range\": \"far\"}", StandardCharsets.UTF_8);
        assertEquals(IdentifyPolicy.DEFAULT_RANGE, IdentifyConfig.load(file).range());
    }

    @Test
    void resetRestoresDefaults() {
        IdentifyConfig config = new IdentifyConfig();
        config.setEnabled(false);
        config.setRange(30.0);
        config.resetToDefaults();
        assertTrue(config.enabled());
        assertEquals(IdentifyPolicy.DEFAULT_RANGE, config.range());
    }
}
