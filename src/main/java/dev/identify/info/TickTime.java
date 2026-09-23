package dev.identify.info;

import java.util.Locale;

public final class TickTime {
    private TickTime() {
    }

    public static String clock(long ticks) {
        long seconds = Math.max(0L, (ticks + 19L) / 20L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%d:%02d", minutes, secs);
    }

    public static int percent(int value, int max) {
        if (max <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(max, value));
        return (int) Math.round(100.0D * clamped / max);
    }
}
