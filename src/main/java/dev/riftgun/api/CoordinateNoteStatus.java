package dev.riftgun.api;

/** Stable outcomes for coordinate-note creation requests. */
public enum CoordinateNoteStatus {
    CREATED,
    API_NOT_READY,
    WRONG_THREAD,
    SHARING_DISABLED,
    TARGET_DIMENSION_UNAVAILABLE,
    PAPER_REQUIRED,
    INVENTORY_FULL,
    INVALID_REQUEST
}
