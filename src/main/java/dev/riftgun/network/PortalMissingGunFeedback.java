package dev.riftgun.network;

import dev.riftgun.service.PortalShortcutGunMode;
import org.jetbrains.annotations.Nullable;

/** Pure missing-gun feedback policy shared by every server-side shortcut path. */
public final class PortalMissingGunFeedback {
    public static @Nullable String messageKey(PortalAction action, boolean keyboardShortcut,
                                               PortalShortcutGunMode lookupMode) {
        if (keyboardShortcut || action == PortalAction.OPEN_GUI) {
            return keyboardShortcut && lookupMode == PortalShortcutGunMode.HELD_HANDS
                ? "message.riftgun.portal_gun_must_be_held"
                : "message.riftgun.no_portal_gun";
        }
        return action == PortalAction.CYCLE_PLACEMENT_MODE
            ? null : "message.riftgun.no_portal_gun";
    }

    private PortalMissingGunFeedback() {}
}
