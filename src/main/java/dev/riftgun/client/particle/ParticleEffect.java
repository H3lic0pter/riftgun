package dev.riftgun.client.particle;

/** Called at most once per source phase tick on the client thread, when budget permits.
 * Callbacks may be skipped; use the source's phase ticks for timing, not callback counts. */
@FunctionalInterface
public interface ParticleEffect {
    void emit(ParticleEffectContext context);
}
