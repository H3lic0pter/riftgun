package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class CoordinateParserTest {
    @Test
    void parsesAbsoluteAndRelativeCoordinates() {
        assertEquals(12.5, CoordinateParser.parse("12.5", 40.0));
        assertEquals(40.0, CoordinateParser.parse("~", 40.0));
        assertEquals(37.5, CoordinateParser.parse("~-2.5", 40.0));
        assertEquals(43.0F, CoordinateParser.parseYaw("~3", 40.0F));
    }

    @Test
    void rejectsLocalAndNonFiniteCoordinates() {
        assertThrows(NumberFormatException.class, () -> CoordinateParser.parse("^2", 0.0));
        assertThrows(NumberFormatException.class, () -> CoordinateParser.parse("NaN", 0.0));
        assertThrows(NumberFormatException.class, () -> CoordinateParser.parse("Infinity", 0.0));
    }
}
