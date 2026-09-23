package dev.identify.config;

public final class IdentifyPolicy {
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_SHOW_BLOCKS = true;
    public static final boolean DEFAULT_SHOW_ENTITIES = true;
    public static final boolean DEFAULT_SHOW_ICON = true;
    public static final boolean DEFAULT_SHOW_DETAILS = true;
    public static final boolean DEFAULT_SHOW_MOD_NAME = true;
    public static final boolean DEFAULT_ITEM_TOOLTIPS = true;
    public static final boolean DEFAULT_COMPARE_ITEMS = true;

    public static final double MIN_RANGE = 4.0D;
    public static final double MAX_RANGE = 32.0D;
    public static final double DEFAULT_RANGE = 8.0D;

    public static final int MIN_POSITION = 0;
    public static final int MAX_POSITION = 100;
    public static final int DEFAULT_X_POSITION = 50;
    public static final int DEFAULT_Y_POSITION = 0;

    public static final int EDGE_MARGIN = 4;

    private IdentifyPolicy() {
    }

    public static double clampRange(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_RANGE;
        }
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, value));
    }

    public static int clampPosition(int value) {
        return Math.max(MIN_POSITION, Math.min(MAX_POSITION, value));
    }

    public static int place(int percent, int screen, int size) {
        int free = screen - size - EDGE_MARGIN * 2;
        if (free <= 0) {
            return EDGE_MARGIN;
        }
        return EDGE_MARGIN + Math.round(free * clampPosition(percent) / 100.0F);
    }
}
