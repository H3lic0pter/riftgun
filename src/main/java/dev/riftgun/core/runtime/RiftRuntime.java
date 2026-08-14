package dev.riftgun.core.runtime;

import dev.riftgun.service.AvailableDimensionPolicy;
import dev.riftgun.service.DefaultPortalEntityEligibilityPolicy;
import dev.riftgun.service.DestinationDimensionPolicy;
import dev.riftgun.service.DestinationSafetyInspector;
import dev.riftgun.service.FixedOpenDurationClosePolicy;
import dev.riftgun.service.PortalClosePolicy;
import dev.riftgun.service.PortalEntityEligibilityPolicy;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalMotionHistory;
import dev.riftgun.service.PortalMotionPredictor;
import dev.riftgun.service.PortalPlacementCapabilities;
import dev.riftgun.service.PortalPlacementResolver;
import dev.riftgun.service.SafeDestinationResolver;
import dev.riftgun.service.ServerPortalMotionHistory;
import dev.riftgun.service.VanillaDestinationSafetyInspector;
import dev.riftgun.service.VanillaInventoryPortalGunLocator;
import dev.riftgun.service.VanillaPortalMotionPredictor;
import dev.riftgun.service.VanillaPortalPlacementResolver;
import java.util.Objects;

/** Immutable, once-installed graph of common gameplay services. */
public record RiftRuntime(
    DestinationDimensionPolicy dimensionPolicy,
    DestinationSafetyInspector safetyInspector,
    SafeDestinationResolver safeDestinationResolver,
    PortalEntityEligibilityPolicy entityEligibility,
    PortalClosePolicy closePolicy,
    PortalPlacementCapabilities placementCapabilities,
    PortalMotionHistory motionHistory,
    PortalMotionPredictor motionPredictor,
    PortalPlacementResolver placementResolver
) {
    private static final OnceInstalled<RiftRuntime> CURRENT =
        new OnceInstalled<>("RiftRuntime");

    public RiftRuntime {
        Objects.requireNonNull(dimensionPolicy, "dimensionPolicy");
        Objects.requireNonNull(safetyInspector, "safetyInspector");
        Objects.requireNonNull(safeDestinationResolver, "safeDestinationResolver");
        Objects.requireNonNull(entityEligibility, "entityEligibility");
        Objects.requireNonNull(closePolicy, "closePolicy");
        Objects.requireNonNull(placementCapabilities, "placementCapabilities");
        Objects.requireNonNull(motionHistory, "motionHistory");
        Objects.requireNonNull(motionPredictor, "motionPredictor");
        Objects.requireNonNull(placementResolver, "placementResolver");
    }

    public static RiftRuntime current() {
        return CURRENT.current();
    }

    public static void install(RiftRuntime runtime) {
        CURRENT.install(runtime);
    }

    public static void bootstrapDefaults() {
        install(defaults());
        if (PortalGunLocator.LOCATORS.isEmpty()) {
            PortalGunLocator.register(new VanillaInventoryPortalGunLocator());
        }
    }

    public static RiftRuntime defaults() {
        return new RiftRuntime(
            new AvailableDimensionPolicy(),
            new VanillaDestinationSafetyInspector(),
            SafeDestinationResolver.IDENTITY,
            new DefaultPortalEntityEligibilityPolicy(),
            new FixedOpenDurationClosePolicy(),
            PortalPlacementCapabilities.DEFAULT,
            new ServerPortalMotionHistory(),
            new VanillaPortalMotionPredictor(),
            new VanillaPortalPlacementResolver());
    }
}
