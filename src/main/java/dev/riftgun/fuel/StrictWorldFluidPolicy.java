package dev.riftgun.fuel;

public final class StrictWorldFluidPolicy implements WorldFluidOverflowPolicy {
    @Override
    public int acceptedAmount(int stored, int nominalCapacity, int sourceAmount) {
        if (stored < 0 || sourceAmount < 1 || stored + sourceAmount > nominalCapacity) return 0;
        return sourceAmount;
    }
}
