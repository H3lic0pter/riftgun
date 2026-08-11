package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class EntityRelocationArrivalLatchTest {
    @Test
    void guardCoversTheFullDropCorridorWithOneBlockHorizontalMargin() {
        AABB volume = EntityRelocationArrivalLatch.guardVolume(
            new Vec3(10.0, 64.0, -4.0), new Vec3(10.0, 67.0, -4.0), 2.0F);

        assertTrue(volume.intersects(new AABB(8.1, 64.0, -4.2, 8.7, 65.8, -3.6)));
        assertTrue(volume.intersects(new AABB(9.7, 66.8, -4.3, 10.3, 68.0, -3.7)));
        assertFalse(volume.intersects(new AABB(12.01, 64.0, -4.2, 12.61, 65.8, -3.6)));
        assertFalse(volume.intersects(new AABB(9.7, 67.26, -4.3, 10.3, 68.0, -3.7)));
    }

    @Test
    void protectionRequiresBothLeavingAndCooldownExpiry() {
        assertTrue(EntityRelocationArrivalLatch.shouldBlock(false, 120L, 120L));
        assertTrue(EntityRelocationArrivalLatch.shouldBlock(true, 119L, 120L));
        assertFalse(EntityRelocationArrivalLatch.shouldBlock(true, 120L, 120L));
    }

    @Test
    void zeroCooldownStillRequiresLeavingTheArrivalVolume() {
        assertTrue(EntityRelocationArrivalLatch.shouldBlock(false, 40L, 40L));
        assertFalse(EntityRelocationArrivalLatch.shouldBlock(true, 40L, 40L));
    }
}
