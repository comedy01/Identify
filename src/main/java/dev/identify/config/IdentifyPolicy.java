package dev.identify.config;

public final class IdentifyPolicy {
    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_SHOW_BLOCKS = true;
    public static final boolean DEFAULT_SHOW_ENTITIES = true;
    public static final boolean DEFAULT_SHOW_ICON = true;
    public static final boolean DEFAULT_SHOW_DETAILS = true;
    public static final boolean DEFAULT_SHOW_MOD_NAME = true;
    public static final boolean DEFAULT_ITEM_TOOLTIPS = true;

    public static final double MIN_RANGE = 4.0D;
    public static final double MAX_RANGE = 32.0D;
    public static final double DEFAULT_RANGE = 8.0D;

    public static final int MIN_Y_OFFSET = 0;
    public static final int MAX_Y_OFFSET = 120;
    public static final int DEFAULT_Y_OFFSET = 4;

    private IdentifyPolicy() {
    }

    public static double clampRange(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_RANGE;
        }
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, value));
    }

    public static int clampYOffset(int value) {
        return Math.max(MIN_Y_OFFSET, Math.min(MAX_Y_OFFSET, value));
    }
}
