package dev.riftgun.fuel;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Standard NeoForge container interaction with Rift Gun transfer limits and direction priority. */
public final class PortalGunFluidInteractions {
    public static boolean interact(Player player, InteractionHand hand, IFluidHandler sidedHandler,
                                   @Nullable IFluidHandler unsidedHandler) {
        IFluidHandler fallback = unsidedHandler == sidedHandler ? null : unsidedHandler;
        return PortalGunBucketTransferPolicy.extractFirst(
            maximum -> fillFrom(player, hand, sidedHandler, maximum),
            maximum -> fillFrom(player, hand, fallback, maximum),
            maximum -> emptyInto(player, hand, sidedHandler, maximum),
            maximum -> emptyInto(player, hand, fallback, maximum));
    }

    private static boolean fillFrom(Player player, InteractionHand hand,
                                    @Nullable IFluidHandler source, int maximum) {
        if (source == null) return false;
        return PortalGunFluidTransfer.fillFrom(player, hand, source, maximum);
    }

    private static boolean emptyInto(Player player, InteractionHand hand,
                                     @Nullable IFluidHandler destination, int maximum) {
        if (destination == null) return false;
        return apply(player, hand, FluidUtil.tryEmptyContainer(
            player.getItemInHand(hand), destination, maximum, player, true));
    }

    private static boolean apply(Player player, InteractionHand hand, FluidActionResult result) {
        if (!result.isSuccess()) return false;
        player.setItemInHand(hand, result.getResult());
        return true;
    }

    private PortalGunFluidInteractions() {}
}
