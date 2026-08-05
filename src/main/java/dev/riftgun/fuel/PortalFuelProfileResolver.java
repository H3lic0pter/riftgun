package dev.riftgun.fuel;

import java.util.Optional;
import net.minecraft.world.level.material.Fluid;

@FunctionalInterface
public interface PortalFuelProfileResolver {
    Optional<PortalFuelProfile> resolve(Fluid fluid);
}
