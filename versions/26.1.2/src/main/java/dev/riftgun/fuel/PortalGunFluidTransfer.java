package dev.riftgun.fuel;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** 26.1.2 compatibility boundary for the legacy direct-block interaction path. */
@SuppressWarnings("removal")
final class PortalGunFluidTransfer {
    static boolean fillFrom(Player player, InteractionHand hand, IFluidHandler source, int maximum) {
        FluidActionResult result = FluidUtil.tryFillContainer(
            player.getItemInHand(hand), source, maximum, player, true);
        if (!result.isSuccess()) return false;
        player.setItemInHand(hand, result.getResult());
        return true;
    }

    private PortalGunFluidTransfer() {}
}
