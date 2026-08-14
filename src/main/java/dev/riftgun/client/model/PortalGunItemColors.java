package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalGunVisualState;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint for the Rift Gun's fuel tube.
 *
 * <p>The gun model has seven nested liquid columns inside the fuel-tube glass (tint indices
 * 2 through 8). Exactly one column receives the stored fuel color; the others are made fully
 * transparent. The zero-point core uses indices 9 and 10. The glass itself (tintindex 1) is
 * never tinted.
 */
public final class PortalGunItemColors implements ItemColor {
    /** Alpha applied to the tinted liquid; opaque makes the fuel read as a solid color. */
    private static final int LIQUID_ALPHA = 0xFF;
    /** Fully transparent ARGB used to hide the liquid column in an empty gun. */
    private static final int HIDDEN_LIQUID = 0x00000000;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        boolean coreTint = tintIndex == PortalGunCoreColors.OUTER_TINT
            || tintIndex == PortalGunCoreColors.INNER_TINT;
        if (!coreTint && !PortalGunFluidLevel.isLiquidTint(tintIndex)) return -1;

        PortalGunVisualState visual = PortalGunVisualState.current(stack);
        if (coreTint) {
            if (!visual.coreVisible()) return HIDDEN_LIQUID;
            return tintIndex == PortalGunCoreColors.INNER_TINT
                ? PortalGunCoreColors.inner(visual.fuelRgb())
                : PortalGunCoreColors.outer(visual.fuelRgb());
        }
        if (tintIndex != visual.liquidTint()) return HIDDEN_LIQUID;
        return LIQUID_ALPHA << 24 | visual.fuelRgb();
    }
}
