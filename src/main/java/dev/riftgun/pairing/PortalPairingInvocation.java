package dev.riftgun.pairing;

/** Determines whether a pairing operation follows the gun's current function mode. */
enum PortalPairingInvocation {
    MODE_BOUND,
    SHORTCUT;

    boolean allows(PortalFunctionMode mode) {
        return this == SHORTCUT || mode == PortalFunctionMode.PORTAL_PAIRING;
    }
}
