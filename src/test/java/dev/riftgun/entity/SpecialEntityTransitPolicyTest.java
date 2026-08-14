package dev.riftgun.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

final class SpecialEntityTransitPolicyTest {
    @Test
    void portalDenyOverridesConventionalAndDatapackAllowRules() {
        SpecialEntityTransitPolicy<String> policy = SpecialEntityTransitPolicy.compile(
            rules(Set.of("tnt"), Set.of("tnt")), rules(Set.of(), Set.of()), Set.of());

        assertFalse(policy.allowsPortal("tnt", true));
    }

    @Test
    void portalAndRelocationPermissionsAreIndependent() {
        SpecialEntityTransitPolicy<String> policy = SpecialEntityTransitPolicy.compile(
            rules(Set.of("falling_block"), Set.of()),
            rules(Set.of("experience_orb"), Set.of()), Set.of());

        assertTrue(policy.allowsPortal("falling_block", false));
        assertFalse(policy.allowsRelocation("falling_block", false));
        assertFalse(policy.allowsPortal("experience_orb", false));
        assertTrue(policy.allowsRelocation("experience_orb", false));
    }

    @Test
    void emptyDenySetsExposeTreeFastPaths() {
        SpecialEntityTransitPolicy<String> policy = SpecialEntityTransitPolicy.compile(
            rules(Set.of(), Set.of()), rules(Set.of(), Set.of()), Set.of());

        assertFalse(policy.hasPortalDenials());
        assertFalse(policy.hasRelocationDenials());
    }

    @Test
    void sweptTypeRequiresTagAndPortalPermission() {
        SpecialEntityTransitPolicy<String> enabled = SpecialEntityTransitPolicy.compile(
            rules(Set.of("tnt"), Set.of()), rules(Set.of(), Set.of()), Set.of("tnt"));
        SpecialEntityTransitPolicy<String> denied = SpecialEntityTransitPolicy.compile(
            rules(Set.of("tnt"), Set.of("tnt")), rules(Set.of(), Set.of()), Set.of("tnt"));
        SpecialEntityTransitPolicy<String> conventional = SpecialEntityTransitPolicy.compile(
            rules(Set.of(), Set.of()), rules(Set.of(), Set.of()), Set.of("vehicle"));

        assertTrue(enabled.isSweptType("tnt"));
        assertTrue(conventional.isSweptType("vehicle"));
        assertFalse(denied.isSweptType("tnt"));
        assertFalse(enabled.isSweptType("experience_orb"));
    }

    private static SpecialEntityTransitPolicy.AccessRules<String> rules(
            Set<String> allowed, Set<String> denied) {
        return new SpecialEntityTransitPolicy.AccessRules<>(allowed, denied);
    }
}
