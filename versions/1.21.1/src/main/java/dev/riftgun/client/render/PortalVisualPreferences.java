package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import dev.riftgun.client.compat.immersiveportal.ImmersivePortalCompat;
import dev.riftgun.core.config.RiftConfigs;
import net.minecraft.resources.ResourceLocation;

public final class PortalVisualPreferences {
    public static PortalVisualType selected() {
        return PortalVisualRegistry.resolve(selectedId());
    }

    public static ResourceLocation selectedId() {
        ResourceLocation configured = configuredId();
        return PortalVisualRegistry.contains(configured) ? configured : PortalVisualRegistry.DEFAULT_ID;
    }

    public static ResourceLocation configuredId() {
        ResourceLocation parsed = ResourceLocation.tryParse(RiftConfigs.client().portalVisualType());
        if (parsed != null && PortalVisualRegistry.registered(parsed)) return parsed;
        ResourceLocation custom = ResourceLocation.tryParse(ClientConfig.VALUES.portalVisualType.get());
        return custom != null && PortalVisualRegistry.registered(custom) ? custom : PortalVisualRegistry.DEFAULT_ID;
    }

    public static void select(ResourceLocation id) {
        ResourceLocation resolved = PortalVisualSelection.resolve(
            PortalVisualRegistry.values(), id, PortalVisualRegistry.DEFAULT_ID);
        ClientConfig.VALUES.skinRecommendations.useCustom(
            dev.riftgun.config.SkinRecommendationConfig.Category.PORTAL_VISUAL);
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
        ClientConfig.VALUES.portalVisualType.set(value);
        ClientConfig.publishSnapshot();
        ClientConfig.SPEC.save();
        ImmersivePortalCompat.sendSelection();
    }

    private PortalVisualPreferences() {}
    public static void notifySelectionChanged() {
        ImmersivePortalCompat.sendSelection();
    }
}
