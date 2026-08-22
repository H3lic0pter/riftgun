package dev.riftgun.client.compat.immersiveportal;

/** Loading cover that fades after the destination client chunk arrives. */
final class ImmersivePortalCoverState {
    private static final float FADE_STEP = 0.2F;
    private boolean ready;
    private float readiness;

    void markReady() {
        ready = true;
    }

    void tick() {
        if (ready) readiness = Math.min(1.0F, readiness + FADE_STEP);
    }

    float readiness() {
        return readiness;
    }
}
