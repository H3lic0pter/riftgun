package dev.riftgun.client.particle;

import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.client.render.PortalVisualStyles;
import dev.riftgun.client.render.TintableSplashParticle;
import dev.riftgun.core.particle.ParticleDynamics;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.particle.RiftParticles;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalVisualSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;

/** Owns emission deduplication and particle lifetimes; all methods run on the client thread. */
public final class ParticleEffectManager {
    private static final int MAX_SPAWNS_PER_TICK = 256;
    private static final int MAX_LIVE_PARTICLES = 2048;
    private static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;
    private static final Map<UUID, Stamp> LAST_EMISSIONS = new HashMap<>();
    private static final Map<Particle, LiveParticle> LIVE = new HashMap<>();
    private static ClientLevel trackedLevel;
    private static int remaining;
    private static long tick;

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (trackedLevel != level) {
            clear();
            trackedLevel = level;
        }
        if (level == null || minecraft.isPaused()) return;
        tick++;
        // Preserve the original portal emission density at every vanilla particle setting.
        remaining = MAX_SPAWNS_PER_TICK;
        Map<UUID, Owner> seen = new HashMap<>();
        Set<UUID> activeSources = new HashSet<>();
        List<Source> sources = new ArrayList<>();
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof PortalVisualSource portal)) continue;
            if (portal.phase() == PortalLifecycle.Phase.CLOSED) continue;
            activeSources.add(portal.visualId());
            if (entity instanceof PortalEntity interactive && interactive.pairingDormant()) continue;
            if (minecraft.player == null || minecraft.player.position()
                    .distanceToSqr(portal.placement().center()) > MAX_DISTANCE_SQUARED) continue;
            String effectId = PortalVisualRegistry.resolveStored(portal.visualType()).renderer().particleEffectId();
            ParticleEffect effect = ParticleEffectRegistry.resolve(effectId);
            if (effect == null) continue;
            UUID id = portal.visualId();
            Owner owner = new Owner(id, effectId, effect);
            seen.put(id, owner);
            sources.add(new Source(portal, owner));
        }
        LAST_EMISSIONS.keySet().retainAll(seen.keySet());
        LIVE.entrySet().removeIf(entry -> {
            Owner owner = entry.getValue().owner();
            Particle particle = entry.getKey();
            Owner current = seen.get(owner.source());
            // Native splashes outlive a closing portal, as they did before effect management.
            boolean sourceChanged = current == null
                ? !entry.getValue().outlivesSource() || activeSources.contains(owner.source())
                : !owner.equals(current);
            if (sourceChanged
                    || tick >= entry.getValue().expiresAt()
                    || ParticleEffectRegistry.resolve(owner.effectId()) != owner.effect()) particle.remove();
            return !particle.isAlive();
        });
        // Release removed/changed effects before new ones claim the shared live budget.
        for (Source source : sources) {
            var portal = source.portal();
            Owner owner = source.owner();
            Stamp stamp = new Stamp(owner.effectId(), owner.effect(), portal.phase(), portal.phaseTicks());
            if (stamp.equals(LAST_EMISSIONS.put(owner.source(), stamp)) || remaining <= 0) continue;
            owner.effect().emit(new ParticleEffectContext(portal, PortalVisualStyles.resolve(portal).splashRgb(),
                level.getRandom(), new ParticleEffectContext.Sink() {
                    @Override public boolean spawn(Vec3 position, Vec3 velocity, ParticleDynamics dynamics) {
                        return ParticleEffectManager.spawn(minecraft, owner, position, velocity, dynamics);
                    }

                    @Override public boolean spawnSplash(Vec3 position, Vec3 velocity, int rgb) {
                        return ParticleEffectManager.spawnSplash(minecraft, owner, position, velocity, rgb);
                    }
                }));
        }
    }

    private static boolean spawn(Minecraft minecraft, Owner owner, Vec3 position, Vec3 velocity,
                                 ParticleDynamics dynamics) {
        if (remaining <= 0 || LIVE.size() >= MAX_LIVE_PARTICLES) return false;
        if (!finite(position) || !finite(velocity)) throw new IllegalArgumentException("Non-finite particle position or velocity");
        java.util.Objects.requireNonNull(dynamics, "dynamics");
        remaining--;
        var particle = minecraft.particleEngine.createParticle(RiftParticles.DYNAMIC.get(),
            position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        if (!(particle instanceof DynamicParticle dynamic)) return false;
        dynamic.configure(dynamics);
        // The engine may evict a particle without calling remove(). Bound our reference's
        // lifetime independently, allowing two ticks for its pending-add queue.
        LIVE.put(dynamic, new LiveParticle(owner, tick + dynamics.lifetimeTicks() + 2L, false));
        return true;
    }

    private static boolean spawnSplash(Minecraft minecraft, Owner owner, Vec3 position, Vec3 velocity, int rgb) {
        if (remaining <= 0 || LIVE.size() >= MAX_LIVE_PARTICLES) return false;
        if (!finite(position) || !finite(velocity)) throw new IllegalArgumentException("Non-finite particle position or velocity");
        remaining--;
        var particle = minecraft.particleEngine.createParticle(RiftContent.PORTAL_SPLASH.get(),
            position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        if (!(particle instanceof TintableSplashParticle splash)) return false;
        splash.setColor(((rgb >>> 16) & 255) / 255.0F, ((rgb >>> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
        LIVE.put(splash, new LiveParticle(owner, tick + splash.getLifetime() + 2L, true));
        return true;
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    /** Stop particles already emitted by this source; future phase ticks may emit again. */
    public static void stop(UUID source) {
        LAST_EMISSIONS.remove(source);
        LIVE.entrySet().removeIf(entry -> {
            if (!entry.getValue().owner().source().equals(source)) return false;
            entry.getKey().remove();
            return true;
        });
    }

    public static int activeCount() { return LIVE.size(); }

    public static void clear() {
        LIVE.keySet().forEach(Particle::remove);
        LIVE.clear();
        LAST_EMISSIONS.clear();
        remaining = 0;
        tick = 0;
    }

    private record Stamp(String effectId, ParticleEffect effect, PortalLifecycle.Phase phase, int phaseTicks) {}
    private record Owner(UUID source, String effectId, ParticleEffect effect) {}
    private record Source(PortalVisualSource portal, Owner owner) {}
    private record LiveParticle(Owner owner, long expiresAt, boolean outlivesSource) {}

    private ParticleEffectManager() {}
}
