package dev.riftgun.client.render;

import dev.riftgun.RiftGun;
import dev.riftgun.client.compat.immersiveportal.ImmersivePortalCompat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class PortalVisualRegistry {
    public static final ResourceLocation CLASSIC_ID = id("classic");
    public static final ResourceLocation SWIRL_ID = id("swirl");
    public static final ResourceLocation ENDFRAME_ID = id("endframe");
    public static final ResourceLocation IMMERSIVE_PORTAL_ID = id("immersive_portal");
    public static final ResourceLocation DEFAULT_ID = SWIRL_ID;
    private static final Map<ResourceLocation, PortalVisualType> TYPES = new LinkedHashMap<>();

    static {
        register(new PortalVisualType(CLASSIC_ID, "screen.riftgun.visual.classic",
            "screen.riftgun.visual.classic_description", new ClassicPortalVisualRenderer()));
        register(new PortalVisualType(SWIRL_ID, "screen.riftgun.visual.swirl",
            "screen.riftgun.visual.swirl_description", new SwirlPortalVisualRenderer(),
            SwirlVisualOptions.DESCRIPTOR));
        register(new PortalVisualType(ENDFRAME_ID, "screen.riftgun.visual.endframe",
            "screen.riftgun.visual.endframe_description", new EndframePortalVisualRenderer(),
            EndframeVisualOptions.DESCRIPTOR));
        if (ImmersivePortalCompat.isLoaded()) {
            register(new PortalVisualType(IMMERSIVE_PORTAL_ID,
                "screen.riftgun.visual.immersive_portal",
                "screen.riftgun.visual.immersive_portal_description",
                new ImmersivePortalVisualRenderer()));
        }
    }

    public static PortalVisualType register(PortalVisualType type) {
        Objects.requireNonNull(type);
        if (TYPES.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("Duplicate portal visual type: " + type.id());
        }
        return type;
    }

    public static PortalVisualType resolve(ResourceLocation id) {
        if (IMMERSIVE_PORTAL_ID.equals(id) && !ImmersivePortalCompat.isAvailable()) {
            return TYPES.get(DEFAULT_ID);
        }
        return TYPES.getOrDefault(id, TYPES.get(DEFAULT_ID));
    }

    public static boolean contains(ResourceLocation id) {
        return TYPES.containsKey(id)
            && (!IMMERSIVE_PORTAL_ID.equals(id) || ImmersivePortalCompat.isAvailable());
    }

    public static List<PortalVisualType> values() {
        return TYPES.values().stream()
            .filter(type -> !IMMERSIVE_PORTAL_ID.equals(type.id())
                || ImmersivePortalCompat.isAvailable())
            .toList();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, path);
    }

    private PortalVisualRegistry() {}
}
