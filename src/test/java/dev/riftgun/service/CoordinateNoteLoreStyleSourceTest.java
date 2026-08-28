package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class CoordinateNoteLoreStyleSourceTest {
    @Test
    void everyCoordinateNoteLoreLineExplicitlyDisablesItalics() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/CoordinateSharingService.java"));

        assertTrue(source.contains(
            "style -> style.withColor(color).withItalic(false)"));
        assertEquals(7, occurrences(source, "lore.add(noteLore("));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length()) / needle.length();
    }
}
