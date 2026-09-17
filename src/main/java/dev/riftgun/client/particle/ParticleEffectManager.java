package dev.riftgun.client.particle;

import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.client.render.PortalVisualStyles;
import dev.riftgun.core.particle.ParticleDynamics;
import dev.riftgun.particle.RiftParticles;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalVisualSource;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Owns emission deduplication and particle lifetimes; all methods run on the client thread. */
public final class ParticleEffectManager {
    private static final int MAX_SPAWNS_PER_TICK = 256;
    private static final int MAX_LIVE_PARTICLES = 2048;
    private static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;
    private static final Map<UUID, Stamp> LAST_EMISSIONS = new HashMap<>();
    private static final Map<DynamicParticle, LiveParticle> LIVE = new HashMap<>();
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
        remaining = switch (minecraft.options.particles().get()) {
            case ALL -> MAX_SPAWNS_PER_TICK;
            case DECREASED -> MAX_SPAWNS_PER_TICK / 2;
            case MINIMAL -> 0;
        };
        Map<UUID, Owner> seen = new HashMap<>();
        List<Source> sources = new ArrayList<>();
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof PortalVisualSource portal)) continue;
            if (entity instanceof PortalEntity interactive && interactive.pairingDormant()) continue;
            if (portal.phase() == PortalLifecycle.Phase.CLOSED) continue;
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
            DynamicParticle particle = entry.getKey();
            if (!owner.equals(seen.get(owner.source()))
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
                level.getRandom(), (position, velocity, dynamics) ->
                    spawn(minecraft, owner, position, velocity, dynamics)));
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
        LIVE.put(dynamic, new LiveParticle(owner, tick + dynamics.lifetimeTicks() + 2L));
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
        LIVE.keySet().forEach(DynamicParticle::remove);
        LIVE.clear();
        LAST_EMISSIONS.clear();
        remaining = 0;
        tick = 0;
    }

    private record Stamp(String effectId, ParticleEffect effect, PortalLifecycle.Phase phase, int phaseTicks) {}
    private record Owner(UUID source, String effectId, ParticleEffect effect) {}
    private record Source(PortalVisualSource portal, Owner owner) {}
    private record LiveParticle(Owner owner, long expiresAt) {}

    private ParticleEffectManager() {}
}
