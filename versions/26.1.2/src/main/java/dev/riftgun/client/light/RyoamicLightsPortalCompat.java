package dev.riftgun.client.light;

import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.RiftGun;
import org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers;

/** Loaded only after the optional RyoamicLights mod has been detected. */
final class RyoamicLightsPortalCompat {
    static void register() {
        DynamicLightHandlers.registerDynamicLightHandler(
            RiftContent.PORTAL.get(), PortalDynamicLightLevel::forPortal);
        DynamicLightHandlers.registerDynamicLightHandler(
            RiftContent.ENTITY_RELOCATION_PORTAL.get(), PortalDynamicLightLevel::forRelocationPortal);
    }

    private RyoamicLightsPortalCompat() {}
}
