package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlayerData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalDestinationActionsTest {
    @Test
    void mapSectionExpansionUsesThePersistedGroupAction() {
        PortalPlayerData data = new PortalPlayerData();
        CompoundTag request = new CompoundTag();
        Nbt.putUUID(request, "Group", PortalPlayerData.JOURNEYMAP_SECTION_ID);
        request.putBoolean("Expanded", false);

        assertTrue(PortalDestinationActions.setExpanded(data, request));
        assertFalse(data.expandedGroups().contains(PortalPlayerData.JOURNEYMAP_SECTION_ID));

        Nbt.putUUID(request, "Group", PortalPlayerData.XAERO_MINIMAP_SECTION_ID);
        request.putBoolean("Expanded", true);
        assertTrue(PortalDestinationActions.setExpanded(data, request));
        assertTrue(data.expandedGroups().contains(PortalPlayerData.XAERO_MINIMAP_SECTION_ID));
    }
}
