package dev.riftgun.fuel;

import com.mojang.serialization.Codec;
import dev.riftgun.RiftGun;
import dev.riftgun.module.PortalGunModuleSettings;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortalGunComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RiftGun.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID =
        COMPONENTS.registerComponentType("portal_gun_fluid", builder -> builder
            .persistent(SimpleFluidContent.CODEC)
            .networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BUCKET_MODE =
        COMPONENTS.registerComponentType("portal_gun_bucket_mode", builder -> builder
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> MODULES =
        COMPONENTS.registerComponentType("portal_gun_modules", builder -> builder
            .persistent(ItemContainerContents.CODEC)
            .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortalGunModuleSettings>> MODULE_SETTINGS =
        COMPONENTS.registerComponentType("portal_gun_module_settings", builder -> builder
            .persistent(PortalGunModuleSettings.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> INSTANCE_ID =
        COMPONENTS.registerComponentType("portal_gun_instance_id", builder -> builder
            .persistent(UUIDUtil.CODEC)
            .networkSynchronized(UUIDUtil.STREAM_CODEC));

    private PortalGunComponents() {}
}
