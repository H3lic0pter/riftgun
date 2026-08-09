package dev.riftgun.portal;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Side-specific player exclusions for a portal pair. */
public record PortalExclusions(@Nullable UUID entryPlayerId, @Nullable UUID exitPlayerId) {
    public static final PortalExclusions NONE = new PortalExclusions(null, null);
}
