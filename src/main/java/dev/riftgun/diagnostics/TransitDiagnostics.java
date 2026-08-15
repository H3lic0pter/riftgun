package dev.riftgun.diagnostics;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/** Opt-in portal transit diagnostics. Disabled mode avoids probes and diagnostic scans. */
public final class TransitDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = "[RG-TRANSIT-DIAGNOSTICS]";
    private static final int MAXIMUM_PROBES = 256;
    private static final long OVERFLOW_WARNING_INTERVAL_TICKS = 200L;
    private static final Deque<Probe> PROBES = new ArrayDeque<>();
    private static long lastOverflowWarningAt = Long.MIN_VALUE;

    public static boolean enabled() {
        return RiftConfigs.server().diagnostics().transitEnabled();
    }

    public static void portal(String message, Object... arguments) {
        info("portal " + message, arguments);
    }

    public static void relocation(String message, Object... arguments) {
        info("relocation " + message, arguments);
    }

    public static void ticket(String message, Object... arguments) {
        info("ticket " + message, arguments);
    }

    public static void warning(String message, Object... arguments) {
        if (enabled()) LOGGER.warn(PREFIX + " " + message, arguments);
    }

    public static void trackPostcondition(Entity moved, ResourceKey<Level> sourceDimension,
                                          Vec3 expectedPosition, String route, long now) {
        if (!enabled()) return;
        if (PROBES.size() >= MAXIMUM_PROBES) {
            PROBES.removeFirst();
            if (now - lastOverflowWarningAt >= OVERFLOW_WARNING_INTERVAL_TICKS) {
                lastOverflowWarningAt = now;
                LOGGER.warn("{} postcondition probe limit reached; discarded oldest probe", PREFIX);
            }
        }
        PROBES.addLast(new Probe(moved.getUUID(), sourceDimension, moved.level().dimension(),
            expectedPosition, route, now));
    }

    public static void tick(MinecraftServer server) {
        if (!enabled()) {
            reset();
            return;
        }
        if (PROBES.isEmpty()) return;
        long now = server.overworld().getGameTime();
        var iterator = PROBES.iterator();
        while (iterator.hasNext()) {
            Probe probe = iterator.next();
            long age = now - probe.startedAt();
            if (age != 1L && age < 5L) continue;
            ServerLevel destination = server.getLevel(probe.destinationDimension());
            ServerLevel source = server.getLevel(probe.sourceDimension());
            Entity atDestination = destination == null ? null : destination.getEntity(probe.entityId());
            Entity atSource = source == null ? null : source.getEntity(probe.entityId());
            portal("postcondition route={} ageTicks={} entity={} destinationPresent={} sourcePresent={} actualDimension={} actualPos={} expectedPos={} distance={}",
                probe.route(), age, probe.entityId(), atDestination != null,
                atSource != null && atSource != atDestination,
                atDestination == null ? "null" : atDestination.level().dimension().location(),
                atDestination == null ? "null" : atDestination.position(), probe.expectedPosition(),
                atDestination == null ? -1.0 : atDestination.position().distanceTo(probe.expectedPosition()));
            if (age >= 5L) iterator.remove();
        }
    }

    public static void reset() {
        PROBES.clear();
        lastOverflowWarningAt = Long.MIN_VALUE;
    }

    private static void info(String message, Object... arguments) {
        if (enabled()) LOGGER.info(PREFIX + " " + message, arguments);
    }

    private record Probe(UUID entityId, ResourceKey<Level> sourceDimension,
                         ResourceKey<Level> destinationDimension, Vec3 expectedPosition,
                         String route, long startedAt) {}

    private TransitDiagnostics() {}
}
