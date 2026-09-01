package dev.riftgun.ui;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlayerTargetSessionTest {
    @Test
    void expandingRequestsListOnceAndPersistsExpansion() {
        PortalPlayerTargetSession session = new PortalPlayerTargetSession(null, false);
        var commands = session.toggleExpanded();
        assertTrue(session.expanded());
        assertEquals(PortalPlayerTargetSession.CommandType.REQUEST_LIST, commands.get(0).type());
        assertEquals(PortalPlayerTargetSession.CommandType.SET_EXPANDED, commands.get(1).type());
        assertNull(session.requestListIfNeeded(true, true));
    }

    @Test
    void unavailableSelectionClearsAtSharedSeam() {
        UUID selected = UUID.randomUUID();
        PortalPlayerTargetSession session = new PortalPlayerTargetSession(selected, true);
        assertTrue(session.clearUnavailableSelection(ignored -> false));
        assertNull(session.selectedId());
        assertFalse(session.clearUnavailableSelection(ignored -> true));
    }

    @Test
    void disconnectedScreenDefersInitialListRequest() {
        PortalPlayerTargetSession session = new PortalPlayerTargetSession(null, true);
        assertNull(session.requestListIfNeeded(true, false));
        assertEquals(PortalPlayerTargetSession.CommandType.REQUEST_LIST,
            session.requestListIfNeeded(true, true).type());
    }
}
