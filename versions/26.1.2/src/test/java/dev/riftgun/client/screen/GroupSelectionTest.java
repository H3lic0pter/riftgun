package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class GroupSelectionTest {
    @Test
    void mouseCyclingWrapsAtBothEnds() {
        UUID first = UUID.randomUUID();
        UUID middle = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        List<UUID> groups = List.of(first, middle, last);

        assertEquals(first, GroupSelection.cycle(groups, last, 1));
        assertEquals(last, GroupSelection.cycle(groups, first, -1));
        assertEquals(middle, GroupSelection.cycle(groups, first, 1));
    }
}
