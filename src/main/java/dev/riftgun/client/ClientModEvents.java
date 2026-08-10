package dev.riftgun.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.riftgun.RiftGun;
import dev.riftgun.client.render.PortalRenderTypes;
import dev.riftgun.client.render.PortalRenderer;
import dev.riftgun.client.render.TintableSplashParticle;
import dev.riftgun.client.light.PortalDynamicLights;
import dev.riftgun.fuel.PortalFluids;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.network.PortalNetworking;
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
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import dev.riftgun.module.PortalModuleMenus;
import dev.riftgun.client.screen.PortalModuleScreen;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RiftGun.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.riftgun.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.riftgun"
    );
    public static final KeyMapping CYCLE_PLACEMENT = new KeyMapping(
        "key.riftgun.cycle_placement", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.riftgun"
    );
    public static final KeyMapping FORCE_FRONT = new KeyMapping(
        "key.riftgun.force_front", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping FORCE_SURFACE = new KeyMapping(
        "key.riftgun.force_surface", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping CLOSE_PORTALS = new KeyMapping(
        "key.riftgun.close_portals", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(RiftGun.PORTAL.get(), PortalRenderer::new);
        PortalNetworking.setClientContextWriter(PortalClientState::writeGunReference);
        event.enqueueWork(PortalDynamicLights::initialize);
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
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.withDefaultNamespace("rendertype_rift_portal_swirl"),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
            ),
            PortalRenderTypes::setSwirlShader
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(CYCLE_PLACEMENT);
        event.register(FORCE_FRONT);
        event.register(FORCE_SURFACE);
        event.register(CLOSE_PORTALS);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RiftGun.PORTAL_SPLASH.get(), TintableSplashParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(fluidStyle(PortalFuelProfiles.UNSTABLE_RGB), PortalFluids.UNSTABLE_TYPE.get());
        event.registerFluidType(fluidStyle(PortalFuelProfiles.PORTAL_RGB), PortalFluids.PORTAL_TYPE.get());
        event.registerFluidType(fluidStyle(PortalFuelProfiles.DIMENSIONAL_RGB), PortalFluids.DIMENSIONAL_TYPE.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new DynamicFluidContainerModel.Colors(),
            PortalFluids.UNSTABLE_BUCKET.get(), PortalFluids.PORTAL_BUCKET.get(),
            PortalFluids.DIMENSIONAL_BUCKET.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PortalModuleMenus.MODULES.get(), PortalModuleScreen::new);
    }

    private static IClientFluidTypeExtensions fluidStyle(int rgb) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_still");
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_flow");
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return ResourceLocation.withDefaultNamespace("block/water_overlay");
            }

            @Override
            public int getTintColor() {
                return 0xFF000000 | rgb;
            }
        };
    }

    private ClientModEvents() {}
}
