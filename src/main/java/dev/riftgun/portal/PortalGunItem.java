package dev.riftgun.portal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PortalGunItem extends Item {
    private static final double RANGE = 96.0;

    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && player instanceof ServerPlayer serverPlayer) {
            PortalEntity.openPair(serverPlayer, blockHit);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}

