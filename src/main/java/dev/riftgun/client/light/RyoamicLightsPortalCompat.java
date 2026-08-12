package dev.riftgun.client.light;

import dev.riftgun.RiftGun;
import org.thinkingstudio.ryoamiclights.api.DynamicLightHandlers;

/** Loaded only after the optional RyoamicLights mod has been detected. */
final class RyoamicLightsPortalCompat {
    static void register() {
        DynamicLightHandlers.registerDynamicLightHandler(
            RiftGun.PORTAL.get(), PortalDynamicLightLevel::forPortal);
        DynamicLightHandlers.registerDynamicLightHandler(
            RiftGun.ENTITY_RELOCATION_PORTAL.get(), PortalDynamicLightLevel::forRelocationPortal);
    }

    private RyoamicLightsPortalCompat() {}
}
