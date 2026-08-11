package dev.riftgun.portal;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.crisis.ForcedCrisisPreparation;
import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.crisis.PortalCrisisCoordinator;
import dev.riftgun.crisis.PortalCrisisPlan;
import dev.riftgun.crisis.PortalCrisisTestOverrides;
import dev.riftgun.sound.PortalSounds;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Owns crisis selection, relocation exits, commit/abort and crisis-return transit. */
final class PortalCrisisController {
    private final PortalEntity portal;
    private final PortalCrisisSession session = new PortalCrisisSession();

    PortalCrisisController(PortalEntity portal) {
        this.portal = portal;
    }

    void configure(PortalCrisisConfigurationSnapshot configuration) {
        session.configure(configuration);
    }

    PortalCrisisConfigurationSnapshot configuration() {
        return session.configuration();
    }

    void copyPairStateTo(PortalCrisisController target) {
        session.copyPairStateTo(target.session, maximumTrackedPlayers());
    }

    boolean isReturnExit() {
        return session.isReturnExit();
    }

    boolean allowsReturn(Entity entity) {
        return session.allowsReturn(entity.getUUID(), !entity.getPassengers().isEmpty());
    }

    boolean returnLinkValid() {
        if (!(portal.level() instanceof ServerLevel level)) return false;
        PortalEntity parent = session.findParent(level.getServer());
        return session.returnLevel(level.getServer()) != null && parent != null
            && parent.phase() != PortalLifecycle.Phase.CLOSING
            && parent.phase() != PortalLifecycle.Phase.CLOSED;
    }

    @Nullable Prepared prepare(ServerPlayer player, ServerLevel targetLevel,
                               Vec3 normalDestination, Vec3 normalMomentum,
                               float destinationYaw, boolean mountedTransit) {
        var forced = PortalCrisisTestOverrides.forced(player.getUUID());
        if (forced.isPresent()) {
            return prepareForced(forced.get(), player, targetLevel, normalDestination,
                normalMomentum, destinationYaw, mountedTransit);
        }
        if (!reserveRoll(player)) return null;
        var selected = PortalCrisisCoordinator.prepare(configuration(), player, targetLevel,
            normalDestination, normalMomentum, destinationYaw, mountedTransit, canCreateExit());
        if (selected.isEmpty()) return null;
        PortalCrisisPlan plan = selected.get();
        PortalEntity crisisExit = plan.relocation() == null
            ? null : createExit(player, targetLevel, plan.relocation());
        return plan.relocation() == null || crisisExit != null
            ? new Prepared(plan, crisisExit, null) : null;
    }

    void transitReturn(ServerPlayer player) {
        PortalExitTarget target = session.returnTarget();
        ServerLevel targetLevel = returnLevel();
        if (target == null || targetLevel == null) {
            portal.transit().leave(player.getUUID());
            return;
        }
        ServerLevel sourceLevel = (ServerLevel) portal.level();
        Vec3 sourcePosition = player.position();
        Entity moved = PortalTransitService.complete(player, targetLevel,
            new PortalTransitService.TransitPlan(
                target.position(), Vec3.ZERO, target.yaw(), player.getXRot()), false);
        if (!(moved instanceof ServerPlayer movedPlayer)) {
            portal.transit().leave(player.getUUID());
            return;
        }

        long now = portal.serverTime();
        portal.transit().markInside(movedPlayer.getUUID(), now);
        PortalEntity parent = parentPortal();
        if (parent != null) {
            parent.transit().markInside(movedPlayer.getUUID(), now);
            PortalEntity normalExit = parent.linkedPortal();
            if (normalExit != null) normalExit.transit().markInside(movedPlayer.getUUID(), now);
        }
        PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
        PortalSounds.playTransit(targetLevel, movedPlayer.position(), portal.soundSnapshot());
    }

    void load(CompoundTag tag) {
        session.load(tag, maximumTrackedPlayers());
    }

    void save(CompoundTag tag) {
        session.save(tag);
    }

    private @Nullable Prepared prepareForced(ResourceLocation crisisId, ServerPlayer player,
                                             ServerLevel targetLevel, Vec3 normalDestination,
                                             Vec3 normalMomentum, float destinationYaw,
                                             boolean mountedTransit) {
        ForcedCrisisPreparation preparation = PortalCrisisCoordinator.prepareForced(crisisId,
            player, targetLevel, normalDestination, normalMomentum, destinationYaw,
            mountedTransit, canCreateExit());
        if (preparation.plan().isEmpty()) {
            warnForcedFailure(player, crisisId, preparation.failure());
            return null;
        }
        PortalCrisisPlan plan = preparation.plan().get();
        PortalEntity crisisExit = plan.relocation() == null
            ? null : createExit(player, targetLevel, plan.relocation());
        if (plan.relocation() != null && crisisExit == null) {
            warnForcedFailure(player, crisisId,
                ForcedCrisisPreparation.Failure.DESTINATION_UNAVAILABLE);
            return null;
        }
        return new Prepared(plan, crisisExit, crisisId);
    }

    private void warnForcedFailure(ServerPlayer player, ResourceLocation crisisId,
                                   ForcedCrisisPreparation.Failure failure) {
        String reasonKey = switch (failure) {
            case SPECTATOR -> "message.riftgun.crisis_test.failure.spectator";
            case MOUNTED_TRANSIT -> "message.riftgun.crisis_test.failure.mounted";
            case CRISIS_EXIT_LIMIT -> "message.riftgun.crisis_test.failure.exit_limit";
            case DESTINATION_UNAVAILABLE -> "message.riftgun.crisis_test.failure.destination";
            case UNKNOWN_CRISIS -> "message.riftgun.crisis_test.failure.unknown";
            case INTERNAL_ERROR -> "message.riftgun.crisis_test.failure.internal";
            case NONE -> throw new IllegalArgumentException("NONE is not a forced crisis failure");
        };
        Component crisisName = Component.translatable(
            "crisis." + crisisId.getNamespace() + "." + crisisId.getPath());
        player.sendSystemMessage(Component.translatable(reasonKey, crisisName)
            .withStyle(ChatFormatting.RED));
    }

    private boolean reserveRoll(ServerPlayer player) {
        if (player.isSpectator()) return false;
        PortalEntity linked = portal.linkedPortal();
        return session.reserve(player.getUUID(),
            linked == null ? null : linked.crisis().session, maximumTrackedPlayers());
    }

    private boolean canCreateExit() {
        PortalEntity linked = portal.linkedPortal();
        return session.canCreateExit(linked == null ? null : linked.crisis().session,
            ServerConfig.VALUES.maximumCrisisExits.get());
    }

    private @Nullable PortalEntity createExit(ServerPlayer player, ServerLevel targetLevel,
                                               PortalCrisisPlan.Relocation relocation) {
        PortalEntity crisisExit = PortalEntity.create(targetLevel, portal.ownerId(),
            relocation.exitPlacement(), portal.fuelRgb(), portal.fuelId(), portal.runtimeOptions(),
            portal.lifecycleStartedAt(), null, true);
        crisisExit.crisis().session.configureReturn(player.getUUID(),
            new PortalExitTarget(UUID.randomUUID(), portal.level().dimension(),
                portal.outputPosition(player), portal.getYRot()),
            portal.getUUID(), portal.level().dimension());
        crisisExit.closeStartedAt(portal.closeStartedAt());
        crisisExit.acquireChunkTicket();
        if (!targetLevel.addFreshEntity(crisisExit)) {
            crisisExit.releaseChunkTicket();
            return null;
        }
        return crisisExit;
    }

    private void commitExit(PortalEntity crisisExit) {
        PortalEntity linked = portal.linkedPortal();
        session.commitExit(linked == null ? null : linked.crisis().session);
        if (crisisExit.level() instanceof ServerLevel level) {
            PortalSounds.playOpening(level, crisisExit.placement().center(), crisisExit.soundSnapshot());
        }
    }

    private @Nullable ServerLevel returnLevel() {
        return portal.level() instanceof ServerLevel level
            ? session.returnLevel(level.getServer()) : null;
    }

    private @Nullable PortalEntity parentPortal() {
        return portal.level() instanceof ServerLevel level
            ? session.findParent(level.getServer()) : null;
    }

    private static int maximumTrackedPlayers() {
        return ServerConfig.VALUES.maximumTrackedCrisisPlayers.get();
    }

    final class Prepared {
        private final PortalCrisisPlan plan;
        private final @Nullable PortalEntity crisisExit;
        private final @Nullable ResourceLocation forcedCrisisId;

        private Prepared(PortalCrisisPlan plan, @Nullable PortalEntity crisisExit,
                         @Nullable ResourceLocation forcedCrisisId) {
            this.plan = plan;
            this.crisisExit = crisisExit;
            this.forcedCrisisId = forcedCrisisId;
        }

        PortalCrisisPlan plan() {
            return plan;
        }

        void commit(ServerPlayer player) {
            if (crisisExit != null) {
                crisisExit.transit().markInside(player.getUUID(), portal.serverTime());
                commitExit(crisisExit);
            }
            boolean applied = PortalCrisisCoordinator.apply(plan, player);
            if (applied && forcedCrisisId != null) {
                PortalCrisisTestOverrides.consume(player.getUUID(), forcedCrisisId);
            }
        }

        void abort() {
            if (crisisExit != null) crisisExit.discard();
        }
    }
}
