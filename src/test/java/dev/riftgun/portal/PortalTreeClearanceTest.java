package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalTreeClearanceTest {
    @Test
    void playerFitsIntoOneBlockOfExitSpaceWithoutBeingLifted() {
        PortalPlacement exit = verticalExit(0.0F);
        Vec3 source = new Vec3(10.0, 64.0, 20.0);
        AABB body = new AABB(9.7, 64.0, 19.7, 10.3, 65.8, 20.3);
        Vec3 destination = PortalTreeClearance.verticalPosition(exit, source, body);
        assertEquals(0.41, destination.z, 1.0E-9);
        assertEquals(-1.0, destination.y, 1.0E-9);

        AABB predicted = body.move(destination.subtract(source));
        Vec3 result = PortalTreeClearance.destination(exit, destination, List.of(predicted), 0.0,
            bounds -> bounds.maxZ > 1.0);

        assertEquals(destination.x, result.x, 1.0E-9);
        assertEquals(destination.y, result.y, 1.0E-9);
        assertEquals(destination.z, result.z, 1.0E-9);
        assertTrue(predicted.maxZ < 1.0);
    }

    @Test
    void diagonalExitUsesActualBodyProjectionInsteadOfFixedPlayerWidth() {
        PortalPlacement exit = verticalExit(45.0F);
        AABB body = new AABB(-0.7, 0.0, -0.7, 0.7, 1.4, 0.7);
        Vec3 destination = PortalTreeClearance.verticalPosition(exit, Vec3.ZERO, body);
        AABB moved = body.move(destination);
        Vec3 normal = exit.normal();
        double radius = Math.abs(normal.x) * 0.7 + Math.abs(normal.z) * 0.7;

        assertEquals(0.11, moved.getCenter().subtract(exit.center()).dot(normal) - radius, 1.0E-9);
    }

    @Test
    void collidingVerticalExitLiftsExactlyOnceEvenIfStillBlocked() {
        PortalPlacement exit = verticalExit(0.0F);
        Vec3 destination = new Vec3(0.0, -1.0, 0.41);
        AtomicInteger probes = new AtomicInteger();

        Vec3 result = PortalTreeClearance.destination(exit, destination,
            List.of(new AABB(-0.3, -1.0, 0.11, 0.3, 0.8, 0.71)), 0.0, bounds -> {
                probes.incrementAndGet();
                return true;
            });

        assertEquals(0.0, result.x, 1.0E-9);
        assertEquals(0.0, result.y, 1.0E-9);
        assertEquals(0.41, result.z, 1.0E-9);
        assertEquals(1, probes.get(), "do not retry or check the lifted position");
    }

    @Test
    void passengerCollisionLiftsTheWholeTreeUsingOneSharedDestination() {
        PortalPlacement exit = verticalExit(0.0F);
        AABB vehicle = new AABB(-0.7, -0.3, 0.20, 0.7, 0.3, 1.60);
        AABB passenger = new AABB(-0.3, 0.0, 0.01, 0.3, 1.8, 0.61);
        AtomicInteger probes = new AtomicInteger();

        Vec3 result = PortalTreeClearance.destination(exit, Vec3.ZERO,
            List.of(vehicle, passenger), 0.0, bounds -> {
                assertTrue(bounds.minZ >= 0.11, "clear the entire tree before probing collisions");
                return probes.incrementAndGet() == 2;
            });

        assertEquals(1.0, result.y, 1.0E-9);
        assertEquals(0.10, result.z, 1.0E-9);
        assertEquals(2, probes.get());
    }

    @Test
    void horizontalExitsKeepTriggerClearanceWithoutCollisionLiftsOrRefusals() {
        AABB body = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
        for (PortalOrientation orientation : List.of(PortalOrientation.TOP, PortalOrientation.BOTTOM)) {
            PortalPlacement exit = new PortalPlacement(Vec3.ZERO, orientation,
                PortalGeometry.HORIZONTAL, 0.0F, null, null);
            double correction = PortalTreeClearance.outwardCorrection(exit, List.of(body), 0.4);
            Vec3 result = PortalTreeClearance.destination(exit, Vec3.ZERO, List.of(body), 0.4,
                bounds -> { fail("horizontal exits do not use collision lifts"); return true; });
            assertEquals(exit.normal().scale(correction), result);
        }
    }

    private static PortalPlacement verticalExit(float yaw) {
        return new PortalPlacement(Vec3.ZERO, PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_VERTICAL, yaw, null, null);
    }

    @Test
    void correctionUsesThePassengerBoundNearestTheExitPlane() {
        PortalPlacement exit = new PortalPlacement(Vec3.ZERO, PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_VERTICAL, 0.0F, null, null);
        AABB vehicle = new AABB(-0.7, -0.3, 0.20, 0.7, 0.3, 1.60);
        AABB player = new AABB(-0.3, 0.0, 0.01, 0.3, 1.8, 0.61);

        double correction = PortalTreeClearance.outwardCorrection(
            exit, List.of(vehicle, player), 0.0);

        assertEquals(0.10, correction, 1.0E-9);
    }
}
