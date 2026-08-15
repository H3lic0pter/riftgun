package dev.riftgun.module;

import dev.riftgun.core.RiftConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortalModuleMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, RiftConstants.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PortalModuleMenu>> MODULES = MENUS.register(
        "portal_modules", () -> IMenuTypeExtension.create(PortalModuleMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private PortalModuleMenus() {}
}
