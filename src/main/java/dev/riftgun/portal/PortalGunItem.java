package dev.riftgun.portal;

import dev.riftgun.network.PortalRequestHandler;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunWorldScoop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (PortalGunMode.bucketMode(stack)) PortalGunWorldScoop.tryScoop(serverPlayer, hand);
            else PortalRequestHandler.openSelectedFromItem(serverPlayer, hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
