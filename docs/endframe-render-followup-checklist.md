# EndFrame rendering follow-up checklist

## Scope and fixed decisions

- [x] Keep the existing EndFrame texture, easing, depth offsets and
  `STAR_RADIUS_SCALE = 0.7773F`.
- [x] Keep the custom shader path for normal rendering.
- [x] Under an active shader pack, leave the inner disc empty and render only
  the rotating frame through a clamped entity pipeline.
- [x] Skip every EndFrame submission in an Iris shadow pass.
- [x] Align the shader-pack fallback vertex budget with Swirl.
- [x] Use `0.45F` for the additive fallback glow multiplier in EndFrame and
  Swirl on both supported nodes.

## Geometry and hot path

- [x] Reduce the star rim from 48 to 24 segments.
- [x] Submit valid degenerate quads (`center, p1, p2, center`) to the QUADS
  pipeline, including opposite back-face winding.
- [x] Precompute the 25 rim points; do not run trigonometry in the star vertex
  loop.
- [x] Expand basis components directly; do not allocate `Vec3` or temporary
  geometry records per star vertex.
- [x] Compute fallback frame rotation sine/cosine once per visible portal and
  reuse it for both faces and both layers.
- [x] Use CPU-rotated UVs with `CLAMP_TO_EDGE` in the shader-pack fallback.

| Path | Star | Frame surface | Frame glow | Total |
| --- | ---: | ---: | ---: | ---: |
| EndFrame custom | 192 | 8 | 0 | 200 |
| EndFrame shader fallback | 0 | 8 | 8 | 16 |
| Swirl shader fallback | 192 edge | 8 | 8 | 208 |

## Automated verification

- [x] Geometry tests lock segment count, winding and 192/200/16 vertex
  budgets.
- [x] Source-contract tests lock precomputation, one render-path lookup,
  fallback routing, clamp resources and the shared `0.45F` glow multiplier.
- [x] `1.21.1:test`, `1.21.1:build`, `26.1.2:test` and `26.1.2:build` pass
  together (2026-08-21, Gradle 9.7.0, 16 actionable tasks).
- [x] `git diff --check` passes.

## Real shader-pack gate

Test pack: `ComplementaryReimagined_r5.8.1.zip` from the user's PCL
instances. Test-only Iris, Sodium and shader-pack files live under ignored
`versions/*/run/` directories and must never be committed.

- [x] 26.1.2 reaches a shader-lit single-player world with Iris 1.11.3 and
  Sodium 0.9.1; the log contains no RiftGun pipeline compilation error.
- [ ] Capture a visible EndFrame portal with shader pack off/on on 1.21.1.
- [ ] Capture a visible EndFrame portal with shader pack off/on on 26.1.2.
- [ ] Verify front/back views, frame rotation, restrained glow and zero
  EndFrame submissions during the shadow pass.
- [ ] Record controlled JFR runs for 1/10/30 visible portals, EndFrame versus
  Swirl, shader pack off/on, with the same world, camera, resolution and JVM.
- [ ] Require EndFrame render-thread CPU and allocation to be no more than
  `1.15x` Swirl for every matching scene before merge/release.

The screenshot/JFR items remain mandatory gates. The automated client launched
from the current PTY could not be attached to or focused from the verification
process, so no portal-free world screenshot is accepted as EndFrame evidence.

## External observation

On 26.1.2, disconnecting the shader-enabled development client produced Iris
`Missing program minecraft:pipeline/gui_* in override list` fatal messages and
a client crash report. The stack is in `Minecraft.disconnect`/GUI rendering,
not a RiftGun EndFrame pipeline. Track it as an Iris/NeoForge integration risk;
do not treat it as either proof against this change or a passed EndFrame gate.

## Commit sequence

- [x] `perf(render): optimize endframe geometry`
- [x] `fix(render): add endframe shader fallback`
- [x] `fix(render): reduce portal glow intensity`
