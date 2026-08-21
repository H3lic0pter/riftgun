package dev.riftgun.client;

import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalRenderFrameState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderFrameEvents {
    @SubscribeEvent
    public static void beforeFrame(RenderFrameEvent.Pre event) {
        PortalRenderFrameState.refresh();
    }

    private ClientRenderFrameEvents() {}
}
