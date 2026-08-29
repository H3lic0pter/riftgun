package dev.riftgun.pairing;

import net.minecraft.network.chat.Component;

/** Shared endpoint numerals and colors sourced from the pairing-mode icon. */
public final class PortalPairingLabels {
    public static final String FIRST_TEXT = "I";
    public static final String SECOND_TEXT = "II";
    public static final int FIRST_RGB = 0x9CC9D8;
    public static final int SECOND_RGB = 0xE19A52;

    public static Component first() {
        return colored(FIRST_TEXT, FIRST_RGB);
    }

    public static Component second() {
        return colored(SECOND_TEXT, SECOND_RGB);
    }

    public static Component forEndpoint(PortalPairingEndpoint endpoint) {
        return endpoint == PortalPairingEndpoint.A ? first()
            : endpoint == PortalPairingEndpoint.B ? second() : Component.empty();
    }

    public static int colorArgb(PortalPairingEndpoint endpoint) {
        return 0xFF000000 | (endpoint == PortalPairingEndpoint.A ? FIRST_RGB : SECOND_RGB);
    }

    private static Component colored(String value, int rgb) {
        return Component.literal(value).withStyle(style -> style.withColor(rgb));
    }

    private PortalPairingLabels() {}
}
