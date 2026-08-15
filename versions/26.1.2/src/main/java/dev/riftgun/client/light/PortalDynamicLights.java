package dev.riftgun.client.light;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Dynamic-light integration boundary.
 *
 * <p>RyoamicLights (the 1.21.x provider) does not exist for 26.1.2, so portal environment
 * lighting is currently a no-op kept as a single seam for a future provider.
 */
public final class PortalDynamicLights {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("No 26.1.2 dynamic-light provider installed; portal environment lighting is disabled");
    }

    private PortalDynamicLights() {}
}
