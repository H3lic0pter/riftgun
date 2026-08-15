package dev.riftgun.crisis;

import java.util.Optional;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Extension seam for one independently registered unstable-transit crisis. */
public interface PortalCrisis {
//? if >=1.21.11 {
    /*Identifier id();
*///?} else {
    ResourceLocation id();
//?}

    int defaultWeight();

    boolean eligible(PortalCrisisCapabilitySnapshot capabilities);

    Optional<PortalCrisisPlan> prepare(PortalCrisisContext context);

    /** Structural constraint retained when operator testing bypasses normal survival eligibility. */
    default boolean supportsForcedMountedTransit() {
        return true;
    }

    /** Whether this crisis needs one of the portal pair's bounded crisis-exit slots. */
    default boolean requiresRelocation() {
        return false;
    }
}
