package dev.riftgun.client.render;

import dev.riftgun.portal.PortalEntity;

@FunctionalInterface
public interface PortalVisualStyleProvider {
    PortalVisualStyle resolve(PortalEntity portal);
}
