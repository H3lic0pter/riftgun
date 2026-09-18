package dev.riftgun.client.render;

import dev.riftgun.client.particle.ParticleEffect;
import dev.riftgun.client.particle.ParticleEffectContext;
import dev.riftgun.core.particle.ParticleDynamics;
import dev.riftgun.core.visual.WaterSplashGeometry;
import dev.riftgun.portal.PortalLifecycle;

/** Sparse fuel-colored droplets emitted from the animated water mask's actual rim. */
public final class WaterSplashParticleEffect implements ParticleEffect {
    @Override public void emit(ParticleEffectContext context) {
        var portal = context.portal();
        if (portal.phase() != PortalLifecycle.Phase.OPEN
            || portal.openingTicks() + portal.phaseTicks() < WaterSplashGeometry.OPENING_TICKS
            || Math.floorMod(portal.phaseTicks() + portal.visualId().hashCode(), 16) != 0) return;
        var mesh = WaterSplashGeometry.forPortal(portal.visualId());
        int index = context.random().nextInt(mesh.rimCount());
        float x = mesh.rimX(index);
        float y = mesh.rimY(index);
        float radius = Math.max(portal.portalWidth(), portal.portalHeight()) * 0.5F;
        float wave = mesh.wave(x, y, portal.visualAge(0));
        var edge = portal.right().scale(x * radius * wave).add(portal.up().scale(y * radius * wave));
        int rgb = context.color() & 0xFFFFFF;
        var dynamics = new ParticleDynamics(18 + context.random().nextInt(9), 0.035F, 0.005F,
            0xE0000000 | rgb, rgb, 0.15F, 0.95F, 0.02F, true, true);
        // Emit on both sides so the two-sided surface has matching details from either view.
        for (int face = -1; face <= 1; face += 2) {
            var position = portal.placement().center().add(edge).add(portal.normal().scale(0.025 * face));
            var velocity = edge.normalize().scale(0.012).add(portal.normal().scale(0.008 * face));
            if (!context.particles().spawn(position, velocity, dynamics)) break;
        }
    }
}
