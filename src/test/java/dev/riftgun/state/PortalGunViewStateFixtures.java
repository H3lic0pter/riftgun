package dev.riftgun.state;

import dev.riftgun.module.PlayerExcludeMode;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import java.util.Map;
import java.util.UUID;

public final class PortalGunViewStateFixtures {
    public static PortalGunViewState representative() {
        return new PortalGunViewState(
            UUID.fromString("c3edbeaf-a959-46ba-b669-feb04f32999e"), null,
            new PortalGunViewState.Fuel(false, 4200, 8000, false, false, true,
                "riftgun:test_fluid", 0x123456, true),
            new PortalGunViewState.Navigation(true, true, true, "minecraft:the_nether",
                DimensionalTraversalMode.AUTOMATIC_SEARCH),
            new PortalGunViewState.Placement(96, 48, 32, true, true, true, true,
                true, true, PortalFunctionMode.PORTAL_PAIRING,
                PortalFloatingFallback.FRONT, PortalFloatingFallback.REMOTE),
            new PortalGunViewState.Transit(15, true, true, false, true, 60, 120,
                true, true, 8, 20, true, true, PlayerExcludeMode.EXIT_ONLY,
                true, true, true, true, true, true),
            new PortalGunViewState.Modules(Map.of(PortalModuleKind.REMOTE, 1,
                PortalModuleKind.PORTAL_PAIRING, 1), PortalModuleRules.defaults()));
    }

    private PortalGunViewStateFixtures() {}
}
