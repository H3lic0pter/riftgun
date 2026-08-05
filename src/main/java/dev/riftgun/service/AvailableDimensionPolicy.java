package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AvailableDimensionPolicy implements DestinationDimensionPolicy {
    @Override
    public Result validate(ServerPlayer player, Destination destination) {
        return player.getServer() != null && player.getServer().getLevel(destination.dimension()) != null
            ? Result.permit()
            : Result.denied(Component.translatable("message.riftgun.dimension_unavailable"));
    }
}
