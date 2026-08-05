package dev.riftgun.fuel;

@FunctionalInterface
public interface WorldFluidOverflowPolicy {
    int acceptedAmount(int stored, int nominalCapacity, int sourceAmount);
}
