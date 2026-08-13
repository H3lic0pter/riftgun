package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalTransformTest {
    @Test
    void fallingIntoTopLeavesBottomDownward() {
        Vec3 result = PortalTransform.between(new Vec3(0.0, -1.0, 0.0),
            PortalOrientation.TOP, 0.0F, PortalOrientation.BOTTOM, 90.0F);
        assertVec(new Vec3(0.0, -1.0, 0.0), result);
    }

    @Test
    void risingIntoBottomLeavesTopUpward() {
        Vec3 result = PortalTransform.between(new Vec3(0.0, 1.0, 0.0),
            PortalOrientation.BOTTOM, 0.0F, PortalOrientation.TOP, 90.0F);
        assertVec(new Vec3(0.0, 1.0, 0.0), result);
    }

    @Test
    void verticalPortalsPreserveSpeedAndFlipNormal() {
        Vec3 input = new Vec3(0.25, 0.4, -1.2);
        Vec3 result = PortalTransform.between(input,
            PortalOrientation.VERTICAL, 0.0F, PortalOrientation.VERTICAL, 180.0F);
        assertEquals(input.length(), result.length(), 1.0E-8);
        assertEquals(1.2, result.dot(PortalOrientation.VERTICAL.normal(180.0F)), 1.0E-8);
    }

    @Test
    void enteringFromTheBackStillFacesAwayFromTheExit() {
        Vec3 input = PortalOrientation.VERTICAL.normal(0.0F);
        Vec3 result = PortalTransform.between(input,
            PortalOrientation.VERTICAL, 0.0F, PortalOrientation.VERTICAL, 180.0F);

        assertEquals(1.0, result.dot(PortalOrientation.VERTICAL.normal(180.0F)), 1.0E-8);
    }

    @Test
    void everyOrientationPairPreservesSpeedAndExitsOutward() {
        for (PortalOrientation source : PortalOrientation.values()) {
            for (PortalOrientation target : PortalOrientation.values()) {
                Vec3 input = source.normal(35.0F).scale(-2.0)
                    .add(source.traversalRight(35.0F).scale(0.3))
                    .add(source.traversalUp(35.0F).scale(0.2));

                Vec3 result = PortalTransform.between(
                    input, source, 35.0F, target, 215.0F);

                assertEquals(input.length(), result.length(), 1.0E-8,
                    source + " -> " + target);
                org.junit.jupiter.api.Assertions.assertTrue(
                    result.dot(target.normal(215.0F)) > 0.0,
                    source + " -> " + target);
            }
        }
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1.0E-8);
        assertEquals(expected.y, actual.y, 1.0E-8);
        assertEquals(expected.z, actual.z, 1.0E-8);
    }
}
