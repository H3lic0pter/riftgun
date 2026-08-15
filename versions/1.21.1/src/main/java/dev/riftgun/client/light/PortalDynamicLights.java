package dev.riftgun.client.light;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Optional dynamic-light integration boundary; safe to load without provider mods. */
public final class PortalDynamicLights {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        if (!ModList.get().isLoaded("ryoamiclights")) {
            LOGGER.info("RyoamicLights is not installed; portal environment lighting is disabled");
            return;
        }

        try {
            RyoamicLightsPortalCompat.register();
            LOGGER.info("RyoamicLights portal environment lighting enabled");
        } catch (LinkageError | RuntimeException error) {
            LOGGER.warn("RyoamicLights portal integration could not be initialized", error);
        }
    }

    private PortalDynamicLights() {}
}
