package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PortalDestinationTest {
    private static final RiftResourceId DIMENSION = RiftResourceId.parse("riftworld:reality/test");

    @Test
    void carriesAStableDimensionIdAndArrivalPose() {
        PortalDestination destination = new PortalDestination(DIMENSION, 1.5, 64.0, -2.5, 90.0F);

        assertEquals(DIMENSION, destination.dimensionId());
        assertEquals(1.5, destination.x());
        assertEquals(64.0, destination.y());
        assertEquals(-2.5, destination.z());
        assertEquals(90.0F, destination.yaw());
    }

    @Test
    void rejectsNonFiniteArrivalPosesAtTheApiBoundary() {
        assertThrows(IllegalArgumentException.class,
            () -> new PortalDestination(DIMENSION, Double.NaN, 64.0, 0.0, 0.0F));
        assertThrows(IllegalArgumentException.class,
            () -> new PortalDestination(DIMENSION, 0.0, Double.POSITIVE_INFINITY, 0.0, 0.0F));
        assertThrows(IllegalArgumentException.class,
            () -> new PortalDestination(DIMENSION, 0.0, 64.0, 0.0, Float.NaN));
    }
}
