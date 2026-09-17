package dev.riftgun.client.render;

import dev.riftgun.client.particle.ParticleEffect;
import dev.riftgun.client.particle.ParticleEffectContext;
import dev.riftgun.core.particle.ParticleDynamics;
import dev.riftgun.portal.PortalEntity;
import net.minecraft.world.phys.Vec3;

/** The original portal perimeter emission pattern, with fading, shrinking dynamic droplets. */
public final class PortalSplashEffect implements ParticleEffect {
    @Override public void emit(ParticleEffectContext context) {
        var portal = context.portal();
        int count = PortalSplashPattern.particleCount(portal.phase());
        if (count == 0) return;
        float scale = PortalSplashPattern.edgeScale(portal.phase(), portal.phaseTicks());
        var random = context.random();
        double offset = random.nextDouble();
        for (int index = 0; index < count; index++) {
            var point = PortalSplashPattern.sampleEdge(portal.portalWidth(), portal.portalHeight(),
                scale, offset + index / (double) count);
            Vec3 edge = portal.right().scale(point.right()).add(portal.up().scale(point.up()));
            Vec3 position = portal.placement().center().add(edge)
                .add(portal.normal().scale(PortalEntity.DEPTH * (0.6 + random.nextDouble() * 0.25)));
            Vec3 horizontal = new Vec3(edge.x, 0, edge.z);
            if (horizontal.lengthSqr() < 1.0E-6) {
                double angle = random.nextDouble() * Math.PI * 2;
                horizontal = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            } else horizontal = horizontal.normalize();
            double jitter = (random.nextDouble() - 0.5) * 0.7;
            double cosine = Math.cos(jitter);
            double sine = Math.sin(jitter);
            double speed = 0.025 + random.nextDouble() * 0.04;
            Vec3 velocity = new Vec3((horizontal.x * cosine - horizontal.z * sine) * speed,
                0.1, (horizontal.x * sine + horizontal.z * cosine) * speed);
            int life = 12 + random.nextInt(15);
            float size = 0.06F + random.nextFloat() * 0.04F;
            int rgb = context.color() & 0xFFFFFF;
            var dynamics = new ParticleDynamics(life, size, size * 0.4F,
                0xFF000000 | rgb, rgb, 1, 0.98F,
                (random.nextFloat() - 0.5F) * 0.16F, true, false);
            if (!context.particles().spawn(position, velocity, dynamics)) break;
        }
    }
}
