package dev.riftgun.appearance.client;

import com.google.gson.JsonObject;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;
import java.util.HashMap;
import java.util.Map;

/** Resource-pack contract, without references to version-specific model APIs. */
public record PortalGunSkinDefinition(String id, String name, String model,
                                     boolean layered, boolean flat,
                                     boolean fluid, boolean zeroPoint, Map<Integer, ModeColors> modeColors) {
    public PortalGunSkinDefinition {
        modeColors = Map.copyOf(modeColors);
    }

    public PortalGunSkinDefinition(String id, String name, String model, boolean layered,
                                   boolean flat, boolean fluid, boolean zeroPoint) {
        this(id, name, model, layered, flat, fluid, zeroPoint, Map.of());
    }

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
        Map<Integer, ModeColors> modeColors = new HashMap<>();
        if (dynamic.has("mode_colors")) {
            for (var entry : dynamic.getAsJsonObject("mode_colors").entrySet()) {
                int tint = Integer.parseInt(entry.getKey());
                if (tint < PortalGunVisualSnapshot.MODE_FIRST_TINT
                    || tint > PortalGunVisualSnapshot.MAX_TINT_INDEX
                    || !entry.getKey().equals(Integer.toString(tint))) {
                    throw new IllegalArgumentException("Mode tint must be an integer from "
                        + PortalGunVisualSnapshot.MODE_FIRST_TINT + " to "
                        + PortalGunVisualSnapshot.MAX_TINT_INDEX + ": " + entry.getKey());
                }
                modeColors.put(tint, ModeColors.parse(entry.getValue().getAsJsonObject()));
            }
        }
        if (!renderer.equals("layered") && (fluid || zeroPoint || dynamic.has("mode_colors"))) {
            throw new IllegalArgumentException("Dynamic skin requires layered renderer: " + id);
        }
        return new PortalGunSkinDefinition(id, name, model, renderer.equals("layered"),
            display.equals("flat"), fluid, zeroPoint, modeColors);
    }

    public int color(int liquidTint, boolean coreVisible, int fuelRgb, boolean pairingMode, int tintIndex) {
        if (!layered) return -1;
        if (tintIndex >= PortalGunVisualSnapshot.MODE_FIRST_TINT) {
            ModeColors colors = modeColors.get(tintIndex);
            return colors == null ? -1 : pairingMode ? colors.pairing() : colors.coordinate();
        }
        return PortalGunVisualSnapshot.color(fluid ? liquidTint : 0, zeroPoint && coreVisible,
            fuelRgb, tintIndex);
    }

    /** Parsed once on resource reload; omitted palettes leave the texture unchanged. */
    public record ModeColors(int coordinate, int pairing) {
        private static ModeColors parse(JsonObject json) {
            return new ModeColors(rgb(json, "coordinate"), rgb(json, "pairing"));
        }

        private static int rgb(JsonObject json, String key) {
            if (json == null || !json.has(key) || !json.get(key).isJsonPrimitive()
                || !json.getAsJsonPrimitive(key).isString()) {
                throw new IllegalArgumentException("Mode color requires a #RRGGBB string: " + key);
            }
            String value = json.get(key).getAsString();
            if (!value.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("Invalid mode color: " + value);
            }
            return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
        }
    }

    public int geometryKey(int liquidTint, boolean coreVisible) {
        return PortalGunVisualSnapshot.geometryKey(fluid ? liquidTint : 0, zeroPoint && coreVisible);
    }
}
