package dev.riftgun.fuel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.util.GsonHelper;

/** One datapack portal-fuel definition before its fluid selector is expanded. */
record PortalFuelDefinition(
//? if >=1.21.11 {
    /*Identifier id,
*///?} else {
    ResourceLocation id,
//?}
//? if >=1.21.11 {
    /*Identifier fluid,
*///?} else {
    ResourceLocation fluid,
//?}
//? if >=1.21.11 {
    /*Identifier tag,
*///?} else {
    ResourceLocation tag,
//?}
    int rgb,
    boolean crossDimension,
    int minimumConsumption,
    int maximumConsumption
) {
//? if >=1.21.11 {
    /*static PortalFuelDefinition parse(Identifier id, JsonObject json) {
*///?} else {
    static PortalFuelDefinition parse(ResourceLocation id, JsonObject json) {
//?}
//? if >=1.21.11 {
        /*Identifier fluid = optionalId(json, "fluid");
*///?} else {
        ResourceLocation fluid = optionalId(json, "fluid");
//?}
//? if >=1.21.11 {
        /*Identifier tag = optionalId(json, "tag");
*///?} else {
        ResourceLocation tag = optionalId(json, "tag");
//?}
        if ((fluid == null) == (tag == null)) {
            throw new JsonParseException("exactly one of fluid or tag is required");
        }
        int rgb = color(json.get("color").getAsString());
        int minimum = GsonHelper.getAsInt(json, "minimum_consumption");
        int maximum = GsonHelper.getAsInt(json, "maximum_consumption");
        return new PortalFuelDefinition(id, fluid, tag, rgb,
            GsonHelper.getAsBoolean(json, "cross_dimension", false), minimum, maximum);
    }

    PortalFuelProfile profile() {
        return new PortalFuelProfile(id, rgb, crossDimension,
            minimumConsumption, maximumConsumption);
    }

//? if >=1.21.11 {
    /*private static Identifier optionalId(JsonObject json, String key) {
*///?} else {
    private static ResourceLocation optionalId(JsonObject json, String key) {
//?}
        if (!json.has(key)) return null;
//? if >=1.21.11 {
        /*Identifier parsed = Identifier.tryParse(GsonHelper.getAsString(json, key));
*///?} else {
        ResourceLocation parsed = ResourceLocation.tryParse(GsonHelper.getAsString(json, key));
//?}
        if (parsed == null) throw new JsonParseException("invalid " + key + " id");
        return parsed;
    }

    private static int color(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        if (normalized.length() != 6) throw new JsonParseException("color must be RRGGBB");
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException exception) {
            throw new JsonParseException("invalid color", exception);
        }
    }
}
