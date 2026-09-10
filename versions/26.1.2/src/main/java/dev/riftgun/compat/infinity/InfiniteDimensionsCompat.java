package dev.riftgun.compat.infinity;

import dev.riftgun.data.Destination;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Infinite Dimensions has no release for this game node. */
public final class InfiniteDimensionsCompat {
    public static final int MAX_TEXT_LENGTH = 16384;
    public static boolean available() { return false; }
    public static ResourceKey<Level> resolve(ServerPlayer player, String text) { throw unavailable(); }
    public static boolean prepare(ServerPlayer player, Destination destination) { throw unavailable(); }
    private static IllegalArgumentException unavailable() {
        return new IllegalArgumentException("message.riftgun.infinity_unavailable");
    }
    private InfiniteDimensionsCompat() {}
}
