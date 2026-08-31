package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

final class PortalShortcutCommandTest {
    @Test
    void remoteQuickActionUsesTheForcedOpenWireContract() {
        PortalShortcutCommand command = PortalShortcutCommand.forcedOpen(
            PortalPlacementMode.REMOTE);
        CompoundTag payload = new CompoundTag();
        command.writeTo(payload);

        assertEquals(PortalAction.OPEN_SELECTED, command.action());
        assertEquals(PortalPlacementMode.REMOTE.name(), Nbt.getString(payload, "PlacementMode"));
    }

    @Test
    void pairingShortcutCarriesTheSelectedEndpoint() {
        PortalShortcutCommand command = PortalShortcutCommand.pairingEndpoint(true);
        CompoundTag payload = new CompoundTag();
        command.writeTo(payload);

        assertEquals(PortalAction.PLACE_PAIRING_ENDPOINT, command.action());
        assertTrue(Nbt.getBoolean(payload, "EndpointA"));
    }
}
