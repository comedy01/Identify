package dev.identify.hud;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

final class BossBars {
    private static Field events;
    private static boolean resolved;

    private BossBars() {
    }

    static int count(Object overlay) {
        if (overlay == null) {
            return 0;
        }
        Field field = field(overlay.getClass());
        if (field == null) {
            return 0;
        }
        try {
            Object value = field.get(overlay);
            return value instanceof Map<?, ?> map ? map.size() : 0;
        } catch (IllegalAccessException | RuntimeException e) {
            return 0;
        }
    }

    private static Field field(Class<?> type) {
        if (resolved) {
            return events;
        }
        resolved = true;
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field candidate : current.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(candidate.getType()) && !Modifier.isStatic(candidate.getModifiers())) {
                    try {
                        candidate.setAccessible(true);
                        events = candidate;
                    } catch (RuntimeException e) {
                        events = null;
                    }
                    return events;
                }
            }
        }
        return null;
    }
}
