package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalPairingEntityTargetPreviewSourceTest {
    @Test
    void entityTargetMarkerIsScopedToItsGunAndEntityPairingMode() throws IOException {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path root = Path.of("versions", version, "src", "main", "java", "dev", "riftgun");
            String portal = Files.readString(root.resolve(Path.of("portal", "PortalEntity.java")));
            String preview = Files.readString(root.resolve(
                Path.of("client", "render", "PortalPlacementPreview.java")));
            String targetPreview = Files.readString(root.resolve(
                Path.of("client", "render", "PortalPairingEntityTargetPreview.java")));

            assertTrue(portal.contains("PAIRING_GUN"), version);
            assertTrue(portal.contains("entityData.set(PAIRING_GUN"), version);
            assertTrue(preview.contains("PortalPairingEntityTargetPreview.segments(minecraft)"), version);
            assertTrue(targetPreview.contains("PortalPairingEndpoint.ENTITY_TARGET"), version);
            assertTrue(targetPreview.contains("gunId.equals(portal.pairingGunId())"), version);
            assertTrue(targetPreview.contains("PortalFunctionMode.PORTAL_PAIRING"), version);
            assertTrue(targetPreview.contains("PortalPlacementMode.ENTITY_RELOCATION"), version);
            assertTrue(targetPreview.contains(
                "PortalPairingPreviewGeometry.entityTargetSegments("), version);
        }
    }
}
