package dev.identify.info;

import java.util.Locale;

public final class ModNames {
    private ModNames() {
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
