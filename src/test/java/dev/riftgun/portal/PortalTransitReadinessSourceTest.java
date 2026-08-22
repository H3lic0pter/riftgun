package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalTransitReadinessSourceTest {
    @Test
    void allTransitPathsUseTheVersionAwareReadinessSeam() throws IOException {
        String orchestrator = read("src/main/java/dev/riftgun/portal/PortalTransitOrchestrator.java");

        assertEquals(3, occurrences(orchestrator, "target.transitReadinessAt(now)"));
        assertFalse(orchestrator.contains("target.phase()"));
        assertFalse(orchestrator.contains("target.lifecyclePhaseAt(now)"));
    }

    @Test
    void bothNodesExposeClockDerivedLifecyclePhase() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = read("versions/" + node
                + "/src/main/java/dev/riftgun/portal/PortalEntity.java");
            assertTrue(source.contains("PortalLifecycle.Phase lifecyclePhaseAt(long now)"));
            assertTrue(source.contains(
                "PortalPairClock.phase(lifecycleStartedAt, closeStartedAt, now)"));
            assertTrue(source.contains("PortalTransitReadiness transitReadinessAt(long now)"));
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static int occurrences(String source, String needle) {
        return source.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }
}
