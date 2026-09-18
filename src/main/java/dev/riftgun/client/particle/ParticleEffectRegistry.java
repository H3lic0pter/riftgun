package dev.riftgun.client.particle;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.client.render.PortalSplashEffect;
import dev.riftgun.client.render.WaterSplashParticleEffect;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/** Client-thread registry. Effect IDs are independent of Minecraft particle type IDs. */
public final class ParticleEffectRegistry {
    public static final String NONE = "riftgun:none";
    public static final String PORTAL_SPLASH = "riftgun:portal_splash";
    public static final String WATER_SPLASH = "riftgun:water_splash";
    private static final Map<String, ParticleEffect> EFFECTS = new LinkedHashMap<>();
    private static final Set<String> DISABLED = new HashSet<>();

    static {
        register(PORTAL_SPLASH, new PortalSplashEffect());
        register(WATER_SPLASH, new WaterSplashParticleEffect());
    }

    public static void register(String id, ParticleEffect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (!PortalGunSkin.validId(id) || NONE.equals(id)) {
            throw new IllegalArgumentException("Invalid or reserved particle effect ID: " + id);
        }
        if (EFFECTS.putIfAbsent(id, effect) != null) {
            throw new IllegalArgumentException("Duplicate particle effect ID: " + id);
        }
    }

    public static void unregister(String id) {
        EFFECTS.remove(id);
        DISABLED.remove(id);
    }

    public static void setEnabled(String id, boolean enabled) {
        if (!EFFECTS.containsKey(id)) throw new IllegalArgumentException("Unknown particle effect: " + id);
        if (enabled) DISABLED.remove(id); else DISABLED.add(id);
    }

    /** Unknown, disabled, or NONE effects resolve to null and emit nothing. */
    public static ParticleEffect resolve(String id) {
        return DISABLED.contains(id) ? null : EFFECTS.get(id);
    }

    public static Map<String, ParticleEffect> entries() {
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(EFFECTS));
    }

    private ParticleEffectRegistry() {}
}
