package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalTransform;
import dev.riftgun.portal.PortalViewTransform;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class RemotePortalPlacementTest {
    @Test
    void walkingIntoRemotePortalLeavesExitBehindWithTheSameEntryAngle() {
        float pitch = 12.0F;
        for (float shotYaw : new float[] {-170.0F, 0.0F, 90.0F, 179.0F}) {
            var entrance = RemotePortalPlacementResolver.placement(new Vec3(8.0, 65.0, 12.0),
                PortalOrientation.VERTICAL, PortalGeometry.FLOATING_VERTICAL, shotYaw);
            for (float offset : new float[] {-45.0F, 0.0F, 45.0F}) {
                float inputYaw = shotYaw + offset;
                Vec3 look = Vec3.directionFromRotation(pitch, inputYaw).normalize();
                float facing = (float) look.dot(entrance.normal());
                assertTrue(facing < 0.0F, "The shooter must approach the entrance's front");
                for (float exitYaw : new float[] {-90.0F, 0.0F, 137.0F}) {
                    var rotation = PortalViewTransform.playerRotation(look, inputYaw, pitch,
                        entrance.orientation(), entrance.yaw(), PortalOrientation.VERTICAL, exitYaw,
                        facing, 0.35F);
                    Vec3 outputLook = Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()).normalize();
                    double outward = outputLook.dot(PortalOrientation.VERTICAL.normal(exitYaw));
                    assertTrue(outward > 0.0, "The exit must be behind the player");
                    // Minecraft's sine lookup quantizes directionFromRotation by about 0.006 degrees.
                    assertEquals(-facing, outward, 2.0E-4, "Keep the angle to the portal face");
                    assertEquals(pitch, rotation.pitch(), 0.01);
                    Vec3 momentum = PortalTransform.between(look, entrance.orientation(), entrance.yaw(),
                        PortalOrientation.VERTICAL, exitYaw).normalize();
                    assertEquals(1.0, outputLook.dot(momentum), 1.0E-5,
                        "View and forward motion must leave in the same direction");
                }
            }
        }
    }

    @Test
    void horizontalRemotePortalsKeepTheirTraversalYaw() {
        for (var orientation : new PortalOrientation[] {PortalOrientation.TOP, PortalOrientation.BOTTOM}) {
            var placement = RemotePortalPlacementResolver.placement(Vec3.ZERO, orientation,
                PortalGeometry.HORIZONTAL, 37.0F);
            assertEquals(37.0F, placement.yaw());
            assertEquals(orientation, placement.orientation());
        }
    }
}
