package dev.riftgun.client;

import dev.riftgun.core.registry.RiftContent;
import com.mojang.blaze3d.platform.InputConstants;
import dev.riftgun.RiftGun;
import dev.riftgun.client.light.PortalDynamicLights;
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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    private static final KeyMapping.Category RIFTGUN_CATEGORY = new KeyMapping.Category(
        Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "riftgun"));
    public static final KeyMapping OPEN_CONFIG = new KeyMapping(
        "key.riftgun.open_config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, RIFTGUN_CATEGORY
    );
    public static final KeyMapping CYCLE_PLACEMENT = new KeyMapping(
        "key.riftgun.cycle_placement", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, RIFTGUN_CATEGORY
    );
    public static final KeyMapping OPEN_MODE_RADIAL = new KeyMapping(
        "key.riftgun.open_mode_radial", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping OPEN_PRECISION_PLACEMENT = new KeyMapping(
        // Keep the translation/configuration ID so existing B-key remaps survive the rename.
        "key.riftgun.open_surface_face_preview", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping FORCE_FRONT = new KeyMapping(
        "key.riftgun.force_front", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping FORCE_SURFACE = new KeyMapping(
        "key.riftgun.force_surface", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping CLOSE_PORTALS = new KeyMapping(
        "key.riftgun.close_portals", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping ENTITY_RELOCATION = new KeyMapping(
        "key.riftgun.entity_relocation", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping PORTAL_PAIRING_OPERATION = new KeyMapping(
        "key.riftgun.portal_pairing_operation", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );
    public static final KeyMapping TOGGLE_FUNCTION_MODE = new KeyMapping(
        "key.riftgun.toggle_function_mode", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
        RIFTGUN_CATEGORY
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(RiftContent.PORTAL.get(), PortalRenderer::new);
        EntityRenderers.register(RiftContent.ENTITY_RELOCATION_PORTAL.get(), EntityRelocationPortalRenderer::new);
        PortalNetworking.setClientContextWriter(PortalClientState::writeGunReference);
        event.enqueueWork(PortalDynamicLights::initialize);
    }

    @SubscribeEvent
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(PortalRenderTypes.Pipelines.PORTAL);
        event.registerPipeline(PortalRenderTypes.Pipelines.SWIRL);
        event.registerPipeline(PortalRenderTypes.Pipelines.SWIRL_GLOW);
        event.registerPipeline(PortalRenderTypes.Pipelines.SWIRL_FALLBACK_GLOW);
        event.registerPipeline(PortalRenderTypes.Pipelines.SWIRL_EDGE);
        event.registerPipeline(PortalRenderTypes.Pipelines.ENDFRAME_STAR);
        event.registerPipeline(PortalRenderTypes.Pipelines.ENDFRAME_FRAME);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(RIFTGUN_CATEGORY);
        event.register(OPEN_CONFIG);
        event.register(CYCLE_PLACEMENT);
        event.register(OPEN_MODE_RADIAL);
        event.register(OPEN_PRECISION_PLACEMENT);
        event.register(FORCE_FRONT);
        event.register(FORCE_SURFACE);
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
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(
            Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "layered"),
            PortalGunLayeredModel.Unbaked.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        registerFluidModel(event, PortalFluids.UNSTABLE, PortalFluids.FLOWING_UNSTABLE,
            PortalFuelProfiles.UNSTABLE_RGB);
        registerFluidModel(event, PortalFluids.PORTAL, PortalFluids.FLOWING_PORTAL,
            PortalFuelProfiles.PORTAL_RGB);
        registerFluidModel(event, PortalFluids.DIMENSIONAL, PortalFluids.FLOWING_DIMENSIONAL,
            PortalFuelProfiles.DIMENSIONAL_RGB);
    }

    private static void registerFluidModel(RegisterFluidModelsEvent event,
                                           DeferredHolder<Fluid, ? extends Fluid> source,
                                           DeferredHolder<Fluid, ? extends Fluid> flowing,
                                           int rgb) {
        event.register(
            new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still")),
                new Material(Identifier.withDefaultNamespace("block/water_flow")),
                null,
                FluidTintSources.constant(0xFF000000 | rgb)
            ),
            source, flowing
        );
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PortalModuleMenus.MODULES.get(), PortalModuleScreen::new);
    }

    private ClientModEvents() {}
}
