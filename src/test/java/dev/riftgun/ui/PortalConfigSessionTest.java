package dev.riftgun.ui;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalConfigSessionTest {
    @Test
    void dirtyFormRequiresConfirmationAndCancelRestoresIt() {
        PortalConfigSession session = new PortalConfigSession();
        UUID target = UUID.randomUUID();
        session.open(PortalConfigPage.EDIT_DESTINATION, target);
        session.markDirty();

        assertTrue(session.requestClose());
        assertEquals(PortalConfigPage.CONFIRM_DIRTY, session.page());
        assertTrue(session.cancelConfirmation());
        assertEquals(PortalConfigPage.EDIT_DESTINATION, session.page());
        assertEquals(target, session.target());
        assertTrue(session.dirty());
    }

    @Test
    void cleanFormClosesAndDropsTarget() {
        PortalConfigSession session = new PortalConfigSession();
        session.open(PortalConfigPage.EDIT_DESTINATION, UUID.randomUUID());

        assertFalse(session.requestClose());
        assertEquals(PortalConfigPage.NONE, session.page());
        assertNull(session.target());
    }
}
