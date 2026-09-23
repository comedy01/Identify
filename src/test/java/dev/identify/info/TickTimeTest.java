package dev.identify.info;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TickTimeTest {
    @Test
    void clockRoundsUpToWholeSeconds() {
        assertEquals("0:00", TickTime.clock(0));
        assertEquals("0:01", TickTime.clock(1));
        assertEquals("0:01", TickTime.clock(20));
        assertEquals("0:02", TickTime.clock(21));
        assertEquals("1:00", TickTime.clock(1200));
    }

    @Test
    void clockShowsHoursWhenNeeded() {
        assertEquals("20:00", TickTime.clock(24000));
        assertEquals("1:00:00", TickTime.clock(72000));
    }

    @Test
    void clockClampsNegativeValues() {
        assertEquals("0:00", TickTime.clock(-500));
    }

    @Test
    void percentIsClamped() {
        assertEquals(50, TickTime.percent(125, 250));
        assertEquals(100, TickTime.percent(400, 250));
        assertEquals(0, TickTime.percent(-4, 250));
        assertEquals(0, TickTime.percent(5, 0));
    }
}
