package dev.riftgun.portal;

import dev.riftgun.network.PortalRequestHandler;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunCapabilityPolicy;
import dev.riftgun.fuel.PortalGunFluidInteractions;
import dev.riftgun.fuel.PortalGunWorldScoop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidUtil;

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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !PortalGunCapabilityPolicy.allows(
            PortalGunCapabilityPolicy.Access.DIRECT_INTERACTION, PortalGunMode.bucketMode(stack))) {
            return InteractionResult.PASS;
        }
        var sided = FluidUtil.getFluidHandler(context.getLevel(), context.getClickedPos(),
            context.getClickedFace());
        var unsided = FluidUtil.getFluidHandler(context.getLevel(), context.getClickedPos(), null);
        var primary = sided.orElseGet(() -> unsided.orElse(null));
        boolean transferred = primary != null && PortalGunFluidInteractions.interact(
            player, context.getHand(), primary, unsided.orElse(null));
        return transferred
            ? InteractionResult.sidedSuccess(context.getLevel().isClientSide())
            : InteractionResult.PASS;
    }
}
