package dev.riftgun.client.screen;

import java.util.List;
import java.util.UUID;

final class GroupSelection {
    static UUID cycle(List<UUID> groups, UUID selected, int direction) {
        if (groups.isEmpty()) return selected;
        int current = groups.indexOf(selected);
        if (current < 0) current = direction >= 0 ? -1 : 0;
        return groups.get(Math.floorMod(current + Integer.signum(direction), groups.size()));
    }

    private GroupSelection() {}
}
