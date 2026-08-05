package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.AABB;

final class SidePortalCandidateSelector {
    static PortalPlacement choose(List<Candidate> candidates, AABB playerBounds) {
        var bodyCenter = playerBounds.getCenter();
        return candidates.stream()
            .min(Comparator.<Candidate>comparingInt(Candidate::backingBlocks).reversed()
                .thenComparingDouble(value -> value.placement().center().distanceToSqr(bodyCenter))
                .thenComparingDouble(value -> value.placement().center().y))
            .orElseThrow()
            .placement();
    }

    record Candidate(PortalPlacement placement, int backingBlocks) {}

    private SidePortalCandidateSelector() {}
}
