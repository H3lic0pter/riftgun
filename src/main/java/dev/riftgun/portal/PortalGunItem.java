package dev.riftgun.portal;

import dev.riftgun.network.PortalRequestHandler;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunCapabilityPolicy;
import dev.riftgun.fuel.PortalGunFluidInteractions;
import dev.riftgun.fuel.PortalGunWorldScoop;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
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
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (hasMatterAnchor(stack) && PortalModuleRules.current().matterAnchorPreventsDespawn()
            && entity.getAge() != Short.MIN_VALUE) {
            entity.setUnlimitedLifetime();
        }
        return false;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return !hasMatterAnchor(stack)
            || !source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    private static boolean hasMatterAnchor(ItemStack stack) {
        return PortalGunModules.activeCount(
            stack, PortalModuleKind.MATTER_ANCHOR, PortalModuleRules.current()) > 0;
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
