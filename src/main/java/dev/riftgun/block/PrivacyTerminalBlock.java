package dev.riftgun.block;

import dev.riftgun.network.PortalNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
//? if >=1.21.11 {
/*import net.minecraft.world.level.block.state.properties.EnumProperty;
*///?} else {
import net.minecraft.world.level.block.state.properties.DirectionProperty;
//?}
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Opens the Player Portal Privacy configuration screen. */
public final class PrivacyTerminalBlock extends Block {
    //? if >=1.21.11 {
    /*public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    *///?} else {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    //?}

    public PrivacyTerminalBlock() {
        super(Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PortalNetworking.sendPrivacyTerminal(serverPlayer);
        }
//? if >=1.21.11 {
        /*return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
*///?} else {
        return InteractionResult.sidedSuccess(level.isClientSide());
//?}
    }
}
