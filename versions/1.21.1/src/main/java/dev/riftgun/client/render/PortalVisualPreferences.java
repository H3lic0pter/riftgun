package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import net.minecraft.resources.ResourceLocation;

public final class PortalVisualPreferences {
    public static PortalVisualType selected() {
        return PortalVisualRegistry.resolve(selectedId());
    }

    public static ResourceLocation selectedId() {
        ResourceLocation configured = ResourceLocation.tryParse(
            dev.riftgun.client.appearance.SkinRecommendations.current().visual());
        return PortalVisualRegistry.contains(configured) ? configured : PortalVisualRegistry.DEFAULT_ID;
    }

    public static void select(ResourceLocation id) {
        ResourceLocation resolved = PortalVisualSelection.resolve(
            PortalVisualRegistry.values(), id, PortalVisualRegistry.DEFAULT_ID);
        dev.riftgun.client.appearance.SkinRecommendations.selectVisual(resolved.toString());
    }

    public static void cycle(int direction) {
        select(PortalVisualSelection.cycle(PortalVisualRegistry.values(), selectedId(), direction,
            PortalVisualRegistry.DEFAULT_ID));
    }

    public static void flush() {
        ClientConfig.SPEC.save();
    }

    private PortalVisualPreferences() {}
}
