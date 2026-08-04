package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface DestinationSafetyInspector {
    SafetyReport inspect(ServerLevel level, Destination destination);
}

