package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class RiftResourceIdTest {
    @Test
    void parsesStableNamespacedIdentifiersWithoutMinecraftTypes() {
        RiftResourceId id = RiftResourceId.parse("riftworld:reality/123e4567-e89b-12d3-a456-426614174000");

        assertEquals("riftworld", id.namespace());
        assertEquals("reality/123e4567-e89b-12d3-a456-426614174000", id.path());
        assertEquals("riftworld:reality/123e4567-e89b-12d3-a456-426614174000", id.toString());
    }

    @Test
    void rejectsIdentifiersThatMinecraftCannotRepresent() {
        assertThrows(IllegalArgumentException.class, () -> RiftResourceId.parse("overworld"));
        assertThrows(IllegalArgumentException.class, () -> RiftResourceId.parse("RiftWorld:reality"));
        assertThrows(IllegalArgumentException.class, () -> RiftResourceId.parse("riftworld:reality space"));
    }
}
