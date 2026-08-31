package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EntityRelocationRangeSourceTest {
    @Test
    void targetingUsesMaximumWhilePairingFloatingFallbackUsesRemoteDistance() throws IOException {
        String relocation = read("src/main/java/dev/riftgun/relocation/EntityRelocationManager.java");
        String findTarget = relocation.substring(relocation.indexOf("private static Optional<Entity> findTarget"),
            relocation.indexOf("private static boolean isRelocatableType"));
        assertTrue(findTarget.contains("capabilities.maximumSurfaceRange()"));
        assertFalse(findTarget.contains("capabilities.remoteDistance()"));

        String pairing = read("src/main/java/dev/riftgun/pairing/PortalPairingManager.java");
        String fixedTarget = pairing.substring(pairing.indexOf("private static boolean setRelocationTarget"),
            pairing.indexOf("private static PortalRuntimeOptions runtimeOptions"));
        assertTrue(fixedTarget.contains(
            "capabilities.maximumSurfaceRange(), capabilities.maximumSurfaceRange()"));
        assertTrue(fixedTarget.contains("capabilities.remoteDistance()"));
        assertTrue(fixedTarget.contains("floatingDistance"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
