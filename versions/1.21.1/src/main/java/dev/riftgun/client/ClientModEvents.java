package dev.riftgun.client;

import dev.riftgun.core.registry.RiftContent;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riftgun.RiftGun;
import dev.riftgun.client.model.PortalGunLayeredModel;
import dev.riftgun.client.render.EntityRelocationPortalRenderer;
import dev.riftgun.client.render.PortalRenderTypes;
import dev.riftgun.client.render.PortalRenderer;
import dev.riftgun.client.render.TintableSplashParticle;
import dev.riftgun.client.screen.PortalModuleScreen;
import dev.riftgun.fuel.PortalFluids;
import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.module.PortalModuleMenus;
import dev.riftgun.network.PortalNetworking;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RiftGun.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final ModelResourceLocation PORTAL_GUN_MODEL = ModelResourceLocation.inventory(
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "portal_gun"));
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.riftgun.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.riftgun"
    );
    public static final KeyMapping CYCLE_PLACEMENT = new KeyMapping(
        "key.riftgun.cycle_placement", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.riftgun"
    );
    public static final KeyMapping OPEN_MODE_RADIAL = new KeyMapping(
        "key.riftgun.open_mode_radial", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping OPEN_PRECISION_PLACEMENT = new KeyMapping(
        // Keep the translation/configuration ID so existing B-key remaps survive the rename.
        "key.riftgun.open_surface_face_preview", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
        "key.categories.riftgun"
    );
    public static final KeyMapping FORCE_FRONT = new KeyMapping(
        "key.riftgun.force_front", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping FORCE_SURFACE = new KeyMapping(
        "key.riftgun.force_surface", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping FORCE_REMOTE = new KeyMapping(
        "key.riftgun.force_remote", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping CLOSE_PORTALS = new KeyMapping(
        "key.riftgun.close_portals", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping ENTITY_RELOCATION = new KeyMapping(
        "key.riftgun.entity_relocation", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping PORTAL_PAIRING_OPERATION = new KeyMapping(
        "key.riftgun.portal_pairing_operation", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );
    public static final KeyMapping TOGGLE_FUNCTION_MODE = new KeyMapping(
        "key.riftgun.toggle_function_mode", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        "key.categories.riftgun"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(RiftContent.PORTAL.get(), PortalRenderer::new);
        EntityRenderers.register(RiftContent.ENTITY_RELOCATION_PORTAL.get(), EntityRelocationPortalRenderer::new);
        PortalNetworking.setClientContextWriter(PortalClientState::writeGunReference);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.withDefaultNamespace("rendertype_rift_pairing_marker"),
                DefaultVertexFormat.POSITION_COLOR_NORMAL
            ),
            PortalRenderTypes::setPairingMarkerShader
        );
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
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.withDefaultNamespace("rendertype_rift_endframe"),
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP
            ),
            PortalRenderTypes::setEndframeShader
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
        event.register(CYCLE_PLACEMENT);
        event.register(OPEN_MODE_RADIAL);
        event.register(OPEN_PRECISION_PLACEMENT);
        event.register(FORCE_FRONT);
        event.register(FORCE_SURFACE);
        event.register(FORCE_REMOTE);
        event.register(CLOSE_PORTALS);
        event.register(ENTITY_RELOCATION);
        event.register(PORTAL_PAIRING_OPERATION);
        event.register(TOGGLE_FUNCTION_MODE);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(RiftContent.PORTAL_SPLASH.get(), TintableSplashParticle.Provider::new);
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
        event.register(new dev.riftgun.client.model.PortalGunItemColors(),
            RiftContent.PORTAL_GUN.get());
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().computeIfPresent(PORTAL_GUN_MODEL,
            (ignored, model) -> new PortalGunLayeredModel(model));
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
