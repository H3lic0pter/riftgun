package dev.riftgun.client.render;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

final class PortalVisualSelection {
    static ResourceLocation resolve(List<PortalVisualType> types, ResourceLocation requested,
                                    ResourceLocation fallback) {
        return types.stream().anyMatch(type -> type.id().equals(requested)) ? requested : fallback;
    }

    static ResourceLocation cycle(List<PortalVisualType> types, ResourceLocation selected, int direction,
                                  ResourceLocation fallback) {
        if (types.isEmpty()) return fallback;
        int current = -1;
        for (int index = 0; index < types.size(); index++) {
            if (types.get(index).id().equals(selected)) {
                current = index;
                break;
            }
        }
        if (current < 0) current = direction >= 0 ? -1 : 0;
        return types.get(Math.floorMod(current + Integer.signum(direction), types.size())).id();
    }

    private PortalVisualSelection() {}
}
