package dev.riftgun.crisis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Public registration seam; registrations are immutable once a server starts. */
public final class PortalCrisisRegistry {
    private static final Map<ResourceLocation, PortalCrisis> CRISES = new LinkedHashMap<>();
    private static boolean frozen;

    static {
        BuiltinPortalCrises.registerAll();
    }

    public static synchronized void register(PortalCrisis crisis) {
        Objects.requireNonNull(crisis, "crisis");
        if (frozen) throw new IllegalStateException("portal crisis registry is frozen");
        if (crisis.defaultWeight() < 0 || crisis.defaultWeight() > PortalCrisisEngine.TOTAL_WEIGHT) {
            throw new IllegalArgumentException("invalid default crisis weight for " + crisis.id());
        }
        if (CRISES.putIfAbsent(crisis.id(), crisis) != null) {
            throw new IllegalArgumentException("duplicate portal crisis: " + crisis.id());
        }
    }

    public static synchronized void freeze() {
        int defaultTotal = CRISES.values().stream().mapToInt(PortalCrisis::defaultWeight).sum();
        if (defaultTotal > PortalCrisisEngine.TOTAL_WEIGHT) {
            throw new IllegalStateException("default portal crisis weights exceed 1000");
        }
        frozen = true;
    }

    public static synchronized List<PortalCrisis> definitions() {
        return List.copyOf(new ArrayList<>(CRISES.values()));
    }

    public static synchronized Map<ResourceLocation, Integer> defaultWeights() {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        CRISES.values().forEach(crisis -> result.put(crisis.id(), crisis.defaultWeight()));
        return result;
    }

    public static synchronized PortalCrisis find(ResourceLocation id) {
        return CRISES.get(id);
    }

    private PortalCrisisRegistry() {}
}
