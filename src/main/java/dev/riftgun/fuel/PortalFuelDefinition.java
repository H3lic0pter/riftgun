package dev.riftgun.fuel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/** One datapack portal-fuel definition before its fluid selector is expanded. */
record PortalFuelDefinition(
    ResourceLocation id,
    ResourceLocation fluid,
    ResourceLocation tag,
    int rgb,
    boolean crossDimension,
    int minimumConsumption,
    int maximumConsumption
) {
    static PortalFuelDefinition parse(ResourceLocation id, JsonObject json) {
        ResourceLocation fluid = optionalId(json, "fluid");
        ResourceLocation tag = optionalId(json, "tag");
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

    private static ResourceLocation optionalId(JsonObject json, String key) {
        if (!json.has(key)) return null;
        ResourceLocation parsed = ResourceLocation.tryParse(GsonHelper.getAsString(json, key));
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
