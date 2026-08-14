package dev.riftgun.crisis;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.RiftGun;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class BuiltinPortalCrises {
    private static final ResourceLocation HIGH_FALL = id("high_altitude_fall");
    private static final ResourceLocation LAVA = id("lava_hazard");
    private static final ResourceLocation SPATIAL_TEAR = id("spatial_tear");
    private static final ResourceLocation WEAKNESS = id("weakness");
    private static final ResourceLocation NAUSEA = id("nausea");

    static void registerAll() {
        PortalCrisisRegistry.register(new Definition(HIGH_FALL, 8, false, true,
            capabilities -> !capabilities.mountedTransit() && capabilities.fallRescue(),
            BuiltinPortalCrises::prepareHighFall));
        PortalCrisisRegistry.register(new Definition(LAVA, 5, false, true,
            capabilities -> !capabilities.mountedTransit() && capabilities.lavaResistant(),
            BuiltinPortalCrises::prepareLava));
        PortalCrisisRegistry.register(new Definition(SPATIAL_TEAR, 2, true, false,
            PortalCrisisCapabilitySnapshot::spatialTearReady,
            BuiltinPortalCrises::prepareSpatialTear));
        PortalCrisisRegistry.register(new Definition(WEAKNESS, 30, true, false, ignored -> true,
            BuiltinPortalCrises::prepareWeakness));
        PortalCrisisRegistry.register(new Definition(NAUSEA, 55, true, false, ignored -> true,
            BuiltinPortalCrises::prepareNausea));
    }

    private static Optional<PortalCrisisPlan> prepareHighFall(PortalCrisisContext context) {
        if (!context.relocationAllowed()) return Optional.empty();
        ServerLevel level = context.targetLevel();
        BlockPos normal = BlockPos.containing(context.normalDestination());
        level.getChunk(normal);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            normal.getX(), normal.getZ());
        int spawnY = Math.min(level.getMaxBuildHeight() - 3,
            surfaceY + RiftConfigs.server().crises().highFallHeight());
        if (spawnY - surfaceY < RiftConfigs.server().crises().minimumHighFallDrop()) {
            return Optional.empty();
        }
        Vec3 destination = new Vec3(context.normalDestination().x, spawnY,
            context.normalDestination().z);
        AABB playerSpace = context.player().getBoundingBox().move(
            destination.subtract(context.player().position()));
        if (!level.noCollision(playerSpace)) return Optional.empty();

        Vec3 momentum = new Vec3(context.normalMomentum().x,
            Math.min(context.normalMomentum().y, -0.2), context.normalMomentum().z);
        PortalPlacement exit = floatingExit(destination, context.destinationYaw());
        if (!level.noCollision(exit.bounds().deflate(0.002))) return Optional.empty();
        int cooldown = context.capabilities().fallGuard()
            ? RiftConfigs.server().crises().guardedHighFallCooldownTicks()
            : RiftConfigs.server().crises().highFallCooldownTicks();
        return Optional.of(new PortalCrisisPlan(HIGH_FALL,
            new PortalCrisisPlan.Relocation(destination, momentum, exit),
            PortalCrisisPlan.Effect.NONE, cooldown));
    }

    private static Optional<PortalCrisisPlan> prepareLava(PortalCrisisContext context) {
        if (!context.relocationAllowed()) return Optional.empty();
        ServerLevel level = context.targetLevel();
        BlockPos normal = BlockPos.containing(context.normalDestination());
        level.getChunk(normal);
        int radius = RiftConfigs.server().crises().lavaSearchRadius();
        int checks = RiftConfigs.server().crises().lavaCandidateChecks();
        for (int attempt = 0; attempt < checks; attempt++) {
            int x = normal.getX() + (attempt == 0 ? 0 : context.player().getRandom().nextInt(-radius, radius + 1));
            int z = normal.getZ() + (attempt == 0 ? 0 : context.player().getRandom().nextInt(-radius, radius + 1));
            if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
            int y = (attempt & 1) == 0
                ? normal.getY() + context.player().getRandom().nextInt(-6, 7)
                : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, Mth.clamp(y, level.getMinBuildHeight() + 1,
                level.getMaxBuildHeight() - 2), z);
            Optional<PortalCrisisPlan> plan = lavaCandidate(context, feet);
            if (plan.isPresent()) return plan;
        }
        return Optional.empty();
    }

    private static Optional<PortalCrisisPlan> lavaCandidate(PortalCrisisContext context, BlockPos feet) {
        Vec3 destination = Vec3.atBottomCenterOf(feet);
        if (!safe(context, destination)) return Optional.empty();
        for (Direction lavaDirection : Direction.Plane.HORIZONTAL) {
            if (!context.targetLevel().getFluidState(feet.relative(lavaDirection)).is(FluidTags.LAVA)
                || !context.targetLevel().getFluidState(feet.relative(lavaDirection)).isSource()) continue;
            if (!hasTwoStepEscape(context, feet, lavaDirection)) continue;
            float yaw = lavaDirection.toYRot();
            PortalPlacement exit = floatingExit(destination, yaw);
            if (!context.targetLevel().noCollision(exit.bounds().deflate(0.002))) continue;
            return Optional.of(new PortalCrisisPlan(LAVA,
                new PortalCrisisPlan.Relocation(destination, Vec3.ZERO, exit),
                PortalCrisisPlan.Effect.NONE, 0));
        }
        return Optional.empty();
    }

    private static boolean hasTwoStepEscape(PortalCrisisContext context, BlockPos feet,
                                            Direction lavaDirection) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction == lavaDirection) continue;
            if (safe(context, Vec3.atBottomCenterOf(feet.relative(direction)))
                && safe(context, Vec3.atBottomCenterOf(feet.relative(direction, 2)))) return true;
        }
        return false;
    }

    private static boolean safe(PortalCrisisContext context, Vec3 position) {
        Destination destination = new Destination(UUID.randomUUID(), "crisis",
            PortalPlayerData.DEFAULT_GROUP_ID, context.targetLevel().dimension(),
            position.x, position.y, position.z, context.destinationYaw(),
            context.targetLevel().getGameTime(), 0L, false);
        return context.safetyInspector().inspect(context.targetLevel(), destination).safe();
    }

    private static Optional<PortalCrisisPlan> prepareSpatialTear(PortalCrisisContext context) {
        if (!safe(context, context.normalDestination())) return Optional.empty();
        int protectionTicks = RiftConfigs.server().crises().spatialTearProtectionTicks();
        return Optional.of(PortalCrisisPlan.effect(SPATIAL_TEAR, player -> {
            player.setHealth(1.0F);
            player.invulnerableTime = Math.max(player.invulnerableTime, protectionTicks);
        }, RiftConfigs.server().crises().spatialTearCooldownTicks()));
    }

    private static Optional<PortalCrisisPlan> prepareWeakness(PortalCrisisContext context) {
        return Optional.of(PortalCrisisPlan.effect(WEAKNESS, player -> player.addEffect(
            new MobEffectInstance(MobEffects.WEAKNESS,
                RiftConfigs.server().crises().weaknessDurationTicks(),
                RiftConfigs.server().crises().weaknessAmplifier())), 0));
    }

    private static Optional<PortalCrisisPlan> prepareNausea(PortalCrisisContext context) {
        return Optional.of(PortalCrisisPlan.effect(NAUSEA, player -> {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                RiftConfigs.server().crises().nauseaDurationTicks(),
                RiftConfigs.server().crises().nauseaAmplifier()));
            player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS,
                (float) RiftConfigs.server().crises().nauseaSoundVolume(),
                (float) RiftConfigs.server().crises().nauseaSoundPitch());
        }, 0));
    }

    private static PortalPlacement floatingExit(Vec3 output, float yaw) {
        Vec3 normal = Vec3.directionFromRotation(0.0F, yaw).normalize();
        PortalGeometry geometry = PortalGeometry.FLOATING_VERTICAL;
        Vec3 center = output.subtract(normal.scale(0.85)).add(0.0, geometry.height() * 0.5, 0.0);
        return new PortalPlacement(center, PortalOrientation.VERTICAL, geometry, yaw, null, null);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, path);
    }

    private record Definition(ResourceLocation id, int defaultWeight,
                              boolean supportsForcedMountedTransit,
                              boolean requiresRelocation,
                              Predicate<PortalCrisisCapabilitySnapshot> eligibility,
                              Function<PortalCrisisContext, Optional<PortalCrisisPlan>> preparation)
        implements PortalCrisis {
        @Override
        public boolean eligible(PortalCrisisCapabilitySnapshot capabilities) {
            return eligibility.test(capabilities);
        }

        @Override
        public Optional<PortalCrisisPlan> prepare(PortalCrisisContext context) {
            return preparation.apply(context);
        }
    }

    private BuiltinPortalCrises() {}
}
