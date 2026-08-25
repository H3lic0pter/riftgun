package dev.riftgun.api;

/** Semantic version of the integration contract, independent from the Rift Gun mod version. */
public record RiftGunApiVersion(int major, int minor, int patch) {
    public static final RiftGunApiVersion CURRENT = new RiftGunApiVersion(1, 2, 0);

    public RiftGunApiVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("API version components cannot be negative");
        }
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
