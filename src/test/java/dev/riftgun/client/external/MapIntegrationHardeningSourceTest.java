package dev.riftgun.client.external;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MapIntegrationHardeningSourceTest {
    @Test
    void bothVersionNodesGuardAdapterConstructionAndCleanTickWork() throws Exception {
        for (String version : new String[] { "1.21.1", "26.1.2" }) {
            Path clientRoot = Path.of("versions", version, "src/main/java/dev/riftgun/client");
            String integration = Files.readString(clientRoot.resolve(
                "external/ClientMapWaypointIntegration.java"));
            String events = Files.readString(clientRoot.resolve("ClientGameEvents.java"));

            assertTrue(integration.contains("installAdapter(ExternalDestinationSource.JOURNEYMAP)"));
            assertTrue(integration.contains("catch (LinkageError | RuntimeException exception)"));
            assertTrue(integration.contains("public static boolean journeyMapDirty()"));
            assertTrue(events.contains("if (!ClientMapWaypointIntegration.journeyMapDirty()) return;"));
        }
    }
}
