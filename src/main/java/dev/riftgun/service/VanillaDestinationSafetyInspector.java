package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class VanillaDestinationSafetyInspector implements DestinationSafetyInspector {
    private static final double HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;

    @Override
    public SafetyReport inspect(net.minecraft.server.level.ServerLevel level, Destination destination) {
        double x = destination.x();
        double y = destination.y();
        double z = destination.z();
        AABB playerSpace = new AABB(x - HALF_WIDTH, y, z - HALF_WIDTH,
            x + HALF_WIDTH, y + PLAYER_HEIGHT, z + HALF_WIDTH);

        int flags = 0;
        if (!level.noCollision(playerSpace)) flags |= SafetyReport.COLLISION;

        BlockPos supportPos = BlockPos.containing(x, y - 0.05, z);
        BlockState support = level.getBlockState(supportPos);
        if (support.getCollisionShape(level, supportPos).isEmpty()) flags |= SafetyReport.NO_SUPPORT;

        BlockPos feet = BlockPos.containing(x, y, z);
        BlockPos head = BlockPos.containing(x, y + 1.0, z);
        if (hazardous(level.getBlockState(feet), level, feet) || hazardous(level.getBlockState(head), level, head)) {
            flags |= SafetyReport.HAZARD;
        }
        return new SafetyReport(flags);
    }

    private static boolean hazardous(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        return state.is(BlockTags.FIRE)
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.POWDER_SNOW)
            || state.getFluidState().is(FluidTags.LAVA);
    }
}

