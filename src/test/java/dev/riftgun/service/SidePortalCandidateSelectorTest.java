package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertSame;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class SidePortalCandidateSelectorTest {
    @Test
    void completeBackingWinsEvenWhenTheHangingCandidateIsCloser() {
        PortalPlacement hangingAbove = placement(2.0);
        PortalPlacement wallBelow = placement(1.0);

        PortalPlacement result = SidePortalCandidateSelector.choose(List.of(
            new SidePortalCandidateSelector.Candidate(hangingAbove, 1),
            new SidePortalCandidateSelector.Candidate(wallBelow, 2)
        ), playerBounds(1.1, 2.9));

        assertSame(wallBelow, result);
    }

    @Test
    void bodyDistanceBreaksTiesWhenBackingIsEqual() {
        PortalPlacement above = placement(2.0);
        PortalPlacement below = placement(1.0);

        PortalPlacement result = SidePortalCandidateSelector.choose(List.of(
            new SidePortalCandidateSelector.Candidate(above, 1),
            new SidePortalCandidateSelector.Candidate(below, 1)
        ), playerBounds(0.0, 1.8));

        assertSame(below, result);
    }

    @Test
    void lowerCandidateBreaksAnExactTie() {
        PortalPlacement above = placement(2.0);
        PortalPlacement below = placement(1.0);

        PortalPlacement result = SidePortalCandidateSelector.choose(List.of(
            new SidePortalCandidateSelector.Candidate(above, 1),
            new SidePortalCandidateSelector.Candidate(below, 1)
        ), playerBounds(0.6, 2.4));

        assertSame(below, result);
    }

    private static PortalPlacement placement(double centerY) {
        return new PortalPlacement(new Vec3(0.0, centerY, 0.0), PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_VERTICAL, 180.0F, null, null);
    }

    private static net.minecraft.world.phys.AABB playerBounds(double minY, double maxY) {
        return new net.minecraft.world.phys.AABB(-0.3, minY, -0.3, 0.3, maxY, 0.3);
    }
}
