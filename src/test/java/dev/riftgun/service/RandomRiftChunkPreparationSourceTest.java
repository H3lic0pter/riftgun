package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RandomRiftChunkPreparationSourceTest {
    @Test
    void randomSearchNeverLoadsOrGeneratesAChunkSynchronously() throws IOException {
        String manager = Files.readString(Path.of(
            "src/main/java/dev/riftgun/service/RandomRiftManager.java"));

        assertFalse(manager.contains("level.getChunk("));
        assertTrue(manager.contains("addPreparationTicket"));
        assertTrue(manager.contains("isPositionEntityTicking"));
        assertTrue(manager.contains("RandomRiftSearchPolicy.candidateProbe("));
        assertFalse(manager.contains("new BlockPos(candidateX, 0, candidateZ)"));
        assertTrue(manager.contains("removePreparationTicket"));
    }
}
