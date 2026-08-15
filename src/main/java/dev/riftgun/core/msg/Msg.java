package dev.riftgun.core.msg;
import dev.riftgun.core.msg.Msg;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Version seam for the 1.21.6 player-message rework: {@code displayClientMessage}
 * was replaced by {@code sendSystemMessage} (chat) plus an explicit system-chat
 * packet for the action-bar variant. Shared call sites stay on this helper.
 */
public final class Msg {
    public static void displayClientMessage(Player player, Component message, boolean actionBar) {
        //? if >=1.21.11 {
        /*if (actionBar) {
            ((ServerPlayer) player).connection.send(new ClientboundSystemChatPacket(message, true));
        } else {
            player.sendSystemMessage(message);
        }
        *///?} else {
        Msg.displayClientMessage(player, message, actionBar);
        //?}
    }

    private Msg() {}
}
