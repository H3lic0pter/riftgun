package dev.riftgun.core.msg;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Version seam for the 1.21.6 player-message rework. Shared call sites stay on
 * this helper so both client and server players use their polymorphic methods.
 */
public final class Msg {
    public static void displayClientMessage(Player player, Component message, boolean actionBar) {
        //? if >=1.21.11 {
        /*if (actionBar) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
        *///?} else {
        player.displayClientMessage(message, actionBar);
        //?}
    }

    private Msg() {}
}
