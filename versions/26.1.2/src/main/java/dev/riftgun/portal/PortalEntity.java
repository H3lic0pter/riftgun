package dev.riftgun.portal;

import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.RiftGun;
import dev.riftgun.fuel.PortalFuelProfile;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.service.PortalPlacementResult;
import dev.riftgun.service.PortalSupportArea;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import dev.riftgun.module.PortalEntityAccessSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PortalEntity extends Entity implements PortalVisualSource {
    public static final float DEPTH = (float) PortalPlacement.DEPTH;

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
    private static final EntityDataAccessor<Optional<BlockPos>> ANCHOR =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> ANCHOR_FACE =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final TicketType PORTAL_TICKET =
        new TicketType(TicketType.NO_TIMEOUT, 0);

    private @Nullable UUID linkedPortalId;
    private @Nullable ResourceKey<Level> linkedDimension;
    private @Nullable UUID ownerId;
    private @Nullable UUID excludedPlayerId;
    private boolean exitPortal;
    private final PortalDeferredExitController deferredExit = new PortalDeferredExitController(this);
    private final PortalTransitOrchestrator transit = new PortalTransitOrchestrator(this);
    private boolean waitingForLinkedOpen;
    private boolean synchronizePairOnOpen;
    private boolean closingPair;
    private @Nullable BlockPos ticketPosition;
    private boolean ticketHeld;
    private PortalEntityAccessSnapshot entityAccess = PortalEntityAccessSnapshot.NONE;
    private int openDurationTicks = PortalOpenDuration.ticks(PortalOpenDuration.DEFAULT_SECONDS);
    private int transitCooldownTicks = 20;
    private boolean fallGuard;
    private boolean entityFallGuard;
    private double horizontalTriggerExtend;
    private PortalAperture aperture = PortalAperture.STANDARD;
    private PortalSoundSnapshot sounds = PortalSoundSnapshot.defaults();
    private final PortalCrisisController crisis = new PortalCrisisController(this);
    private long lifecycleStartedAt;
    private long closeStartedAt = -1L;
    private long lastProjectileEffectAt = Long.MIN_VALUE;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static boolean openPair(ServerPlayer player, PortalPairPlacement pair,
                                   PortalFuelProfile fuel, PortalRuntimeOptions options,
                                   PortalExclusions exclusions, BooleanSupplier commitFuel) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel entryLevel = (ServerLevel) player.level();
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
        PortalSounds.playShot(player, entry.sounds);
        playOpeningSounds(entryLevel, pair.entry(), entry.sounds);
        playOpeningSounds(exitLevel, pair.exit(), exit.sounds);
        return true;
    }

    public static boolean openDeferredExit(ServerPlayer player, PortalPlacement placement,
                                           PortalFuelProfile fuel, PortalExitTarget target,
                                           PortalRuntimeOptions options, PortalExclusions exclusions,
                                           BooleanSupplier commitFuel) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel entryLevel = (ServerLevel) player.level();
        PortalEntity entry = create(entryLevel, player.getUUID(), placement,
            fuel.rgb(), fuel.id().toString(), options, server.overworld().getGameTime(),
            exclusions.entryPlayerId(), false);
        entry.deferredExit.configure(target, exclusions.exitPlayerId());
        entry.acquireChunkTicket();
        boolean added = entryLevel.addFreshEntity(entry);
        if (!added || !commitFuel.getAsBoolean()) {
            removeFailedOpen(entry, added);
            return false;
        }

        closeOwnedPortals(server, player.getUUID(), Set.of(entry.getUUID()));
        PortalSounds.playShot(player, entry.sounds);
        playOpeningSounds(entryLevel, placement, entry.sounds);
        return true;
    }

    private static void removeFailedOpen(PortalEntity portal, boolean added) {
        if (added) portal.discard();
        else portal.releaseChunkTicket();
    }

    private static void playOpeningSounds(ServerLevel level, PortalPlacement placement,
                                          PortalSoundSnapshot sounds) {
        PortalSounds.playOpening(level, placement.center(), sounds);
    }

    private static void link(PortalEntity entry, PortalEntity exit) {
        entry.linkedPortalId = exit.getUUID();
        entry.linkedDimension = exit.level().dimension();
        exit.linkedPortalId = entry.getUUID();
        exit.linkedDimension = entry.level().dimension();
    }

    void acquireChunkTicket() {
        if (ticketHeld || !(level() instanceof ServerLevel serverLevel)) return;
        BlockPos position = blockPosition();
        serverLevel.getChunk(position.getX() >> 4, position.getZ() >> 4);
        serverLevel.getChunkSource().addRegionTicket(
            PORTAL_TICKET, new ChunkPos(position.getX(), position.getZ()), 3, position, true);
        ticketPosition = position;
        ticketHeld = true;
    }

    static PortalEntity create(ServerLevel level, @Nullable UUID owner,
                               PortalPlacement placement, int fuelRgb, String fuelId,
                               PortalRuntimeOptions options, long startedAt,
                               @Nullable UUID excludedPlayerId, boolean exitPortal) {
        PortalEntity portal = new PortalEntity(RiftContent.PORTAL.get(), level);
        portal.ownerId = owner;
        portal.excludedPlayerId = excludedPlayerId;
        portal.setPos(placement.center());
        portal.setYRot(placement.yaw());
        portal.setYHeadRot(placement.yaw());
        portal.entityData.set(ORIENTATION, placement.orientation().ordinal());
        portal.entityData.set(GEOMETRY, placement.geometry().ordinal());
        portal.setAttachment(PortalAttachment.of(placement.anchor(), placement.anchorFace()));
        portal.entityData.set(FUEL_RGB, fuelRgb);
        portal.entityData.set(FUEL_ID, fuelId);
        portal.entityAccess = options.entityAccess();
        portal.openDurationTicks = options.openDurationTicks();
        portal.transitCooldownTicks = options.transitCooldownTicks();
        portal.fallGuard = options.fallGuard();
        portal.entityFallGuard = options.entityFallGuard();
        portal.exitPortal = exitPortal;
        portal.horizontalTriggerExtend = options.horizontalTriggerExtend();
        portal.aperture = options.aperture();
        portal.sounds = options.sounds();
        portal.crisis.configure(options.crises());
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
        builder.define(ANCHOR, Optional.empty());
        builder.define(ANCHOR_FACE, -1);
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

    @Override
    public UUID visualId() {
        return getUUID();
    }

    @Override
    public float visualProgress(float partialTick) {
        return PortalLifecycle.visibleProgress(phase(), phaseTicks(), partialTick);
    }

    @Override
    public float visualAge(float partialTick) {
        return tickCount + partialTick;
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
        PortalAttachment attachment = attachment();
        return new PortalPlacement(position(), orientation(), geometry(), getYRot(),
            attachment.anchor(), attachment.face());
    }

    private PortalAttachment attachment() {
        return PortalAttachment.fromSynced(entityData.get(ANCHOR), entityData.get(ANCHOR_FACE));
    }

    private void setAttachment(PortalAttachment attachment) {
        entityData.set(ANCHOR, attachment.syncedAnchor());
        entityData.set(ANCHOR_FACE, attachment.syncedFace());
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

        SweptPortalIndex.refresh(this);

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

        if (PortalPairClock.shouldSynchronize(
                waitingForLinkedOpen, synchronizePairOnOpen)) {
            synchronizeOpenPair(now);
            return;
        }
        if (deferredExit.active() && deferredExit.targetLevel() == null) {
            startClosing();
            return;
        }
        if (crisis.isReturnExit() && !crisis.returnLinkValid()) {
            startClosing();
            return;
        }
        if (RiftRuntime.current().closePolicy().shouldClose(this)) {
            startClosing();
        } else {
            transit.tick();
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

    void releaseChunkTicket() {
        if (!ticketHeld || ticketPosition == null || !(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.getChunkSource().removeRegionTicket(
            PORTAL_TICKET, new ChunkPos(ticketPosition.getX(), ticketPosition.getZ()), 3, ticketPosition, true);
        ticketHeld = false;
    }

    @Override
    public void remove(RemovalReason reason) {
        SweptPortalIndex.unregister(this);
        releaseChunkTicket();
        super.remove(reason);
    }

    private boolean anchorStillValid() {
        PortalAttachment attachment = attachment();
        if (!attachment.anchored() || !(level() instanceof ServerLevel serverLevel)) return true;
        if (geometry().expanded() && !PortalSupportArea.hasFullExpandedSupport(serverLevel, placement())) {
            return false;
        }
        BlockPos anchor = attachment.anchor();
        if (serverLevel.getBlockState(anchor).getCollisionShape(serverLevel, anchor).isEmpty()) return false;
        return !serverLevel.getBlockCollisions(null, placement().bounds().deflate(0.002)).iterator().hasNext();
    }

    @Nullable PortalEntity createDeferredExit(ServerLevel targetLevel, PortalExitTarget target,
                                              List<Entity> movedEntities,
                                              @Nullable UUID deferredExitExclude) {
        PortalPlacementResult result = RiftRuntime.current().placementResolver().resolveExitPrepared(
            targetLevel, target, placement(), aperture);
        if (!result.successful()) return null;

        long now = serverTime();
        PortalEntity exit = create(targetLevel, ownerId, result.pair().exit(),
            fuelRgb(), fuelId(), runtimeOptions(), now, deferredExitExclude, true);
        crisis.copyPairStateTo(exit.crisis);
        exit.acquireChunkTicket();
        if (!targetLevel.addFreshEntity(exit)) {
            exit.releaseChunkTicket();
            return null;
        }

        link(this, exit);
        deferredExit.complete();
        waitingForLinkedOpen = true;
        exit.synchronizePairOnOpen = true;
        for (Entity moved : movedEntities) {
            transit.markInside(moved.getUUID(), serverTime());
            exit.transit.markInside(moved.getUUID(), serverTime());
        }
        playOpeningSounds(targetLevel, result.pair().exit(), exit.sounds);
        return exit;
    }

    PortalRuntimeOptions runtimeOptions() {
        return new PortalRuntimeOptions(entityAccess, openDurationTicks, aperture,
            transitCooldownTicks, fallGuard, entityFallGuard, horizontalTriggerExtend,
            sounds, crisis.configuration());
    }

    void warnDeferredExitFailure(MinecraftServer server, List<Entity> movedEntities) {
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

    void warnDeferredTeleportFailure(MinecraftServer server, Entity root) {
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

    Vec3 transformVector(Vec3 vector, PortalEntity target) {
        return PortalTransform.between(vector, orientation(), getYRot(), target.orientation(), target.getYRot());
    }

    PortalSoundSnapshot soundSnapshot() {
        return sounds;
    }

    PortalTransitOrchestrator transit() {
        return transit;
    }

    PortalCrisisController crisis() {
        return crisis;
    }

    PortalDeferredExitController deferredExit() {
        return deferredExit;
    }

    PortalEntityAccessSnapshot entityAccess() {
        return entityAccess;
    }

    @Nullable UUID ownerId() {
        return ownerId;
    }

    @Nullable UUID excludedPlayerId() {
        return excludedPlayerId;
    }

    boolean isExitPortal() {
        return exitPortal;
    }

    double horizontalTriggerExtend() {
        return horizontalTriggerExtend;
    }

    int transitCooldownTicks() {
        return transitCooldownTicks;
    }

    boolean fallGuard() {
        return fallGuard;
    }

    boolean claimProjectileEffect(long now) {
        int cooldown = RiftConfigs.server().projectile().effectCooldownTicks();
        if (cooldown > 0 && lastProjectileEffectAt != Long.MIN_VALUE
            && now - lastProjectileEffectAt < cooldown) return false;
        lastProjectileEffectAt = now;
        return true;
    }

    boolean allowsProjectile(Projectile projectile) {
        return phase() == PortalLifecycle.Phase.OPEN && entityAccess.allows(projectile)
            && !projectile.isPassenger() && PortalProjectileState.canTransit(projectile);
    }

    boolean entityAccessAllowsProjectiles() {
        return entityAccess.projectile();
    }

    boolean trySweptProjectile(Projectile projectile, Vec3 start, Vec3 end) {
        return transit.trySweptProjectile(projectile, start, end);
    }

    boolean trySweptSpecialEntity(Entity entity, Vec3 start, Vec3 end) {
        return transit.trySweptSpecialEntity(entity, start, end);
    }

    boolean entityFallGuard() {
        return entityFallGuard;
    }

    long lifecycleStartedAt() {
        return lifecycleStartedAt;
    }

    long closeStartedAt() {
        return closeStartedAt;
    }

    void closeStartedAt(long closeStartedAt) {
        this.closeStartedAt = closeStartedAt;
    }

    Vec3 outputPosition(Entity entity) {
        if (entity instanceof Projectile) {
            return PortalExitClearance.projectilePosition(
                placement(), entity.getBbWidth(), entity.getBbHeight());
        }
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
        if (level() instanceof ServerLevel serverLevel) {
            PortalSounds.playClosing(serverLevel, position(), sounds);
        }
        if (!closingPair) {
            closingPair = true;
            PortalEntity linked = linkedPortal();
            if (linked != null) linked.startClosing();
            closingPair = false;
        }
    }

    @Nullable PortalEntity linkedPortal() {
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

    long serverTime() {
        return level() instanceof ServerLevel serverLevel
            ? serverLevel.getServer().overworld().getGameTime()
            : level().getGameTime();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        CompoundTag tag = new CompoundTag();
        for (String key : input.keySet()) {
            input.read(key, ExtraCodecs.NBT).ifPresent(value -> tag.put(key, value));
        }
        readAdditionalFromCompound(tag);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        addAdditionalToCompound(tag);
        output.store(tag);
    }

    private void readAdditionalFromCompound(CompoundTag tag) {
        if (Nbt.hasUUID(tag, "LinkedPortal")) linkedPortalId = Nbt.getUUID(tag, "LinkedPortal");
        Identifier linkedDimensionId = Identifier.tryParse(Nbt.getString(tag, "LinkedDimension"));
        if (linkedDimensionId != null) {
            linkedDimension = ResourceKey.create(Registries.DIMENSION, linkedDimensionId);
        }
        if (Nbt.hasUUID(tag, "Owner")) ownerId = Nbt.getUUID(tag, "Owner");
        if (Nbt.hasUUID(tag, "ExcludedPlayer")) excludedPlayerId = Nbt.getUUID(tag, "ExcludedPlayer");
        exitPortal = Nbt.getBoolean(tag, "ExitPortal");
        horizontalTriggerExtend = tag.contains("HorizontalTriggerExtend")
            ? Math.max(0.0, Nbt.getDouble(tag, "HorizontalTriggerExtend")) : 0.0;
        if (Nbt.contains(tag, "Attachment")) {
            setAttachment(PortalAttachment.load(Nbt.getCompound(tag, "Attachment")));
        } else {
            CompoundTag legacyAttachment = new CompoundTag();
            if (tag.contains("Anchor")) legacyAttachment.putLong("Anchor", Nbt.getLong(tag, "Anchor"));
            if (tag.contains("AnchorFace")) legacyAttachment.putString("Face", Nbt.getString(tag, "AnchorFace"));
            setAttachment(PortalAttachment.load(legacyAttachment));
        }
        deferredExit.load(tag);

        entityData.set(PHASE, Nbt.getInt(tag, "Phase"));
        entityData.set(PHASE_TICKS, Nbt.getInt(tag, "PhaseTicks"));
        long now = serverTime();
        int savedTicks = entityData.get(PHASE_TICKS);
        PortalLifecycle.Phase savedPhase = phase();
        lifecycleStartedAt = tag.contains("LifecycleStartedAt")
            ? Nbt.getLong(tag, "LifecycleStartedAt")
            : switch (savedPhase) {
                case CHARGING -> now - savedTicks;
                case OPENING -> now - PortalLifecycle.CHARGE_TICKS - savedTicks;
                case OPEN, CLOSING, CLOSED -> now - PortalLifecycle.CHARGE_TICKS
                    - PortalLifecycle.ANIMATION_TICKS - savedTicks;
            };
        closeStartedAt = tag.contains("CloseStartedAt")
            ? Nbt.getLong(tag, "CloseStartedAt")
            : savedPhase == PortalLifecycle.Phase.CLOSING ? now - savedTicks : -1L;
        waitingForLinkedOpen = Nbt.getBoolean(tag, "WaitingForLinkedOpen");
        synchronizePairOnOpen = Nbt.getBoolean(tag, "SynchronizePairOnOpen");
        entityData.set(ORIENTATION, Nbt.getInt(tag, "Orientation"));
        entityData.set(GEOMETRY, Nbt.getInt(tag, "Geometry"));
        if (tag.contains("FuelRgb")) entityData.set(FUEL_RGB, Nbt.getInt(tag, "FuelRgb"));
        if (tag.contains("FuelId")) entityData.set(FUEL_ID, Nbt.getString(tag, "FuelId"));
        if (tag.contains("EntityAccess")) {
            entityAccess = PortalEntityAccessSnapshot.load(Nbt.getCompound(tag, "EntityAccess"));
        }
        openDurationTicks = tag.contains("OpenDurationTicks")
            ? Math.max(1, Nbt.getInt(tag, "OpenDurationTicks"))
            : PortalOpenDuration.ticks(PortalOpenDuration.DEFAULT_SECONDS);
        transitCooldownTicks = tag.contains("TransitCooldownTicks")
            ? Math.max(0, Nbt.getInt(tag, "TransitCooldownTicks")) : 20;
        fallGuard = Nbt.getBoolean(tag, "FallGuard");
        entityFallGuard = Nbt.getBoolean(tag, "EntityFallGuard");
        aperture = tag.contains("Aperture")
            ? PortalAperture.byOrdinal(Nbt.getInt(tag, "Aperture")) : PortalAperture.STANDARD;
        sounds = tag.contains("PortalSounds")
            ? PortalSoundSnapshot.load(Nbt.getCompound(tag, "PortalSounds"))
            : PortalSoundSnapshot.defaults();
        crisis.load(tag);
    }

    private void addAdditionalToCompound(CompoundTag tag) {
        if (linkedPortalId != null) Nbt.putUUID(tag, "LinkedPortal", linkedPortalId);
        if (linkedDimension != null) tag.putString("LinkedDimension", linkedDimension.location().toString());
        if (ownerId != null) Nbt.putUUID(tag, "Owner", ownerId);
        if (excludedPlayerId != null) Nbt.putUUID(tag, "ExcludedPlayer", excludedPlayerId);
        tag.putBoolean("ExitPortal", exitPortal);
        tag.putDouble("HorizontalTriggerExtend", horizontalTriggerExtend);
        tag.put("Attachment", attachment().save());
        deferredExit.save(tag);
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
        tag.putBoolean("EntityFallGuard", entityFallGuard);
        tag.putInt("Aperture", aperture.ordinal());
        tag.put("PortalSounds", sounds.save());
        crisis.save(tag);
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
