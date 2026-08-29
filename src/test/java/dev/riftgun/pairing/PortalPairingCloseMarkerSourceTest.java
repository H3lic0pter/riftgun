package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingCloseMarkerSourceTest {
    @Test
    void closePortalActionClearsPendingMarkersFromTheWholeInventory() throws IOException {
        String handler = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "network", "PortalRequestHandler.java"));
        String storage = Files.readString(Path.of("src", "main", "java", "dev", "riftgun",
            "pairing", "PortalPairingPendingEndpoints.java"));

        assertTrue(handler.contains(
            "PortalPairingPendingEndpoints.clearAll(player.getInventory());"));
        assertTrue(storage.contains("inventory.getContainerSize()"));
        assertTrue(storage.contains("inventory.setChanged()"));
    }
}
