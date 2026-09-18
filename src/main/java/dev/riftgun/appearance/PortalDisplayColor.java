package dev.riftgun.appearance;

import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import net.minecraft.world.item.ItemStack;

/** Cosmetic RGB policy. Never constructs or changes a gameplay fuel profile. */
public final class PortalDisplayColor {
    public static final int UNSET = -1;

    public static int configured(ItemStack gun) {
        return gun.getOrDefault(PortalGunComponents.DISPLAY_COLOR, UNSET);
    }

    public static int resolve(ItemStack gun, int fuelRgb) {
        int color = configured(gun);
        return resolve(color != UNSET && PortalGunModules.activeCount(
            gun, PortalModuleKind.COLOR, PortalModuleRules.current()) > 0, color, fuelRgb);
    }

    public static int resolve(boolean moduleActive, int configuredRgb, int fuelRgb) {
        return (moduleActive && configuredRgb >= 0 && configuredRgb <= 0xFFFFFF
            ? configuredRgb : fuelRgb) & 0xFFFFFF;
    }

    private PortalDisplayColor() {}
}
