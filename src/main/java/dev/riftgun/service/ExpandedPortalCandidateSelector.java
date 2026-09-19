package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalAperture;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

final class ExpandedPortalCandidateSelector {
    /** Partially supported modes use the same support/body-position ordering as 1x2 portals. */
    static PortalPlacement chooseAttached(List<SidePortalCandidateSelector.Candidate> candidates,
                                          Vec3 hit, AABB playerBounds, PortalAperture aperture) {
        if (!aperture.requiresFullSupport()) return SidePortalCandidateSelector.choose(candidates, playerBounds);
        return candidates.stream()
            .min(Comparator.comparing(SidePortalCandidateSelector.Candidate::placement,
                byPosition(hit, playerBounds.getCenter())))
            .orElseThrow().placement();
    }

    static PortalPlacement choose(List<PortalPlacement> candidates, Vec3 hit, Vec3 playerCenter) {
        return candidates.stream()
            .min(byPosition(hit, playerCenter))
            .orElseThrow();
    }

    private static Comparator<PortalPlacement> byPosition(Vec3 hit, Vec3 playerCenter) {
        return Comparator.comparingDouble((PortalPlacement value) -> value.center().distanceToSqr(hit))
            .thenComparingDouble(value -> value.center().distanceToSqr(playerCenter));
    }

    private ExpandedPortalCandidateSelector() {}
}
