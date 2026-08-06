package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertSame;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ExpandedPortalCandidateSelectorTest {
    @Test
    void exactHitLocationWinsBeforePlayerDistance() {
        PortalPlacement hitSide = placement(1.0, 1.0);
        PortalPlacement playerSide = placement(-1.0, 1.0);

        PortalPlacement selected = ExpandedPortalCandidateSelector.choose(
            List.of(playerSide, hitSide), new Vec3(0.9, 1.0, 0.0), new Vec3(-2.0, 1.0, 0.0));

        assertSame(hitSide, selected);
    }

    @Test
    void playerDistanceBreaksAnExactHitTie() {
        PortalPlacement left = placement(-1.0, 1.0);
        PortalPlacement right = placement(1.0, 1.0);

        PortalPlacement selected = ExpandedPortalCandidateSelector.choose(
            List.of(right, left), new Vec3(0.0, 1.0, 0.0), new Vec3(-2.0, 1.0, 0.0));

        assertSame(left, selected);
    }

    private static PortalPlacement placement(double x, double y) {
        return new PortalPlacement(new Vec3(x, y, 0.0), PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_EXPANDED, 180.0F, null, null);
    }
}
