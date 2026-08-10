package dev.riftgun.crisis;

import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Optional adapter for accessory or expanded inventories without a hard dependency. */
@FunctionalInterface
public interface PortalCrisisInventorySource {
    Stream<ItemStack> items(ServerPlayer player);
}
