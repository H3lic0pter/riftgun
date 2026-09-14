package dev.riftgun.portal;

import dev.riftgun.network.PortalRequestHandler;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunCapabilityPolicy;
import dev.riftgun.fuel.PortalGunFluidInteractions;
import dev.riftgun.fuel.PortalGunWorldScoop;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.21.11 {
/*import net.minecraft.world.InteractionResult;
*///?} else {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class PortalGunItem extends Item {
    public PortalGunItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if >=1.21.11 {
    /*public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
    *///?} else {
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
    //?}
        if (player instanceof ServerPlayer serverPlayer) {
            dev.riftgun.appearance.GunPresentationDefaults.initialize(serverPlayer, stack);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) PortalGunVisualState.ensureInitialized(stack);
        if (hasMatterAnchor(stack) && PortalModuleRules.current().matterAnchorPreventsDespawn()
            && entity.getAge() != Short.MIN_VALUE) {
            entity.setUnlimitedLifetime();
        }
        return false;
    }

    //? if >=1.21.11 {
    /*@Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
    *///?} else {
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    //?}
        if (!level.isClientSide()) PortalGunVisualState.ensureInitialized(stack);
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
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // A fuel/visual snapshot can replace the stack after firing. Keep the same gun steady,
        // while real slot, gun, bucket-mode and skin changes still use the native equip motion.
        var identity = PortalGunIdentity.existing(oldStack);
        if (!slotChanged && newStack.is(this) && identity != null
            && identity.equals(PortalGunIdentity.existing(newStack))
            && PortalGunMode.bucketMode(oldStack) == PortalGunMode.bucketMode(newStack)
            && java.util.Objects.equals(
                oldStack.get(dev.riftgun.fuel.PortalGunComponents.SKIN),
                newStack.get(dev.riftgun.fuel.PortalGunComponents.SKIN))) return false;
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    @Override
    //? if >=1.21.11 {
    /*public InteractionResult use(Level level, Player player, InteractionHand hand) {
    *///?} else {
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    //?}
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (PortalGunMode.bucketMode(stack)) PortalGunWorldScoop.tryScoop(serverPlayer, hand);
            else PortalRequestHandler.openSelectedFromItem(serverPlayer, hand);
        }
        //? if >=1.21.11 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        // Always synchronize the native swing for third-person observers. The client
        // hand extension selects the first-person presentation without changing that swing.
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        //?}
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
        //? if >=1.21.11 {
        /*return transferred
            ? context.getLevel().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER
            : InteractionResult.PASS;
        *///?} else {
        return transferred
            ? InteractionResult.sidedSuccess(context.getLevel().isClientSide())
            : InteractionResult.PASS;
        //?}
    }
}
