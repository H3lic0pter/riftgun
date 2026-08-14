package dev.riftgun.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class RegistrySlotTest {
    @Test
    void failsFastBeforeInstallAndRejectsReplacement() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("riftgun", "test");
        RegistrySlot<String> slot = new RegistrySlot<>(id);

        assertEquals(id, slot.id());
        assertThrows(IllegalStateException.class, slot::get);
        slot.install(() -> "installed");
        assertEquals("installed", slot.get());
        assertThrows(IllegalStateException.class, () -> slot.install(() -> "replacement"));
    }
}
