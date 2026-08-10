package dev.riftgun.service;

import dev.riftgun.data.PortalPlacementMode;

/** Resolves placement semantics at the boundary where a portal-open request originates. */
public enum PortalOpenOrigin {
    GUI {
        @Override
        public PortalPlacementMode resolvePlacement(PortalPlacementMode requested) {
            return PortalPlacementMode.FRONT;
        }
    },
    ITEM;

    public PortalPlacementMode resolvePlacement(PortalPlacementMode requested) {
        return requested;
    }
}
