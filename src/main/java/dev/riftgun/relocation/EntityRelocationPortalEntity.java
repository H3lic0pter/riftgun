package dev.riftgun.relocation;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.RiftConstants;
import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
//? if >=1.21.11 {
/*import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
*///?} else {
//?}
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Visual-only TOP gate used by Entity Relocation entrances and reusable saved-destination exits. */
public final class EntityRelocationPortalEntity extends Entity implements PortalVisualSource {
    //? if >=1.21.11 {
/*private static final TicketType EXIT_TICKET =
    dev.riftgun.portal.PortalChunkTickets.RELOCATION_EXIT.get();
*///?} else {
private static final TicketType<UUID> EXIT_TICKET = TicketType.create("riftgun_entity_relocation_exit", UUID::compareTo);
//?}
    private static final EntityDataAccessor<Float> SIDE = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_SIDE = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> FUEL_RGB = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICKS = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OPENING_TICKS = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EXIT = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ORIENTATION = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> FOLLOW_TARGET = SynchedEntityData.defineId(
//? if >=1.21.11 {
        /*EntityRelocationPortalEntity.class, dev.riftgun.portal.PortalEntityDataSerializers.OPTIONAL_UUID.get());
*///?} else {
        EntityRelocationPortalEntity.class, EntityDataSerializers.OPTIONAL_UUID);
//?}
    private static final EntityDataAccessor<Boolean> FOLLOW_PLAYER_HEAD = SynchedEntityData.defineId(
        EntityRelocationPortalEntity.class, EntityDataSerializers.BOOLEAN);

    private int openDurationTicks = 60;
    private int reservations;
    private PortalSoundSnapshot sounds = PortalSoundSnapshot.defaults();
    private long lastProjectileEffectAt = Long.MIN_VALUE;
    private ChunkPos ticketChunk;
    private int ticketReservations;

    public EntityRelocationPortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static EntityRelocationPortalEntity createEntrance(
            Level level, Vec3 center, float side, int rgb, UUID followTarget,
            PortalSoundSnapshot sounds, int openingTicks) {
        return create(level, center, side, rgb, false, followTarget, 1, sounds, openingTicks);
    }

    public static EntityRelocationPortalEntity createExit(
            Level level, Vec3 center, float side, int rgb, int openDurationTicks,
            PortalSoundSnapshot sounds, int openingTicks) {
        return createExit(level, center, side, rgb, openDurationTicks, sounds, false, openingTicks);
    }

    public static EntityRelocationPortalEntity createExit(
            Level level, Vec3 center, float side, int rgb, int openDurationTicks,
            PortalSoundSnapshot sounds, boolean bottom, int openingTicks) {
        EntityRelocationPortalEntity portal = create(
            level, center, side, rgb, true, null, openDurationTicks, sounds, openingTicks);
        portal.entityData.set(ORIENTATION,
            (bottom ? PortalOrientation.BOTTOM : PortalOrientation.TOP).ordinal());
        return portal;
    }

    public static EntityRelocationPortalEntity createPlayerDestinationExit(
            ServerLevel level, net.minecraft.server.level.ServerPlayer player, float side, int rgb,
            int openDurationTicks, PortalSoundSnapshot sounds, int openingTicks) {
        Vec3 center = EntityRelocationGeometry.playerDestinationExitCenter(
            player.position(), player.getBoundingBox().maxY);
        EntityRelocationPortalEntity portal = create(
            level, center, side, rgb, true, player.getUUID(), openDurationTicks, sounds,
            openingTicks);
        portal.entityData.set(ORIENTATION, PortalOrientation.BOTTOM.ordinal());
        portal.entityData.set(YAW, player.getYRot());
        portal.entityData.set(FOLLOW_PLAYER_HEAD, true);
        return portal;
    }

    public static EntityRelocationPortalEntity createExit(
            Level level, Vec3 center, float side, int rgb, int openDurationTicks,
            PortalSoundSnapshot sounds, PortalOrientation orientation, float yaw,
            int openingTicks) {
        EntityRelocationPortalEntity portal = create(
            level, center, side, rgb, true, null, openDurationTicks, sounds, openingTicks);
        portal.entityData.set(ORIENTATION,
            (orientation == null ? PortalOrientation.TOP : orientation).ordinal());
        portal.entityData.set(YAW, yaw);
        return portal;
    }

    private static EntityRelocationPortalEntity create(
            Level level, Vec3 center, float side, int rgb, boolean exit, UUID followTarget,
            int openDurationTicks, PortalSoundSnapshot sounds, int openingTicks) {
        EntityRelocationPortalEntity portal = new EntityRelocationPortalEntity(
            RiftContent.ENTITY_RELOCATION_PORTAL.get(), level);
        portal.setPos(center);
        float normalizedSide = EntityRelocationGeometry.normalizeSide(side);
        portal.entityData.set(SIDE, normalizedSide);
        portal.entityData.set(TARGET_SIDE, normalizedSide);
        portal.entityData.set(FUEL_RGB, rgb);
        portal.entityData.set(EXIT, exit);
        portal.entityData.set(FOLLOW_TARGET, Optional.ofNullable(followTarget));
        portal.entityData.set(OPENING_TICKS, Math.max(1, openingTicks));
        portal.openDurationTicks = Math.max(1, openDurationTicks);
        portal.sounds = sounds == null ? PortalSoundSnapshot.defaults() : sounds;
        return portal;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIDE, 1.0F);
        builder.define(TARGET_SIDE, 1.0F);
        builder.define(FUEL_RGB, PortalFuelProfiles.DIMENSIONAL_RGB);
        builder.define(PHASE, PortalLifecycle.Phase.OPENING.ordinal());
        builder.define(PHASE_TICKS, 0);
        builder.define(OPENING_TICKS, EntityRelocationLifecycle.OPENING_TICKS);
        builder.define(EXIT, false);
        builder.define(ORIENTATION, PortalOrientation.TOP.ordinal());
        builder.define(YAW, 0.0F);
        builder.define(FOLLOW_TARGET, Optional.empty());
        builder.define(FOLLOW_PLAYER_HEAD, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        followTarget();
        growTowardRequestedSide();

        PortalLifecycle.Phase current = phase();
        if (current == PortalLifecycle.Phase.CLOSED) {
            discardPortal();
            return;
        }
        if (EntityRelocationLifecycle.shouldBeginClosing(
                current, phaseTicks(), openDurationTicks, reservations)) {
            beginClosing();
            return;
        }

        PortalLifecycle.Step next = PortalLifecycle.tick(current, phaseTicks(),
            openingTicks(), EntityRelocationLifecycle.CLOSING_TICKS);
        setPhase(next.phase(), next.phaseTicks());
        if (next.phase() != current) {
            TransitDiagnostics.relocation("portal phase portal={} exit={} dimension={} pos={} {}->{} ticketHeld={}",
//? if >=1.21.11 {
                /*getUUID(), isExit(), level().dimension().identifier(), position(),
*///?} else {
                getUUID(), isExit(), level().dimension().location(), position(),
//?}
                current, next.phase(), ticketReservations > 0);
        }
        if (next.phase() == PortalLifecycle.Phase.CLOSED) discardPortal();
    }

    private void followTarget() {
        if (entityData.get(FOLLOW_PLAYER_HEAD) && phase() != PortalLifecycle.Phase.OPENING) {
            entityData.set(FOLLOW_TARGET, Optional.empty());
            entityData.set(FOLLOW_PLAYER_HEAD, false);
            return;
        }
        entityData.get(FOLLOW_TARGET).ifPresent(id -> {
            if (level() instanceof ServerLevel serverLevel) {
                Entity target = serverLevel.getEntity(id);
                if (target != null) {
                    double centerY = entityData.get(FOLLOW_PLAYER_HEAD)
                        ? target.getBoundingBox().maxY + 1.0
                        : EntityRelocationGeometry.centerY(target.getY(), PortalEntity.DEPTH);
                    setPos(target.getX(), centerY, target.getZ());
                    if (entityData.get(FOLLOW_PLAYER_HEAD)) entityData.set(YAW, target.getYRot());
                }
            }
        });
    }

    private void growTowardRequestedSide() {
        float current = entityData.get(SIDE);
        float target = entityData.get(TARGET_SIDE);
        if (current >= target) return;
        float step = Math.max(0.05F, (target - current) * 0.35F);
        entityData.set(SIDE, Math.min(target, current + step));
    }

    public boolean tryReserve(float requiredSide) {
        if (!isExit() || phase() != PortalLifecycle.Phase.OPEN) return false;
        reservations++;
        requestSide(requiredSide);
        return true;
    }

    boolean claimProjectileEffect(long now) {
        int cooldown = RiftConfigs.server().projectile().effectCooldownTicks();
        if (cooldown > 0 && lastProjectileEffectAt != Long.MIN_VALUE
            && now - lastProjectileEffectAt < cooldown) return false;
        lastProjectileEffectAt = now;
        return true;
    }

    public void releaseReservation(boolean successful) {
        if (reservations > 0) reservations--;
        if (successful && phase() == PortalLifecycle.Phase.OPEN) {
            entityData.set(PHASE_TICKS, 0);
        }
    }

    public void freezeAndClose() {
        entityData.set(FOLLOW_TARGET, Optional.empty());
        entityData.set(FOLLOW_PLAYER_HEAD, false);
        beginClosing();
    }

    public void beginClosing() {
        PortalLifecycle.Phase current = phase();
        if (current == PortalLifecycle.Phase.CLOSING || current == PortalLifecycle.Phase.CLOSED) return;
        entityData.set(FOLLOW_TARGET, Optional.empty());
        entityData.set(FOLLOW_PLAYER_HEAD, false);
        reservations = 0;
        setPhase(PortalLifecycle.Phase.CLOSING, 0);
        if (level() instanceof ServerLevel serverLevel) {
            PortalSounds.playClosing(serverLevel, position(), sounds);
        }
    }

    public void requestSide(float requiredSide) {
        float target = Math.max(entityData.get(TARGET_SIDE),
            EntityRelocationGeometry.normalizeSide(requiredSide));
        entityData.set(TARGET_SIDE, target);
    }

    public PortalLifecycle.Phase phase() {
        return PortalLifecycle.Phase.byOrdinal(entityData.get(PHASE));
    }

    public int phaseTicks() {
        return entityData.get(PHASE_TICKS);
    }

    public int remainingOpenTicks() {
        return EntityRelocationLifecycle.remainingOpenTicks(
            phase(), phaseTicks(), openDurationTicks);
    }

    public boolean isExit() {
        return entityData.get(EXIT);
    }

    private void setPhase(PortalLifecycle.Phase phase, int phaseTicks) {
        entityData.set(PHASE, phase.ordinal());
        entityData.set(PHASE_TICKS, Math.max(0, phaseTicks));
    }

    private void discardPortal() {
        if (isExit()) EntityRelocationManager.unregisterExit(getUUID());
        discard();
    }

    void acquireChunkTicket() {
        if (!isExit() || !(level() instanceof ServerLevel serverLevel)) return;
        ticketReservations++;
        if (ticketReservations > 1) return;
        ticketChunk = chunkPosition();
        // 26.1.2 rejects chunk creation outside the world bounds; bail out
        // instead of crashing when a placement carries a corrupt position.
        if (!dev.riftgun.portal.PortalChunkGuard.inWorldBounds(serverLevel, ticketChunk)) {
            ticketReservations--;
            return;
        }
        //? if >=1.21.11 {
        /*serverLevel.getChunkSource().addTicketAndLoadWithRadius(
            EXIT_TICKET, ticketChunk, 3);
        *///?} else {
        serverLevel.getChunkSource().addRegionTicket(
            EXIT_TICKET, ticketChunk, 3, getUUID(), true);
        //?}
        TransitDiagnostics.ticket("relocation exit acquired portal={} dimension={} chunk={} entityTicking={}",
//? if >=1.21.11 {
            /*getUUID(), serverLevel.dimension().identifier(), ticketChunk,
*///?} else {
            getUUID(), serverLevel.dimension().location(), ticketChunk,
//?}
            serverLevel.isPositionEntityTicking(blockPosition()));
    }

    void releaseChunkTicket() {
        if (ticketReservations <= 0) return;
        ticketReservations--;
        if (ticketReservations > 0) return;
        if (ticketChunk == null || !(level() instanceof ServerLevel serverLevel)) return;
        //? if >=1.21.11 {
        /*serverLevel.getChunkSource().removeTicketWithRadius(
            EXIT_TICKET, ticketChunk, 3);
        *///?} else {
        serverLevel.getChunkSource().removeRegionTicket(
            EXIT_TICKET, ticketChunk, 3, getUUID(), true);
        //?}
        TransitDiagnostics.ticket("relocation exit released portal={} dimension={} chunk={} removalReasonPending={}",
//? if >=1.21.11 {
            /*getUUID(), serverLevel.dimension().identifier(), ticketChunk, getRemovalReason());
*///?} else {
            getUUID(), serverLevel.dimension().location(), ticketChunk, getRemovalReason());
//?}
        ticketChunk = null;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (ticketReservations > 0) {
            ticketReservations = 1;
            releaseChunkTicket();
        }
        super.remove(reason);
    }

    //? if >=1.21.11 {
    /*@Override
    protected void readAdditionalSaveData(ValueInput input) {
        CompoundTag tag = new CompoundTag();
        for (String key : input.keySet()) {
            input.read(key, ExtraCodecs.NBT).ifPresent(value -> tag.put(key, value));
        }
        readAdditionalFromCompound(tag);
    }

    private void readAdditionalFromCompound(CompoundTag tag) {
    *///?} else {
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    //?}
        entityData.set(SIDE, EntityRelocationGeometry.normalizeSide(Nbt.getFloat(tag, "Side")));
        entityData.set(TARGET_SIDE, Math.max(entityData.get(SIDE), Nbt.getFloat(tag, "TargetSide")));
        entityData.set(FUEL_RGB, Nbt.getInt(tag, "FuelRgb"));
        entityData.set(EXIT, Nbt.getBoolean(tag, "Exit"));
        entityData.set(ORIENTATION, tag.contains("Orientation")
            ? Nbt.getInt(tag, "Orientation")
            : Nbt.getBoolean(tag, "Bottom") ? PortalOrientation.BOTTOM.ordinal()
            : PortalOrientation.TOP.ordinal());
        entityData.set(YAW, Nbt.getFloat(tag, "Yaw"));
        openDurationTicks = Math.max(1, Nbt.getInt(tag, "OpenDurationTicks"));
        entityData.set(OPENING_TICKS, tag.contains("OpeningTicks")
            ? Math.max(1, Nbt.getInt(tag, "OpeningTicks"))
            : EntityRelocationLifecycle.OPENING_TICKS);
        sounds = tag.contains("Sounds")
            ? PortalSoundSnapshot.load(Nbt.getCompound(tag, "Sounds")) : PortalSoundSnapshot.defaults();
        PortalLifecycle.Phase saved = PortalLifecycle.Phase.byOrdinal(Nbt.getInt(tag, "Phase"));
        if (!isExit() && saved != PortalLifecycle.Phase.CLOSED) saved = PortalLifecycle.Phase.CLOSING;
        setPhase(saved, Nbt.getInt(tag, "PhaseTicks"));
        entityData.set(FOLLOW_TARGET, Optional.empty());
        reservations = 0;
    }

    //? if >=1.21.11 {
    /*@Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        addAdditionalToCompound(tag);
        output.store(tag);
    }

    private void addAdditionalToCompound(CompoundTag tag) {
    *///?} else {
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    //?}
        tag.putFloat("Side", entityData.get(SIDE));
        tag.putFloat("TargetSide", entityData.get(TARGET_SIDE));
        tag.putInt("FuelRgb", fuelRgb());
        tag.putBoolean("Exit", isExit());
        tag.putInt("Orientation", orientation().ordinal());
        tag.putFloat("Yaw", entityData.get(YAW));
        tag.putInt("OpenDurationTicks", openDurationTicks);
        tag.putInt("OpeningTicks", openingTicks());
        tag.putInt("Phase", phase().ordinal());
        tag.putInt("PhaseTicks", phaseTicks());
        tag.put("Sounds", sounds.save());
    }

    @Override public UUID visualId() { return getUUID(); }
    @Override public PortalOrientation orientation() {
        return PortalOrientation.byOrdinal(entityData.get(ORIENTATION));
    }
    @Override public PortalGeometry geometry() { return PortalGeometry.HORIZONTAL; }
    @Override public float portalWidth() { return entityData.get(SIDE); }
    @Override public float portalHeight() { return entityData.get(SIDE); }
    @Override public Vec3 normal() { return orientation().normal(entityData.get(YAW)); }
    @Override public Vec3 up() { return orientation().up(entityData.get(YAW)); }
    @Override public Vec3 right() { return orientation().right(entityData.get(YAW)); }
    @Override public int fuelRgb() { return entityData.get(FUEL_RGB); }

    @Override
    public PortalPlacement placement() {
        return new PortalPlacement(position(), orientation(), PortalGeometry.HORIZONTAL,
            entityData.get(YAW), null, null);
    }

    @Override
    public float visualProgress(float partialTick) {
        return EntityRelocationLifecycle.visibleProgress(
            phase(), phaseTicks(), partialTick, openingTicks());
    }

    public int openingTicks() {
        return Math.max(1, entityData.get(OPENING_TICKS));
    }

    boolean chunkTicketHeld() {
        return ticketReservations > 0;
    }

    @Override public float visualAge(float partialTick) { return tickCount + partialTick; }
    public int visualAgeTicks() { return tickCount; }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    //? if >=1.21.11 {
    //?} else {
    @Override public AABB getBoundingBoxForCulling() {
        float half = portalWidth() * 0.5F;
        return new AABB(getX() - half, getY() - 0.25, getZ() - half,
            getX() + half, getY() + 0.25, getZ() + half).inflate(0.5);
    }
    //?}
    @Override public boolean isAlwaysTicking() { return true; }
    @Override public boolean fireImmune() { return true; }

    //? if >=1.21.11 {
    /*@Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
    *///?} else {
    //?}
}
