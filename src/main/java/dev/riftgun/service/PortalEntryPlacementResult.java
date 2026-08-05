package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import org.jetbrains.annotations.Nullable;

public record PortalEntryPlacementResult(@Nullable PortalPlacement placement, @Nullable String errorKey) {
    public static PortalEntryPlacementResult success(PortalPlacement placement) {
        return new PortalEntryPlacementResult(placement, null);
    }

    public static PortalEntryPlacementResult failure(String errorKey) {
        return new PortalEntryPlacementResult(null, errorKey);
    }

    public boolean successful() {
        return placement != null;
    }
}
