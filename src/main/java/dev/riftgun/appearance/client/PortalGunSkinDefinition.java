package dev.riftgun.appearance.client;

import com.google.gson.JsonObject;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;

/** Resource-pack contract, without references to version-specific model APIs. */
public record PortalGunSkinDefinition(String id, String name, String model,
                                     boolean layered, boolean flat,
                                     boolean fluid, boolean zeroPoint) {
    public static final PortalGunSkinDefinition DEFAULT = new PortalGunSkinDefinition(
        PortalGunSkin.DEFAULT, "skin.riftgun.default", "riftgun:item/portal_gun", true, false, true, true);

    public static PortalGunSkinDefinition parse(String id, JsonObject json) {
        String name = json.get("name").getAsString();
        String model = json.get("model").getAsString();
        String renderer = json.get("renderer").getAsString();
        String display = json.get("display").getAsString();
        if (!PortalGunSkin.validId(id) || !PortalGunSkin.validId(model) || name.isBlank()
            || !(renderer.equals("standard") || renderer.equals("layered"))
            || !(display.equals("3d") || display.equals("flat"))) {
            throw new IllegalArgumentException("Invalid portal gun skin description: " + id);
        }
        JsonObject dynamic = json.has("dynamic") ? json.getAsJsonObject("dynamic") : new JsonObject();
        boolean fluid = dynamic.has("fluid") && dynamic.get("fluid").getAsBoolean();
        boolean zeroPoint = dynamic.has("zero_point") && dynamic.get("zero_point").getAsBoolean();
        if (!renderer.equals("layered") && (fluid || zeroPoint)) {
            throw new IllegalArgumentException("Dynamic skin requires layered renderer: " + id);
        }
        return new PortalGunSkinDefinition(id, name, model, renderer.equals("layered"),
            display.equals("flat"), fluid, zeroPoint);
    }

    public int geometryKey(int liquidTint, boolean coreVisible) {
        return PortalGunVisualSnapshot.geometryKey(fluid ? liquidTint : 0, zeroPoint && coreVisible);
    }
}
