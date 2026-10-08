package dev.identify.info;

import java.util.Locale;
import java.util.function.Function;

public final class ModNames {
    private ModNames() {
    }

    public static String label(String namespace, Function<String, String> lookup) {
        String name = null;
        if (namespace != null && !namespace.isEmpty()) {
            try {
                name = lookup.apply(namespace);
            } catch (RuntimeException e) {
                name = null;
            }
        }
        return name == null || name.trim().isEmpty() ? pretty(namespace) : name;
    }

    public static String pretty(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(namespace.length());
        boolean startOfWord = true;
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            if (c == '_' || c == '-') {
                out.append(' ');
                startOfWord = true;
            } else if (startOfWord) {
                out.append(String.valueOf(c).toUpperCase(Locale.ROOT));
                startOfWord = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
