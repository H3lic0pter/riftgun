package dev.riftgun.client.light;

import dev.riftgun.config.ClientConfig;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;

public final class PortalDynamicLightLevel {
    public static final int DEFAULT_MAXIMUM = 9;
    public static final int MINIMUM = 0;
    public static final int MAXIMUM = 15;

    public static int forPortal(PortalEntity portal) {
        return forLifecycle(portal.phase(), portal.phaseTicks(), configuredMaximum());
    }

    static int forLifecycle(PortalLifecycle.Phase phase, int phaseTicks, int maximum) {
        int boundedMaximum = Math.max(MINIMUM, Math.min(MAXIMUM, maximum));
        float progress = PortalLifecycle.visibleProgress(phase, phaseTicks, 0.0F);
        return Math.round(boundedMaximum * progress);
    }

    public static int configuredMaximum() {
        return ClientConfig.VALUES.portalDynamicLightLevel.get();
    }

    private PortalDynamicLightLevel() {}
}
