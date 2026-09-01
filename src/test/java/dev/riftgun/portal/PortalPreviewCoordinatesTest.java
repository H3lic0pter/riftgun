package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PortalPreviewCoordinatesTest {
    @Test
    void fixedWorldPointMovesSmoothlyOppositeToViewAtLargeCoordinates() {
        Vec3 point = new Vec3(29_999_999.75, 80.5, -29_999_995.25);
        Vec3 firstView = new Vec3(29_999_995.62655, 78.0, -29_999_999.37345);
        Vec3 secondView = firstView.add(0.01, 0.0, -0.01);

        float firstX = PortalPreviewCoordinates.relativeTo(firstView.x, point.x);
        float secondX = PortalPreviewCoordinates.relativeTo(secondView.x, point.x);
        float firstZ = PortalPreviewCoordinates.relativeTo(firstView.z, point.z);
        float secondZ = PortalPreviewCoordinates.relativeTo(secondView.z, point.z);

        assertEquals(-0.01, secondX - firstX, 1.0E-6);
        assertEquals(0.01, secondZ - firstZ, 1.0E-6);
    }
}
