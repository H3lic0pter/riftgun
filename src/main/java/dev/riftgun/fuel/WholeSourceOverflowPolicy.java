package dev.riftgun.fuel;

public final class WholeSourceOverflowPolicy implements WorldFluidOverflowPolicy {
    @Override
    public int acceptedAmount(int stored, int nominalCapacity, int sourceAmount) {
        if (stored < 0 || nominalCapacity < 1 || sourceAmount < 1 || stored >= nominalCapacity) return 0;
        return sourceAmount;
    }
}
