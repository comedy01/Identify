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
}
