package dev.riftgun.fuel;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Standard NeoForge container interaction with Rift Gun transfer limits and direction priority. */
public final class PortalGunFluidInteractions {
    public static boolean interact(Player player, InteractionHand hand, IFluidHandler blockHandler) {
        var inventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (inventory == null) return false;
        return PortalGunBucketTransferPolicy.extractFirst(
            maximum -> apply(player, hand, FluidUtil.tryFillContainerAndStow(
                player.getItemInHand(hand), blockHandler, inventory, maximum, player, true)),
            maximum -> apply(player, hand, FluidUtil.tryEmptyContainerAndStow(
                player.getItemInHand(hand), blockHandler, inventory, maximum, player, true)));
    }

    private static boolean apply(Player player, InteractionHand hand, FluidActionResult result) {
        if (!result.isSuccess()) return false;
        player.setItemInHand(hand, result.getResult());
        return true;
    }

    private PortalGunFluidInteractions() {}
}
