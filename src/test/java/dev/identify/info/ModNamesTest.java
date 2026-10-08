package dev.identify.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ModNamesTest {
    @Test
    void capitalizesWords() {
        assertEquals("Minecraft", ModNames.pretty("minecraft"));
        assertEquals("Create Aeronautics", ModNames.pretty("create_aeronautics"));
        assertEquals("Farmers Delight", ModNames.pretty("farmers-delight"));
    }

    @Test
    void handlesEmptyInput() {
        assertEquals("", ModNames.pretty(""));
        assertEquals("", ModNames.pretty(null));
    }

    @Test
    void prefersLoaderName() {
        assertEquals("Twilight Forest", ModNames.label("twilightforest", namespace -> "Twilight Forest"));
    }

    @Test
    void fallsBackWhenLoaderHasNoName() {
        assertEquals("Ae2", ModNames.label("ae2", namespace -> null));
        assertEquals("Ae2", ModNames.label("ae2", namespace -> " "));
        assertEquals("Ae2", ModNames.label("ae2", namespace -> {
            throw new IllegalStateException();
        }));
    }
}
