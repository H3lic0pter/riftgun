package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SneakKeyLabelSourceTest {
    @Test
    void actionHintsUseTheConfiguredSneakKeyInBothClients() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path client = Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client");
            String labels = Files.readString(client.resolve("PortalInputLabels.java"));
            String events = Files.readString(client.resolve("ClientGameEvents.java"));
            String radial = Files.readString(client.resolve(
                Path.of("screen", "ModeRadialScreen.java")));
            String settings = Files.readString(client.resolve(
                Path.of("screen", "PortalConfigScreen.java")));

            assertTrue(labels.contains("options.keyShift.getTranslatedKeyMessage()"));
            assertTrue(events.contains("PortalInputLabels.sneakKey()"));
            assertTrue(radial.contains("PortalInputLabels.sneakKey()"));
            assertTrue(settings.contains("PortalInputLabels.sneakKey()"));
        }
    }
}
