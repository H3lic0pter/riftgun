package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalExitTargetTest {
    @Test
    void deferredTargetSurvivesEntityNbtRoundTrip() {
        PortalExitTarget original = new PortalExitTarget(
            UUID.randomUUID(), Level.NETHER, new Vec3(12.25, 70.0, -31.75), 135.0F);

        assertEquals(original, PortalExitTarget.load(original.save()));
    }
}
