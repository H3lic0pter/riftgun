package dev.riftgun.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

final class PortalOpenContractTest {
    @Test
    void onlyOpenedStatusReportsSuccess() {
        PortalOpenResult opened = PortalOpenResult.success();
        PortalOpenResult rejected = PortalOpenResult.rejected(
            PortalOpenStatus.NO_PORTAL_GUN,
            Component.translatable("message.riftgun.portal_gun_required"));

        assertTrue(opened.opened());
        assertFalse(rejected.opened());
        assertEquals(PortalOpenStatus.NO_PORTAL_GUN, rejected.status());
        assertEquals("message.riftgun.portal_gun_required", rejected.message().getString());
        assertEquals(new RiftGunApiVersion(1, 2, 0), RiftGunApi.portals().version());
    }

    @Test
    void requestRequiresAnOpenerDestinationSourceAndIntent() {
        PortalDestination destination = new PortalDestination(
            RiftResourceId.parse("riftworld:reality/test"), 0.0, 64.0, 0.0, 0.0F);
        RiftResourceId source = RiftResourceId.parse("riftworld:reality/test");

        assertThrows(NullPointerException.class,
            () -> new PortalOpenRequest(null, destination, source, PortalOpenIntent.PLAYER_REQUEST));
    }
}
