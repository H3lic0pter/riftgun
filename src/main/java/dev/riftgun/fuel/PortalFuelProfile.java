package dev.riftgun.fuel;

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

public record PortalFuelProfile(
//? if >=1.21.11 {
    /*Identifier id,
*///?} else {
    ResourceLocation id,
//?}
    int rgb,
    boolean crossDimension,
    int minimumConsumption,
    int maximumConsumption
) {
    public PortalFuelProfile {
        if ((rgb & 0xFF000000) != 0) throw new IllegalArgumentException("rgb must be a 24-bit color");
        if (minimumConsumption < 1 || maximumConsumption < minimumConsumption) {
            throw new IllegalArgumentException("invalid consumption range");
        }
    }
}
