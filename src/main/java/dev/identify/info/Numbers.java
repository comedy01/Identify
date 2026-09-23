package dev.identify.info;

import java.util.Locale;

public final class Numbers {
    private static final double EPSILON = 0.05D;

    private Numbers() {
    }

    public static String trim(double value) {
        if (Math.abs(value - Math.round(value)) < EPSILON) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public static String signed(double value) {
        String text = trim(value);
        return value > 0.0D ? "+" + text : text;
    }

    public static boolean significant(double delta) {
        return Math.abs(delta) >= EPSILON;
    }
}
