package dev.riftgun.block;

import dev.riftgun.network.PortalNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** Opens the Player Portal Privacy configuration screen. */
public final class PrivacyTerminalBlock extends Block {
    public PrivacyTerminalBlock() {
        super(Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(2.0F, 6.0F)
            .sound(SoundType.METAL));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PortalNetworking.sendPrivacyTerminal(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
