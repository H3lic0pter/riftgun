package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.data.PortalPlacementMode;
import org.junit.jupiter.api.Test;

final class PortalOpenOriginTest {
    @Test
    void guiAlwaysOpensInFront() {
        for (PortalPlacementMode requested : PortalPlacementMode.values()) {
            assertEquals(PortalPlacementMode.FRONT, PortalOpenOrigin.GUI.resolvePlacement(requested));
        }
    }

    @Test
    void itemInteractionRetainsItsRequestedPlacement() {
        for (PortalPlacementMode requested : PortalPlacementMode.values()) {
            assertEquals(requested, PortalOpenOrigin.ITEM.resolvePlacement(requested));
        }
    }
}
