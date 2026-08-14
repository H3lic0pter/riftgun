package dev.riftgun.fuel;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public final class PortalGunWorldScoop {
    public static boolean tryScoop(ServerPlayer player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        BlockHitResult hit = sourceHit(player);
        if (hit.getType() != HitResult.Type.BLOCK) return fail(player, "message.riftgun.scoop_no_source");

        BlockPos pos = hit.getBlockPos();
        if (!player.level().mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), gun)) {
            return fail(player, "message.riftgun.scoop_protected");
        }

        BlockState state = player.level().getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource() || !(state.getBlock() instanceof BucketPickup pickup)) {
            return fail(player, "message.riftgun.scoop_no_source");
        }

        FluidStack source = new FluidStack(fluidState.getType(), PortalGunTank.WORLD_SOURCE_AMOUNT);
        Optional<PortalFuelProfile> profile = PortalFuelProfiles.resolve(source.getFluid());
        PortalGunTank tank = new PortalGunTank(gun);
        if (profile.isEmpty()) return fail(player, "message.riftgun.scoop_invalid_fluid");
        if (!tank.canFillWorldSource(source, PortalGunBucketTransferPolicy.OVERFLOW_POLICY)) {
            return fail(player, tank.getFluid().getAmount() >= tank.nominalCapacity()
                ? "message.riftgun.scoop_full" : "message.riftgun.scoop_mixed");
        }

        ItemStack pickedUp = pickup.pickupBlock(player, player.level(), pos, state);
        if (pickedUp.isEmpty() || !tank.tryFillWorldSource(
            source, PortalGunBucketTransferPolicy.OVERFLOW_POLICY)) {
            return fail(player, "message.riftgun.scoop_failed");
        }

        pickup.getPickupSound(state).ifPresent(sound ->
            player.level().playSound(null, pos, sound, SoundSource.PLAYERS, 1.0F, 1.0F));
        player.level().gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        player.displayClientMessage(Component.translatable("message.riftgun.scoop_success",
            PortalGunTank.WORLD_SOURCE_AMOUNT, tank.getFluid().getAmount(), tank.nominalCapacity()), true);
        return true;
    }

    private static BlockHitResult sourceHit(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        return player.serverLevel().clip(new ClipContext(
            eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return false;
    }

    private PortalGunWorldScoop() {}
}
