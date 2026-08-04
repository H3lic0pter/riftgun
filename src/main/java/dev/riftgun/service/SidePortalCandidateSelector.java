package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.Vec3;

final class SidePortalCandidateSelector {
    static PortalPlacement choose(List<Candidate> candidates, Vec3 eyePosition) {
        return candidates.stream()
            .min(Comparator.<Candidate>comparingInt(Candidate::backingBlocks).reversed()
                .thenComparingDouble(value -> value.placement().distanceToSqr(eyePosition)))
            .orElseThrow()
            .placement();
    }

    record Candidate(PortalPlacement placement, int backingBlocks) {}

    private SidePortalCandidateSelector() {}
}
