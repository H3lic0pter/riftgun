package dev.riftgun.ui;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerSettings;

/** Pure transitions for settings edited by the portal configuration GUI. */
public final class PortalConfigSettings {
    public static PortalPlayerSettings toggle(PortalPlayerSettings current, Toggle toggle) {
        return new PortalPlayerSettings(
            toggle == Toggle.SAFETY_CHECK ? !current.safetyCheckEnabled() : current.safetyCheckEnabled(),
            toggle == Toggle.CONFIRM_DELETION ? !current.confirmDeletion() : current.confirmDeletion(),
            toggle == Toggle.CONFIRM_DISCARD ? !current.confirmDiscardedChanges()
                : current.confirmDiscardedChanges(),
            toggle == Toggle.CONFIRM_CLEAR_FLUID ? !current.confirmClearFluid()
                : current.confirmClearFluid(),
            toggle == Toggle.ANIMATIONS ? !current.animationsEnabled() : current.animationsEnabled(),
            toggle == Toggle.SOUNDS ? !current.soundsEnabled() : current.soundsEnabled(),
            current.sort(), current.placementMode(), current.smartDistance(), current.predictionMode(),
            current.portalSounds());
    }

    public static PortalPlayerSettings cycleSort(PortalPlayerSettings current) {
        return new PortalPlayerSettings(current.safetyCheckEnabled(), current.confirmDeletion(),
            current.confirmDiscardedChanges(), current.confirmClearFluid(), current.animationsEnabled(),
            current.soundsEnabled(), current.sort().next(), current.placementMode(), current.smartDistance(),
            current.predictionMode(), current.portalSounds());
    }

    public static PortalPlayerSettings cyclePlacementMode(
        PortalPlayerSettings current, boolean entityRelocationEnabled, boolean remoteInstalled
    ) {
        PortalPlacementMode mode = current.placementMode().next();
        while (mode == PortalPlacementMode.ENTITY_RELOCATION && !entityRelocationEnabled
            || mode == PortalPlacementMode.REMOTE && !remoteInstalled) {
            mode = mode.next();
        }
        return current.withPlacementMode(mode);
    }

    public static PortalPlayerSettings cyclePredictionMode(PortalPlayerSettings current) {
        return current.withPredictionMode(current.predictionMode().next());
    }

    public enum Toggle {
        SAFETY_CHECK, CONFIRM_DELETION, CONFIRM_DISCARD, CONFIRM_CLEAR_FLUID, ANIMATIONS, SOUNDS
    }

    private PortalConfigSettings() {}
}
