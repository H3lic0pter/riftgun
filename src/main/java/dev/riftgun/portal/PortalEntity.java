package dev.riftgun.portal;

import dev.riftgun.RiftGun;
import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.service.PortalPlacementResult;
import dev.riftgun.service.PortalServices;
import dev.riftgun.service.PortalSupportArea;
import dev.riftgun.module.PortalEntityAccessSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PortalEntity extends Entity {
    public static final float DEPTH = (float) PortalPlacement.DEPTH;

    /** Blend width for the exit-facing adjustment, in look·normal units. */
    private static final float FACING_THRESHOLD = 0.35F;

    private static final EntityDataAccessor<Integer> PHASE =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICKS =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORIENTATION =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GEOMETRY =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUEL_RGB =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> FUEL_ID =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.STRING);
    private static final TicketType<BlockPos> PORTAL_TICKET =
        TicketType.create("riftgun_portal", Vec3i::compareTo);

    private @Nullable UUID linkedPortalId;
    private @Nullable ResourceKey<Level> linkedDimension;
    private @Nullable UUID ownerId;
    private @Nullable UUID excludedPlayerId;
    private @Nullable UUID deferredExitExclude;
    private boolean exitPortal;
    private @Nullable BlockPos anchor;
    private @Nullable Direction anchorFace;
    private @Nullable PortalExitTarget deferredTarget;
    private final PortalTransitGate transitGate = new PortalTransitGate();
    private boolean creatingDeferredExit;
    private boolean waitingForLinkedOpen;
    private boolean synchronizePairOnOpen;
    private boolean closingPair;
    private @Nullable BlockPos ticketPosition;
    private boolean ticketHeld;
    private PortalEntityAccessSnapshot entityAccess = PortalEntityAccessSnapshot.NONE;
    private int openDurationTicks = PortalOpenDuration.ticks(PortalOpenDuration.DEFAULT_SECONDS);
    private int transitCooldownTicks = 20;
    private boolean fallGuard;
    private double horizontalTriggerExtend;
    private PortalAperture aperture = PortalAperture.STANDARD;
    private long lifecycleStartedAt;
    private long closeStartedAt = -1L;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static boolean openPair(ServerPlayer player, PortalPairPlacement pair,
                                   PortalFuelProfile fuel, PortalRuntimeOptions options,
                                   PortalExclusions exclusions, BooleanSupplier commitFuel) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        ServerLevel entryLevel = player.serverLevel();
        ServerLevel exitLevel = server.getLevel(pair.exitDimension());
        if (exitLevel == null) return false;

        long startedAt = server.overworld().getGameTime();
        PortalEntity entry = create(entryLevel, player.getUUID(), pair.entry(),
            fuel.rgb(), fuel.id().toString(), options, startedAt,
            exclusions.entryPlayerId(), false);
        PortalEntity exit = create(exitLevel, player.getUUID(), pair.exit(),
            fuel.rgb(), fuel.id().toString(), options, startedAt,
            exclusions.exitPlayerId(), true);
        link(entry, exit);
        entry.acquireChunkTicket();
        exit.acquireChunkTicket();

        boolean entryAdded = entryLevel.addFreshEntity(entry);
        boolean exitAdded = exitLevel.addFreshEntity(exit);
        if (!entryAdded || !exitAdded || !commitFuel.getAsBoolean()) {
            removeFailedOpen(entry, entryAdded);
            removeFailedOpen(exit, exitAdded);
            return false;
        }

        closeOwnedPortals(server, player.getUUID(), Set.of(entry.getUUID(), exit.getUUID()));
        playOpeningSounds(entryLevel, pair.entry());
        return true;
    }

    public static boolean openDeferredExit(ServerPlayer player, PortalPlacement placement,
                                           PortalFuelProfile fuel, PortalExitTarget target,
                                           PortalRuntimeOptions options, PortalExclusions exclusions,
                                           BooleanSupplier commitFuel) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        ServerLevel entryLevel = player.serverLevel();
        PortalEntity entry = create(entryLevel, player.getUUID(), placement,
            fuel.rgb(), fuel.id().toString(), options, server.overworld().getGameTime(),
            exclusions.entryPlayerId(), false);
        entry.deferredTarget = target;
        entry.deferredExitExclude = exclusions.exitPlayerId();
        entry.acquireChunkTicket();
        boolean added = entryLevel.addFreshEntity(entry);
        if (!added || !commitFuel.getAsBoolean()) {
            removeFailedOpen(entry, added);
            return false;
        }

        closeOwnedPortals(server, player.getUUID(), Set.of(entry.getUUID()));
        playOpeningSounds(entryLevel, placement);
        return true;
    }

    private static void removeFailedOpen(PortalEntity portal, boolean added) {
        if (added) portal.discard();
        else portal.releaseChunkTicket();
    }

    private static void playOpeningSounds(ServerLevel level, PortalPlacement placement) {
        level.playSound(null, placement.center().x, placement.center().y, placement.center().z,
            SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.7F, 1.15F);
        level.playSound(null, placement.center().x, placement.center().y, placement.center().z,
            SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.25F, 1.55F);
    }

    private static void link(PortalEntity entry, PortalEntity exit) {
        entry.linkedPortalId = exit.getUUID();
        entry.linkedDimension = exit.level().dimension();
        exit.linkedPortalId = entry.getUUID();
        exit.linkedDimension = entry.level().dimension();
    }

    private void acquireChunkTicket() {
        if (ticketHeld || !(level() instanceof ServerLevel serverLevel)) return;
        BlockPos position = blockPosition();
        serverLevel.getChunk(position.getX() >> 4, position.getZ() >> 4);
        serverLevel.getChunkSource().addRegionTicket(
            PORTAL_TICKET, new ChunkPos(position), 3, position, true);
        ticketPosition = position;
        ticketHeld = true;
    }

    private static PortalEntity create(ServerLevel level, @Nullable UUID owner,
                                       PortalPlacement placement, int fuelRgb, String fuelId,
                                       PortalRuntimeOptions options, long startedAt,
                                       @Nullable UUID excludedPlayerId, boolean exitPortal) {
        PortalEntity portal = new PortalEntity(RiftGun.PORTAL.get(), level);
        portal.ownerId = owner;
        portal.excludedPlayerId = excludedPlayerId;
        portal.setPos(placement.center());
        portal.setYRot(placement.yaw());
        portal.setYHeadRot(placement.yaw());
        portal.entityData.set(ORIENTATION, placement.orientation().ordinal());
        portal.entityData.set(GEOMETRY, placement.geometry().ordinal());
        portal.anchor = placement.anchor();
        portal.anchorFace = placement.anchorFace();
        portal.entityData.set(FUEL_RGB, fuelRgb);
        portal.entityData.set(FUEL_ID, fuelId);
        portal.entityAccess = options.entityAccess();
        portal.openDurationTicks = options.openDurationTicks();
        portal.transitCooldownTicks = options.transitCooldownTicks();
        portal.fallGuard = options.fallGuard();
        portal.exitPortal = exitPortal;
        portal.horizontalTriggerExtend = options.horizontalTriggerExtend();
        portal.aperture = options.aperture();
        portal.lifecycleStartedAt = startedAt;
        return portal;
    }

    public static void closeOwnedPortals(MinecraftServer server, UUID owner) {
        closeOwnedPortals(server, owner, Set.of());
    }

    private static void closeOwnedPortals(MinecraftServer server, UUID owner, Set<UUID> excluded) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PortalEntity portal && owner.equals(portal.ownerId)
                    && !excluded.contains(portal.getUUID())) portal.startClosing();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, PortalLifecycle.Phase.CHARGING.ordinal());
        builder.define(PHASE_TICKS, 0);
        builder.define(ORIENTATION, PortalOrientation.VERTICAL.ordinal());
        builder.define(GEOMETRY, PortalGeometry.FLOATING_VERTICAL.ordinal());
        builder.define(FUEL_RGB, PortalFuelProfiles.DIMENSIONAL_RGB);
        builder.define(FUEL_ID, "riftgun:dimensional_portal_fluid");
    }

    public PortalLifecycle.Phase phase() {
        return PortalLifecycle.Phase.byOrdinal(entityData.get(PHASE));
    }

    public int phaseTicks() {
        return entityData.get(PHASE_TICKS);
    }

    public int openDurationTicks() {
        return openDurationTicks;
    }

    public PortalOrientation orientation() {
        return PortalOrientation.byOrdinal(entityData.get(ORIENTATION));
    }

    public PortalGeometry geometry() {
        return PortalGeometry.byOrdinal(entityData.get(GEOMETRY));
    }

    public int fuelRgb() {
        return entityData.get(FUEL_RGB);
    }

    public String fuelId() {
        return entityData.get(FUEL_ID);
    }

    public float portalWidth() {
        return geometry().width();
    }

    public float portalHeight() {
        return geometry().height();
    }

    public Vec3 normal() {
        return orientation().normal(getYRot());
    }

    public Vec3 up() {
        return orientation().up(getYRot());
    }

    public Vec3 right() {
        return orientation().right(getYRot());
    }

    public PortalPlacement placement() {
        return new PortalPlacement(position(), orientation(), geometry(), getYRot(), anchor, anchorFace);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (!ticketHeld) acquireChunkTicket();

        long now = serverTime();
        PortalLifecycle.Phase nextPhase = PortalPairClock.phase(lifecycleStartedAt, closeStartedAt, now);
        int nextPhaseTicks = PortalPairClock.phaseTicks(lifecycleStartedAt, closeStartedAt, now);
        entityData.set(PHASE, nextPhase.ordinal());
        entityData.set(PHASE_TICKS, nextPhaseTicks);

        if (nextPhase == PortalLifecycle.Phase.CLOSED) {
            releaseChunkTicket();
            discard();
            return;
        }
        if (tickCount % 5 == 0 && !anchorStillValid()) {
            startClosing();
            return;
        }
        if (nextPhase != PortalLifecycle.Phase.OPEN) return;

        if (synchronizePairOnOpen) {
            synchronizeOpenPair(now);
            return;
        }
        if (waitingForLinkedOpen) {
            if (linkedPortal() == null) startClosing();
            return;
        }
        if (deferredTarget != null && targetLevel() == null) {
            startClosing();
            return;
        }
        if (PortalServices.CLOSE_POLICY.shouldClose(this)) {
            startClosing();
        } else {
            teleportTouchingEntities();
        }
    }

    private void synchronizeOpenPair(long now) {
        PortalEntity linked = linkedPortal();
        if (linked == null) {
            startClosing();
            return;
        }
        long openStartedAt = PortalPairClock.openPhaseStartedAt(now);
        resetOpenClock(this, openStartedAt);
        resetOpenClock(linked, openStartedAt);
    }

    private static void resetOpenClock(PortalEntity portal, long startedAt) {
        portal.lifecycleStartedAt = startedAt;
        portal.closeStartedAt = -1L;
        portal.waitingForLinkedOpen = false;
        portal.synchronizePairOnOpen = false;
        portal.entityData.set(PHASE, PortalLifecycle.Phase.OPEN.ordinal());
        portal.entityData.set(PHASE_TICKS, 0);
    }

    private @Nullable ServerLevel targetLevel() {
        if (deferredTarget == null || !(level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getServer().getLevel(deferredTarget.dimension());
    }

    private void releaseChunkTicket() {
        if (!ticketHeld || ticketPosition == null || !(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.getChunkSource().removeRegionTicket(
            PORTAL_TICKET, new ChunkPos(ticketPosition), 3, ticketPosition, true);
        ticketHeld = false;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseChunkTicket();
        super.remove(reason);
    }

    private boolean anchorStillValid() {
        if (anchor == null || anchorFace == null || !(level() instanceof ServerLevel serverLevel)) return true;
        if (geometry().expanded() && !PortalSupportArea.hasFullExpandedSupport(serverLevel, placement())) {
            return false;
        }
        if (serverLevel.getBlockState(anchor).getCollisionShape(serverLevel, anchor).isEmpty()) return false;
        return !serverLevel.getBlockCollisions(null, placement().bounds().deflate(0.002)).iterator().hasNext();
    }

    private void teleportTouchingEntities() {
        long now = serverTime();
        AABB search = placement().bounds().inflate(0.6, 2.0, 0.6);
        PortalTransitEligibility eligibility = new PortalTransitEligibility(
            placement(), entityAccess, ownerId, excludedPlayerId, exitPortal, horizontalTriggerExtend);
        List<Entity> touching = level().getEntities(this, search, eligibility::allows);
        Set<UUID> touchingIds = new HashSet<>(touching.size());
        for (Entity entity : touching) touchingIds.add(entity.getUUID());
        transitGate.retainInside(touchingIds, now, transitCooldownTicks);

        PortalEntity target = linkedPortal();
        if (target != null) {
            if (target.phase() != PortalLifecycle.Phase.OPEN) return;
            for (Entity entity : touching) {
                if (!transitGate.enter(entity.getUUID(), now, transitCooldownTicks)) continue;
                teleportTree(entity, target);
            }
            return;
        }

        if (deferredTarget == null || creatingDeferredExit) return;
        for (Entity entity : touching) {
            if (!transitGate.enter(entity.getUUID(), now, transitCooldownTicks)) continue;
            teleportDeferredTree(entity);
            return;
        }
    }

    private @Nullable Entity teleportTree(Entity root, PortalEntity target) {
        var passengers = new ArrayList<>(root.getPassengers());
        root.ejectPassengers();
        Entity movedRoot = teleportSingle(root, target);
        if (movedRoot == null) {
            for (Entity passenger : passengers) passenger.startRiding(root, true);
            transitGate.leave(root.getUUID());
            return null;
        }
        // Mark both doors so a large body that spans the two portals is not immediately
        // pulled back through the one it just left.
        long now = serverTime();
        transitGate.markInside(movedRoot.getUUID(), now, transitCooldownTicks);
        target.transitGate.markInside(movedRoot.getUUID(), now, transitCooldownTicks);
        for (Entity passenger : passengers) {
            Entity movedPassenger = teleportTree(passenger, target);
            if (movedPassenger != null) movedPassenger.startRiding(movedRoot, true);
        }
        target.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS, 0.6F, 1.4F);
        return movedRoot;
    }

    private @Nullable Entity teleportSingle(Entity entity, PortalEntity target) {
        Vec3 momentum = transformVector(entity.getDeltaMovement(), target);
        double outwardSpeed = momentum.dot(target.normal());
        if (outwardSpeed < 0.12) momentum = momentum.add(target.normal().scale(0.12 - outwardSpeed));

        Vec3 look = transformVector(entity.getLookAngle(), target).normalize();
        if (entity instanceof Player) {
            // Backing through the entrance (back to the door) should exit facing the
            // portal, mirrored so entering from the portal's left exits from the target's
            // right. Walk-in players keep the standard away-facing exit unchanged.
            float dot = (float) entity.getLookAngle().normalize().dot(normal());
            if (dot > 0.0F) {
                float t = Mth.clamp(dot / FACING_THRESHOLD, 0.0F, 1.0F);
                Vec3 mirrored = PortalTransform.betweenFactors(entity.getLookAngle(), orientation(), getYRot(),
                    target.orientation(), target.getYRot(), -1.0F, 1.0F).normalize();
                Vec3 flipped = PortalTransform.betweenFactors(entity.getLookAngle(), orientation(), getYRot(),
                    target.orientation(), target.getYRot(), -1.0F, -1.0F).normalize();
                look = mirrored.lerp(flipped, t).normalize();
            }
        }
        float newYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float newPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0, 1.0)));
        Vec3 destination = target.outputPosition(entity);

        ServerLevel targetLevel = (ServerLevel) target.level();
        Entity moved;
        if (entity.level() == targetLevel) {
            boolean successful = entity.teleportTo(targetLevel, destination.x, destination.y, destination.z,
                Set.<RelativeMovement>of(), newYaw, newPitch);
            moved = successful ? entity : null;
        } else {
            moved = entity.changeDimension(new DimensionTransition(targetLevel, destination, momentum,
                newYaw, newPitch, DimensionTransition.DO_NOTHING));
        }
        if (moved != null) {
            moved.setDeltaMovement(momentum);
            moved.hasImpulse = true;
            if (fallGuard) moved.fallDistance = 0.0F;
            if (moved instanceof ServerPlayer player) PortalServices.MOTION_HISTORY.reset(player);
        }
        return moved;
    }

    private void teleportDeferredTree(Entity root) {
        PortalExitTarget target = deferredTarget;
        ServerLevel targetLevel = targetLevel();
        if (target == null || targetLevel == null) {
            transitGate.leave(root.getUUID());
            return;
        }

        creatingDeferredExit = true;
        if (targetLevel.isPositionEntityTicking(BlockPos.containing(target.position()))) {
            PortalEntity exit = createDeferredExit(targetLevel, target, List.of());
            transitGate.leave(root.getUUID());
            if (exit == null) warnExitGenerationFailure(targetLevel.getServer(), List.of());
            creatingDeferredExit = false;
            return;
        }

        List<Entity> movedEntities = new ArrayList<>();
        Entity movedRoot = teleportBootstrapTree(root, targetLevel, target, movedEntities);
        if (movedRoot == null) {
            transitGate.leave(root.getUUID());
            warnTeleportFailure(targetLevel.getServer(), root);
            creatingDeferredExit = false;
            return;
        }

        PortalEntity exit = createDeferredExit(targetLevel, target, movedEntities);
        if (exit == null) warnExitGenerationFailure(targetLevel.getServer(), movedEntities);
        targetLevel.playSound(null, movedRoot.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS, 0.6F, 1.4F);
        creatingDeferredExit = false;
    }

    private @Nullable Entity teleportBootstrapTree(Entity root, ServerLevel targetLevel,
                                                   PortalExitTarget target, List<Entity> movedEntities) {
        var passengers = new ArrayList<>(root.getPassengers());
        root.ejectPassengers();
        Entity movedRoot = teleportBootstrapSingle(root, targetLevel, target);
        if (movedRoot == null) {
            for (Entity passenger : passengers) passenger.startRiding(root, true);
            return null;
        }
        movedEntities.add(movedRoot);
        for (Entity passenger : passengers) {
            Entity movedPassenger = teleportBootstrapTree(passenger, targetLevel, target, movedEntities);
            if (movedPassenger != null) movedPassenger.startRiding(movedRoot, true);
        }
        return movedRoot;
    }

    private @Nullable Entity teleportBootstrapSingle(Entity entity, ServerLevel targetLevel,
                                                      PortalExitTarget target) {
        Entity moved;
        if (entity.level() == targetLevel) {
            boolean successful = entity.teleportTo(targetLevel,
                target.position().x, target.position().y, target.position().z,
                Set.<RelativeMovement>of(), target.yaw(), entity.getXRot());
            moved = successful ? entity : null;
        } else {
            moved = entity.changeDimension(new DimensionTransition(
                targetLevel, target.position(), Vec3.ZERO, target.yaw(), entity.getXRot(),
                DimensionTransition.DO_NOTHING));
        }
        if (moved != null) {
            moved.setDeltaMovement(Vec3.ZERO);
            moved.hasImpulse = true;
            if (moved instanceof ServerPlayer player) PortalServices.MOTION_HISTORY.reset(player);
        }
        return moved;
    }

    private @Nullable PortalEntity createDeferredExit(ServerLevel targetLevel, PortalExitTarget target,
                                                       List<Entity> movedEntities) {
        PortalPlacementResult result = PortalServices.PLACEMENT_RESOLVER.resolveExitPrepared(
            targetLevel, target, placement(), aperture);
        if (!result.successful()) return null;

        long now = serverTime();
        PortalEntity exit = create(targetLevel, ownerId, result.pair().exit(),
            fuelRgb(), fuelId(), runtimeOptions(), now, deferredExitExclude, true);
        exit.acquireChunkTicket();
        if (!targetLevel.addFreshEntity(exit)) {
            exit.releaseChunkTicket();
            return null;
        }

        link(this, exit);
        deferredTarget = null;
        waitingForLinkedOpen = true;
        exit.synchronizePairOnOpen = true;
        for (Entity moved : movedEntities) {
            transitGate.markInside(moved.getUUID(), serverTime(), transitCooldownTicks);
            exit.transitGate.markInside(moved.getUUID(), serverTime(), transitCooldownTicks);
        }
        playOpeningSounds(targetLevel, result.pair().exit());
        return exit;
    }

    private PortalRuntimeOptions runtimeOptions() {
        return new PortalRuntimeOptions(entityAccess, openDurationTicks, aperture,
            transitCooldownTicks, fallGuard, horizontalTriggerExtend);
    }

    private void warnExitGenerationFailure(MinecraftServer server, List<Entity> movedEntities) {
        boolean warned = false;
        for (Entity entity : movedEntities) {
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("message.riftgun.exit_generation_failed"), true);
                warned = true;
            }
        }
        if (!warned && ownerId != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            if (owner != null) owner.displayClientMessage(
                Component.translatable("message.riftgun.exit_generation_failed"), true);
        }
    }

    private void warnTeleportFailure(MinecraftServer server, Entity root) {
        boolean warned = false;
        for (Entity entity : root.getSelfAndPassengers().toList()) {
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(
                    Component.translatable("message.riftgun.destination_teleport_failed"), true);
                warned = true;
            }
        }
        if (!warned && ownerId != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            if (owner != null) owner.displayClientMessage(
                Component.translatable("message.riftgun.destination_teleport_failed"), true);
        }
    }

    private Vec3 transformVector(Vec3 vector, PortalEntity target) {
        return PortalTransform.between(vector, orientation(), getYRot(), target.orientation(), target.getYRot());
    }

    private Vec3 outputPosition(Entity entity) {
        return switch (orientation()) {
            case VERTICAL -> position().add(normal().scale(0.85)).subtract(up().scale(portalHeight() * 0.5));
            case TOP -> position().add(normal().scale(0.15));
            case BOTTOM -> position().add(normal().scale(entity.getBbHeight() + 0.15));
        };
    }

    public void startClosing() {
        if (closeStartedAt >= 0L || phase() == PortalLifecycle.Phase.CLOSED) return;
        closeStartedAt = serverTime();
        entityData.set(PHASE, PortalLifecycle.Phase.CLOSING.ordinal());
        entityData.set(PHASE_TICKS, 0);
        if (!closingPair) {
            closingPair = true;
            PortalEntity linked = linkedPortal();
            if (linked != null) linked.startClosing();
            closingPair = false;
        }
    }

    private @Nullable PortalEntity linkedPortal() {
        if (linkedPortalId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity local = serverLevel.getEntity(linkedPortalId);
        if (local instanceof PortalEntity portal) return portal;

        MinecraftServer server = serverLevel.getServer();
        if (linkedDimension != null) {
            ServerLevel linkedLevel = server.getLevel(linkedDimension);
            if (linkedLevel != null) {
                Entity remote = linkedLevel.getEntity(linkedPortalId);
                if (remote instanceof PortalEntity portal) return portal;
            }
        }
        for (ServerLevel candidate : server.getAllLevels()) {
            if (candidate == serverLevel) continue;
            Entity remote = candidate.getEntity(linkedPortalId);
            if (remote instanceof PortalEntity portal) return portal;
        }
        return null;
    }

    private long serverTime() {
        return level() instanceof ServerLevel serverLevel
            ? serverLevel.getServer().overworld().getGameTime()
            : level().getGameTime();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("LinkedPortal")) linkedPortalId = tag.getUUID("LinkedPortal");
        ResourceLocation linkedDimensionId = ResourceLocation.tryParse(tag.getString("LinkedDimension"));
        if (linkedDimensionId != null) {
            linkedDimension = ResourceKey.create(Registries.DIMENSION, linkedDimensionId);
        }
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        if (tag.hasUUID("ExcludedPlayer")) excludedPlayerId = tag.getUUID("ExcludedPlayer");
        if (tag.hasUUID("DeferredExitExclude")) deferredExitExclude = tag.getUUID("DeferredExitExclude");
        exitPortal = tag.getBoolean("ExitPortal");
        horizontalTriggerExtend = tag.contains("HorizontalTriggerExtend")
            ? Math.max(0.0, tag.getDouble("HorizontalTriggerExtend")) : 0.0;
        if (tag.contains("Anchor")) anchor = BlockPos.of(tag.getLong("Anchor"));
        if (tag.contains("AnchorFace")) {
            try {
                anchorFace = Direction.valueOf(tag.getString("AnchorFace"));
            } catch (IllegalArgumentException ignored) {
                anchorFace = null;
            }
        }
        if (tag.contains("DeferredTarget")) {
            deferredTarget = PortalExitTarget.load(tag.getCompound("DeferredTarget"));
        }

        entityData.set(PHASE, tag.getInt("Phase"));
        entityData.set(PHASE_TICKS, tag.getInt("PhaseTicks"));
        long now = serverTime();
        int savedTicks = entityData.get(PHASE_TICKS);
        PortalLifecycle.Phase savedPhase = phase();
        lifecycleStartedAt = tag.contains("LifecycleStartedAt")
            ? tag.getLong("LifecycleStartedAt")
            : switch (savedPhase) {
                case CHARGING -> now - savedTicks;
                case OPENING -> now - PortalLifecycle.CHARGE_TICKS - savedTicks;
                case OPEN, CLOSING, CLOSED -> now - PortalLifecycle.CHARGE_TICKS
                    - PortalLifecycle.ANIMATION_TICKS - savedTicks;
            };
        closeStartedAt = tag.contains("CloseStartedAt")
            ? tag.getLong("CloseStartedAt")
            : savedPhase == PortalLifecycle.Phase.CLOSING ? now - savedTicks : -1L;
        waitingForLinkedOpen = tag.getBoolean("WaitingForLinkedOpen");
        synchronizePairOnOpen = tag.getBoolean("SynchronizePairOnOpen");
        entityData.set(ORIENTATION, tag.getInt("Orientation"));
        entityData.set(GEOMETRY, tag.getInt("Geometry"));
        if (tag.contains("FuelRgb")) entityData.set(FUEL_RGB, tag.getInt("FuelRgb"));
        if (tag.contains("FuelId")) entityData.set(FUEL_ID, tag.getString("FuelId"));
        if (tag.contains("EntityAccess")) {
            entityAccess = PortalEntityAccessSnapshot.load(tag.getCompound("EntityAccess"));
        }
        openDurationTicks = tag.contains("OpenDurationTicks")
            ? Math.max(1, tag.getInt("OpenDurationTicks"))
            : PortalOpenDuration.ticks(PortalOpenDuration.DEFAULT_SECONDS);
        transitCooldownTicks = tag.contains("TransitCooldownTicks")
            ? Math.max(0, tag.getInt("TransitCooldownTicks")) : 20;
        fallGuard = tag.getBoolean("FallGuard");
        aperture = tag.contains("Aperture")
            ? PortalAperture.byOrdinal(tag.getInt("Aperture")) : PortalAperture.STANDARD;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (linkedPortalId != null) tag.putUUID("LinkedPortal", linkedPortalId);
        if (linkedDimension != null) tag.putString("LinkedDimension", linkedDimension.location().toString());
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (excludedPlayerId != null) tag.putUUID("ExcludedPlayer", excludedPlayerId);
        if (deferredExitExclude != null) tag.putUUID("DeferredExitExclude", deferredExitExclude);
        tag.putBoolean("ExitPortal", exitPortal);
        tag.putDouble("HorizontalTriggerExtend", horizontalTriggerExtend);
        if (anchor != null) tag.putLong("Anchor", anchor.asLong());
        if (anchorFace != null) tag.putString("AnchorFace", anchorFace.name());
        if (deferredTarget != null) tag.put("DeferredTarget", deferredTarget.save());
        tag.putInt("Phase", entityData.get(PHASE));
        tag.putInt("PhaseTicks", entityData.get(PHASE_TICKS));
        tag.putLong("LifecycleStartedAt", lifecycleStartedAt);
        if (closeStartedAt >= 0L) tag.putLong("CloseStartedAt", closeStartedAt);
        tag.putBoolean("WaitingForLinkedOpen", waitingForLinkedOpen);
        tag.putBoolean("SynchronizePairOnOpen", synchronizePairOnOpen);
        tag.putInt("Orientation", entityData.get(ORIENTATION));
        tag.putInt("Geometry", entityData.get(GEOMETRY));
        tag.putInt("FuelRgb", entityData.get(FUEL_RGB));
        tag.putString("FuelId", entityData.get(FUEL_ID));
        tag.put("EntityAccess", entityAccess.save());
        tag.putInt("OpenDurationTicks", openDurationTicks);
        tag.putInt("TransitCooldownTicks", transitCooldownTicks);
        tag.putBoolean("FallGuard", fallGuard);
        tag.putInt("Aperture", aperture.ordinal());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return placement().bounds().inflate(0.5);
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
