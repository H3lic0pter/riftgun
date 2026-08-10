package dev.riftgun.crisis;

import dev.riftgun.service.DestinationSafetyInspector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Context available only after one crisis has won the roulette. */
public record PortalCrisisContext(
    ServerPlayer player,
    ServerLevel targetLevel,
    Vec3 normalDestination,
    Vec3 normalMomentum,
    float destinationYaw,
    PortalCrisisCapabilitySnapshot capabilities,
    DestinationSafetyInspector safetyInspector,
    boolean relocationAllowed
) {}
