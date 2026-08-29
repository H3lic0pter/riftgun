package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingLabelSourceTest {
    @Test
    void everyEndpointFacingClientTextUsesTheSharedColoredNumerals() throws IOException {
        assertEquals("I", PortalPairingLabels.FIRST_TEXT);
        assertEquals("II", PortalPairingLabels.SECOND_TEXT);
        String manager = read("src/main/java/dev/riftgun/pairing/PortalPairingManager.java");
        assertTrue(manager.contains("PortalPairingLabels.forEndpoint(endpoint)"));

        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String root = "versions/" + version + "/src/main/java/dev/riftgun/client/";
            String events = read(root + "ClientGameEvents.java");
            String radial = read(root + "screen/ModeRadialScreen.java");
            assertTrue(events.contains("PortalPairingLabels.first()"));
            assertTrue(events.contains("PortalPairingLabels.second()"));
            assertTrue(radial.contains("PortalPairingLabels.first()"));
            assertTrue(radial.contains("PortalPairingLabels.second()"));
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
