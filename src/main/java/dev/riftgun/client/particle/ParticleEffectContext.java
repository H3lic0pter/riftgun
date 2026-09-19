package dev.riftgun.client.particle;

import dev.riftgun.core.particle.ParticleDynamics;
import dev.riftgun.portal.PortalVisualSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record ParticleEffectContext(PortalVisualSource portal, int color, RandomSource random, Sink particles) {
    public interface Sink {
        /** Returns false when the client budget is exhausted or the engine rejects the particle. */
        boolean spawn(Vec3 position, Vec3 velocity, ParticleDynamics dynamics);

        /** Emits the original tinted vanilla splash, preserving its native animation and physics. */
        boolean spawnSplash(Vec3 position, Vec3 velocity, int rgb);
    }
}
