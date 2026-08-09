package dev.riftgun.portal;

import dev.riftgun.module.PortalEntityAccessSnapshot;
import dev.riftgun.service.PortalPrivacyService;
import dev.riftgun.service.PortalServices;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Immutable trigger and privacy policy evaluated before reserving an entity for transit. */
record PortalTransitEligibility(
    PortalPlacement placement,
    PortalEntityAccessSnapshot entityAccess,
    @Nullable UUID ownerId,
    @Nullable UUID excludedPlayerId,
    boolean exitPortal,
    double horizontalTriggerExtend
) {
    PortalTransitEligibility {
        if (entityAccess == null) entityAccess = PortalEntityAccessSnapshot.NONE;
        horizontalTriggerExtend = Math.max(0.0, horizontalTriggerExtend);
    }

    boolean allows(Entity root) {
        if (root instanceof PortalEntity || root.isPassenger()) return false;
        if (!PortalServices.ENTITY_ELIGIBILITY.allowsTree(root, entityAccess::allows)) return false;
        if (!PortalTriggerShape.intersects(
            placement, root.getBoundingBox(), horizontalTriggerExtend)) return false;
        if (containsExcludedPlayer(root)) return false;
        return !exitPortal || !containsTransitProtectedPlayer(root);
    }

    private boolean containsExcludedPlayer(Entity root) {
        return excludedPlayerId != null && root.getSelfAndPassengers().anyMatch(entity ->
            entity instanceof Player player && player.getUUID().equals(excludedPlayerId));
    }

    private boolean containsTransitProtectedPlayer(Entity root) {
        return root.getSelfAndPassengers().anyMatch(entity -> {
            if (!(entity instanceof ServerPlayer player)) return false;
            if (ownerId != null && ownerId.equals(player.getUUID())) return false;
            return PortalPrivacyService.transitProtectsTarget(player);
        });
    }
}
