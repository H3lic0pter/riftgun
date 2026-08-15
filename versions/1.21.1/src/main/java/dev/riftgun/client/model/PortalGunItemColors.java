package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;
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
    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        PortalGunVisualState visual = PortalGunVisualState.current(stack);
        return PortalGunVisualSnapshot.color(
            visual.liquidTint(), visual.coreVisible(), visual.fuelRgb(), tintIndex);
    }
}
