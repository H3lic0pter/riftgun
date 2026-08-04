package dev.riftgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalRenderTypes;
import dev.riftgun.client.render.PortalRenderer;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(RiftGun.PORTAL.get(), PortalRenderer::new);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.withDefaultNamespace("rendertype_rift_portal"),
                DefaultVertexFormat.POSITION_COLOR
            ),
            PortalRenderTypes::setPortalShader
        );
    }

    private ClientModEvents() {}
}

