package dev.riftgun.portal;

import dev.riftgun.RiftGun;
import dev.riftgun.service.PortalServices;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PortalEntity extends Entity {
    public static final float DEPTH = (float) PortalPlacement.DEPTH;

    private static final EntityDataAccessor<Integer> PHASE =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE_TICKS =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ORIENTATION =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GEOMETRY =
        SynchedEntityData.defineId(PortalEntity.class, EntityDataSerializers.INT);

    private @Nullable UUID linkedPortalId;
    private @Nullable UUID ownerId;
    private @Nullable BlockPos anchor;
    private @Nullable Direction anchorFace;
    private final PortalTransitGate transitGate = new PortalTransitGate();
    private boolean closingPair;

    public PortalEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void openPair(ServerPlayer player, PortalPairPlacement pair) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel entryLevel = player.serverLevel();
        ServerLevel exitLevel = server.getLevel(pair.exitDimension());
        if (exitLevel == null) return;

        // The resolver has already validated both placements. Existing portals are closed only now.
        closeOwnedPortals(server, player.getUUID());

        PortalEntity entry = create(entryLevel, player.getUUID(), pair.entry());
        PortalEntity exit = create(exitLevel, player.getUUID(), pair.exit());
        entry.linkedPortalId = exit.getUUID();
        exit.linkedPortalId = entry.getUUID();

        BlockPos exitBlock = BlockPos.containing(pair.exit().center());
        exitLevel.getChunk(exitBlock.getX() >> 4, exitBlock.getZ() >> 4);
        exitLevel.getChunkSource().addRegionTicket(
            TicketType.PORTAL, new ChunkPos(exitBlock), 3, exitBlock, true
        );

        entryLevel.addFreshEntity(entry);
        exitLevel.addFreshEntity(exit);
        entryLevel.playSound(null, pair.entry().center().x, pair.entry().center().y, pair.entry().center().z,
            SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.7F, 1.15F);
        entryLevel.playSound(null, pair.entry().center().x, pair.entry().center().y, pair.entry().center().z,
            SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.25F, 1.55F);
    }

    private static PortalEntity create(ServerLevel level, UUID owner, PortalPlacement placement) {
        PortalEntity portal = new PortalEntity(RiftGun.PORTAL.get(), level);
        portal.ownerId = owner;
        portal.setPos(placement.center());
        portal.setYRot(placement.yaw());
        portal.setYHeadRot(placement.yaw());
        portal.entityData.set(ORIENTATION, placement.orientation().ordinal());
        portal.entityData.set(GEOMETRY, placement.geometry().ordinal());
        portal.anchor = placement.anchor();
        portal.anchorFace = placement.anchorFace();
        return portal;
    }

    public static void closeOwnedPortals(MinecraftServer server, UUID owner) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PortalEntity portal && owner.equals(portal.ownerId)) portal.startClosing();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PHASE, PortalLifecycle.Phase.CHARGING.ordinal());
        builder.define(PHASE_TICKS, 0);
        builder.define(ORIENTATION, PortalOrientation.VERTICAL.ordinal());
        builder.define(GEOMETRY, PortalGeometry.FLOATING_VERTICAL.ordinal());
    }

    public PortalLifecycle.Phase phase() {
        return PortalLifecycle.Phase.byOrdinal(entityData.get(PHASE));
    }

    public int phaseTicks() {
        return entityData.get(PHASE_TICKS);
    }

    public PortalOrientation orientation() {
        return PortalOrientation.byOrdinal(entityData.get(ORIENTATION));
    }

    public PortalGeometry geometry() {
        return PortalGeometry.byOrdinal(entityData.get(GEOMETRY));
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

        PortalLifecycle.Step next = PortalLifecycle.tick(phase(), phaseTicks());
        entityData.set(PHASE, next.phase().ordinal());
        entityData.set(PHASE_TICKS, next.phaseTicks());

        if (next.phase() == PortalLifecycle.Phase.CLOSED) {
            discard();
            return;
        }
        if (tickCount % 5 == 0 && !anchorStillValid()) {
            startClosing();
            return;
        }
        if (next.phase() == PortalLifecycle.Phase.OPEN && PortalServices.CLOSE_POLICY.shouldClose(this)) {
            startClosing();
        } else if (next.phase() == PortalLifecycle.Phase.OPEN) {
            teleportTouchingEntities();
        }
    }

    private boolean anchorStillValid() {
        if (anchor == null || anchorFace == null || !(level() instanceof ServerLevel serverLevel)) return true;
        if (serverLevel.getBlockState(anchor).getCollisionShape(serverLevel, anchor).isEmpty()) return false;
        return !serverLevel.getBlockCollisions(null, placement().bounds().deflate(0.002)).iterator().hasNext();
    }

    private void teleportTouchingEntities() {
        PortalEntity target = linkedPortal();
        if (target == null || target.phase() != PortalLifecycle.Phase.OPEN) return;

        AABB search = placement().bounds().inflate(0.6, 2.0, 0.6);
        List<Entity> touching = level().getEntities(this, search, this::canTeleport);
        Set<UUID> touchingIds = new HashSet<>(touching.size());
        for (Entity entity : touching) touchingIds.add(entity.getUUID());
        transitGate.retainInside(touchingIds);

        for (Entity entity : touching) {
            if (!transitGate.enter(entity.getUUID())) continue;
            target.transitGate.markInside(entity.getUUID());
            teleportTree(entity, target);
        }
    }

    private boolean canTeleport(Entity entity) {
        if (entity instanceof PortalEntity || entity.isPassenger()
            || !PortalServices.ENTITY_ELIGIBILITY.allowsTree(entity)) return false;
        Vec3 delta = entity.getBoundingBox().getCenter().subtract(position());
        double normalRadius = orientation() == PortalOrientation.VERTICAL
            ? entity.getBbWidth() * 0.5 : entity.getBbHeight() * 0.5;
        return Math.abs(delta.dot(right())) <= portalWidth() * 0.5 + entity.getBbWidth() * 0.5
            && Math.abs(delta.dot(up())) <= portalHeight() * 0.5 + entity.getBbHeight() * 0.5
            && Math.abs(delta.dot(normal())) <= 0.45 + normalRadius;
    }

    private void teleportTree(Entity root, PortalEntity target) {
        var passengers = new ArrayList<>(root.getPassengers());
        root.ejectPassengers();
        teleportSingle(root, target);
        for (Entity passenger : passengers) {
            teleportTree(passenger, target);
            passenger.startRiding(root, true);
        }
        target.level().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private void teleportSingle(Entity entity, PortalEntity target) {
        Vec3 momentum = transformVector(entity.getDeltaMovement(), target);
        double outwardSpeed = momentum.dot(target.normal());
        if (outwardSpeed < 0.12) momentum = momentum.add(target.normal().scale(0.12 - outwardSpeed));

        Vec3 look = transformVector(entity.getLookAngle(), target).normalize();
        float newYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float newPitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0, 1.0)));
        Vec3 destination = target.outputPosition(entity);

        entity.setDeltaMovement(momentum);
        entity.teleportTo((ServerLevel) target.level(), destination.x, destination.y, destination.z,
            Set.<RelativeMovement>of(), newYaw, newPitch);
        entity.setDeltaMovement(momentum);
        entity.hasImpulse = true;
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

    private @Nullable PortalEntity linkedPortal() {
        if (linkedPortalId == null || !(level() instanceof ServerLevel serverLevel)) return null;
        Entity entity = serverLevel.getEntity(linkedPortalId);
        if (entity instanceof PortalEntity portal) return portal;
        MinecraftServer server = serverLevel.getServer();
        for (ServerLevel candidate : server.getAllLevels()) {
            if (candidate == serverLevel) continue;
            Entity remote = candidate.getEntity(linkedPortalId);
            if (remote instanceof PortalEntity portal) return portal;
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("LinkedPortal")) linkedPortalId = tag.getUUID("LinkedPortal");
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        if (tag.contains("Anchor")) anchor = BlockPos.of(tag.getLong("Anchor"));
        if (tag.contains("AnchorFace")) {
            try {
                anchorFace = Direction.valueOf(tag.getString("AnchorFace"));
            } catch (IllegalArgumentException ignored) {
                anchorFace = null;
            }
        }
        entityData.set(PHASE, tag.getInt("Phase"));
        entityData.set(PHASE_TICKS, tag.getInt("PhaseTicks"));
        entityData.set(ORIENTATION, tag.getInt("Orientation"));
        entityData.set(GEOMETRY, tag.getInt("Geometry"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (linkedPortalId != null) tag.putUUID("LinkedPortal", linkedPortalId);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        if (anchor != null) tag.putLong("Anchor", anchor.asLong());
        if (anchorFace != null) tag.putString("AnchorFace", anchorFace.name());
        tag.putInt("Phase", entityData.get(PHASE));
        tag.putInt("PhaseTicks", entityData.get(PHASE_TICKS));
        tag.putInt("Orientation", entityData.get(ORIENTATION));
        tag.putInt("Geometry", entityData.get(GEOMETRY));
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
