package dev.riftgun;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.config.ServerConfig;
import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.core.fuel.RiftFuelStores;
import dev.riftgun.fuel.PortalFluids;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.fuel.PortalGunCapabilityPolicy;
import dev.riftgun.fuel.PortalGunMode;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.NeoForgeNetworkAdapter;
import dev.riftgun.module.PortalModules;
import dev.riftgun.module.PortalModuleRegistry;
import dev.riftgun.module.PortalModuleMenus;
import dev.riftgun.portal.PortalChunkTickets;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalGunItem;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import dev.riftgun.recipe.RiftGunRecipes;
import dev.riftgun.sound.PortalSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RiftGun.MOD_ID)
public final class RiftGun {
    public static final String MOD_ID = "riftgun";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, MOD_ID);

    private static final DeferredHolder<Block, dev.riftgun.block.PrivacyTerminalBlock> PRIVACY_TERMINAL =
        BLOCKS.register("privacy_terminal", id -> new dev.riftgun.block.PrivacyTerminalBlock(id));
    private static final DeferredHolder<Item, BlockItem> PRIVACY_TERMINAL_ITEM = ITEMS.register(
        "privacy_terminal",
        id -> new BlockItem(PRIVACY_TERMINAL.get(), new Item.Properties()
            .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id)))
    );

    private static final DeferredHolder<Item, PortalGunItem> PORTAL_GUN = ITEMS.register(
        "portal_gun",
        id -> new PortalGunItem(new Item.Properties().stacksTo(1)
            .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id))
            .component(PortalGunComponents.VISUAL_STATE.get(), PortalGunVisualState.UNINITIALIZED))
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RIFT_GUN_TAB = CREATIVE_TABS.register(
        "riftgun",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(PORTAL_GUN.get()))
            .title(Component.translatable("itemGroup.riftgun"))
            .build()
    );

    private static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL = ENTITY_TYPES.register(
        "portal",
        () -> EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
            .sized(1.2F, 2.2F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("riftgun", "portal")))
    );

    private static final DeferredHolder<EntityType<?>, EntityType<EntityRelocationPortalEntity>>
        ENTITY_RELOCATION_PORTAL = ENTITY_TYPES.register(
            "entity_relocation_portal",
            () -> EntityType.Builder.<EntityRelocationPortalEntity>of(
                    EntityRelocationPortalEntity::new, MobCategory.MISC)
                .sized(1.0F, 0.05F)
                .clientTrackingRange(10)
                .updateInterval(1)
                .build(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ENTITY_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("riftgun", "entity_relocation_portal")))
        );

    private static final DeferredHolder<ParticleType<?>, SimpleParticleType> PORTAL_SPLASH = PARTICLE_TYPES.register(
        "portal_splash",
        () -> new SimpleParticleType(false)
    );

    static {
        RiftContent.install(new RiftContent.Installation(
            PRIVACY_TERMINAL::get, PRIVACY_TERMINAL_ITEM::get, PORTAL_GUN::get,
            PORTAL::get, ENTITY_RELOCATION_PORTAL::get, PORTAL_SPLASH::get));
        RiftFuelStores.install(PortalGunTank::new);
    }

    public RiftGun(IEventBus modBus, ModContainer container) {
        RiftRuntime.bootstrapDefaults();
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        CREATIVE_TABS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLE_TYPES.register(modBus);
        PortalGunComponents.COMPONENTS.register(modBus);
        PortalModules.register(modBus);
        PortalModuleMenus.register(modBus);
        PortalFluids.register(modBus);
        RiftGunRecipes.register(modBus);
        PortalSounds.register(modBus);
        PortalChunkTickets.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(NeoForgeNetworkAdapter::register);
        modBus.addListener(this::onConfigLoaded);
        modBus.addListener(this::onConfigReloaded);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        publishConfig(event.getConfig());
    }

    private void onConfigReloaded(ModConfigEvent.Reloading event) {
        publishConfig(event.getConfig());
    }

    private static void publishConfig(ModConfig config) {
        if (config.getSpec() == ServerConfig.SPEC) ServerConfig.publishSnapshot();
        if (config.getSpec() == ClientConfig.SPEC) ClientConfig.publishSnapshot();
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == RIFT_GUN_TAB.get()) acceptModItems(event);
    }

    private void acceptModItems(BuildCreativeModeTabContentsEvent event) {
        event.accept(PORTAL_GUN.get());
        event.accept(PortalModules.BASIC_MODULE.get());
        event.accept(PortalModules.ADVANCED_BASIC_MODULE.get());
        PortalModuleRegistry.definitions().forEach(definition -> event.accept(definition.item().get()));
        event.accept(PortalFluids.UNSTABLE_BUCKET.get());
        event.accept(PortalFluids.PORTAL_BUCKET.get());
        event.accept(PortalFluids.DIMENSIONAL_BUCKET.get());
        event.accept(PRIVACY_TERMINAL_ITEM.get());
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Fluid.ITEM,
            (stack, ignored) -> PortalGunCapabilityPolicy.allows(
                PortalGunCapabilityPolicy.Access.CAPABILITY, PortalGunMode.bucketMode(stack))
                ? new dev.riftgun.fuel.LegacyFluidHandlerAdapter(new dev.riftgun.fuel.PortalGunTank(stack)) : null,
            PORTAL_GUN.get());
    }
}
