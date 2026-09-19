package dev.riftgun.ui;

import java.util.Locale;

/** Local picker draft. Hue survives grayscale edits; RGB changes never touch gun data. */
public final class PortalColorPickerState {
    private float hue;
    private float saturation = 1;
    private float value;
    private int rgb;

    public PortalColorPickerState(int rgb) { setRgb(rgb); }
    public float hue() { return hue; }
    public float saturation() { return saturation; }
    public float value() { return value; }
    public int rgb() { return rgb; }
    public String hex() { return String.format(Locale.ROOT, "#%06X", rgb); }

    public void setRgb(int color) {
        rgb = color & 0xFFFFFF;
        float r = (rgb >> 16 & 255) / 255F, g = (rgb >> 8 & 255) / 255F, b = (rgb & 255) / 255F;
        float maximum = Math.max(r, Math.max(g, b));
        float delta = maximum - Math.min(r, Math.min(g, b));
        value = maximum;
        if (maximum > 0) saturation = delta / maximum;
        if (delta > 0) {
            float sector = maximum == r ? (g - b) / delta
                : maximum == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
            hue = (sector / 6 + 1) % 1;
        }
    }

    public void hue(float hue) {
        this.hue = hue - (float) Math.floor(hue);
        rgb = rgb(this.hue, saturation, value);
    }

    public void shade(float saturation, float value) {
        this.saturation = Math.clamp(saturation, 0, 1);
        this.value = Math.clamp(value, 0, 1);
        rgb = rgb(hue, this.saturation, this.value);
    }

    public static int parseHex(String text) {
        if (!text.matches("#?[0-9a-fA-F]{6}")) return -1;
        return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16);
    }

    public static int rgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6;
        int sector = (int) h;
        float fraction = h - sector;
        float p = value * (1 - saturation);
        float q = value * (1 - saturation * fraction);
        float t = value * (1 - saturation * (1 - fraction));
        return switch (sector) {
            case 0 -> pack(value, t, p);
            case 1 -> pack(q, value, p);
            case 2 -> pack(p, value, t);
            case 3 -> pack(p, q, value);
            case 4 -> pack(t, p, value);
            default -> pack(value, p, q);
        };
    }

    private static int pack(float r, float g, float b) {
        return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }
}
