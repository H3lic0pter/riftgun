package dev.riftgun.compat.infinity;

import dev.riftgun.data.Destination;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.lerariemann.infinity.InfinityMod;
import net.lerariemann.infinity.access.MinecraftServerAccess;
import net.lerariemann.infinity.util.InfinityMethods;
import net.lerariemann.infinity.util.core.RandomProvider;
import net.lerariemann.infinity.util.teleport.PortalCreator;

/** Optional 2.7.2 integration. All upstream access stays behind the presence check. */
public final class InfiniteDimensionsCompat {
    public static final int MAX_TEXT_LENGTH = 16384;

    public static boolean available() { return ModList.get().isLoaded("infinity"); }

    public static ResourceKey<Level> resolve(ServerPlayer player, String text) {
        requireReady(player.getServer());
        if (text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException("message.riftgun.infinity_text_too_long");
        return ResourceKey.create(Registries.DIMENSION,
            InfinityMethods.dimTextToId(InfinityMethods.dimTextPreprocess(text)));
    }

    /** Returns true once the upstream server has installed the queued dimension. */
    public static boolean prepare(ServerPlayer player, Destination destination) {
        MinecraftServer server = player.getServer();
        requireReady(server);
        var access = (MinecraftServerAccess) server;
        var level = server.getLevel(destination.dimension());
        if (level != null) {
            if (!InfinityMethods.dimExists(level)) throw new IllegalArgumentException("message.riftgun.dimension_unavailable");
            return true;
        }
        if (access.infinity$hasToAdd(destination.dimension())) return false;
        var required = InfinityMod.provider.getPortalKeyAsItem();
        ItemStack key = ItemStack.EMPTY;
        if (required.isPresent()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(required.get())) { key = stack; break; }
            }
            if (key.isEmpty()) throw new IllegalArgumentException("message.riftgun.infinity_key_missing");
        }
        boolean created = PortalCreator.tryAddInfinityDimension(server, destination.dimension().location());
        if (!created) throw new IllegalArgumentException("message.riftgun.dimension_unavailable");
        if (!key.isEmpty() && !player.getAbilities().instabuild && RandomProvider.rule("consumePortalKey")) {
            key.shrink(1);
            player.getInventory().setChanged();
        }
        if (destination.infinityText() != null) {
            PortalCreator.recordIdTranslation(server, destination.dimension().location(),
                InfinityMethods.dimTextPreprocess(destination.infinityText()));
        }
        return server.getLevel(destination.dimension()) != null;
    }

    private static void requireReady(MinecraftServer server) {
        if (!available()) throw new IllegalArgumentException("message.riftgun.infinity_unavailable");
        if (!(server instanceof MinecraftServerAccess access) || access.infinity$needsInvocation()
            || InfinityMod.provider == null) throw new IllegalArgumentException("error.infinity.invocation_needed");
    }

    private InfiniteDimensionsCompat() {}
}
