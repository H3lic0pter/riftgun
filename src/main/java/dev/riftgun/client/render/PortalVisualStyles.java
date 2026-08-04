package dev.riftgun.client.render;

import dev.riftgun.portal.PortalEntity;
import java.util.Objects;

/** Code-only visual seam. No player-facing color setting is exposed yet. */
public final class PortalVisualStyles {
    private static PortalVisualStyleProvider provider = ignored -> PortalVisualStyle.PALE_GREEN;

    public static PortalVisualStyle resolve(PortalEntity portal) {
        return provider.resolve(portal);
    }

    public static void setProvider(PortalVisualStyleProvider next) {
        provider = Objects.requireNonNull(next);
    }

    private PortalVisualStyles() {}
}
