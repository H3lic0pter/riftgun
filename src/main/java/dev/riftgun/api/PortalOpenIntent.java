package dev.riftgun.api;

/** Describes why a caller requested a portal; it never bypasses Rift Gun policy. */
public enum PortalOpenIntent {
    PLAYER_REQUEST,
    ADMIN_DIAGNOSTIC
}
