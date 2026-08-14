package dev.riftgun.core.visual;

/** Version-neutral item-model semantics derived from synchronized gun state. */
public record PortalGunVisualSnapshot(
    int liquidTint,
    boolean coreVisible,
    int fuelRgb,
    int geometryKey,
    int liquidArgb,
    int outerCoreArgb,
    int innerCoreArgb
) {
    public static final int LIQUID_FIRST_TINT = 2;
    public static final int LIQUID_LAST_TINT = 8;
    public static final int OUTER_CORE_TINT = 9;
    public static final int INNER_CORE_TINT = 10;
    public static final int VARIANT_COUNT = 16;
    public static final int HIDDEN = 0x00000000;

    public static PortalGunVisualSnapshot create(int liquidTint, boolean coreVisible, int fuelRgb) {
        int rgb = fuelRgb & 0xFFFFFF;
        return new PortalGunVisualSnapshot(liquidTint, coreVisible, rgb,
            geometryKey(liquidTint, coreVisible),
            0xFF000000 | rgb, outerCoreArgb(rgb), innerCoreArgb(rgb));
    }

    public static int geometryKey(int liquidTint, boolean coreVisible) {
        int liquidSlot = isLiquidTint(liquidTint) ? liquidTint - 1 : 0;
        return liquidSlot | (coreVisible ? 8 : 0);
    }

    public static boolean includesTint(int geometryKey, int tintIndex) {
        if (geometryKey < 0 || geometryKey >= VARIANT_COUNT) return false;
        if (isLiquidTint(tintIndex)) {
            int liquidSlot = geometryKey & 7;
            return liquidSlot != 0 && tintIndex == liquidSlot + 1;
        }
        if (isCoreTint(tintIndex)) return (geometryKey & 8) != 0;
        return true;
    }

    public static int color(int liquidTint, boolean coreVisible, int fuelRgb, int tintIndex) {
        int rgb = fuelRgb & 0xFFFFFF;
        if (tintIndex == OUTER_CORE_TINT) return coreVisible ? outerCoreArgb(rgb) : HIDDEN;
        if (tintIndex == INNER_CORE_TINT) return coreVisible ? innerCoreArgb(rgb) : HIDDEN;
        if (isLiquidTint(tintIndex)) return tintIndex == liquidTint ? 0xFF000000 | rgb : HIDDEN;
        return -1;
    }

    public boolean includesTint(int tintIndex) {
        if (isLiquidTint(tintIndex)) {
            return liquidTint != 0 && tintIndex == liquidTint;
        }
        if (isCoreTint(tintIndex)) return coreVisible;
        return true;
    }

    public int color(int tintIndex) {
        return color(liquidTint, coreVisible, fuelRgb, tintIndex);
    }

    public static boolean isLiquidTint(int tintIndex) {
        return tintIndex >= LIQUID_FIRST_TINT && tintIndex <= LIQUID_LAST_TINT;
    }

    public static boolean isCoreTint(int tintIndex) {
        return tintIndex == OUTER_CORE_TINT || tintIndex == INNER_CORE_TINT;
    }

    public static int outerCoreArgb(int rgb) {
        return 0xFF000000 | mix(rgb >> 16 & 0xFF) << 16
            | mix(rgb >> 8 & 0xFF) << 8 | mix(rgb & 0xFF);
    }

    private static int mix(int component) {
        return Math.round(component + (255 - component) * 0.15F);
    }

    public static int innerCoreArgb(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        int maximum = Math.max(red, Math.max(green, blue));
        if (maximum == 0) return 0xFF000000;
        float brighten = 255.0F / maximum;
        red = Math.round(red * brighten);
        green = Math.round(green * brighten);
        blue = Math.round(blue * brighten);
        int brightMaximum = Math.max(red, Math.max(green, blue));
        red = saturate(red, brightMaximum);
        green = saturate(green, brightMaximum);
        blue = saturate(blue, brightMaximum);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int saturate(int component, int maximum) {
        return Math.max(0, Math.round(maximum - (maximum - component) * 1.2F));
    }
}
