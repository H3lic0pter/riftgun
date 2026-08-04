package dev.riftgun;

import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalGunItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RiftGun.MOD_ID)
public final class RiftGun {
    public static final String MOD_ID = "riftgun";

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final DeferredHolder<Item, PortalGunItem> PORTAL_GUN = ITEMS.register(
        "portal_gun",
        () -> new PortalGunItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<PortalEntity>> PORTAL = ENTITY_TYPES.register(
        "portal",
        () -> EntityType.Builder.<PortalEntity>of(PortalEntity::new, MobCategory.MISC)
            .sized(1.2F, PortalEntity.HEIGHT)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build("portal")
    );

    public RiftGun(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PORTAL_GUN.get());
        }
    }
}

