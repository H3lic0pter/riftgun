package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import net.minecraft.resources.Identifier;

public final class PortalVisualPreferences {
    public static PortalVisualType selected() {
        return PortalVisualRegistry.resolve(selectedId());
    }

    public static Identifier selectedId() {
        Identifier parsed = Identifier.tryParse(dev.riftgun.client.appearance.SkinRecommendations.current().visual());
        if (parsed != null && PortalVisualRegistry.values().stream().anyMatch(type -> type.id().equals(parsed))) return parsed;
        return PortalVisualRegistry.DEFAULT_ID;
    }

    public static void select(Identifier id) {
        Identifier resolved = PortalVisualSelection.resolve(
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
