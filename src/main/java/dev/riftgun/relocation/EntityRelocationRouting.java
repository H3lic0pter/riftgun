package dev.riftgun.relocation;

import dev.riftgun.data.PortalPlacementMode;

/** Resolves explicit, shortcut and SMART requests without knowing about ray casts or networking. */
public final class EntityRelocationRouting {
    public static PortalPlacementMode normalizePlacementMode(PortalPlacementMode mode,
                                                              boolean relocationAvailable) {
        return mode == PortalPlacementMode.ENTITY_RELOCATION && !relocationAvailable
            ? PortalPlacementMode.SMART : mode;
    }

    public static Route decide(boolean moduleInstalled, EntityRelocationSettings settings,
                               PortalPlacementMode mode, Trigger trigger,
                               boolean eligibleTarget) {
        boolean explicit = trigger == Trigger.SHORTCUT
            || mode == PortalPlacementMode.ENTITY_RELOCATION;
        if (!moduleInstalled || !settings.enabled()) {
            return explicit ? Route.UNAVAILABLE : Route.PORTAL;
        }
        if (explicit) return eligibleTarget ? Route.RELOCATE : Route.UNAVAILABLE;
        if (mode == PortalPlacementMode.SMART && settings.smartRouting() && eligibleTarget) {
            return Route.RELOCATE;
        }
        return Route.PORTAL;
    }

    public enum Trigger {
        INTERACTION,
        SHORTCUT
    }

    public enum Route {
        RELOCATE,
        PORTAL,
        UNAVAILABLE
    }

    private EntityRelocationRouting() {}
}
