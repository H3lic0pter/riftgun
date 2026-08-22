package dev.riftgun.client.compat.immersiveportal;

import dev.riftgun.client.render.PortalVisualRenderContext;
import dev.riftgun.portal.PortalEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

/**
 * Isolated bridge for IP client objects. It intentionally cannot affect Rift
 * Gun portal entities, transport, fuel, permissions, or lifecycle.
 */
final class ImmersivePortalBridge {
    static boolean render(PortalVisualRenderContext context) {
        if (!(context.portal() instanceof PortalEntity portal)) return false;
        return ImmersivePortalVisualCache.sync(portal, context.visibleProgress());
    }

    static void tick(Minecraft minecraft) {
        ImmersivePortalVisualCache.tick(minecraft);
    }

    static void track(Entity entity) {
        ImmersivePortalVisualCache.track(entity);
    }

    static void untrack(Entity entity) {
        ImmersivePortalVisualCache.untrack(entity);
    }

    static void clear() {
        ImmersivePortalVisualCache.reset();
    }

    static float readiness(UUID portalId) {
        return ImmersivePortalVisualCache.readiness(portalId);
    }

    private ImmersivePortalBridge() {}
}
