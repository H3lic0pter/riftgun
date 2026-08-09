package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PortalPrivacyLedgerTest {
    @Test
    void oneShotGrantIsBoundToTargetAndRequester() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        UUID target = UUID.randomUUID();
        UUID otherTarget = UUID.randomUUID();
        UUID requester = UUID.randomUUID();

        assertTrue(ledger.prompt(target, requester, 10L, 30L));
        assertTrue(ledger.allowOnce(target, requester, 11L, 30L));
        assertFalse(ledger.consume(otherTarget, requester, 12L));
        assertTrue(ledger.consume(target, requester, 12L));
        assertFalse(ledger.consume(target, requester, 12L));
    }

    @Test
    void grantRequiresAnUnexpiredPendingRequest() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        UUID target = UUID.randomUUID();
        UUID requester = UUID.randomUUID();

        assertFalse(ledger.allowOnce(target, requester, 10L, 20L));
        assertTrue(ledger.prompt(target, requester, 10L, 20L));
        assertFalse(ledger.allowOnce(target, requester, 31L, 20L));
    }
}
