package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPlacementRangeRoutingSourceTest {
    @Test
    void routesHardwareAndRemoteRangesToTheirOwningPlacementModes() throws Exception {
        String resolver = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/VanillaPortalPlacementResolver.java"));
        String coordinator = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/PortalOpenCoordinator.java"));

        assertTrue(resolver.contains(
            "case REMOTE -> remote(player, constraints.remoteDistance(), constraints.aperture(),\n"
                + "                constraints.floatingOrientation())"));
        assertTrue(resolver.contains(
            "case SURFACE -> surface(player, false, constraints.smartDistance(),\n"
                + "                constraints.maximumSurfaceRange(), constraints.aperture())"));
        assertTrue(resolver.contains(
            "constraints.maximumSurfaceRange(), constraints.remoteDistance(),"));
        assertTrue(coordinator.contains(
            "gunCapabilities.maximumSurfaceRange(),\n"
                + "            gunCapabilities.remoteDistance(),"));
    }
}
