package dev.riftgun.service;

/** Decides whether an exit can be created now without preloading a remote chunk. */
public enum PortalOpenRoute {
    IMMEDIATE_PAIR,
    DEFERRED_EXIT;

    public static PortalOpenRoute decide(boolean crossDimension, boolean targetTicksEntities,
                                         boolean deferUnloadedSameDimension) {
        return !targetTicksEntities && (crossDimension || deferUnloadedSameDimension)
            ? DEFERRED_EXIT : IMMEDIATE_PAIR;
    }
}
