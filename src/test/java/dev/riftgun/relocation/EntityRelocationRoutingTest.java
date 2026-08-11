package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.data.PortalPlacementMode;
import org.junit.jupiter.api.Test;

final class EntityRelocationRoutingTest {
    @Test
    void explicitModeAndShortcutRequireAnEnabledInstalledModule() {
        EntityRelocationSettings enabled = new EntityRelocationSettings(true, false);
        EntityRelocationSettings disabled = new EntityRelocationSettings(false, true);

        assertEquals(EntityRelocationRouting.Route.RELOCATE,
            EntityRelocationRouting.decide(true, enabled, PortalPlacementMode.ENTITY_RELOCATION,
                EntityRelocationRouting.Trigger.INTERACTION, true));
        assertEquals(EntityRelocationRouting.Route.UNAVAILABLE,
            EntityRelocationRouting.decide(false, enabled, PortalPlacementMode.ENTITY_RELOCATION,
                EntityRelocationRouting.Trigger.INTERACTION, true));
        assertEquals(EntityRelocationRouting.Route.UNAVAILABLE,
            EntityRelocationRouting.decide(true, disabled, PortalPlacementMode.FRONT,
                EntityRelocationRouting.Trigger.SHORTCUT, true));
    }

    @Test
    void smartOnlyPrioritizesEligibleEntitiesWhenRoutingIsEnabled() {
        assertEquals(EntityRelocationRouting.Route.RELOCATE,
            EntityRelocationRouting.decide(true, new EntityRelocationSettings(true, true),
                PortalPlacementMode.SMART, EntityRelocationRouting.Trigger.INTERACTION, true));
        assertEquals(EntityRelocationRouting.Route.PORTAL,
            EntityRelocationRouting.decide(true, new EntityRelocationSettings(true, false),
                PortalPlacementMode.SMART, EntityRelocationRouting.Trigger.INTERACTION, true));
        assertEquals(EntityRelocationRouting.Route.PORTAL,
            EntityRelocationRouting.decide(true, new EntityRelocationSettings(true, true),
                PortalPlacementMode.SMART, EntityRelocationRouting.Trigger.INTERACTION, false));
    }

    @Test
    void unavailableEntityRelocationModeFallsBackToSmart() {
        assertEquals(PortalPlacementMode.SMART,
            EntityRelocationRouting.normalizePlacementMode(
                PortalPlacementMode.ENTITY_RELOCATION, false));
        assertEquals(PortalPlacementMode.ENTITY_RELOCATION,
            EntityRelocationRouting.normalizePlacementMode(
                PortalPlacementMode.ENTITY_RELOCATION, true));
        assertEquals(PortalPlacementMode.SURFACE,
            EntityRelocationRouting.normalizePlacementMode(PortalPlacementMode.SURFACE, false));
    }
}
