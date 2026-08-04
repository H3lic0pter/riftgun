package dev.riftgun.client.render;

public record PortalVisualStyle(int splashColor, int surfaceColor, int borderColor) {
    public static final PortalVisualStyle PALE_GREEN = new PortalVisualStyle(
        0xFFA8F0B6,
        0xFF78D998,
        0xFFD1FFDA
    );
}
