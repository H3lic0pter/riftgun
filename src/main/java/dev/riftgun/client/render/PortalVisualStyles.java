package dev.riftgun.client.render;

import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalVisualSource;
import java.util.Objects;

/** Code-only visual seam. No player-facing color setting is exposed yet. */
public final class PortalVisualStyles {
    private static PortalVisualStyleProvider provider = portal -> PortalVisualStyle.fromRgb(portal.fuelRgb());

    public static PortalVisualStyle resolve(PortalEntity portal) {
        return provider.resolve(portal);
    }

    public static PortalVisualStyle resolve(PortalVisualSource portal) {
        return portal instanceof PortalEntity entity
            ? resolve(entity) : PortalVisualStyle.fromRgb(portal.fuelRgb());
    }

    public static void setProvider(PortalVisualStyleProvider next) {
        provider = Objects.requireNonNull(next);
    }

    private PortalVisualStyles() {}
}
