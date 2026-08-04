package dev.riftgun.service;

import dev.riftgun.portal.PortalPairPlacement;
import org.jetbrains.annotations.Nullable;

public record PortalPlacementResult(@Nullable PortalPairPlacement pair, @Nullable String errorKey) {
    public static PortalPlacementResult success(PortalPairPlacement pair) {
        return new PortalPlacementResult(pair, null);
    }

    public static PortalPlacementResult failure(String errorKey) {
        return new PortalPlacementResult(null, errorKey);
    }

    public boolean successful() {
        return pair != null;
    }
}
