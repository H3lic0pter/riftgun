package dev.riftgun.client.render;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public record PortalVisualType(
    ResourceLocation id,
    String nameKey,
    String descriptionKey,
    PortalVisualRenderer renderer
) {
    public PortalVisualType {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nameKey);
        Objects.requireNonNull(descriptionKey);
        Objects.requireNonNull(renderer);
    }
}
