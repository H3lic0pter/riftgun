package dev.riftgun.service;

final class RandomRiftSearchPolicy {
    static boolean hasCapacity(int activeSearches, int maximumConcurrentSearches) {
        return activeSearches < maximumConcurrentSearches;
    }

    static CandidateProbe candidateProbe(int chunkX, int chunkZ, int minimumBuildHeight) {
        return new CandidateProbe((chunkX << 4) + 8, minimumBuildHeight + 1, (chunkZ << 4) + 8);
    }

    static int searchCeiling(boolean ceilingDimension, int minimumBuildHeight,
                             int logicalHeight, int maximumBuildHeight) {
        return ceilingDimension ? minimumBuildHeight + logicalHeight : maximumBuildHeight;
    }

    record CandidateProbe(int x, int y, int z) {}

    private RandomRiftSearchPolicy() {}
}
