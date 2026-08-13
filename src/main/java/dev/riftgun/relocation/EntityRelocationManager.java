package dev.riftgun.relocation;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.crisis.PortalCrisisCoordinator;
import dev.riftgun.crisis.PortalCrisisPlan;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.fuel.PortalFuelCost;
import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.fuel.PortalFuelUse;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.fuel.PortalFuelManager;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalProjectileState;
import dev.riftgun.portal.ProjectileMotion;
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
import java.util.UUID;
import net.minecraft.server.level.TicketType;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Server-authoritative five-tick relocation transactions, independent of normal portal pairs. */
public final class EntityRelocationManager {
    private static final TicketType<UUID> PREPARATION_TICKET =
        TicketType.create("riftgun_entity_relocation_preparation", UUID::compareTo);
    private static final List<Transaction> ACTIVE = new ArrayList<>();
    private static final List<PendingPreparation> PREPARING = new ArrayList<>();
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
        Entity target = findTarget(owner, capabilities).orElse(null);
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
                                 PortalGunCapabilities capabilities, Entity target) {
        MinecraftServer server = owner.getServer();
        if (server == null) return true;
        EntityRelocationTree tree = EntityRelocationTree.capture(target);
        List<Entity> treeMembers = tree.members(target);
        ResolvedDestination destination = resolveDestination(server, data);
        if (destination == null) {
            message(owner, "message.riftgun.no_destination_selected");
            return true;
        }
        List<PermissionRequirement> permissions = permissionRequirements(
            server, owner, treeMembers, destination);
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
        EntityRelocationFuelPolicy.Quote fuelQuote = EntityRelocationFuelPolicy.quote(
            treeMembers.stream().map(EntityRelocationFuelPolicy::classify).toList(),
            profile.maximumConsumption(), relocationFuelMultipliers());
        int reserve = fuelQuote.maximumReservation();
        if (tank.getFluid().getAmount() - state.reservedFuel(gunId) < reserve) {
            message(owner, "message.riftgun.fuel_insufficient");
            return true;
        }
        long now = server.overworld().getGameTime();
        EntityRelocationRegistry.Begin begin = state.begin(gunId, tree.memberIds(), reserve, now);
        if (begin.status() != EntityRelocationRegistry.BeginStatus.ACCEPTED) {
            TransitDiagnostics.relocation("request rejected owner={} target={} gun={} status={} preparing={}",
                owner.getUUID(), target.getUUID(), gunId, begin.status(),
                isPreparing(target.getUUID()));
            String rejectionMessage = switch (begin.status()) {
                case GUN_CAPACITY -> "message.riftgun.entity_relocation_capacity";
                case TARGET_BUSY -> preparingMessageShown(target.getUUID())
                    ? "message.riftgun.entity_relocation_preparing_busy"
                    : null;
                case TARGET_COOLDOWN -> "message.riftgun.entity_relocation_target_cooldown";
                default -> "message.riftgun.entity_relocation_failed";
            };
            if (rejectionMessage != null) message(owner, rejectionMessage);
            return true;
        }

        List<PortalPrivacyService.GrantReservation> privacyReservations =
            reservePrivacyGrants(server, owner, permissions);
        if (privacyReservations == null) {
            state.fail(begin.reservation());
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        EntityRelocationTree.Metrics treeMetrics = tree.metrics(target);
        float side = EntityRelocationGeometry.sideLength(
            (float) treeMetrics.width(), (float) treeMetrics.depth());
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
        int openingTicks = target instanceof Projectile
            ? ServerConfig.VALUES.projectileRelocationOpeningTicks.get()
            : EntityRelocationLifecycle.OPENING_TICKS;
        if (deferred) {
            beginPreparation(destinationLevel, begin.reservation(), owner, target, tree,
                locatedGun.saveReference(),
                destination, profile, sounds, capabilities, crises, privacyReservations,
                snapshotPermissions(permissions),
                openingTicks, fuelQuote, now);
            return true;
        }
        PreparedRoute preparedRoute = prepareRoute(target, tree, destination, destinationLevel, crises);
        EntityRelocationExitService.Handle exitSetup = prepareExit(server, destinationLevel, destination, preparedRoute,
            side, profile.rgb(), sounds, openingTicks);
        if (exitSetup == null) {
            state.fail(begin.reservation());
            releasePrivacyGrants(server, privacyReservations);
            message(owner, "message.riftgun.entity_relocation_failed");
            return true;
        }
        EntityRelocationPortalEntity visual = EntityRelocationPortalEntity.createEntrance(
            owner.level(), center, side, profile.rgb(), target.getUUID(), sounds, openingTicks);
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
            crises, privacyReservations, visual.getUUID(),
            exitSetup, preparedRoute, openingTicks, fuelQuote, tree, now));
        return true;
    }

    private static void beginPreparation(
            ServerLevel destinationLevel, EntityRelocationRegistry.Reservation reservation,
            ServerPlayer owner, Entity target, EntityRelocationTree tree,
            net.minecraft.nbt.CompoundTag gunReference, ResolvedDestination destination,
            PortalFuelProfile profile, PortalSoundSnapshot sounds,
            PortalGunCapabilities capabilities, PortalCrisisConfigurationSnapshot crises,
            List<PortalPrivacyService.GrantReservation> privacyReservations,
            List<PermissionSnapshot> permissions,
            int openingTicks, EntityRelocationFuelPolicy.Quote fuelQuote, long now) {
        UUID ticketId = reservation.id();
        ChunkPos chunk = new ChunkPos(BlockPos.containing(destination.position()));
        TransitDiagnostics.relocation("prepare begin reservation={} owner={} target={} source={} destination={} chunk={} timeoutTicks={} tickingBeforeTicket={}",
            reservation.id(), owner.getUUID(), target.getUUID(),
            target.level().dimension().location(), destination.dimension().location(), chunk,
            ServerConfig.VALUES.destinationReadinessTimeoutTicks.get(),
            destinationLevel.isPositionEntityTicking(BlockPos.containing(destination.position())));
        destinationLevel.getChunkSource().addRegionTicket(
            PREPARATION_TICKET, chunk, 3, ticketId, true);
        EntityRelocationPreparation preparation = new EntityRelocationPreparation(
            now, ServerConfig.VALUES.destinationReadinessTimeoutTicks.get(),
            () -> {
                destinationLevel.getChunkSource().removeRegionTicket(
                    PREPARATION_TICKET, chunk, 3, ticketId, true);
                TransitDiagnostics.ticket("relocation preparation released reservation={} destination={} chunk={}",
                    reservation.id(), destination.dimension().location(), chunk);
            });
        PREPARING.add(new PendingPreparation(
            reservation, owner.getUUID(), target.getUUID(), target.level().dimension(),
            gunReference,
            destination, profile, sounds, capabilities.fallGuard(),
            capabilities.entityFallGuard(), crises, privacyReservations, permissions,
            openingTicks, fuelQuote, tree, preparation));
    }

    private static List<PermissionSnapshot> snapshotPermissions(
            List<PermissionRequirement> requirements) {
        return requirements.stream().map(requirement -> new PermissionSnapshot(
            requirement.target().getUUID(), requirement.purpose(), requirement.requiresGrant()))
            .toList();
    }

    private static @Nullable List<PermissionRequirement> permissionRequirements(
            MinecraftServer server, ServerPlayer owner, List<Entity> subjects,
            ResolvedDestination destination) {
        List<PermissionRequirement> requirements = new ArrayList<>(subjects.size() + 1);
        for (Entity subject : subjects) {
            if (subject instanceof ServerPlayer playerSubject
                && !playerSubject.getUUID().equals(owner.getUUID())) {
                requirements.add(new PermissionRequirement(
                    playerSubject, PortalRequestPurpose.ENTITY_RELOCATION_SUBJECT));
            }
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
        tickPreparations(server);
        if (ACTIVE.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<Transaction> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Transaction tx = iterator.next();
            long elapsed = now - tx.startedAt();
            EntityRelocationPortalEntity exit = resolveExit(server, tx.exitSetup());
            ServerLevel exitLevel = server.getLevel(tx.exitSetup().dimension());
            boolean destinationTicking = exit != null && exitLevel != null
                && exitLevel.isPositionEntityTicking(exit.blockPosition());
            EntityRelocationCompletionPolicy.Decision decision =
                EntityRelocationCompletionPolicy.decide(elapsed, tx.openingTicks(),
                    ServerConfig.VALUES.destinationReadinessTimeoutTicks.get(),
                    exit != null, destinationTicking);
            if (decision == EntityRelocationCompletionPolicy.Decision.WAITING) {
                if (elapsed == tx.openingTicks() || elapsed % 20L == 0L) {
                    TransitDiagnostics.relocation("waiting for exit reservation={} elapsedTicks={} exitPresent={} phase={} phaseTicks={} ticketHeld={} chunkTicking={}",
                        tx.reservation().id(), elapsed, exit != null,
                        exit == null ? "null" : exit.phase(), exit == null ? -1 : exit.phaseTicks(),
                        exit != null && exit.chunkTicketHeld(), exitLevel != null && exit != null
                            && exitLevel.isPositionEntityTicking(exit.blockPosition()));
                }
                continue;
            }
            if (decision != EntityRelocationCompletionPolicy.Decision.READY) {
                iterator.remove();
                TransitDiagnostics.warning("relocation completion gate failed reservation={} decision={} elapsedTicks={}",
                    tx.reservation().id(), decision, elapsed);
                fail(server, tx);
                continue;
            }
            iterator.remove();
            completeLoaded(server, tx, now);
        }
    }

    private static void tickPreparations(MinecraftServer server) {
        if (PREPARING.isEmpty()) return;
        long now = server.overworld().getGameTime();
        Iterator<PendingPreparation> iterator = PREPARING.iterator();
        while (iterator.hasNext()) {
            PendingPreparation pending = iterator.next();
            ServerLevel targetLevel = server.getLevel(pending.destination().dimension());
            boolean ready = targetLevel != null && targetLevel.isPositionEntityTicking(
                BlockPos.containing(pending.destination().position()));
            if (!ready && pending.preparation().shouldShowPreparingMessage(now)) {
                ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerId());
                if (owner != null) message(owner, "message.riftgun.entity_relocation_preparing");
            }
            EntityRelocationPreparation.Outcome outcome =
                pending.preparation().advance(now, ready);
            if (outcome == EntityRelocationPreparation.Outcome.WAITING) continue;
            iterator.remove();
            TransitDiagnostics.relocation("prepare terminal reservation={} outcome={} elapsedTicks={} destination={} ticking={}",
                pending.reservation().id(), outcome,
                now - pending.preparation().startedAt(), pending.destination().dimension().location(), ready);
            if (outcome == EntityRelocationPreparation.Outcome.TIMED_OUT) {
                abortPreparation(server, pending,
                    "message.riftgun.entity_relocation_preparation_timeout");
                continue;
            }
            startPrepared(server, pending, now);
        }
    }

    private static void startPrepared(MinecraftServer server, PendingPreparation pending, long now) {
        ServerLevel sourceLevel = server.getLevel(pending.sourceDimension());
        Entity target = liveTarget(sourceLevel, pending.targetId());
        ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerId());
        PortalGunLocator.LocatedGun located = owner == null ? null
            : PortalGunLocator.resolveReference(owner, pending.gunReference()).orElse(null);
        ItemStack gun = located == null ? ItemStack.EMPTY : located.stack();
        ResolvedDestination destination = refreshPreparedDestination(server, owner,
            pending.destination());
        ServerLevel targetLevel = destination == null ? null : server.getLevel(destination.dimension());
        String abortReason = preparedAbortReason(server, pending, target, destination, targetLevel, gun);
        if (abortReason != null) {
            TransitDiagnostics.warning("relocation prepared revalidation failed reservation={} reason={} ownerPresent={} targetPresent={}",
                pending.reservation().id(), abortReason, owner != null, target != null);
            abortPreparation(server, pending, "message.riftgun.entity_relocation_failed");
            return;
        }

        if (!pending.tree().matches(target)) {
            abortPreparation(server, pending, "message.riftgun.entity_relocation_failed");
            return;
        }
        EntityRelocationTree.Metrics metrics = pending.tree().metrics(target);
        float side = EntityRelocationGeometry.sideLength(
            (float) metrics.width(), (float) metrics.depth());
        PreparedRoute route = prepareRoute(
            target, pending.tree(), destination, targetLevel, pending.crises());
        if (route.crisis() != null && route.crisis().relocation() != null
            && !targetLevel.isPositionEntityTicking(BlockPos.containing(route.exitCenter()))) {
            route = normalRoute(target, pending.tree(), destination, null);
        }
        EntityRelocationExitService.Handle exit = prepareExit(server, targetLevel, destination, route, side,
            pending.profile().rgb(), pending.sounds(), pending.openingTicks());
        if (exit == null) {
            TransitDiagnostics.warning("relocation prepared exit creation failed reservation={} destination={} exitCenter={}",
                pending.reservation().id(), targetLevel.dimension().location(), route.exitCenter());
            abortPreparation(server, pending, "message.riftgun.entity_relocation_failed");
            return;
        }
        Vec3 center = feetCenter(target);
        EntityRelocationPortalEntity visual = EntityRelocationPortalEntity.createEntrance(
            sourceLevel, center, side, pending.profile().rgb(), target.getUUID(),
            pending.sounds(), pending.openingTicks());
        if (!sourceLevel.addFreshEntity(visual)) {
            TransitDiagnostics.warning("relocation entrance creation failed reservation={} source={} center={}",
                pending.reservation().id(), sourceLevel.dimension().location(), center);
            closeExit(server, exit, false);
            abortPreparation(server, pending, "message.riftgun.entity_relocation_failed");
            return;
        }
        if (owner != null) PortalSounds.playShot(owner, pending.sounds());
        PortalSounds.playOpening(sourceLevel, center, pending.sounds());
        ACTIVE.add(new Transaction(pending.reservation(), pending.ownerId(), pending.targetId(),
            pending.sourceDimension(), gun, destination, pending.profile(), side,
            pending.sounds(), pending.fallGuard(), pending.entityFallGuard(),
            pending.crises(), pending.privacyReservations(),
            visual.getUUID(), exit, route, pending.openingTicks(), pending.fuelQuote(),
            pending.tree(), now));
        TransitDiagnostics.relocation("animation started reservation={} target={} source={} destination={} exitCenter={} openingTicks={} visual={}",
            pending.reservation().id(), pending.targetId(),
            pending.sourceDimension().location(), destination.dimension().location(),
            route.exitCenter(), pending.openingTicks(), visual.getUUID());
    }

    private static @Nullable String preparedAbortReason(
            MinecraftServer server, PendingPreparation pending, @Nullable Entity target,
            @Nullable ResolvedDestination destination, @Nullable ServerLevel targetLevel,
            ItemStack gun) {
        if (target == null) return "target_missing";
        if (destination == null) return "destination_missing";
        if (targetLevel == null) return "destination_level_missing";
        if (!targetLevel.isPositionEntityTicking(BlockPos.containing(destination.position()))) {
            return "destination_not_entity_ticking";
        }
        if (!pending.sourceDimension().equals(destination.dimension())
            && !pending.profile().crossDimension()) return "cross_dimension_denied";
        if (!permissionsStillValid(server, pending)) return "permission_invalid";
        if (!reservationFuelAvailable(pending, gun)) return "gun_or_fuel_invalid";
        return null;
    }

    private static @Nullable ResolvedDestination refreshPreparedDestination(
            MinecraftServer server, @Nullable ServerPlayer owner,
            ResolvedDestination original) {
        if (original.playerId() != null) return original.refresh(server);
        if (owner == null || original.savedDestinationId() == null) return null;
        Destination saved = PortalDataStore.load(owner).destination(original.savedDestinationId())
            .orElse(null);
        return saved == null ? null : ResolvedDestination.saved(saved);
    }

    private static void abortPreparation(MinecraftServer server, PendingPreparation pending,
                                         String messageKey) {
        TransitDiagnostics.warning("relocation preparation aborted reservation={} message={}",
            pending.reservation().id(), messageKey);
        pending.preparation().close();
        registry().fail(pending.reservation());
        releasePrivacyGrants(server, pending.privacyReservations());
        ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerId());
        if (owner != null) message(owner, messageKey);
    }

    private static boolean permissionsStillValid(MinecraftServer server,
                                                  PendingPreparation pending) {
        if (!pending.privacyReservations().stream().allMatch(
            reservation -> PortalPrivacyService.reservationValid(server, reservation))) {
            return false;
        }
        for (PermissionSnapshot permission : pending.permissions()) {
            if (permission.oneShot()) continue;
            ServerPlayer target = server.getPlayerList().getPlayer(permission.targetId());
            if (target == null || !PortalPrivacyService.allowsWithoutRequest(
                target, pending.ownerId(), permission.purpose())) return false;
        }
        return true;
    }

    private static boolean reservationFuelAvailable(PendingPreparation pending, ItemStack gun) {
        PortalGunTank tank = new PortalGunTank(gun);
        return !gun.isEmpty()
            && PortalFuelProfiles.resolve(tank.getFluid())
                .filter(profile -> profile.id().equals(pending.profile().id())).isPresent()
            && tank.getFluid().getAmount() >= registry().reservedFuel(
                pending.reservation().gunId());
    }

    private static boolean isPreparing(UUID targetId) {
        return PREPARING.stream().anyMatch(pending -> pending.targetId().equals(targetId));
    }

    private static boolean preparingMessageShown(UUID targetId) {
        return PREPARING.stream().filter(pending -> pending.targetId().equals(targetId))
            .anyMatch(pending -> pending.preparation().preparingMessageShown());
    }

    private static void completeLoaded(MinecraftServer server, Transaction tx, long now) {
        ServerLevel sourceLevel = server.getLevel(tx.sourceDimension());
        Entity target = liveTarget(sourceLevel, tx.targetId());
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

        if (!tx.tree().matches(target)) {
            fail(server, tx, "message.riftgun.entity_relocation_tree_incomplete");
            return;
        }
        PreparedRoute route = refreshRoute(
            target, tx.tree(), destination, tx.preparedRoute().crisis());
        Vec3 sourcePosition = target.position();
        freezeSource(sourceLevel, tx.visualId());
        long teleportStarted = TransitDiagnostics.enabled() ? System.nanoTime() : 0L;
        TransitDiagnostics.relocation("teleport before reservation={} target={} from={} pos={} to={} pos={} chunkTicking={}",
            tx.reservation().id(), target.getUUID(),
            sourceLevel.dimension().location(), sourcePosition, targetLevel.dimension().location(),
            route.outputPosition(), targetLevel.isPositionEntityTicking(BlockPos.containing(route.outputPosition())));
        if (!targetLevel.noCollision(
            tx.tree().destinationEnvelope(target, route.outputPosition()).deflate(0.001))) {
            TransitDiagnostics.warning("relocation tree clearance failed reservation={} target={} members={} destination={} envelope={}",
                tx.reservation().id(), target.getUUID(), tx.tree().size(),
                targetLevel.dimension().location(),
                tx.tree().destinationEnvelope(target, route.outputPosition()));
            fail(server, tx);
            return;
        }
        EntityRelocationTree.Transfer transfer = tx.tree().transfer(
            target, targetLevel, route.outputPosition(), route.momentum());
        Entity moved = transfer.root();
        TransitDiagnostics.relocation("teleport after reservation={} result={} elapsedMs={} resultDimension={} resultPos={}",
            tx.reservation().id(), moved != null,
            TransitDiagnostics.enabled()
                ? (System.nanoTime() - teleportStarted) / 1_000_000.0 : 0.0,
            moved == null ? "null" : moved.level().dimension().location(),
            moved == null ? "null" : moved.position());
        if (moved == null || !transfer.complete()) {
            TransitDiagnostics.warning("relocation tree transfer incomplete reservation={} target={} expectedMembers={} movedMembers={}",
                tx.reservation().id(), target.getUUID(), tx.tree().size(), transfer.members().size());
            fail(server, tx, "message.riftgun.entity_relocation_tree_incomplete");
            return;
        }
        TransitDiagnostics.trackPostcondition(
            moved, tx.sourceDimension(), route.outputPosition(), "relocation", now);
        closeExit(server, tx.exitSetup(), true);
        finishSuccessful(server, sourceLevel, targetLevel, sourcePosition, moved,
            transfer.members(), tx, use,
            route.momentum(), route.crisis(), now);
    }

    private static void finishSuccessful(MinecraftServer server, ServerLevel sourceLevel,
                                         ServerLevel targetLevel, Vec3 sourcePosition, Entity moved,
                                         List<Entity> movedMembers, Transaction tx,
                                         PortalFuelUse use, Vec3 momentum,
                                         @Nullable PortalCrisisPlan crisis, long now) {
        if (!PortalFuelManager.consume(tx.gun(), use)) {
            ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
            if (owner != null) message(owner, "message.riftgun.entity_relocation_failed");
        }
        moved.setDeltaMovement(momentum);
        moved.hasImpulse = true;
        for (Entity member : movedMembers) {
            if (member != moved) member.setDeltaMovement(Vec3.ZERO);
            if (member instanceof Projectile projectile) {
                ProjectileMotion.alignToVelocity(projectile, member.getDeltaMovement());
                PortalProjectileState.recordSuccessfulTransit(projectile);
            }
            if (dev.riftgun.portal.PortalFallGuardPolicy.applies(
                member, tx.fallGuard(), tx.entityFallGuard())) member.fallDistance = 0.0F;
        }
        if (moved instanceof ServerPlayer player && crisis != null) {
            PortalCrisisCoordinator.apply(crisis, player);
        }
        EntityRelocationExitImmunity.registerTree(moved, now,
            ServerConfig.VALUES.entityRelocationExitPortalImmunityTicks.get());
        if (moved instanceof Projectile) {
            Entity sourceVisual = sourceLevel.getEntity(tx.visualId());
            if (sourceVisual instanceof EntityRelocationPortalEntity sourcePortal
                && sourcePortal.claimProjectileEffect(now)) {
                PortalSounds.playTransit(sourceLevel, sourcePosition, tx.sounds());
            }
            EntityRelocationPortalEntity exitPortal = resolveExit(server, tx.exitSetup());
            if (exitPortal == null || exitPortal.claimProjectileEffect(now)) {
                PortalSounds.playTransit(targetLevel, moved.position(), tx.sounds());
            }
        } else {
            PortalSounds.playTransit(sourceLevel, sourcePosition, tx.sounds());
            PortalSounds.playTransit(targetLevel, moved.position(), tx.sounds());
        }
        registry().complete(tx.reservation(), now);
        TransitDiagnostics.relocation("completed reservation={} target={} destination={} now={}",
            tx.reservation().id(), moved.getUUID(),
            moved.level().dimension().location(), now);
    }

    private static void fail(MinecraftServer server, Transaction tx) {
        fail(server, tx, "message.riftgun.entity_relocation_failed");
    }

    private static void fail(MinecraftServer server, Transaction tx, String messageKey) {
        TransitDiagnostics.warning("relocation failed reservation={} target={} source={} destination={}",
            tx.reservation().id(), tx.targetId(),
            tx.sourceDimension().location(), tx.destination().dimension().location());
        registry().fail(tx.reservation());
        releasePrivacyGrants(server, tx.privacyReservations());
        closeExit(server, tx.exitSetup(), false);
        ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
        if (owner != null) message(owner, messageKey);
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

    private static @Nullable EntityRelocationExitService.Handle prepareExit(
            MinecraftServer server, ServerLevel targetLevel, ResolvedDestination destination,
            PreparedRoute route, float side, int rgb, PortalSoundSnapshot sounds,
            int openingTicks) {
        int durationTicks = PortalOpenDuration.ticks(
            ServerConfig.VALUES.entityRelocationExitDurationSeconds.get());
        ServerPlayer followPlayer = null;
        if (destination.playerId() != null
            && (route.crisis() == null || route.crisis().relocation() == null)) {
            followPlayer = destination.resolvePlayer(server);
            if (followPlayer == null || followPlayer.serverLevel() != targetLevel) return null;
        }
        EntityRelocationExitIndex.DestinationKey key = route.shareable()
            ? destination.sharedKey() : null;
        return EntityRelocationExitService.open(server, new EntityRelocationExitService.OpenRequest(
            targetLevel, followPlayer, route.exitCenter(), side, rgb, durationTicks, sounds,
            route.exitOrientation(), route.exitYaw(), openingTicks, key));
    }

    private static PreparedRoute prepareRoute(Entity target, EntityRelocationTree tree,
                                              ResolvedDestination destination,
                                              ServerLevel targetLevel,
                                              PortalCrisisConfigurationSnapshot crises) {
        PreparedRoute normal = normalRoute(target, tree, destination, null);
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

    private static PreparedRoute refreshRoute(Entity target, EntityRelocationTree tree,
                                              ResolvedDestination destination,
                                              @Nullable PortalCrisisPlan crisis) {
        if (crisis != null && crisis.relocation() != null) {
            PortalCrisisPlan.Relocation relocation = crisis.relocation();
            return new PreparedRoute(destination.dimension(), relocation.exitPlacement().center(),
                relocation.exitPlacement().orientation(), relocation.exitPlacement().yaw(),
                relocation.destination(), relocation.momentum(), relocation.destination(), crisis, false);
        }
        return normalRoute(target, tree, destination, crisis);
    }

    private static PreparedRoute normalRoute(Entity target, EntityRelocationTree tree,
                                             ResolvedDestination destination,
                                             @Nullable PortalCrisisPlan crisis) {
        double treeHeight = tree.metrics(target).height();
        if (destination.playerId() != null) {
            return new PreparedRoute(destination.dimension(), destination.playerExitCenter(),
                PortalOrientation.BOTTOM, destination.yaw(),
                destination.position().add(0.0, 0.15, 0.0),
                relocationMomentum(target, target.getDeltaMovement(),
                    PortalOrientation.BOTTOM, destination.yaw()),
                destination.position(), crisis, false);
        }
        Vec3 exitCenter = EntityRelocationGeometry.savedDestinationBottomExitCenter(
            destination.position(), treeHeight);
        return new PreparedRoute(destination.dimension(), exitCenter, PortalOrientation.BOTTOM,
            destination.yaw(), EntityRelocationGeometry.bottomOutputPosition(
                exitCenter, treeHeight), relocationMomentum(
                    target, target.getDeltaMovement(), PortalOrientation.BOTTOM, destination.yaw()),
            destination.position(), crisis, true);
    }

    private static @Nullable Entity liveTarget(@Nullable ServerLevel sourceLevel, UUID targetId) {
        Entity raw = sourceLevel == null ? null : sourceLevel.getEntity(targetId);
        return raw != null && raw.isAlive() && isRelocatableType(raw)
            && !raw.isPassenger() ? raw : null;
    }

    private static boolean dimensionAllowed(Transaction tx, ResolvedDestination destination) {
        return tx.sourceDimension().equals(destination.dimension()) || tx.profile().crossDimension();
    }

    private static PortalFuelUse fuelUse(Transaction tx, Entity target) {
        int amount = tx.fuelQuote().cost(() -> PortalFuelCost.choose(
            tx.profile().minimumConsumption(), tx.profile().maximumConsumption(),
            ServerConfig.VALUES.randomConsumption.get(), target.getRandom()::nextInt));
        return new PortalFuelUse(tx.profile(), amount);
    }

    private static EntityRelocationFuelPolicy.Multipliers relocationFuelMultipliers() {
        return new EntityRelocationFuelPolicy.Multipliers(
            ServerConfig.VALUES.passiveRelocationFuelMultiplier.get(),
            ServerConfig.VALUES.hostileRelocationFuelMultiplier.get(),
            ServerConfig.VALUES.playerRelocationFuelMultiplier.get(),
            ServerConfig.VALUES.bossRelocationFuelMultiplier.get(),
            ServerConfig.VALUES.projectileRelocationFuelMultiplier.get(),
            ServerConfig.VALUES.utilityRelocationFuelMultiplier.get());
    }

    private static Vec3 upwardMomentum(Vec3 momentum) {
        return momentum.y < 0.12 ? new Vec3(momentum.x, 0.12, momentum.z) : momentum;
    }

    private static Vec3 downwardMomentum(Vec3 momentum) {
        return momentum.y > -0.12 ? new Vec3(momentum.x, -0.12, momentum.z) : momentum;
    }

    private static Vec3 relocationMomentum(Entity target, Vec3 momentum,
                                           PortalOrientation exitOrientation, float exitYaw) {
        if (target instanceof Projectile) {
            return EntityRelocationProjectileMotion.exitVelocity(
                momentum, exitOrientation, exitYaw);
        }
        return exitOrientation == PortalOrientation.BOTTOM
            ? downwardMomentum(momentum) : upwardMomentum(momentum);
    }

    private static Vec3 exitCenter(MinecraftServer server,
                                   @Nullable EntityRelocationExitService.Handle setup,
                                   Vec3 fallback) {
        EntityRelocationPortalEntity portal = resolveExit(server, setup);
        return portal == null ? fallback : portal.position();
    }

    private static void closeExit(MinecraftServer server,
                                  @Nullable EntityRelocationExitService.Handle setup,
                                   boolean successful) {
        EntityRelocationExitService.close(server, setup, successful);
    }

    private static @Nullable EntityRelocationPortalEntity resolveExit(
            MinecraftServer server, @Nullable EntityRelocationExitService.Handle setup) {
        return EntityRelocationExitService.resolve(server, setup);
    }

    private static void notifyUnavailablePlayerDestination(MinecraftServer server, Transaction tx) {
        if (tx.destination().playerId() == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(tx.ownerId());
        if (owner != null) owner.displayClientMessage(Component.translatable(
            "message.riftgun.player_destination_unavailable"), false);
    }

    static void unregisterExit(UUID portalId) {
        EntityRelocationExitService.unregister(portalId);
    }

    private static Optional<Entity> findTarget(ServerPlayer owner, PortalGunCapabilities capabilities) {
        double range = capabilities.configuredSurfaceRange();
        Vec3 start = owner.getEyePosition();
        Vec3 end = start.add(owner.getLookAngle().scale(range));
        HitResult block = owner.pick(range, 0.0F, false);
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB search = owner.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        Vec3 finalEnd = end;
        return owner.level().getEntities(owner, search, entity ->
                entity != owner && entity.isAlive() && !entity.isSpectator()
                    && isRelocatableType(entity)
                    && entity.getBoundingBox().inflate(0.3).clip(start, finalEnd).isPresent())
            .stream()
            .sorted(java.util.Comparator.comparingDouble(entity ->
                entity.getBoundingBox().clip(start, finalEnd)
                    .orElse(entity.position()).distanceToSqr(start)))
            .map(EntityRelocationTree::promotedRoot)
            .distinct()
            .filter(root -> treeEligible(root, capabilities))
            .findFirst();
    }

    private static boolean isRelocatableType(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof Projectile
            || entity instanceof ItemEntity || entity instanceof VehicleEntity;
    }

    private static boolean treeEligible(Entity root, PortalGunCapabilities capabilities) {
        if (root.isPassenger()) return false;
        EntityRelocationTree tree = EntityRelocationTree.capture(root);
        if (tree.size() > ServerConfig.VALUES.maximumPassengerTreeSize.get()) return false;
        if (tree.size() > 1 && !ServerConfig.VALUES.enablePassengerTreeRelocation.get()) return false;
        for (Entity member : tree.members(root)) {
            if (!member.isAlive() || member.isSpectator() || !isRelocatableType(member)) return false;
            if (member instanceof Projectile projectile && !PortalProjectileState.canTransit(projectile)) {
                return false;
            }
            if (member instanceof Player) {
                if (!capabilities.playerTarget()) return false;
            } else if (!(member instanceof ItemEntity) && !(member instanceof VehicleEntity)
                && !capabilities.entityAccess().allows(member)) {
                return false;
            }
        }
        return true;
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
        if (registry == null || ACTIVE.isEmpty() && PREPARING.isEmpty()
            && (maximum != configuredMaximum || cooldown != configuredCooldown)) {
            configuredMaximum = maximum;
            configuredCooldown = cooldown;
            registry = new EntityRelocationRegistry(maximum, cooldown);
        }
        return registry;
    }

    public static void reset() {
        for (PendingPreparation pending : PREPARING) pending.preparation().close();
        PREPARING.clear();
        ACTIVE.clear();
        registry = null;
        EntityRelocationExitService.clear();
    }

    public static void cancelAll(MinecraftServer server) {
        for (PendingPreparation pending : PREPARING) {
            pending.preparation().close();
            registry().fail(pending.reservation());
            releasePrivacyGrants(server, pending.privacyReservations());
        }
        PREPARING.clear();
        for (Transaction tx : ACTIVE) {
            registry().fail(tx.reservation());
            releasePrivacyGrants(server, tx.privacyReservations());
            closeExit(server, tx.exitSetup(), false);
        }
        ACTIVE.clear();
    }

    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private record Transaction(EntityRelocationRegistry.Reservation reservation, UUID ownerId,
                               UUID targetId, net.minecraft.resources.ResourceKey<Level> sourceDimension,
                               ItemStack gun, ResolvedDestination destination, PortalFuelProfile profile,
                               float side, PortalSoundSnapshot sounds, boolean fallGuard,
                               boolean entityFallGuard,
                               PortalCrisisConfigurationSnapshot crises,
                               List<PortalPrivacyService.GrantReservation> privacyReservations,
                               UUID visualId, @Nullable EntityRelocationExitService.Handle exitSetup,
                               @Nullable PreparedRoute preparedRoute,
                               int openingTicks,
                               EntityRelocationFuelPolicy.Quote fuelQuote,
                               EntityRelocationTree tree,
                               long startedAt) {}

    private record PendingPreparation(
        EntityRelocationRegistry.Reservation reservation, UUID ownerId, UUID targetId,
        ResourceKey<Level> sourceDimension,
        net.minecraft.nbt.CompoundTag gunReference, ResolvedDestination destination,
        PortalFuelProfile profile, PortalSoundSnapshot sounds, boolean fallGuard,
        boolean entityFallGuard,
        PortalCrisisConfigurationSnapshot crises,
        List<PortalPrivacyService.GrantReservation> privacyReservations,
        List<PermissionSnapshot> permissions, int openingTicks,
        EntityRelocationFuelPolicy.Quote fuelQuote,
        EntityRelocationTree tree,
        EntityRelocationPreparation preparation
    ) {}

    private record PermissionSnapshot(UUID targetId, PortalRequestPurpose purpose,
                                      boolean oneShot) {}

    private record PreparedRoute(ResourceKey<Level> dimension, Vec3 exitCenter,
                                 PortalOrientation exitOrientation, float exitYaw,
                                 Vec3 outputPosition, Vec3 momentum, Vec3 landingPosition,
                                 @Nullable PortalCrisisPlan crisis, boolean shareable) {}

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
