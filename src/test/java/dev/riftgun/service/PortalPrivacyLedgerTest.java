package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PortalPrivacyLedgerTest {
    @Test
    void oneShotGrantIsCheckedWithoutConsumptionAndConsumedExplicitly() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties parties = parties();
        PortalPrivacyLedger.Prompt prompt = ledger.prompt(parties, 10L, 30L);

        assertEquals(PortalPrivacyLedger.ResolutionStatus.ACTIVE,
            ledger.resolve(prompt.token(), parties.targetId(), 11L).status());
        ledger.grantOnce(parties, 11L, 60L);
        assertTrue(ledger.hasGrant(parties.key(), 12L));
        assertTrue(ledger.hasGrant(parties.key(), 12L));
        assertTrue(ledger.consumeGrant(parties.key(), 12L));
        assertFalse(ledger.consumeGrant(parties.key(), 12L));
    }

    @Test
    void requestTokensCannotBeReplayedOrAppliedToAnotherTarget() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties parties = parties();
        PortalPrivacyLedger.Prompt first = ledger.prompt(parties, 10L, 20L);

        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(first.token(), UUID.randomUUID(), 11L).status());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.ACTIVE,
            ledger.resolve(first.token(), parties.targetId(), 11L).status());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(first.token(), parties.targetId(), 11L).status());

        PortalPrivacyLedger.Prompt second = ledger.prompt(parties, 12L, 20L);
        assertNotEquals(first.token(), second.token());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(first.token(), parties.targetId(), 13L).status());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.ACTIVE,
            ledger.resolve(second.token(), parties.targetId(), 13L).status());
    }

    @Test
    void duplicatePromptReusesTheActiveToken() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties parties = parties();
        PortalPrivacyLedger.Prompt first = ledger.prompt(parties, 10L, 30L);
        PortalPrivacyLedger.Prompt duplicate = ledger.prompt(parties, 11L, 30L);

        assertTrue(first.fresh());
        assertFalse(duplicate.fresh());
        assertEquals(first.token(), duplicate.token());
    }

    @Test
    void requestAndGrantProduceDistinctExpirationEvents() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties requestParties = parties();
        PortalPrivacyLedger.Parties grantParties = parties();
        ledger.prompt(requestParties, 10L, 20L);
        PortalPrivacyLedger.Prompt grantPrompt = ledger.prompt(grantParties, 10L, 20L);
        ledger.resolve(grantPrompt.token(), grantParties.targetId(), 11L);
        ledger.grantOnce(grantParties, 11L, 40L);

        var requestExpiry = ledger.expire(30L);
        assertEquals(1, requestExpiry.size());
        assertEquals(PortalPrivacyLedger.Expiration.REQUEST, requestExpiry.getFirst().expiration());
        assertEquals(requestParties, requestExpiry.getFirst().parties());

        var grantExpiry = ledger.expire(51L);
        assertEquals(1, grantExpiry.size());
        assertEquals(PortalPrivacyLedger.Expiration.GRANT, grantExpiry.getFirst().expiration());
        assertEquals(grantParties, grantExpiry.getFirst().parties());
    }

    @Test
    void denyOnceCooldownExpiresIndependently() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.RequestKey key = parties().key();
        ledger.denyOnce(key, 100L, 200L);

        assertEquals(200L, ledger.denyRemainingTicks(key, 100L));
        assertEquals(1L, ledger.denyRemainingTicks(key, 299L));
        assertEquals(0L, ledger.denyRemainingTicks(key, 300L));
    }

    @Test
    void expiredResolutionRetainsPartiesForNotification() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties parties = parties();
        PortalPrivacyLedger.Prompt prompt = ledger.prompt(parties, 10L, 20L);

        PortalPrivacyLedger.Resolution resolution = ledger.resolve(
            prompt.token(), parties.targetId(), 30L);

        assertEquals(PortalPrivacyLedger.ResolutionStatus.EXPIRED, resolution.status());
        assertEquals(parties, resolution.parties());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(prompt.token(), parties.targetId(), 30L).status());
    }

    @Test
    void clearDropsAllTransientState() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties parties = parties();
        PortalPrivacyLedger.Parties deniedParties = parties();
        PortalPrivacyLedger.Prompt prompt = ledger.prompt(parties, 10L, 20L);
        ledger.grantOnce(parties, 10L, 20L);
        ledger.denyOnce(deniedParties.key(), 10L, 20L);

        ledger.clear();

        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(prompt.token(), parties.targetId(), 11L).status());
        assertFalse(ledger.hasGrant(parties.key(), 11L));
        assertEquals(0L, ledger.denyRemainingTicks(deniedParties.key(), 11L));
    }

    @Test
    void clearTargetDoesNotAffectAnotherTargetsState() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        PortalPrivacyLedger.Parties cleared = parties();
        PortalPrivacyLedger.Parties retained = parties();
        PortalPrivacyLedger.Prompt clearedPrompt = ledger.prompt(cleared, 10L, 20L);
        PortalPrivacyLedger.Prompt retainedPrompt = ledger.prompt(retained, 10L, 20L);

        ledger.clearTarget(cleared.targetId());

        assertEquals(PortalPrivacyLedger.ResolutionStatus.MISSING,
            ledger.resolve(clearedPrompt.token(), cleared.targetId(), 11L).status());
        assertEquals(PortalPrivacyLedger.ResolutionStatus.ACTIVE,
            ledger.resolve(retainedPrompt.token(), retained.targetId(), 11L).status());
    }

    @Test
    void oneShotGrantsAreIsolatedByRequestPurpose() {
        PortalPrivacyLedger ledger = new PortalPrivacyLedger();
        UUID target = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        PortalPrivacyLedger.Parties portal = new PortalPrivacyLedger.Parties(
            target, "Target", requester, "Requester", PortalRequestPurpose.PORTAL);
        PortalPrivacyLedger.Parties relocation = new PortalPrivacyLedger.Parties(
            target, "Target", requester, "Requester",
            PortalRequestPurpose.ENTITY_RELOCATION_SUBJECT);

        ledger.grantOnce(relocation, 10L, 60L);

        assertFalse(ledger.hasGrant(portal.key(), 11L));
        assertTrue(ledger.hasGrant(relocation.key(), 11L));
        assertFalse(ledger.consumeGrant(portal.key(), 11L));
        assertTrue(ledger.consumeGrant(relocation.key(), 11L));
    }

    private static PortalPrivacyLedger.Parties parties() {
        return new PortalPrivacyLedger.Parties(
            UUID.randomUUID(), "Target", UUID.randomUUID(), "Requester");
    }
}
