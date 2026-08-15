package dev.riftgun.client.render;

import java.util.Objects;
import net.minecraft.resources.Identifier;

public record PortalVisualType(
    Identifier id,
    String nameKey,
    String descriptionKey,
    PortalVisualRenderer renderer,
    PortalVisualOptions options
) {
    public PortalVisualType(Identifier id, String nameKey, String descriptionKey,
                            PortalVisualRenderer renderer) {
        this(id, nameKey, descriptionKey, renderer, PortalVisualOptions.NONE);
    }

    public PortalVisualType {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nameKey);
        Objects.requireNonNull(descriptionKey);
        Objects.requireNonNull(renderer);
        Objects.requireNonNull(options);
    }
}
