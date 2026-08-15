package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Keeps the shared source tree free of APIs that churn between Minecraft
 * versions. Anything that imports these packages belongs in the per-version
 * node sources (versions/&lt;version&gt;/src) instead of the shared tree.
 *
 * <p>Deliberately not banned yet: {@code net.minecraft.nbt} is used by many
 * shared files; the 1.21.6 NBT rework is handled during the port, not by
 * moving files pre-emptively.
 */
final class SharedSourceBoundaryTest {
    private static final Path MAIN = Path.of("src/main/java/dev/riftgun");
    private static final List<String> BANNED_PREFIXES = List.of(
        "net.minecraft.client.",   // renderer/GUI churn in every major release
        "dev.riftgun.client.",     // version-private package
        "dev.riftgun.config.");    // version-private package

    @Test
    void sharedSourcesDoNotImportVersionSensitivePackages() throws IOException {
        try (var paths = Files.walk(MAIN)) {
            for (Path source : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                for (String banned : BANNED_PREFIXES) {
                    assertFalse(text.contains(banned),
                        () -> source + " imports version-sensitive " + banned
                            + " (move it to versions/<version>/src)");
                }
            }
        }
    }
}
