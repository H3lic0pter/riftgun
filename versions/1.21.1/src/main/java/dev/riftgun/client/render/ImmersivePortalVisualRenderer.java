package dev.riftgun.client.render;

import dev.riftgun.client.compat.immersiveportal.ImmersivePortalCompat;
import dev.riftgun.portal.PortalEntity;

/** Visual-only Immersive Portals adapter. */
final class ImmersivePortalVisualRenderer implements PortalVisualRenderer {
    private final PortalVisualRenderer fallback = new SwirlPortalVisualRenderer();
    private final ImmersivePortalOverlayRenderer overlay = new ImmersivePortalOverlayRenderer();

    @Override
    public void render(PortalVisualRenderContext context) {
        if (!(context.portal() instanceof PortalEntity) || !ImmersivePortalCompat.isAvailable()) {
            fallback.render(context);
            return;
        }
        boolean proxySynced = ImmersivePortalCompat.render(context);
        // The linked Rift entity is rendered again inside IP's destination world.
        // It has no local proxy and must not paint a loading disc over the IP view.
        if (!proxySynced && ImmersivePortalCompat.isRenderingPortalWorld()) return;
        overlay.render(context, ImmersivePortalCompat.readiness(context.portal().visualId()));
    }
}
