package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import net.minecraft.resources.ResourceLocation;

public final class PortalVisualPreferences {
    public static PortalVisualType selected() {
        return PortalVisualRegistry.resolve(selectedId());
    }

    public static ResourceLocation selectedId() {
        ResourceLocation parsed = ResourceLocation.tryParse(ClientConfig.VALUES.portalVisualType.get());
        ResourceLocation resolved = parsed == null ? PortalVisualRegistry.DEFAULT_ID
            : PortalVisualSelection.resolve(PortalVisualRegistry.values(), parsed, PortalVisualRegistry.DEFAULT_ID);
        if (parsed == null || !parsed.equals(resolved)) save(resolved);
        return resolved;
    }

    public static void select(ResourceLocation id) {
        ResourceLocation resolved = PortalVisualSelection.resolve(
            PortalVisualRegistry.values(), id, PortalVisualRegistry.DEFAULT_ID);
        save(resolved);
    }

    public static void cycle(int direction) {
        select(PortalVisualSelection.cycle(PortalVisualRegistry.values(), selectedId(), direction,
            PortalVisualRegistry.DEFAULT_ID));
    }

    public static void flush() {
        ClientConfig.SPEC.save();
    }

    private static void save(ResourceLocation id) {
        String value = id.toString();
        if (value.equals(ClientConfig.VALUES.portalVisualType.get())) return;
        ClientConfig.VALUES.portalVisualType.set(value);
        ClientConfig.SPEC.save();
    }

    private PortalVisualPreferences() {}
}
