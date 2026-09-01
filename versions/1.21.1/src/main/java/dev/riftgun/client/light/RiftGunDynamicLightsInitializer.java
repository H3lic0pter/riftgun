package dev.riftgun.client.light;

import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import dev.riftgun.RiftGun;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** LambDynamicLights entrypoint. This class is loaded only when the optional mod is present. */
public final class RiftGunDynamicLightsInitializer implements DynamicLightsInitializer {
    private static final PortalEntityLuminance PORTAL_LUMINANCE = new PortalEntityLuminance();
    private static final EntityLuminance.Type PORTAL_LUMINANCE_TYPE = EntityLuminance.Type.registerSimple(
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "portal"), PORTAL_LUMINANCE);

    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        context.entityLightSourceManager().onRegisterEvent().register(registration -> {
            registration.register(RiftContent.PORTAL.get(), PORTAL_LUMINANCE);
            registration.register(RiftContent.ENTITY_RELOCATION_PORTAL.get(), PORTAL_LUMINANCE);
        });
    }

    @Override
    @SuppressWarnings("removal")
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        // Compatibility method retained by the 1.21.1 API; entity registration uses the context overload.
    }

    private static final class PortalEntityLuminance implements EntityLuminance {
        @Override
        public Type type() {
            return PORTAL_LUMINANCE_TYPE;
        }

        @Override
        public int getLuminance(ItemLightSourceManager itemLightSourceManager, Entity entity) {
            if (entity instanceof PortalEntity portal) {
                return PortalDynamicLightLevel.forPortal(portal);
            }
            if (entity instanceof EntityRelocationPortalEntity relocationPortal) {
                return PortalDynamicLightLevel.forRelocationPortal(relocationPortal);
            }
            return 0;
        }
    }
}
