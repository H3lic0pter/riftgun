package dev.riftgun.service;

import dev.riftgun.core.nbt.Nbt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalGunReferenceTest {
    @Test
    void referencePreservesLocatorTokenAndExactGunIdentity() {
        UUID identity = UUID.randomUUID();
        CompoundTag token = new CompoundTag();
        token.putInt("Slot", 4);

        CompoundTag reference = PortalGunReference.capture("vanilla_inventory", token, identity);

        assertEquals("vanilla_inventory", PortalGunReference.locatorId(reference));
        assertEquals(4, Nbt.getInt(PortalGunReference.token(reference), "Slot"));
        assertTrue(PortalGunReference.matches(reference, identity));
        assertFalse(PortalGunReference.matches(reference, UUID.randomUUID()));
    }

    @Test
    void referencesWithoutIdentityAreRejected() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Locator", "vanilla_inventory");
        legacy.put("Token", new CompoundTag());

        assertFalse(PortalGunReference.matches(legacy, UUID.randomUUID()));
    }
}
