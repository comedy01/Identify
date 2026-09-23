package dev.identify.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class IdentifyConfig {
    public static final String FILE_NAME = "identify.json";

    private static final Logger LOGGER = LoggerFactory.getLogger("identify");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("enabled")
    private boolean enabled = IdentifyPolicy.DEFAULT_ENABLED;

    @SerializedName("showBlocks")
    private boolean showBlocks = IdentifyPolicy.DEFAULT_SHOW_BLOCKS;

    @SerializedName("showEntities")
    private boolean showEntities = IdentifyPolicy.DEFAULT_SHOW_ENTITIES;

    @SerializedName("showIcon")
    private boolean showIcon = IdentifyPolicy.DEFAULT_SHOW_ICON;

    @SerializedName("showDetails")
    private boolean showDetails = IdentifyPolicy.DEFAULT_SHOW_DETAILS;

    @SerializedName("showModName")
    private boolean showModName = IdentifyPolicy.DEFAULT_SHOW_MOD_NAME;

    @SerializedName("itemTooltips")
    private boolean itemTooltips = IdentifyPolicy.DEFAULT_ITEM_TOOLTIPS;

    @SerializedName("compareItems")
    private boolean compareItems = IdentifyPolicy.DEFAULT_COMPARE_ITEMS;

    @SerializedName("range")
    private double range = IdentifyPolicy.DEFAULT_RANGE;

    @SerializedName("xPosition")
    private int xPosition = IdentifyPolicy.DEFAULT_X_POSITION;

    @SerializedName("yPosition")
    private int yPosition = IdentifyPolicy.DEFAULT_Y_POSITION;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public boolean showBlocks() {
        return showBlocks;
    }

    public void setShowBlocks(boolean value) {
        showBlocks = value;
    }

    public boolean showEntities() {
        return showEntities;
    }

    public void setShowEntities(boolean value) {
        showEntities = value;
    }

    public boolean showIcon() {
        return showIcon;
    }

    public void setShowIcon(boolean value) {
        showIcon = value;
    }

    public boolean showDetails() {
        return showDetails;
    }

    public void setShowDetails(boolean value) {
        showDetails = value;
    }

    public boolean showModName() {
        return showModName;
    }

    public void setShowModName(boolean value) {
        showModName = value;
    }

    public boolean itemTooltips() {
        return itemTooltips;
    }

    public void setItemTooltips(boolean value) {
        itemTooltips = value;
    }

    public boolean compareItems() {
        return compareItems;
    }

    public void setCompareItems(boolean value) {
        compareItems = value;
    }

    public double range() {
        return range;
    }

    public void setRange(double value) {
        range = IdentifyPolicy.clampRange(value);
    }

    public int xPosition() {
        return xPosition;
    }

    public void setXPosition(int value) {
        xPosition = IdentifyPolicy.clampPosition(value);
    }

    public int yPosition() {
        return yPosition;
    }

    public void setYPosition(int value) {
        yPosition = IdentifyPolicy.clampPosition(value);
    }

    public void resetToDefaults() {
        enabled = IdentifyPolicy.DEFAULT_ENABLED;
        showBlocks = IdentifyPolicy.DEFAULT_SHOW_BLOCKS;
        showEntities = IdentifyPolicy.DEFAULT_SHOW_ENTITIES;
        showIcon = IdentifyPolicy.DEFAULT_SHOW_ICON;
        showDetails = IdentifyPolicy.DEFAULT_SHOW_DETAILS;
        showModName = IdentifyPolicy.DEFAULT_SHOW_MOD_NAME;
        itemTooltips = IdentifyPolicy.DEFAULT_ITEM_TOOLTIPS;
        compareItems = IdentifyPolicy.DEFAULT_COMPARE_ITEMS;
        range = IdentifyPolicy.DEFAULT_RANGE;
        xPosition = IdentifyPolicy.DEFAULT_X_POSITION;
        yPosition = IdentifyPolicy.DEFAULT_Y_POSITION;
    }

    private void sanitize() {
        setRange(range);
        setXPosition(xPosition);
        setYPosition(yPosition);
    }

    public static IdentifyConfig load(Path file) {
        if (!Files.isRegularFile(file)) {
            IdentifyConfig fresh = new IdentifyConfig();
            fresh.saveQuietly(file);
            return fresh;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            IdentifyConfig loaded = GSON.fromJson(reader, IdentifyConfig.class);
            if (loaded == null) {
                throw new JsonParseException("config file is empty");
            }
            loaded.sanitize();
            return loaded;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}; using defaults. {}", file, e.toString());
            moveAside(file);
            IdentifyConfig fresh = new IdentifyConfig();
            fresh.saveQuietly(file);
            return fresh;
        }
    }

    public void save(Path file) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(this) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void saveQuietly(Path file) {
        try {
            save(file);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(
                    file,
                    file.resolveSibling(file.getFileName() + ".broken"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not back up unreadable config {}: {}", file, e.toString());
        }
    }
}
