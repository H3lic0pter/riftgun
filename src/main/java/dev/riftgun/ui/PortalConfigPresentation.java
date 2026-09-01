package dev.riftgun.ui;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Text policy shared by both render adapters. */
public final class PortalConfigPresentation {
    public static String gunSettingDescriptionKey(PortalConfigPage page) {
        return switch (page) {
            case PORTAL_DURATION_SETTINGS -> "screen.riftgun.portal_timing_hint";
            case SMART_DISTANCE_SETTINGS -> "screen.riftgun.smart_range_hint";
            case ENTITY_TRANSIT_SETTINGS -> "screen.riftgun.entity_transit_hint";
            case PLAYER_TARGET_SETTINGS -> "screen.riftgun.player_target_hint";
            case APERTURE_SETTINGS -> "screen.riftgun.aperture_hint";
            case FALL_GUARD_SETTINGS -> "screen.riftgun.fall_guard_hint";
            case ENTITY_RELOCATION_SETTINGS -> "screen.riftgun.entity_relocation_hint";
            case PORTAL_PAIRING_SETTINGS -> "screen.riftgun.pairing.settings_hint";
            case REMOTE_SETTINGS -> "screen.riftgun.remote.settings_hint";
            default -> null;
        };
    }

    public static String shortFluidAmount(int amount) {
        if (amount < 1_000) return Integer.toString(amount);
        if (amount % 1_000 == 0) return (amount / 1_000) + "k";
        return String.format(Locale.ROOT, "%.1fk", amount / 1_000.0);
    }

    public static String fluidTranslationKey(String id) {
        if (id.isEmpty()) return "screen.riftgun.empty_fluid";
        int separator = id.indexOf(':');
        String namespace = separator >= 0 ? id.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? id.substring(separator + 1) : id;
        return "fluid." + namespace + "." + path;
    }

    public static String friendlyDimension(String path) {
        String[] words = path.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.isEmpty() ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }

    public static Component toggleLabel(String key, boolean value, Object... arguments) {
        return Component.translatable(key, arguments).append(": ").append(Component.translatable(
            value ? "screen.riftgun.on" : "screen.riftgun.off"));
    }

    private PortalConfigPresentation() {}
}
