package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalFuelProfiles;
import dev.riftgun.fuel.PortalGunTank;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint for the Rift Gun's fuel tube.
 *
 * <p>The gun model has a liquid column inside the fuel-tube glass (tintindex 2). It picks up
 * the stored fuel color when fuel is present and is tinted fully transparent when empty so
 * the glass stays translucent and empty. The glass itself (tintindex 1) is never tinted.
 */
public final class PortalGunItemColors implements ItemColor {
    /** Tint slot for the liquid column element (tintindex 2 in the model). */
    public static final int LIQUID_TINT = 2;

    /** Alpha applied to the tinted liquid; opaque makes the fuel read as a solid color. */
    private static final int LIQUID_ALPHA = 0xFF;
    /** Fully transparent ARGB used to hide the liquid column in an empty gun. */
    private static final int HIDDEN_LIQUID = 0x00000000;

    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex != LIQUID_TINT) return -1;
        var fluid = new PortalGunTank(stack).getFluid();
        if (fluid.isEmpty()) {
            // Empty gun: hide the liquid column so only the translucent glass remains.
            return HIDDEN_LIQUID;
        }
        return PortalFuelProfiles.resolve(fluid)
            .map(profile -> LIQUID_ALPHA << 24 | profile.rgb())
            .orElse(-1);
    }
}
