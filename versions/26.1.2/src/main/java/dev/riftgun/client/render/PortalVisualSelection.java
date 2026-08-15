package dev.riftgun.client.render;

import java.util.List;
import net.minecraft.resources.Identifier;

final class PortalVisualSelection {
    static Identifier resolve(List<PortalVisualType> types, Identifier requested,
                                    Identifier fallback) {
        return types.stream().anyMatch(type -> type.id().equals(requested)) ? requested : fallback;
    }

    static Identifier cycle(List<PortalVisualType> types, Identifier selected, int direction,
                                  Identifier fallback) {
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
