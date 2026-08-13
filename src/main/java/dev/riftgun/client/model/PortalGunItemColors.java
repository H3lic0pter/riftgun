package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.fuel.PortalFuelManager;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint for the Rift Gun's fuel tube.
 *
 * <p>The gun model has seven nested liquid columns inside the fuel-tube glass (tint indices
 * 2 through 8). Exactly one column receives the stored fuel color; the others are made fully
 * transparent. The glass itself (tintindex 1) is never tinted.
 */
public final class PortalGunItemColors implements ItemColor {
    /** Alpha applied to the tinted liquid; opaque makes the fuel read as a solid color. */
    private static final int LIQUID_ALPHA = 0xFF;
    /** Fully transparent ARGB used to hide the liquid column in an empty gun. */
    private static final int HIDDEN_LIQUID = 0x00000000;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (!PortalGunFluidLevel.isLiquidTint(tintIndex)) return -1;
        PortalGunTank tank = new PortalGunTank(stack);
        var fluid = tank.getFluid();
        if (fluid.isEmpty() && PortalFuelManager.hasInfiniteFuel(stack)) {
            return tintIndex == PortalGunFluidLevel.FULL_TINT
                ? LIQUID_ALPHA << 24 | PortalFuelProfiles.DIMENSIONAL_RGB : HIDDEN_LIQUID;
        }
        if (fluid.isEmpty() || tintIndex != PortalGunFluidLevel.tintIndex(
                fluid.getAmount(), tank.nominalCapacity())) return HIDDEN_LIQUID;
        return PortalFuelProfiles.resolve(fluid)
            .map(profile -> LIQUID_ALPHA << 24 | profile.rgb())
            .orElse(-1);
    }
}
