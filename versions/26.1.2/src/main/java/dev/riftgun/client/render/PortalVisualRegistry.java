package dev.riftgun.client.render;

import dev.riftgun.RiftGun;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class PortalVisualRegistry {
    public static final Identifier CLASSIC_ID = id("classic");
    public static final Identifier SWIRL_ID = id("swirl");
    public static final Identifier ENDFRAME_ID = id("endframe");
    public static final Identifier MAGIC_CIRCLE_ID = id("magic_circle");
    public static final Identifier DEFAULT_ID = SWIRL_ID;
    private static final Map<Identifier, PortalVisualType> TYPES = new LinkedHashMap<>();
    private static final Map<String, Identifier> STORED_IDS = new LinkedHashMap<>();

    static {
        register(new PortalVisualType(CLASSIC_ID, "screen.riftgun.visual.classic",
            "screen.riftgun.visual.classic_description", new ClassicPortalVisualRenderer()));
        register(new PortalVisualType(SWIRL_ID, "screen.riftgun.visual.swirl",
            "screen.riftgun.visual.swirl_description", new SwirlPortalVisualRenderer(),
            SwirlVisualOptions.DESCRIPTOR));
        register(new PortalVisualType(ENDFRAME_ID, "screen.riftgun.visual.endframe",
            "screen.riftgun.visual.endframe_description", new EndframePortalVisualRenderer(),
            EndframeVisualOptions.DESCRIPTOR));
        register(new PortalVisualType(MAGIC_CIRCLE_ID, "screen.riftgun.visual.magic_circle",
            "screen.riftgun.visual.magic_circle_description", new MagicCirclePortalVisualRenderer(),
            MagicCircleVisualOptions.DESCRIPTOR));
    }

    public static PortalVisualType register(PortalVisualType type) {
        Objects.requireNonNull(type);
        if (TYPES.putIfAbsent(type.id(), type) != null) {
            throw new IllegalArgumentException("Duplicate portal visual type: " + type.id());
        }
        STORED_IDS.put(type.id().toString(), type.id());
        return type;
    }

    /** Stored IDs need no parsing or allocation on the render path. Unknown values keep a local fallback. */
    public static PortalVisualType resolveStored(String id) {
        return resolve(STORED_IDS.getOrDefault(id, DEFAULT_ID));
    }

    public static PortalVisualType resolve(Identifier id) {
        return TYPES.getOrDefault(id, TYPES.get(DEFAULT_ID));
    }

    public static boolean contains(Identifier id) {
        return TYPES.containsKey(id);
    }

    public static List<PortalVisualType> values() {
        return List.copyOf(TYPES.values());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, path);
    }

    private PortalVisualRegistry() {}
}
