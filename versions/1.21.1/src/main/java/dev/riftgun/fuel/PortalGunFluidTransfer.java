package dev.riftgun.fuel;

import dev.riftgun.core.fuel.PreparedFluidTransfer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** 1.21.1 item-copy transaction used when filling a gun from an external tank. */
final class PortalGunFluidTransfer {
    static boolean fillFrom(Player player, InteractionHand hand, IFluidHandler source, int maximum) {
        var copy = player.getItemInHand(hand).copyWithCount(1);
        IFluidHandlerItem container = FluidUtil.getFluidHandler(copy).orElse(null);
        if (container == null) return false;

        var completed = PreparedFluidTransfer.execute(
            new SourceAdapter(source), new ContainerAdapter(container), maximum);
        if (!completed.success()) return false;
        FluidStack transferred = completed.resource().copyWithAmount(completed.amount());
        player.setItemInHand(hand, container.getContainer());
        playFillSound(player, transferred);
        return true;
    }

    private record SourceAdapter(IFluidHandler handler)
        implements PreparedFluidTransfer.Source<FluidStack> {
        @Override
        public PreparedFluidTransfer.Offer<FluidStack> simulate(int maximum) {
            FluidStack offered = handler.drain(maximum, IFluidHandler.FluidAction.SIMULATE);
            return offered.isEmpty()
                ? PreparedFluidTransfer.Offer.empty()
                : new PreparedFluidTransfer.Offer<>(offered.copyWithAmount(1), offered.getAmount());
        }

        @Override
        public int drain(FluidStack resource, int maximum) {
            FluidStack drained = handler.drain(
                resource.copyWithAmount(maximum), IFluidHandler.FluidAction.EXECUTE);
            return FluidStack.isSameFluidSameComponents(drained, resource) ? drained.getAmount() : 0;
        }
    }

    private record ContainerAdapter(IFluidHandlerItem handler)
        implements PreparedFluidTransfer.Container<FluidStack> {
        @Override
        public int simulateFill(FluidStack resource, int maximum) {
            return handler.fill(resource.copyWithAmount(maximum), IFluidHandler.FluidAction.SIMULATE);
        }

        @Override
        public int fill(FluidStack resource, int maximum) {
            return handler.fill(resource.copyWithAmount(maximum), IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public int drain(FluidStack resource, int maximum) {
            FluidStack drained = handler.drain(
                resource.copyWithAmount(maximum), IFluidHandler.FluidAction.EXECUTE);
            return FluidStack.isSameFluidSameComponents(drained, resource) ? drained.getAmount() : 0;
        }
    }

    private static void playFillSound(Player player, FluidStack transferred) {
        SoundEvent sound = transferred.getFluidType().getSound(transferred, SoundActions.BUCKET_FILL);
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY() + 0.5, player.getZ(),
                sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private PortalGunFluidTransfer() {}
}
