package dev.riftgun.portal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Selects the independently configured player or non-player living-entity fall guard. */
public final class PortalFallGuardPolicy {
    public static boolean applies(Entity entity, boolean playerFallGuard,
                                  boolean entityFallGuard) {
        return applies(entity instanceof ServerPlayer, entity instanceof LivingEntity,
            playerFallGuard, entityFallGuard);
    }

    static boolean applies(boolean player, boolean living, boolean playerFallGuard,
                           boolean entityFallGuard) {
        return player ? playerFallGuard : living && entityFallGuard;
    }

    private PortalFallGuardPolicy() {}
}
