package dev.riftgun.service;

import org.jetbrains.annotations.Nullable;

public record PortalPlacementCapture(@Nullable PortalPlacementIntent intent, @Nullable String errorKey) {
    public static PortalPlacementCapture success(PortalPlacementIntent intent) {
        return new PortalPlacementCapture(intent, null);
    }

    public static PortalPlacementCapture failure(String errorKey) {
        return new PortalPlacementCapture(null, errorKey);
    }

    public boolean successful() {
        return intent != null;
    }
}
