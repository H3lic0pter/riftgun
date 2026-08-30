package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingOwnerLifecycleSourceTest {
    @Test
    void replacingAMarkerAndOpeningACoordinatePortalClearAllOwnerMarkers() throws Exception {
        String pairing = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "pairing", "PortalPairingManager.java"));
        String opening = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "service", "PortalOpenCoordinator.java"));

        assertTrue(pairing.contains("PortalPairingPendingEndpoints.clearAll(player);"));
        assertTrue(pairing.indexOf("PortalPairingPendingEndpoints.clearAll(player);")
            < pairing.indexOf("PortalPairingPendingEndpoints.save("));
        assertTrue(opening.contains("PortalPairingPendingEndpoints.clearAll(player);"));
    }
}
