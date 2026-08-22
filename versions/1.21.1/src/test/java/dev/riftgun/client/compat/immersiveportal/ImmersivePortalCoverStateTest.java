package dev.riftgun.client.compat.immersiveportal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ImmersivePortalCoverStateTest {
    @Test
    void stopsChunkChecksOnceReadyAndStopsTicksAfterFade() {
        ImmersivePortalCoverState cover = new ImmersivePortalCoverState();
        assertTrue(cover.needsDestinationCheck());
        assertTrue(cover.needsTick());

        cover.markReady();
        assertFalse(cover.needsDestinationCheck());
        for (int tick = 0; tick < 5; tick++) cover.tick();

        assertFalse(cover.needsTick());
    }
}
