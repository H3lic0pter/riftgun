package dev.riftgun.portal;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.module.PortalEntityAccessSnapshot;
import dev.riftgun.relocation.EntityRelocationExitImmunity;
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
        return rejectionReason(root) == null;
    }

    @Nullable String rejectionReason(Entity root) {
        if (root instanceof PortalEntity) return "portal_entity";
        if (root.isPassenger()) return "passenger_not_root";
        if (!allowsPassengerTree(RiftConfigs.server().portal().passengerTreeTransitEnabled(),
            !root.getPassengers().isEmpty())) return "passenger_tree_disabled";
        if (!PortalServices.ENTITY_ELIGIBILITY.allowsTree(root, entityAccess::allows)) {
            return "entity_access_denied";
        }
        if (!PortalTriggerShape.intersects(
            placement, root.getBoundingBox(), horizontalTriggerExtend)) return "trigger_shape_miss";
        if (containsExcludedPlayer(root)) return "excluded_player";
        if (exitPortal && EntityRelocationExitImmunity.blocksExit(root)) {
            return "relocation_exit_immunity";
        }
        if (exitPortal && containsTransitProtectedPlayer(root)) return "privacy_protected_player";
        return null;
    }

    static boolean allowsPassengerTree(boolean enabled, boolean hasPassengers) {
        return enabled || !hasPassengers;
    }

    private boolean containsExcludedPlayer(Entity root) {
        return excludedPlayerId != null && root.getSelfAndPassengers().anyMatch(entity ->
            entity instanceof Player player && player.getUUID().equals(excludedPlayerId));
    }

    private boolean containsTransitProtectedPlayer(Entity root) {
        return root.getSelfAndPassengers().anyMatch(entity -> {
            if (!(entity instanceof ServerPlayer player)) return false;
            if (ownerId != null && ownerId.equals(player.getUUID())) return false;
            return !PortalPrivacyService.allowsForeignExitTransit(player, ownerId);
        });
    }
}
