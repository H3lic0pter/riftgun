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
        ImmersivePortalCompat.render(context);
        overlay.render(context, ImmersivePortalCompat.readiness(context.portal().visualId()));
    }
}
