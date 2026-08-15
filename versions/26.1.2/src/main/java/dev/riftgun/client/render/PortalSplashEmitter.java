package dev.riftgun.client.render;

import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.RiftGun;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Emits tinted vanilla splash particles once per synchronized portal phase tick. */
public final class PortalSplashEmitter {
    private static final Map<UUID, EmissionStamp> LAST_EMISSIONS = new HashMap<>();
    private static ClientLevel trackedLevel;

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            resetIfLevelChanged(level);
            return;
        }
        resetIfLevelChanged(level);

        Set<UUID> seen = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof PortalVisualSource portal)) continue;
            UUID portalId = portal.visualId();
            seen.add(portalId);
            emitTick(minecraft, level, portal);
        }
        LAST_EMISSIONS.keySet().retainAll(seen);
    }

    private static void emitTick(Minecraft minecraft, ClientLevel level, PortalVisualSource portal) {
        PortalLifecycle.Phase phase;
        int phaseTicks;
        if (portal instanceof PortalEntity interactive) {
            phase = interactive.phase();
            phaseTicks = interactive.phaseTicks();
        } else if (portal instanceof EntityRelocationPortalEntity relocation) {
            phase = relocation.phase();
            phaseTicks = relocation.phaseTicks();
        } else return;
        int count = PortalSplashPattern.particleCount(phase);
        if (count == 0) {
            LAST_EMISSIONS.remove(portal.visualId());
            return;
        }

        EmissionStamp stamp = new EmissionStamp(phase, phaseTicks);
        if (stamp.equals(LAST_EMISSIONS.put(portal.visualId(), stamp))) return;

        PortalVisualStyle style = PortalVisualStyles.resolve(portal);
        float scale = PortalSplashPattern.edgeScale(phase, phaseTicks);
        RandomSource random = level.random;
        double offset = random.nextDouble();
        for (int index = 0; index < count; index++) {
            PortalSplashPattern.EdgePoint point = PortalSplashPattern.sampleEdge(
                portal.portalWidth(), portal.portalHeight(), scale, offset + index / (double) count);
            emitParticle(minecraft, portal, point, style.splashRgb(), random);
        }
    }

    private static void emitParticle(Minecraft minecraft, PortalVisualSource portal,
                                     PortalSplashPattern.EdgePoint point, int color,
                                     RandomSource random) {
        Vec3 edge = portal.right().scale(point.right()).add(portal.up().scale(point.up()));
        Vec3 position = portal.placement().center().add(edge)
            .add(portal.normal().scale(PortalEntity.DEPTH * (0.6 + random.nextDouble() * 0.25)));

        Vec3 horizontal = new Vec3(edge.x, 0.0, edge.z);
        if (horizontal.lengthSqr() < 1.0E-6) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            horizontal = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        } else {
            horizontal = horizontal.normalize();
        }
        double jitter = (random.nextDouble() - 0.5) * 0.7;
        double cosine = Math.cos(jitter);
        double sine = Math.sin(jitter);
        horizontal = new Vec3(
            horizontal.x * cosine - horizontal.z * sine,
            0.0,
            horizontal.x * sine + horizontal.z * cosine
        );
        double speed = 0.025 + random.nextDouble() * 0.04;

        Particle particle = minecraft.particleEngine.createParticle(
            RiftContent.PORTAL_SPLASH.get(),
            position.x, position.y, position.z,
            horizontal.x * speed, 0.0, horizontal.z * speed
        );
        if (particle != null) {
            particle.setColor(red(color), green(color), blue(color));
        }
    }

    private static void resetIfLevelChanged(ClientLevel level) {
        if (trackedLevel == level) return;
        trackedLevel = level;
        LAST_EMISSIONS.clear();
    }

    private static float red(int color) {
        return ((color >> 16) & 255) / 255.0F;
    }

    private static float green(int color) {
        return ((color >> 8) & 255) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 255) / 255.0F;
    }

    private record EmissionStamp(PortalLifecycle.Phase phase, int phaseTicks) {}

    private PortalSplashEmitter() {}
}
