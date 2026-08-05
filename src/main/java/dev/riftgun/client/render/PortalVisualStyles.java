package dev.riftgun.client.render;

import dev.riftgun.portal.PortalEntity;
import java.util.Objects;

/** Code-only visual seam. No player-facing color setting is exposed yet. */
public final class PortalVisualStyles {
    private static PortalVisualStyleProvider provider = portal -> PortalVisualStyle.fromRgb(portal.fuelRgb());

    public static PortalVisualStyle resolve(PortalEntity portal) {
        return provider.resolve(portal);
    }

    public static void setProvider(PortalVisualStyleProvider next) {
        provider = Objects.requireNonNull(next);
    }

    private PortalVisualStyles() {}
}
