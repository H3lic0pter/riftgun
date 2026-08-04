package dev.riftgun.service;

import dev.riftgun.data.Destination;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

@FunctionalInterface
public interface DestinationDimensionPolicy {
    Result validate(ServerPlayer player, Destination destination);

    record Result(boolean allowed, Component message) {
        public static Result permit() {
            return new Result(true, Component.empty());
        }

        public static Result denied(Component message) {
            return new Result(false, message);
        }
    }
}
