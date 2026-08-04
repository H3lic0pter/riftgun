package dev.riftgun.portal;

import dev.riftgun.RiftGun;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class PortalEntity extends Entity {
    public static final float WIDTH = 1.2F;
    public static final float HEIGHT = 2.2F;
    public static final float DEPTH = 0.12F;
    public static final int COLOR = 0x62FF48;

    private static final String COOLDOWN_TAG = RiftGun.MOD_ID + ":portal_cooldown_until";
    private static final EntityDataAccessor<Integer> PHASE =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICKS =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);

    private UUID linkedPortalId;
    private UUID ownerId;
    private boolean closingPair;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void openPair(ServerPlayer player, BlockHitResult hit) {
        ServerLevel level = player.serverLevel();
        closeOwnedPortals(level, player.getUUID());

        Vec3 horizontalLook = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 entryBottom = player.position().add(horizontalLook.scale(2.0));

        Direction face = hit.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 hitPoint = hit.getLocation().add(normal.scale(0.08));
        Vec3 exitBottom = new Vec3(hitPoint.x, hitPoint.y - HEIGHT * 0.5, hitPoint.z);

        float entryYaw = player.getYRot() + 180.0F;
        float exitYaw = face.getAxis().isHorizontal() ? face.toYRot() : player.getYRot();

        PortalEntity entry = create(level, player.getUUID(), entryBottom, entryYaw);
        PortalEntity exit = create(level, player.getUUID(), exitBottom, exitYaw);
        entry.linkedPortalId = exit.getUUID();
        exit.linkedPortalId = entry.getUUID();

        level.addFreshEntity(entry);
        level.addFreshEntity(exit);
        level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.65F, 1.35F);
    }

    private static PortalEntity create(ServerLevel level, UUID owner, Vec3 bottom, float yaw) {
        PortalEntity portal = new PortalEntity(RiftGun.PORTAL.get(), level);
        portal.ownerId = owner;
        portal.setPos(bottom);
        portal.setYRot(yaw);
        portal.setYHeadRot(yaw);
        return portal;
    }

    private static void closeOwnedPortals(ServerLevel level, UUID owner) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof PortalEntity portal && owner.equals(portal.ownerId)) {
                portal.startClosing();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, PortalLifecycle.Phase.CHARGING.ordinal());
        builder.define(PHASE_TICKS, 0);
    }

    public PortalLifecycle.Phase phase() {
        return PortalLifecycle.Phase.byOrdinal(entityData.get(PHASE));
    }

    public int phaseTicks() {
        return entityData.get(PHASE_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            spawnChargingParticles();
            return;
        }

        PortalLifecycle.Step next = PortalLifecycle.tick(phase(), phaseTicks());
        entityData.set(PHASE, next.phase().ordinal());
        entityData.set(PHASE_TICKS, next.phaseTicks());

        if (next.phase() == PortalLifecycle.Phase.CLOSED) {
            discard();
            return;
        }
        if (next.phase() == PortalLifecycle.Phase.OPEN) {
            teleportTouchingEntities();
        }
    }

    private void spawnChargingParticles() {
        if (phase() != PortalLifecycle.Phase.CHARGING || random.nextFloat() > phaseTicks() / 30.0F) return;
        float spread = Math.max(0.1F, phaseTicks() / (float) PortalLifecycle.CHARGE_TICKS);
        level().addParticle(
            new DustParticleOptions(new Vector3f(0.38F, 1.0F, 0.28F), 1.0F),
            true,
            getX() + (random.nextDouble() - 0.5) * spread,
            getY() + HEIGHT * 0.5 + (random.nextDouble() - 0.5) * spread,
            getZ() + (random.nextDouble() - 0.5) * spread,
            0.0,
            0.0,
            0.0
        );
    }

    private void teleportTouchingEntities() {
        PortalEntity target = linkedPortal();
        if (target == null || target.phase() != PortalLifecycle.Phase.OPEN) return;

        long gameTime = level().getGameTime();
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.12), this::canTeleport)) {
            long cooldownUntil = entity.getPersistentData().getLong(COOLDOWN_TAG);
            if (gameTime < cooldownUntil) continue;
            teleport(entity, target);
            entity.getPersistentData().putLong(COOLDOWN_TAG, gameTime + PortalLifecycle.TRAVEL_COOLDOWN_TICKS);
        }
    }

    private boolean canTeleport(Entity entity) {
        if (entity instanceof PortalEntity || entity.isPassenger()) return false;
        Vec3 local = entity.position().subtract(position()).yRot((float) Math.toRadians(getYRot()));
        return Math.abs(local.x) <= WIDTH * 0.65
            && Math.abs(local.z) <= 0.45
            && local.y >= -0.2
            && local.y <= HEIGHT + 0.2;
    }

    private void teleport(Entity entity, PortalEntity target) {
        Vec3 outward = Vec3.directionFromRotation(0.0F, target.getYRot()).normalize();
        Vec3 destination = target.position().add(outward.scale(0.85));
        float rotation = target.getYRot() - getYRot() + 180.0F;
        float newYaw = entity.getYRot() + rotation;
        Vec3 momentum = entity.getDeltaMovement().yRot((float) Math.toRadians(rotation));

        entity.setDeltaMovement(momentum);
        entity.teleportTo(
            (ServerLevel) level(),
            destination.x,
            destination.y,
            destination.z,
            Set.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z),
            newYaw,
            entity.getXRot()
        );
        entity.hasImpulse = true;
        level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    public void startClosing() {
        if (phase() == PortalLifecycle.Phase.CLOSING || phase() == PortalLifecycle.Phase.CLOSED) return;
        entityData.set(PHASE, PortalLifecycle.Phase.CLOSING.ordinal());
        entityData.set(PHASE_TICKS, 0);
        if (!closingPair) {
            closingPair = true;
            PortalEntity linked = linkedPortal();
            if (linked != null) linked.startClosing();
            closingPair = false;
        }
    }

    private PortalEntity linkedPortal() {
        if (linkedPortalId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(linkedPortalId);
        return entity instanceof PortalEntity portal ? portal : null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("LinkedPortal")) linkedPortalId = tag.getUUID("LinkedPortal");
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        entityData.set(PHASE, tag.getInt("Phase"));
        entityData.set(PHASE_TICKS, tag.getInt("PhaseTicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (linkedPortalId != null) tag.putUUID("LinkedPortal", linkedPortalId);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Phase", entityData.get(PHASE));
        tag.putInt("PhaseTicks", entityData.get(PHASE_TICKS));
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
