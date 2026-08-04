package dev.riftgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalRenderTypes;
import dev.riftgun.client.render.PortalRenderer;
import java.io.IOException;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RiftGun.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.riftgun.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.riftgun"
    );

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

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    private ClientModEvents() {}
}
