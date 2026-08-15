package dev.riftgun.module;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record PortalModuleDefinition(
//? if >=1.21.11 {
    /*Identifier id,
*///?} else {
    ResourceLocation id,
//?}
    PortalModuleKind kind,
    Supplier<? extends Item> item,
    ToIntFunction<PortalModuleRules> maximumCount,
    int accentRgb
) {
    public int maximumCount(PortalModuleRules rules) {
        return Math.max(0, maximumCount.applyAsInt(rules));
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.is(item.get());
    }

    public String descriptionKey() {
        return "tooltip.riftgun.module." + id.getPath() + ".description";
    }
}
