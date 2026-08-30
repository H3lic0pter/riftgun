package dev.riftgun.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private static final Path AUDITED_SHARED_CLIENT_FACADE =
        MAIN.resolve("client/ModeRadialInput.java");
    private static final List<String> BANNED_PREFIXES = List.of(
        "net.minecraft.client.",   // renderer/GUI churn in every major release
        "dev.riftgun.client.",     // version-private package
        "dev.riftgun.config.");    // version-private package

    @Test
    void sharedSourcesDoNotImportVersionSensitivePackages() throws IOException {
        try (var paths = Files.walk(MAIN)) {
            for (Path source : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(source);
                if (source.equals(AUDITED_SHARED_CLIENT_FACADE)) {
                    assertTrue(text.contains("ClientKeyState.down(mapping)"),
                        "shared radial input must route version-sensitive key reads through its adapter");
                    assertFalse(text.contains("InputConstants") || text.contains("GLFW"),
                        "shared radial input leaked a version-sensitive window API");
                    continue;
                }
                for (String banned : BANNED_PREFIXES) {
                    assertFalse(text.contains(banned),
                        () -> source + " imports version-sensitive " + banned
                            + " (move it to versions/<version>/src)");
                }
            }
        }
    }

    @Test
    void nodeSourcesDoNotDuplicateSharedClasses() throws IOException {
        Set<String> shared = fqcns(MAIN);
        try (var nodes = Files.list(Path.of("versions"))) {
            for (Path node : nodes.filter(Files::isDirectory).toList()) {
                Path nodeSrc = node.resolve("src/main/java");
                if (!Files.isDirectory(nodeSrc)) continue;
                for (String fqcn : fqcns(nodeSrc)) {
                    assertFalse(shared.contains(fqcn),
                        () -> fqcn + " exists in both the shared tree and " + node
                            + "; same-FQCN overrides must be avoided");
                }
            }
        }
    }

    private static Set<String> fqcns(Path root) throws IOException {
        Set<String> result = new HashSet<>();
        try (var paths = Files.walk(root)) {
            for (Path source : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relative = root.relativize(source).toString().replace('\\', '/');
                result.add(relative.substring(0, relative.length() - ".java".length()).replace('/', '.'));
            }
        }
        return result;
    }
}
