package dev.riftgun.client;

import dev.riftgun.module.PlayerExcludeMode;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.state.PortalGunViewState;
import java.util.Map;
import java.util.UUID;

/** Typed fixture used by the opt-in in-game GUI capture harness. */
public final class GuiCapturePortalGunState {
    public static PortalGunViewState create() {
        return new PortalGunViewState(UUID.fromString("8b07d37a-5073-49ec-9611-57646619ed32"), null,
            new PortalGunViewState.Fuel(false, 15_000, 24_000, false, false, false,
                "", 0, false),
            new PortalGunViewState.Navigation(true, false, false, "minecraft:overworld",
                DimensionalTraversalMode.EXACT_COORDINATES),
            new PortalGunViewState.Placement(80, 64, 12, true, true, true, true,
                false, true, PortalFunctionMode.PORTAL_PAIRING,
                PortalFloatingFallback.FRONT, PortalFloatingFallback.REMOTE),
            new PortalGunViewState.Transit(0, true, false, true, true, 8, 15,
                false, true, 0, 1, true, true, PlayerExcludeMode.ENTRY_AND_EXIT,
                true, true, false, false, true, false),
            new PortalGunViewState.Modules(Map.ofEntries(
                Map.entry(PortalModuleKind.COORDINATE_OVERRIDE, 1),
                Map.entry(PortalModuleKind.RESERVOIR_EXPANSION, 2),
                Map.entry(PortalModuleKind.PASSIVE_TRANSIT, 1),
                Map.entry(PortalModuleKind.HOSTILE_TRANSIT, 1),
                Map.entry(PortalModuleKind.BOSS_TRANSIT, 1),
                Map.entry(PortalModuleKind.PROJECTILE_TRANSIT, 1),
                Map.entry(PortalModuleKind.SURFACE_RANGE, 3),
                Map.entry(PortalModuleKind.APERTURE_EXPANSION, 1),
                Map.entry(PortalModuleKind.PLAYER_TARGET, 1),
                Map.entry(PortalModuleKind.PORTAL_PAIRING, 1),
                Map.entry(PortalModuleKind.REMOTE, 1)), PortalModuleRules.defaults()));
    }

    private GuiCapturePortalGunState() {}
}
