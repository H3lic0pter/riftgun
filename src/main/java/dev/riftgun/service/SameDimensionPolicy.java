package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SameDimensionPolicy implements DestinationDimensionPolicy {
    @Override
    public Result validate(ServerPlayer player, Destination destination) {
        return player.level().dimension().equals(destination.dimension())
            ? Result.permit()
            : Result.denied(Component.translatable("message.riftgun.same_dimension_only"));
    }
}
