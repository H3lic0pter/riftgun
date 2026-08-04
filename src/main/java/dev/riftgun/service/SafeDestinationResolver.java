package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface SafeDestinationResolver {
    Destination resolve(ServerLevel level, Destination requested, SafetyReport report);

    SafeDestinationResolver IDENTITY = (level, requested, report) -> requested;
}

