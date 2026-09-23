package dev.identify.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NumbersTest {
    @Test
    void trimDropsDecimalOnWholeNumbers() {
        assertEquals("7", Numbers.trim(7.0));
        assertEquals("7", Numbers.trim(7.01));
        assertEquals("1.6", Numbers.trim(1.6));
        assertEquals("0", Numbers.trim(0.0));
    }

    @Test
    void signedAlwaysCarriesASign() {
        assertEquals("+2", Numbers.signed(2.0));
        assertEquals("-1.6", Numbers.signed(-1.6));
        assertEquals("+0.4", Numbers.signed(0.4));
    }

    @Test
    void significantIgnoresRoundingNoise() {
        assertFalse(Numbers.significant(0.0));
        assertFalse(Numbers.significant(0.01));
        assertTrue(Numbers.significant(-0.1));
    }

    @Test
    void horseJumpMatchesKnownValues() {
        assertEquals(1.0, HorseStats.jumpHeight(0.4), 0.15);
        assertEquals(5.3, HorseStats.jumpHeight(1.0), 0.1);
    }

    @Test
    void horseSpeedConvertsToBlocksPerSecond() {
        assertEquals(9.7, HorseStats.blocksPerSecond(0.225), 0.05);
        assertEquals(14.57, HorseStats.blocksPerSecond(0.3375), 0.05);
    }
}
