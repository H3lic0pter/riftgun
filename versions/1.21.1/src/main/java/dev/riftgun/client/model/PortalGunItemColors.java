package dev.riftgun.client.model;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.appearance.client.PortalGunSkinCatalog;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

/**
 * Item tint for the Rift Gun's fuel tube.
 *
 * <p>The gun model has seven nested liquid columns inside the fuel-tube glass (tint indices
 * 2 through 8). Exactly one column receives the stored fuel color; the others are made fully
 * transparent. The zero-point core uses indices 9 and 10, fuel-colored fixed details use index
 * 11, and untinted zero-point markers use index 12. The glass itself (tintindex 1) is never tinted.
 */
public final class PortalGunItemColors implements ItemColor {
    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        var skin = PortalGunSkinCatalog.resolve(PortalGunSkin.current(stack));
        if (!skin.layered()) return -1;
        PortalGunVisualState visual = PortalGunVisualState.current(stack);
        return PortalGunVisualSnapshot.color(
            skin.fluid() ? visual.liquidTint() : 0,
            skin.zeroPoint() && visual.coreVisible(), visual.fuelRgb(), tintIndex);
    }
}
