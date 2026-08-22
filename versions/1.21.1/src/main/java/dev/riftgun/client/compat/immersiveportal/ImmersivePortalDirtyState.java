package dev.riftgun.client.compat.immersiveportal;

import java.util.Objects;

/** Tracks whether an immutable visual snapshot differs from the applied state. */
final class ImmersivePortalDirtyState<T> {
    private T applied;

    boolean update(T next) {
        if (Objects.equals(applied, next)) return false;
        applied = next;
        return true;
    }
}
