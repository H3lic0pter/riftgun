package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GuiTextMarqueeTest {
    @Test
    void shortTextNeverMoves() {
        assertEquals(0, GuiTextMarquee.offset(40, 60, 0L));
        assertEquals(0, GuiTextMarquee.offset(40, 60, 10_000L));
    }

    @Test
    void longTextPausesAtBothEndsAndReturnsWithoutOvershooting() {
        int overflow = 24;
        assertEquals(0, GuiTextMarquee.offset(84, 60, 0L));
        assertEquals(0, GuiTextMarquee.offset(84, 60, 899L));
        assertEquals(overflow, GuiTextMarquee.offset(84, 60, 1_900L));
        assertEquals(overflow, GuiTextMarquee.offset(84, 60, 2_799L));
        assertEquals(0, GuiTextMarquee.offset(84, 60, 3_800L));
        for (long time = 0; time < 7_600L; time += 37L) {
            int offset = GuiTextMarquee.offset(84, 60, time);
            assertTrue(offset >= 0 && offset <= overflow);
        }
    }
}
