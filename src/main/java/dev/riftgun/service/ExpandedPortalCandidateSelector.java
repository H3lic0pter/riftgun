package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.Vec3;

final class ExpandedPortalCandidateSelector {
    static PortalPlacement choose(List<PortalPlacement> candidates, Vec3 hit, Vec3 playerCenter) {
        return candidates.stream()
            .min(Comparator.comparingDouble((PortalPlacement value) -> value.center().distanceToSqr(hit))
                .thenComparingDouble(value -> value.center().distanceToSqr(playerCenter)))
            .orElseThrow();
    }

    private ExpandedPortalCandidateSelector() {}
}
