package dev.riftgun.core.fuel;

import java.util.Objects;
import net.minecraft.world.level.material.Fluid;

/** Loader-neutral fluid value used at the portal-gun storage seam. */
public record PortalFluidContent(Fluid fluid, int amount) {
    public PortalFluidContent {
        if (amount < 0) throw new IllegalArgumentException("amount must be non-negative");
        Objects.requireNonNull(fluid, "fluid");
    }
}
