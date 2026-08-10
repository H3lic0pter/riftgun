package dev.riftgun.fuel;

import java.util.function.IntPredicate;

/** Shared policy seam for player interactions, automation, and world-source scooping. */
public final class PortalGunBucketTransferPolicy {
    public static final int MAX_TRANSFER = PortalGunTank.WORLD_SOURCE_AMOUNT;
    public static WorldFluidOverflowPolicy OVERFLOW_POLICY = new WholeSourceOverflowPolicy();

    public static boolean extractFirst(IntPredicate extract, IntPredicate insert) {
        return extract.test(MAX_TRANSFER) || insert.test(MAX_TRANSFER);
    }

    public static int acceptedExternalFill(int stored, int nominalCapacity, int requested) {
        if (requested <= 0) return 0;
        int offered = Math.min(requested, MAX_TRANSFER);
        int accepted = OVERFLOW_POLICY.acceptedAmount(stored, nominalCapacity, offered);
        return Math.max(0, Math.min(offered, accepted));
    }

    private PortalGunBucketTransferPolicy() {}
}
