package dev.riftgun.client.model;

/** Derives the two zero-point core highlights from the active fuel theme. */
public final class PortalGunCoreColors {
    public static final int OUTER_TINT = 9;
    public static final int INNER_TINT = 10;
    private static final int OUTER_ALPHA = 0xFF;
    private static final int INNER_ALPHA = 0xFF;

    public static int outer(int rgb) {
        return argb(OUTER_ALPHA, mixWithWhite(rgb, 0.15F));
    }

    public static int inner(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int maximum = Math.max(red, Math.max(green, blue));
        if (maximum == 0) return argb(INNER_ALPHA, 0);

        float brighten = 255.0F / maximum;
        red = Math.round(red * brighten);
        green = Math.round(green * brighten);
        blue = Math.round(blue * brighten);
        int brightMaximum = Math.max(red, Math.max(green, blue));
        red = saturate(red, brightMaximum);
        green = saturate(green, brightMaximum);
        blue = saturate(blue, brightMaximum);
        return argb(INNER_ALPHA, red << 16 | green << 8 | blue);
    }

    private static int mixWithWhite(int rgb, float amount) {
        int red = mix(rgb >> 16 & 0xFF, amount);
        int green = mix(rgb >> 8 & 0xFF, amount);
        int blue = mix(rgb & 0xFF, amount);
        return red << 16 | green << 8 | blue;
    }

    private static int mix(int component, float amount) {
        return Math.round(component + (255 - component) * amount);
    }

    private static int saturate(int component, int maximum) {
        return Math.max(0, Math.round(maximum - (maximum - component) * 1.2F));
    }

    private static int argb(int alpha, int rgb) {
        return alpha << 24 | rgb;
    }

    private PortalGunCoreColors() {}
}
