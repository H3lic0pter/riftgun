package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RadialRangeSnapshotSourceTest {
    @Test
    void remoteDistanceUsesFocusedGunSnapshotInsteadOfFullPlayerSnapshot() throws Exception {
        String handler = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalRequestHandler.java"));
        String networking = Files.readString(Path.of(
            "src/main/java/dev/riftgun/network/PortalNetworking.java"));

        assertTrue(handler.contains("PortalNetworking.sendGunSnapshot(player, data, gun);"));
        assertTrue(networking.contains("envelope.putString(\"Kind\", \"GunSnapshot\");"));
        assertTrue(networking.contains("gunSnapshot(player, data, locatedGun)"));
    }
}
