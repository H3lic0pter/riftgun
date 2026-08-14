package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import dev.riftgun.core.config.RiftConfigs;
import net.minecraft.resources.ResourceLocation;

public final class PortalVisualPreferences {
    public static PortalVisualType selected() {
        return PortalVisualRegistry.resolve(selectedId());
    }

    public static ResourceLocation selectedId() {
        ResourceLocation parsed = ResourceLocation.tryParse(RiftConfigs.client().portalVisualType());
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
        if (value.equals(RiftConfigs.client().portalVisualType())) return;
        ClientConfig.VALUES.portalVisualType.set(value);
        ClientConfig.publishSnapshot();
        ClientConfig.SPEC.save();
    }

    private PortalVisualPreferences() {}
}
