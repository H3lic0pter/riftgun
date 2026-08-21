package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalTransitReadinessSourceTest {
    @Test
    void transitUsesClockDerivedTargetPhaseWithoutTickingBypass() throws IOException {
        String orchestrator = read("src/main/java/dev/riftgun/portal/PortalTransitOrchestrator.java");

        assertTrue(orchestrator.contains("target.lifecyclePhaseAt(now)"));
        assertFalse(orchestrator.contains("targetTicking"));
    }

    @Test
    void bothNodesExposeClockDerivedLifecyclePhase() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = read("versions/" + node
                + "/src/main/java/dev/riftgun/portal/PortalEntity.java");
            assertTrue(source.contains("PortalLifecycle.Phase lifecyclePhaseAt(long now)"));
            assertTrue(source.contains(
                "PortalPairClock.phase(lifecycleStartedAt, closeStartedAt, now)"));
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
