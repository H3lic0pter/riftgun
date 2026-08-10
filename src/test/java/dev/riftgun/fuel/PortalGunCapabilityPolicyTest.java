package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalGunCapabilityPolicyTest {
    @Test
    void automationCapabilityIsAvailableRegardlessOfBucketMode() {
        assertTrue(PortalGunCapabilityPolicy.allows(
            PortalGunCapabilityPolicy.Access.CAPABILITY, false));
        assertTrue(PortalGunCapabilityPolicy.allows(
            PortalGunCapabilityPolicy.Access.CAPABILITY, true));
    }

    @Test
    void directBlockTransferStillRequiresBucketMode() {
        org.junit.jupiter.api.Assertions.assertFalse(PortalGunCapabilityPolicy.allows(
            PortalGunCapabilityPolicy.Access.DIRECT_INTERACTION, false));
        assertTrue(PortalGunCapabilityPolicy.allows(
            PortalGunCapabilityPolicy.Access.DIRECT_INTERACTION, true));
    }
}
