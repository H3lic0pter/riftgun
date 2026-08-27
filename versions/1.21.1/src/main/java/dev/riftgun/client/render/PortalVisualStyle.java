package dev.riftgun.client.render;

public record PortalVisualStyle(int splashRgb, int surfaceColor, int borderColor) {
    public static final PortalVisualStyle PALE_GREEN = new PortalVisualStyle(
        0xA8F0B6,
        0xFF78D998,
        0xFFD1FFDA
    );

    public PortalVisualStyle {
        if ((splashRgb & 0xFF000000) != 0) {
            throw new IllegalArgumentException("splashRgb must be a 24-bit RGB color");
        }
    }

    public static PortalVisualStyle fromRgb(int rgb) {
        if (rgb == PALE_GREEN.splashRgb()) return PALE_GREEN;
        return new PortalVisualStyle(rgb, 0xFF000000 | scale(rgb, 0.88F),
            0xFF000000 | mixWithWhite(rgb, 0.62F));
    }

    public PortalVisualStyle dimmed() {
        return new PortalVisualStyle(scale(splashRgb, 0.38F),
            0xFF000000 | scale(surfaceColor & 0xFFFFFF, 0.38F),
            0xFF000000 | scale(borderColor & 0xFFFFFF, 0.55F));
    }

    private static int scale(int rgb, float factor) {
        int red = Math.min(255, Math.round(((rgb >> 16) & 255) * factor));
        int green = Math.min(255, Math.round(((rgb >> 8) & 255) * factor));
        int blue = Math.min(255, Math.round((rgb & 255) * factor));
        return red << 16 | green << 8 | blue;
    }

    private static int mixWithWhite(int rgb, float amount) {
        int red = Math.round(((rgb >> 16) & 255) * (1.0F - amount) + 255 * amount);
        int green = Math.round(((rgb >> 8) & 255) * (1.0F - amount) + 255 * amount);
        int blue = Math.round((rgb & 255) * (1.0F - amount) + 255 * amount);
        return red << 16 | green << 8 | blue;
    }
}
