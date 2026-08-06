package dev.riftgun.client.render;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PortalVisualType(
    ResourceLocation id,
    String nameKey,
    String descriptionKey,
    PortalVisualRenderer renderer,
    PortalVisualOptions options
) {
    public PortalVisualType(ResourceLocation id, String nameKey, String descriptionKey,
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
