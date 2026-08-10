package dev.riftgun.recipe;

import dev.riftgun.RiftGun;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID)
public final class FluidTransmutationEvents {
    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity item) FluidTransmutationService.itemTick(item);
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        FluidTransmutationService.reset();
    }

    private FluidTransmutationEvents() {}
}
