package dev.riftgun.relocation;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.crisis.PortalCrisisCoordinator;
import dev.riftgun.crisis.PortalCrisisPlan;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.fuel.PortalFuelCost;
import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.fuel.PortalFuelUse;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalPrivacyService;
import dev.riftgun.service.PortalRequestPurpose;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Server-authoritative five-tick relocation transactions, independent of normal portal pairs. */
public final class EntityRelocationManager {
    private static final List<Transaction> ACTIVE = new ArrayList<>();
    private static final EntityRelocationExitIndex EXIT_INDEX = new EntityRelocationExitIndex();
    private static EntityRelocationRegistry registry;
    private static int configuredMaximum;
    private static int configuredCooldown;

    public static boolean tryStart(ServerPlayer owner, PortalPlayerData data,
                                   PortalGunLocator.LocatedGun locatedGun, boolean explicit) {
        ItemStack gun = locatedGun.stack();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, data.settings().smartDistance());
        if (!capabilities.entityRelocation()) {
            if (explicit) message(owner, "message.riftgun.entity_relocation_module_required");
            return explicit;
        }
        LivingEntity target = findTarget(owner, capabilities).orElse(null);
        if (target == null) {
            if (explicit) message(owner, "message.riftgun.entity_relocation_target_required");
            return explicit;
        }
        return start(owner, data, locatedGun, capabilities, target);
    }

    public static boolean hasEligibleTarget(ServerPlayer owner, PortalPlayerData data, ItemStack gun) {
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, data.settings().smartDistance());
        return capabilities.entityRelocation() && findTarget(owner, capabilities).isPresent();
    }

    private static boolean start(ServerPlayer owner, PortalPlayerData data,
                                 PortalGunLocator.LocatedGun locatedGun,
                                 PortalGunCapabilities capabilities, LivingEntity target) {
        MinecraftServer server = owner.getServer();
        if (server == null) return true;
        ResolvedDestination destination = resolveDestination(server, data);
        if (destination == null) {
            message(owner, "message.riftgun.no_destination_selected");
            return true;
        }
        List<PermissionRequirement> permissions = permissionRequirements(
            server, owner, target, destination);
        if (permissions == null) return true;
        ItemStack gun = locatedGun.stack();
        PortalGunTank tank = new PortalGunTank(gun);
        var profileResult = PortalFuelProfiles.resolve(tank.getFluid());
        if (profileResult.isEmpty()) {
            message(owner, "message.riftgun.fuel_empty");
            return true;
        }
        PortalFuelProfile profile = profileResult.get();
        if (!owner.level().dimension().equals(destination.dimension()) && !profile.crossDimension()) {
            message(owner, "message.riftgun.fuel_dimension_denied");
            return true;
        }

        EntityRelocationRegistry state = registry();
        UUID gunId = PortalGunIdentity.ensure(gun);
        int reserve = profile.maximumConsumption();
        if (tank.getFluid().getAmount() - state.reservedFuel(gunId) < reserve) {
            message(owner, "message.riftgun.fuel_insufficient");
            return true;
        }
        long now = server.overworld().getGameTime();
        EntityRelocationRegistry.Begin begin = state.begin(gunId, target.getUUID(), reserve, now);
        if (begin.status() != EntityRelocationRegistry.BeginStatus.ACCEPTED) {
            message(owner, switch (begin.status()) {
                case GUN_CAPACITY -> "message.riftgun.entity_relocation_capacity";
                case TARGET_BUSY -> "message.riftgun.entity_relocation_target_busy";
                case TARGET_COOLDOWN -> "message.riftgun.entity_relocation_target_cooldown";
                default -> "message.riftgun.entity_relocation_failed";
            });
            return true;
        }

        List<PortalPrivacyService.GrantReservation> privacyReservations =
            reservePrivacyGrants(server, owner, permissions);
        if (privacyReservations == null) {
            state.fail(begin.reservation());
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        float side = EntityRelocationGeometry.sideLength(target.getBbWidth(), target.getBbWidth());
        Vec3 center = feetCenter(target);
        PortalSoundSnapshot sounds = PortalSoundSnapshot.from(data.settings().portalSounds());
        PortalCrisisConfigurationSnapshot crises =
            PortalCrisisConfigurationSnapshot.capture(tank.getFluid());
        ServerLevel destinationLevel = server.getLevel(destination.dimension());
        if (destinationLevel == null) {
            state.fail(begin.reservation());
            releasePrivacyGrants(server, privacyReservations);
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        boolean deferred = EntityRelocationLifecycle.shouldDeferExit(
            destination.playerId() != null,
            destinationLevel.isPositionEntityTicking(BlockPos.containing(destination.position())));
        PreparedRoute preparedRoute = deferred ? null
            : prepareRoute(target, destination, destinationLevel, crises);
        ExitSetup exitSetup = deferred ? null
            : prepareExit(server, destinationLevel, destination, preparedRoute,
                side, profile.rgb(), sounds);
        if (!deferred && exitSetup == null) {
            state.fail(begin.reservation());
            releasePrivacyGrants(server, privacyReservations);
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        EntityRelocationPortalEntity visual = EntityRelocationPortalEntity.createEntrance(
            owner.level(), center, side, profile.rgb(), target.getUUID(), sounds);
        if (!owner.serverLevel().addFreshEntity(visual)) {
            state.fail(begin.reservation());
            releasePrivacyGrants(server, privacyReservations);
            closeExit(server, exitSetup, false);
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        PortalSounds.playShot(owner, sounds);
        PortalSounds.playOpening(owner.serverLevel(), center, sounds);
        ACTIVE.add(new Transaction(begin.reservation(), owner.getUUID(), target.getUUID(),
            target.level().dimension(), gun, destination, profile, side, sounds,
            capabilities.fallGuard(), capabilities.entityFallGuard(),
            capabilities.transitCooldownTicks(), crises,
            privacyReservations, visual.getUUID(),
            exitSetup, preparedRoute, deferred, now));
        return true;
    }

    private static @Nullable List<PermissionRequirement> permissionRequirements(
            MinecraftServer server, ServerPlayer owner, LivingEntity subject,
            ResolvedDestination destination) {
        List<PermissionRequirement> requirements = new ArrayList<>(2);
        if (subject instanceof ServerPlayer playerSubject
            && !playerSubject.getUUID().equals(owner.getUUID())) {
            requirements.add(new PermissionRequirement(
                playerSubject, PortalRequestPurpose.ENTITY_RELOCATION_SUBJECT));
        }
        ServerPlayer destinationPlayer = destination.resolvePlayer(server);
        if (destination.playerId() != null && destinationPlayer == null) {
            owner.displayClientMessage(Component.translatable(
                "message.riftgun.player_destination_unavailable"), false);
            return null;
        }
        if (destinationPlayer != null && !destinationPlayer.getUUID().equals(owner.getUUID())) {
            requirements.add(new PermissionRequirement(
                destinationPlayer, PortalRequestPurpose.ENTITY_RELOCATION_DESTINATION));
        }

        for (PermissionRequirement requirement : requirements) {
            PortalPrivacyService.Access access = PortalPrivacyService.checkAccess(
                server, requirement.target(), owner, requirement.purpose());
            requirement.access(access);
            switch (access.outcome()) {
                case DENIED, DENIED_ONCE, ALWAYS_DENIED -> {
                    PortalPrivacyService.notifyDenied(
                        owner, requirement.target(), access, requirement.purpose());
                    return null;
                }
                case REQUESTED, ALLOWED -> {}
                case GRANTED_ONCE -> requirement.requiresGrant(true);
            }
        }
        boolean waiting = false;
        for (PermissionRequirement requirement : requirements) {
            if (requirement.access().outcome() != PortalPrivacyService.Outcome.REQUESTED) continue;
            PortalPrivacyService.promptRequest(
                server, requirement.target(), owner, requirement.purpose());
            waiting = true;
        }
        return waiting ? null : requirements;
    }

    private static @Nullable List<PortalPrivacyService.GrantReservation> reservePrivacyGrants(
            MinecraftServer server, ServerPlayer owner, List<PermissionRequirement> requirements) {
        List<PortalPrivacyService.GrantReservation> reservations = new ArrayList<>(requirements.size());
        for (PermissionRequirement requirement : requirements) {
            if (!requirement.requiresGrant()) continue;
            PortalPrivacyService.GrantReservation reservation = PortalPrivacyService.reserveGrant(
                server, requirement.target().getUUID(), owner.getUUID(), requirement.purpose());
            if (reservation == null) {
                releasePrivacyGrants(server, reservations);
                return null;
            }
            reservations.add(reservation);
        }
        return reservations;
    }

    private static void releasePrivacyGrants(
            MinecraftServer server, List<PortalPrivacyService.GrantReservation> reservations) {
        for (PortalPrivacyService.GrantReservation reservation : reservations) {
            PortalPrivacyService.releaseGrant(server, reservation);
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<Transaction> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Transaction tx = iterator.next();
            if (now - tx.startedAt() < EntityRelocationLifecycle.OPENING_TICKS) continue;
            if (!tx.deferred()) {
                ExitReadiness readiness = exitReadiness(server, tx.exitSetup());
                if (readiness == ExitReadiness.WAITING) continue;
                if (readiness == ExitReadiness.FAILED) {
                    iterator.remove();
                    fail(server, tx);
                    continue;
                }
            }
            iterator.remove();
            if (tx.deferred()) completeDeferred(server, tx, now);
            else completeLoaded(server, tx, now);
        }
    }

    private static void completeLoaded(MinecraftServer server, Transaction tx, long now) {
        ServerLevel sourceLevel = server.getLevel(tx.sourceDimension());
        LivingEntity target = liveTarget(sourceLevel, tx.targetId());
        if (target == null) {
            fail(server, tx);
            return;
        }
        ResolvedDestination destination = tx.destination().refresh(server);
        ServerLevel targetLevel = destination == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null || tx.preparedRoute() == null
            || !targetLevel.dimension().equals(tx.preparedRoute().dimension())) {
            fail(server, tx);
            notifyUnavailablePlayerDestination(server, tx);
            return;
        }
        if (!dimensionAllowed(tx, destination)) {
            fail(server, tx);
            return;
        }
        PortalFuelUse use = fuelUse(tx, target);
        if (!canConsume(tx.gun(), use)) {
            fail(server, tx);
            return;
        }

        PreparedRoute route = refreshRoute(target, destination, tx.preparedRoute().crisis());
        Vec3 sourcePosition = target.position();
        freezeSource(sourceLevel, tx.visualId());
        Entity moved = teleport(target, targetLevel, route.outputPosition(), route.momentum());
        if (moved == null) {
            fail(server, tx);
            return;
        }
        Vec3 actualExitCenter = exitCenter(server, tx.exitSetup(), route.exitCenter());
        closeExit(server, tx.exitSetup(), true);
        finishSuccessful(server, sourceLevel, targetLevel, sourcePosition, moved, tx, use,
            route.momentum(), route.crisis(), route.landingPosition(), actualExitCenter, now);
    }

    private static void completeDeferred(MinecraftServer server, Transaction tx, long now) {
        ServerLevel sourceLevel = server.getLevel(tx.sourceDimension());
        LivingEntity target = liveTarget(sourceLevel, tx.targetId());
        if (target == null) {
            fail(server, tx);
            return;
        }
        ResolvedDestination destination = tx.destination().refresh(server);
        ServerLevel targetLevel = destination == null ? null : server.getLevel(destination.dimension());
        if (targetLevel == null || !dimensionAllowed(tx, destination)) {
            fail(server, tx);
            return;
        }
        PortalFuelUse use = fuelUse(tx, target);
        if (!canConsume(tx.gun(), use)) {
            fail(server, tx);
            return;
        }

        Vec3 sourcePosition = target.position();
        Vec3 bootstrapMomentum = upwardMomentum(target.getDeltaMovement());
        freezeSource(sourceLevel, tx.visualId());
        Entity moved = teleport(target, targetLevel,
            destination.position().add(0.0, 0.15, 0.0), bootstrapMomentum);
        if (moved == null) {
            fail(server, tx);
            return;
        }

        PreparedRoute route = prepareRoute(moved, destination, targetLevel, tx.crises());
        if (route.crisis() != null && route.crisis().relocation() != null) {
            Entity crisisMoved = teleport(moved, targetLevel, route.outputPosition(), route.momentum());
            if (crisisMoved != null) moved = crisisMoved;
            else route = normalRoute(moved, destination, null);
        } else {
            route = new PreparedRoute(route.dimension(), route.exitCenter(), route.exitOrientation(),
                route.exitYaw(), moved.position(), bootstrapMomentum, destination.position(),
                route.crisis(), route.shareable());
        }

        ExitSetup exit = prepareExit(server, targetLevel, destination, route,
            tx.side(), tx.profile().rgb(), tx.sounds());
        Vec3 actualExitCenter = exitCenter(server, exit, route.exitCenter());
        closeExit(server, exit, true);
        finishSuccessful(server, sourceLevel, targetLevel, sourcePosition, moved, tx, use,
            route.momentum(), route.crisis(), route.landingPosition(), actualExitCenter, now);
    }

    private static void finishSuccessful(MinecraftServer server, ServerLevel sourceLevel,
                                         ServerLevel targetLevel, Vec3 sourcePosition, Entity moved,
                                         Transaction tx, PortalFuelUse use, Vec3 momentum,
                                         @Nullable PortalCrisisPlan crisis, Vec3 landingPosition,
                                         Vec3 exitCenter, long now) {
        if (!PortalFuelManager.consume(tx.gun(), use)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
            if (owner != null) message(owner, "message.riftgun.entity_relocation_failed");
        }
        moved.setDeltaMovement(momentum);
        moved.hasImpulse = true;
        if (dev.riftgun.portal.PortalFallGuardPolicy.applies(
            moved, tx.fallGuard(), tx.entityFallGuard())) moved.fallDistance = 0.0F;
        if (moved instanceof ServerPlayer player && crisis != null) {
            PortalCrisisCoordinator.apply(crisis, player);
        }
        EntityRelocationArrivalLatch.register(moved, landingPosition, exitCenter, tx.side(), now,
            tx.transitCooldownTicks());
        PortalSounds.playTransit(sourceLevel, sourcePosition, tx.sounds());
        PortalSounds.playTransit(targetLevel, moved.position(), tx.sounds());
        registry().complete(tx.reservation(), now);
    }

    private static void fail(MinecraftServer server, Transaction tx) {
        registry().fail(tx.reservation());
        releasePrivacyGrants(server, tx.privacyReservations());
        closeExit(server, tx.exitSetup(), false);
        ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
        if (owner != null) message(owner, "message.riftgun.entity_relocation_failed");
        ServerLevel source = server.getLevel(tx.sourceDimension());
        if (source != null) {
            Entity visual = source.getEntity(tx.visualId());
            if (visual instanceof EntityRelocationPortalEntity portal) portal.freezeAndClose();
        }
    }

    private static void freezeSource(ServerLevel level, UUID visualId) {
        Entity visual = level.getEntity(visualId);
        if (visual instanceof EntityRelocationPortalEntity portal) portal.freezeAndClose();
    }

    private static @Nullable ExitSetup prepareExit(
            MinecraftServer server, ServerLevel targetLevel, ResolvedDestination destination,
            PreparedRoute route, float side, int rgb, PortalSoundSnapshot sounds) {
        EntityRelocationExitIndex.Lease shared = route.shareable()
            ? reserveSharedExit(server, destination, side) : null;
        if (shared != null) return new ExitSetup(shared, null, targetLevel.dimension());
        int durationTicks = PortalOpenDuration.ticks(
            ServerConfig.VALUES.entityRelocationExitDurationSeconds.get());
        EntityRelocationPortalEntity exit;
        if (destination.playerId() != null
            && (route.crisis() == null || route.crisis().relocation() == null)) {
            ServerPlayer player = destination.resolvePlayer(server);
            if (player == null || player.serverLevel() != targetLevel) return null;
            exit = EntityRelocationPortalEntity.createPlayerDestinationExit(
                targetLevel, player, side, rgb, durationTicks, sounds);
        } else {
            exit = EntityRelocationPortalEntity.createExit(targetLevel, route.exitCenter(), side,
                rgb, durationTicks, sounds, route.exitOrientation(), route.exitYaw());
        }
        if (!targetLevel.addFreshEntity(exit)) return null;
        PortalSounds.playOpening(targetLevel, exit.position(), sounds);
        EntityRelocationExitIndex.DestinationKey key = route.shareable()
            ? destination.sharedKey() : null;
        if (key != null) {
            EXIT_INDEX.register(key, new EntityRelocationExitIndex.ExitReference(
                exit.getUUID(), targetLevel.dimension().location()));
        }
        return new ExitSetup(null, exit.getUUID(), targetLevel.dimension());
    }

    private static PreparedRoute prepareRoute(Entity target, ResolvedDestination destination,
                                              ServerLevel targetLevel,
                                              PortalCrisisConfigurationSnapshot crises) {
        PreparedRoute normal = normalRoute(target, destination, null);
        PortalCrisisPlan crisis = target instanceof ServerPlayer player
            ? PortalCrisisCoordinator.prepare(crises, player, targetLevel,
                destination.position().add(0.0, 0.15, 0.0), normal.momentum(),
                destination.yaw(), false, true).orElse(null)
            : null;
        if (crisis == null || crisis.relocation() == null) {
            return new PreparedRoute(normal.dimension(), normal.exitCenter(),
                normal.exitOrientation(), normal.exitYaw(), normal.outputPosition(),
                normal.momentum(), normal.landingPosition(), crisis, normal.shareable());
        }
        PortalCrisisPlan.Relocation relocation = crisis.relocation();
        return new PreparedRoute(targetLevel.dimension(), relocation.exitPlacement().center(),
            relocation.exitPlacement().orientation(), relocation.exitPlacement().yaw(),
            relocation.destination(), relocation.momentum(), relocation.destination(), crisis, false);
    }

    private static PreparedRoute refreshRoute(Entity target, ResolvedDestination destination,
                                              @Nullable PortalCrisisPlan crisis) {
        if (crisis != null && crisis.relocation() != null) {
            PortalCrisisPlan.Relocation relocation = crisis.relocation();
            return new PreparedRoute(destination.dimension(), relocation.exitPlacement().center(),
                relocation.exitPlacement().orientation(), relocation.exitPlacement().yaw(),
                relocation.destination(), relocation.momentum(), relocation.destination(), crisis, false);
        }
        return normalRoute(target, destination, crisis);
    }

    private static PreparedRoute normalRoute(Entity target, ResolvedDestination destination,
                                             @Nullable PortalCrisisPlan crisis) {
        if (destination.playerId() != null) {
            return new PreparedRoute(destination.dimension(), destination.playerExitCenter(),
                PortalOrientation.BOTTOM, destination.yaw(),
                destination.position().add(0.0, 0.15, 0.0),
                upwardMomentum(target.getDeltaMovement()), destination.position(), crisis, false);
        }
        Vec3 exitCenter = EntityRelocationGeometry.savedDestinationBottomExitCenter(
            destination.position(), target.getBbHeight());
        return new PreparedRoute(destination.dimension(), exitCenter, PortalOrientation.BOTTOM,
            destination.yaw(), EntityRelocationGeometry.bottomOutputPosition(
                exitCenter, target.getBbHeight()), downwardMomentum(target.getDeltaMovement()),
            destination.position(), crisis, true);
    }

    private static @Nullable LivingEntity liveTarget(@Nullable ServerLevel sourceLevel, UUID targetId) {
        Entity raw = sourceLevel == null ? null : sourceLevel.getEntity(targetId);
        return raw instanceof LivingEntity target && target.isAlive()
            && !target.isPassenger() && target.getPassengers().isEmpty() ? target : null;
    }

    private static boolean dimensionAllowed(Transaction tx, ResolvedDestination destination) {
        return tx.sourceDimension().equals(destination.dimension()) || tx.profile().crossDimension();
    }

    private static PortalFuelUse fuelUse(Transaction tx, Entity target) {
        int amount = PortalFuelCost.choose(tx.profile().minimumConsumption(),
            tx.profile().maximumConsumption(), ServerConfig.VALUES.randomConsumption.get(),
            target.getRandom()::nextInt);
        return new PortalFuelUse(tx.profile(), amount);
    }

    private static @Nullable Entity teleport(Entity target, ServerLevel targetLevel,
                                             Vec3 position, Vec3 momentum) {
        float yaw = target.getYRot();
        float pitch = target.getXRot();
        if (target.level() == targetLevel) {
            boolean ok = target.teleportTo(targetLevel, position.x, position.y, position.z,
                Set.<RelativeMovement>of(), yaw, pitch);
            return ok ? target : null;
        }
        return target.changeDimension(new DimensionTransition(
            targetLevel, position, momentum, yaw, pitch, DimensionTransition.DO_NOTHING));
    }

    private static Vec3 upwardMomentum(Vec3 momentum) {
        return momentum.y < 0.12 ? new Vec3(momentum.x, 0.12, momentum.z) : momentum;
    }

    private static Vec3 downwardMomentum(Vec3 momentum) {
        return momentum.y > -0.12 ? new Vec3(momentum.x, -0.12, momentum.z) : momentum;
    }

    private static ExitReadiness exitReadiness(MinecraftServer server,
                                               @Nullable ExitSetup setup) {
        EntityRelocationPortalEntity portal = resolveExit(server, setup);
        if (portal == null) return ExitReadiness.FAILED;
        return switch (portal.phase()) {
            case OPEN -> ExitReadiness.READY;
            case CHARGING, OPENING -> ExitReadiness.WAITING;
            case CLOSING, CLOSED -> ExitReadiness.FAILED;
        };
    }

    private static Vec3 exitCenter(MinecraftServer server, @Nullable ExitSetup setup,
                                   Vec3 fallback) {
        EntityRelocationPortalEntity portal = resolveExit(server, setup);
        return portal == null ? fallback : portal.position();
    }

    private static void closeExit(MinecraftServer server, @Nullable ExitSetup setup,
                                  boolean successful) {
        if (setup == null) return;
        if (setup.sharedExit() != null) {
            releaseSharedExit(server, setup.sharedExit(), successful);
            return;
        }
        if (!successful) {
            EntityRelocationPortalEntity portal = resolveExit(server, setup);
            // Once fully open this exit may already have been reserved by another relocation.
            // Leaving an orphan open for its normal short hold is safer than closing it under
            // another transaction; only abort an exit that is still in its opening animation.
            if (portal != null && portal.phase() != PortalLifecycle.Phase.OPEN) portal.beginClosing();
        }
    }

    private static @Nullable EntityRelocationPortalEntity resolveExit(
            MinecraftServer server, @Nullable ExitSetup setup) {
        if (setup == null) return null;
        if (setup.sharedExit() != null) return resolveExit(server, setup.sharedExit());
        if (setup.portalId() == null) return null;
        ServerLevel level = server.getLevel(setup.dimension());
        Entity entity = level == null ? null : level.getEntity(setup.portalId());
        return entity instanceof EntityRelocationPortalEntity portal && portal.isExit() ? portal : null;
    }

    private static void notifyUnavailablePlayerDestination(MinecraftServer server, Transaction tx) {
        if (tx.destination().playerId() == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
        if (owner != null) owner.displayClientMessage(Component.translatable(
            "message.riftgun.player_destination_unavailable"), false);
    }

    private static @Nullable EntityRelocationExitIndex.Lease reserveSharedExit(
            MinecraftServer server, ResolvedDestination destination, float requiredSide) {
        EntityRelocationExitIndex.DestinationKey key = destination.sharedKey();
        if (key == null) return null;
        return EXIT_INDEX.reserveStable(key, requiredSide, new EntityRelocationExitIndex.CandidateAccess() {
            @Override
            public EntityRelocationExitIndex.Candidate inspect(
                    EntityRelocationExitIndex.ExitReference exit) {
                EntityRelocationPortalEntity portal = resolveExit(server, exit);
                if (portal == null) return EntityRelocationExitIndex.Candidate.missing();
                return switch (portal.phase()) {
                    case OPENING -> EntityRelocationExitIndex.Candidate.opening();
                    case OPEN -> EntityRelocationExitIndex.Candidate.open(portal.remainingOpenTicks());
                    case CLOSING, CLOSED -> EntityRelocationExitIndex.Candidate.closing();
                    case CHARGING -> EntityRelocationExitIndex.Candidate.opening();
                };
            }

            @Override
            public boolean tryReserve(EntityRelocationExitIndex.ExitReference exit, float side) {
                EntityRelocationPortalEntity portal = resolveExit(server, exit);
                return portal != null && portal.tryReserve(side);
            }
        }).orElse(null);
    }

    private static void releaseSharedExit(MinecraftServer server,
                                          @Nullable EntityRelocationExitIndex.Lease lease,
                                          boolean successful) {
        EntityRelocationPortalEntity portal = resolveExit(server, lease);
        if (portal != null) portal.releaseReservation(successful);
    }

    private static @Nullable EntityRelocationPortalEntity resolveExit(
            MinecraftServer server, @Nullable EntityRelocationExitIndex.Lease lease) {
        return lease == null ? null : resolveExit(server, lease.exit());
    }

    private static @Nullable EntityRelocationPortalEntity resolveExit(
            MinecraftServer server, EntityRelocationExitIndex.ExitReference reference) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, reference.dimension());
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(reference.portalId());
        return entity instanceof EntityRelocationPortalEntity portal && portal.isExit() ? portal : null;
    }

    static void unregisterExit(UUID portalId) {
        EXIT_INDEX.unregister(portalId);
    }

    private static Optional<LivingEntity> findTarget(ServerPlayer owner, PortalGunCapabilities capabilities) {
        double range = capabilities.configuredSurfaceRange();
        Vec3 start = owner.getEyePosition();
        Vec3 end = start.add(owner.getLookAngle().scale(range));
        HitResult block = owner.pick(range, 0.0F, false);
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB search = owner.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        Vec3 finalEnd = end;
        return owner.level().getEntitiesOfClass(LivingEntity.class, search, entity ->
                entity != owner && entity.isAlive() && !entity.isSpectator()
                    && !entity.isPassenger() && entity.getPassengers().isEmpty()
                    && (entity instanceof Player ? capabilities.playerTarget()
                        : capabilities.entityAccess().allows(entity))
                    && entity.getBoundingBox().inflate(0.3).clip(start, finalEnd).isPresent())
            .stream().min(java.util.Comparator.comparingDouble(entity ->
                entity.getBoundingBox().clip(start, finalEnd).orElse(entity.position()).distanceToSqr(start)));
    }

    private static @Nullable ResolvedDestination resolveDestination(MinecraftServer server,
                                                                    PortalPlayerData data) {
        UUID playerId = data.selectedPlayerId();
        if (playerId != null) {
            ServerPlayer target = server.getPlayerList().getPlayer(playerId);
            if (target == null) return null;
            return ResolvedDestination.player(playerId, target);
        }
        UUID destinationId = data.selectedDestinationId();
        Destination destination = destinationId == null ? null : data.destination(destinationId).orElse(null);
        return destination == null ? null : ResolvedDestination.saved(destination);
    }

    private static Vec3 feetCenter(Entity target) {
        return new Vec3(target.getX(), EntityRelocationGeometry.centerY(
            target.getY(), PortalEntity.DEPTH), target.getZ());
    }

    private static boolean canConsume(ItemStack gun, PortalFuelUse use) {
        PortalGunTank tank = new PortalGunTank(gun);
        return PortalFuelProfiles.resolve(tank.getFluid())
            .filter(profile -> profile.id().equals(use.profile().id()))
            .isPresent() && tank.getFluid().getAmount() >= use.amount();
    }

    private static EntityRelocationRegistry registry() {
        int maximum = ServerConfig.VALUES.maximumConcurrentEntityRelocations.get();
        int cooldown = ServerConfig.VALUES.entityRelocationTargetCooldownTicks.get();
        if (registry == null || ACTIVE.isEmpty()
            && (maximum != configuredMaximum || cooldown != configuredCooldown)) {
            configuredMaximum = maximum;
            configuredCooldown = cooldown;
            registry = new EntityRelocationRegistry(maximum, cooldown);
        }
        return registry;
    }

    public static void reset() {
        ACTIVE.clear();
        registry = null;
        EXIT_INDEX.clear();
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record Transaction(EntityRelocationRegistry.Reservation reservation, UUID ownerId,
                               UUID targetId, net.minecraft.resources.ResourceKey<Level> sourceDimension,
                               ItemStack gun, ResolvedDestination destination, PortalFuelProfile profile,
                               float side, PortalSoundSnapshot sounds, boolean fallGuard,
                               boolean entityFallGuard,
                               int transitCooldownTicks,
                               PortalCrisisConfigurationSnapshot crises,
                               List<PortalPrivacyService.GrantReservation> privacyReservations,
                               UUID visualId, @Nullable ExitSetup exitSetup,
                               @Nullable PreparedRoute preparedRoute, boolean deferred,
                               long startedAt) {}

    private record PreparedRoute(ResourceKey<Level> dimension, Vec3 exitCenter,
                                 PortalOrientation exitOrientation, float exitYaw,
                                 Vec3 outputPosition, Vec3 momentum, Vec3 landingPosition,
                                 @Nullable PortalCrisisPlan crisis, boolean shareable) {}

    private record ExitSetup(@Nullable EntityRelocationExitIndex.Lease sharedExit,
                             @Nullable UUID portalId, ResourceKey<Level> dimension) {}

    private enum ExitReadiness {
        WAITING,
        READY,
        FAILED
    }

    private record ResolvedDestination(net.minecraft.resources.ResourceKey<Level> dimension,
                                       Vec3 position, float yaw, @Nullable UUID playerId,
                                       @Nullable UUID savedDestinationId,
                                       @Nullable Vec3 visualExitCenter) {
        static ResolvedDestination saved(Destination destination) {
            return new ResolvedDestination(destination.dimension(), destination.position(), destination.yaw(),
                null, destination.id(), null);
        }

        static ResolvedDestination player(UUID playerId, ServerPlayer player) {
            return new ResolvedDestination(player.level().dimension(), player.position(), player.getYRot(),
                playerId, null, EntityRelocationGeometry.playerDestinationExitCenter(
                    player.position(), player.getBoundingBox().maxY));
        }

        @Nullable ServerPlayer resolvePlayer(MinecraftServer server) {
            return playerId == null ? null : server.getPlayerList().getPlayer(playerId);
        }

        @Nullable ResolvedDestination refresh(MinecraftServer server) {
            if (playerId == null) return this;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            return player == null ? null : player(playerId, player);
        }

        @Nullable EntityRelocationExitIndex.DestinationKey sharedKey() {
            return savedDestinationId == null ? null : new EntityRelocationExitIndex.DestinationKey(
                savedDestinationId, dimension.location(), position.x, position.y, position.z);
        }

        Vec3 playerExitCenter() {
            return visualExitCenter == null ? position : visualExitCenter;
        }
    }

    private static final class PermissionRequirement {
        private final ServerPlayer target;
        private final PortalRequestPurpose purpose;
        private boolean requiresGrant;
        private PortalPrivacyService.Access access;

        private PermissionRequirement(ServerPlayer target, PortalRequestPurpose purpose) {
            this.target = target;
            this.purpose = purpose;
        }

        ServerPlayer target() { return target; }
        PortalRequestPurpose purpose() { return purpose; }
        boolean requiresGrant() { return requiresGrant; }
        void requiresGrant(boolean value) { requiresGrant = value; }
        PortalPrivacyService.Access access() { return access; }
        void access(PortalPrivacyService.Access value) { access = value; }
    }

    private EntityRelocationManager() {}
}
