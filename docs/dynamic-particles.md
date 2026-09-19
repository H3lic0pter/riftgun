# Dynamic particle effects

The client particle system separates effect emission from particle animation.
Minecraft owns particle ticking, collision and rendering. RiftGun owns effect
registration, source tracking and emission budgets.

## Register and select an effect

Register effects on the client thread after client initialization. IDs use
`namespace:path`; duplicate IDs throw, and `riftgun:none` is reserved.
The callback runs at most once per portal phase tick and is skipped when the
emission budget is exhausted. Use source phase
ticks for animation timing rather than counting callbacks. Emit through the
context sink so the manager can enforce limits and clean up the particles.

```java
ParticleEffectRegistry.register("example:sparks", context -> {
    if (context.portal().phase() != PortalLifecycle.Phase.OPENING) return;
    int rgb = context.color() & 0xFFFFFF;
    var dynamics = new ParticleDynamics(
        20,                     // lifetime in ticks
        0.08F, 0.01F,           // starting and ending quad half-width
        0xFF000000 | rgb, rgb,  // opaque fuel color, then transparent fuel color
        0.2F, 0.96F,            // vanilla gravity strength, velocity drag
        0.1F,                   // spin in radians per tick
        true, false);           // collision, full brightness
    context.particles().spawn(context.portal().placement().center(),
        context.portal().normal().scale(0.05), dynamics);
});
```

In a portal visual renderer, select it by overriding:

```java
@Override public String particleEffectId() { return "example:sparks"; }
```

Existing renderers default to `riftgun:portal_splash`. Renderers that disable
`usesSplashParticles()` retain their particle-free behavior. An unknown,
unregistered or disabled effect emits nothing.

The built-in portal particle effect uses the original `TintableSplashParticle`
through `context.particles().spawnSplash(position, velocity, rgb)`. It preserves
vanilla size, opacity, lifetime, motion, collision and sprite selection without
adding shrink, fade or spin. Its phase counts remain four
while charging, six while opening, four while closing and zero while open.

## Management

- `ParticleEffectRegistry.entries()` returns a read-only snapshot for listing effects.
- `setEnabled(id, false)` pauses an effect; `setEnabled(id, true)` resumes it.
- `unregister(id)` removes its definition. Re-registering the ID installs a new effect.
- `ParticleEffectManager.stop(portalId)` removes that source's current particles;
  subsequent phase ticks may emit again.
- `ParticleEffectManager.clear()` removes all managed particles and emission stamps.
- `ParticleEffectManager.activeCount()` reports currently tracked particles.

Disabling, unregistering or changing a source's effect removes its old particles
on the next active client tick, before new effects claim capacity.
Native portal splashes finish their natural lifetime
after their source closes or disappears. Dynamic particles stop with their source.
World changes and resource reloads clear all managed state. An independent lifetime deadline
also releases particles silently evicted by Minecraft's particle queues. Pausing
does not advance emission. Sources farther than 64 blocks from the player are
culled. The live budget is 2048 particles. All Minecraft particle settings (All,
Decreased and Minimal) allow the same 256 new particles per tick, preserving
the original portal emission density. These limits are shared across all sources.
The sink returns false when it cannot spawn.

All registry and manager calls must run on the client thread. Do not retain the
emission context or its sink beyond the callback.

## Animation and resources

`ParticleDynamics` interpolates size and ARGB color over a bounded lifetime.
It also sets gravity, drag, spin, collision and full brightness. Gravity follows
Minecraft's `0.04 * gravity` acceleration in blocks per tick squared.

Effect registration is separate from NeoForge's particle type registration.
Dynamic effects share the `riftgun:dynamic` engine type and the neutral sprite
pool in `assets/riftgun/particles/dynamic.json`. A resource pack can replace this
pool. Additional effect IDs do not require additional engine types or packets.
Distinct sprite pools require a separately registered engine particle type and
provider. The built-in portal splash reuses `riftgun:portal_splash` and its original
sprite pool in `assets/riftgun/particles/portal_splash.json`.

This is an internal client API, not a stable addon API or a GUI editor. It does
not add network messages or restore the stashed Water Splash portal visual.
