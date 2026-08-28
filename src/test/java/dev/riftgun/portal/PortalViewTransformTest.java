package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalViewTransformTest {
    private static final float INPUT_YAW = 37.0F;
    private static final float FACING_THRESHOLD = 0.35F;

    @Test
    void everyPairWithAHorizontalPortalKeepsPlayerYaw() {
        Vec3 look = Vec3.directionFromRotation(18.0F, INPUT_YAW);
        for (PortalOrientation source : PortalOrientation.values()) {
            for (PortalOrientation target : PortalOrientation.values()) {
                if (source == PortalOrientation.VERTICAL && target == PortalOrientation.VERTICAL) {
                    continue;
                }
                PortalViewTransform.Rotation rotation = PortalViewTransform.playerRotation(
                    look, INPUT_YAW, 18.0F, source, 13.0F, target, 127.0F,
                    (float) look.dot(source.normal(13.0F)), FACING_THRESHOLD);

                assertEquals(INPUT_YAW, rotation.yaw(), 1.0E-4F,
                    source + " -> " + target);
                assertEquals(18.0F, rotation.pitch(), 1.0E-3F,
                    source + " -> " + target);
            }
        }
    }

    @Test
    void purelyVerticalOutputUsesIncomingYaw() {
        PortalViewTransform.Rotation rotation = PortalViewTransform.playerRotation(
            PortalOrientation.VERTICAL.normal(90.0F).scale(-1.0), INPUT_YAW, 0.0F,
            PortalOrientation.VERTICAL, 90.0F, PortalOrientation.TOP, 173.0F,
            -1.0F, FACING_THRESHOLD);

        assertEquals(INPUT_YAW, rotation.yaw(), 1.0E-4F);
        assertEquals(0.0F, rotation.pitch(), 1.0E-4F);
    }

    @Test
    void horizontalPairKeepsIncomingPitch() {
        Vec3 lookingDown = Vec3.directionFromRotation(60.0F, INPUT_YAW);

        PortalViewTransform.Rotation topExit = PortalViewTransform.playerRotation(
            lookingDown, INPUT_YAW, 60.0F, PortalOrientation.TOP, 0.0F,
            PortalOrientation.TOP, 90.0F, -1.0F, FACING_THRESHOLD);
        PortalViewTransform.Rotation bottomExit = PortalViewTransform.playerRotation(
            lookingDown, INPUT_YAW, 60.0F, PortalOrientation.TOP, 0.0F,
            PortalOrientation.BOTTOM, 90.0F, -1.0F, FACING_THRESHOLD);

        assertEquals(60.0F, topExit.pitch(), 1.0E-3F);
        assertEquals(60.0F, bottomExit.pitch(), 1.0E-3F);
    }

    @Test
    void sideToHorizontalViewDoesNotJumpAcrossFacingThreshold() {
        Vec3 look = Vec3.directionFromRotation(24.0F, INPUT_YAW);
        PortalViewTransform.Rotation front = PortalViewTransform.playerRotation(
            look, INPUT_YAW, 24.0F, PortalOrientation.VERTICAL, 0.0F,
            PortalOrientation.TOP, 90.0F, -0.01F, FACING_THRESHOLD);
        PortalViewTransform.Rotation back = PortalViewTransform.playerRotation(
            look, INPUT_YAW, 24.0F, PortalOrientation.VERTICAL, 0.0F,
            PortalOrientation.TOP, 90.0F, 0.01F, FACING_THRESHOLD);

        assertEquals(front.yaw(), back.yaw(), 1.0E-4F);
        assertEquals(front.pitch(), back.pitch(), 1.0E-4F);
        assertEquals(24.0F, front.pitch(), 1.0E-3F);
    }

    @Test
    void verticalPairKeepsLegacyViewMapping() {
        Vec3 look = Vec3.directionFromRotation(12.0F, INPUT_YAW);
        Vec3 expectedLook = PortalTransform.between(
            look, PortalOrientation.VERTICAL, 15.0F,
            PortalOrientation.VERTICAL, 120.0F).normalize();
        PortalViewTransform.Rotation expected = PortalViewTransform.rotationFor(expectedLook, INPUT_YAW);

        PortalViewTransform.Rotation actual = PortalViewTransform.playerRotation(
            look, INPUT_YAW, 12.0F, PortalOrientation.VERTICAL, 15.0F,
            PortalOrientation.VERTICAL, 120.0F, -1.0F, FACING_THRESHOLD);

        assertEquals(expected.yaw(), actual.yaw(), 1.0E-4F);
        assertEquals(expected.pitch(), actual.pitch(), 1.0E-4F);
    }
}
