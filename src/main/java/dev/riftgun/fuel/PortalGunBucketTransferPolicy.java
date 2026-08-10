package dev.riftgun.fuel;

import java.util.function.IntPredicate;

/** Shared policy seam for player interactions, automation, and world-source scooping. */
public final class PortalGunBucketTransferPolicy {
    public static final int MAX_TRANSFER = PortalGunTank.WORLD_SOURCE_AMOUNT;
    public static WorldFluidOverflowPolicy OVERFLOW_POLICY = new WholeSourceOverflowPolicy();

    public static boolean extractFirst(IntPredicate extract, IntPredicate insert) {
        return extract.test(MAX_TRANSFER) || insert.test(MAX_TRANSFER);
    }

    public static boolean extractFirst(IntPredicate sidedExtract, IntPredicate unsidedExtract,
                                       IntPredicate sidedInsert, IntPredicate unsidedInsert) {
        return sidedExtract.test(MAX_TRANSFER)
            || unsidedExtract.test(MAX_TRANSFER)
            || sidedInsert.test(MAX_TRANSFER)
            || unsidedInsert.test(MAX_TRANSFER);
    }

    public static int acceptedExternalFill(int stored, int nominalCapacity, int requested) {
        if (requested <= 0) return 0;
        int offered = Math.min(requested, MAX_TRANSFER);
        int remaining = Math.max(0, nominalCapacity - stored);
        return Math.min(offered, remaining);
    }

    private PortalGunBucketTransferPolicy() {}
}
