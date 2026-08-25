package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Prevents the standalone API artifact from leaking Rift Gun implementation packages. */
final class PublicApiBoundaryTest {
    private static final Path API = Path.of("src/main/java/dev/riftgun/api");

    @Test
    void publicApiExistsAndDoesNotImportImplementationPackages() throws IOException {
        assertTrue(Files.isDirectory(API), "public API source directory must exist");
        try (var paths = Files.walk(API)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(source)) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("import dev.riftgun.")) continue;
                    assertTrue(trimmed.startsWith("import dev.riftgun.api."),
                        () -> source + " leaks an implementation import: " + trimmed);
                }
            }
        }
    }
}
