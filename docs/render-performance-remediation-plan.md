# Render and Performance Remediation Plan

## 1. Scope and baselines

- Review evidence is pinned to `b0f8a5c...a3dffc7` (`0.1.0-beta.5` to the
  reviewed remote `dev` head).
- Implementation planning uses the current local `dev` head so later work does
  not accidentally overwrite local changes.
- Every claim below distinguishes the reviewed `a3dffc7` state from later local
  work.
- Endframe rendering and GUI icon alignment are out of scope. Do not change
  either as part of this plan.
- No P0 crash or completely broken portal-transit path was found across the
  reviewed range. The remaining release gates are the confirmed correctness and
  performance items listed below.

## 2. Release gates

The next beta/RC is blocked until all mandatory items are checked:

- [ ] The 26.1.2 message seam works for both `ServerPlayer` and client-side
  `LocalPlayer`; the module-removal denial path cannot throw
  `ClassCastException`.
- [ ] Transit uses a clock-derived `OPEN` invariant for a linked destination;
  a non-ticking destination is never accepted merely because its synced phase
  is stale.
- [ ] Iris shader-pack activation is sampled once per render frame. Shadow-pass
  state is resolved at render submission because Iris may render multiple passes
  inside one frame; scoped renderers perform at most one reflective call per
  visible portal submission.
- [ ] Swirl shader-pack fallback no longer builds a 48-segment CPU disc per
  surface/glow face. Target: at least 95% fewer surface/glow vertices and no
  per-vertex trig or `Uv` allocation.
- [ ] `closeOwnedPortals` scales with the number of portals owned by that player,
  not all loaded entities across all dimensions.
- [ ] Both Minecraft nodes build and test successfully after every phase.
- [ ] Automated structural metrics and controlled in-game JFR evidence are
  recorded for the performance phases.

The shared 48-segment edge remains outside the surface/glow vertex metric. A
quad surface reduces the vertical fallback from 768 to 16 surface/glow vertices
(97.9%); including the unchanged 192-vertex edge, total portal geometry changes
from 960 to 208 vertices (78.3%). JFR must confirm that this remaining shared
edge cost does not defeat the CPU objective.

## 3. Confirmed findings

### 3.1 Iris compatibility query is in the portal hot path

**Status: confirmed, original wording corrected. Priority: P1.**

At `a3dffc7`, the selected visual renderer calls
`PortalShaderCompatibility.currentPath()` once per visible portal per frame:

- `versions/26.1.2/.../ClassicPortalVisualRenderer.java:25`
- `versions/26.1.2/.../SwirlPortalVisualRenderer.java:58`
- `versions/1.21.1` has the same call shape.

`currentPath()` calls `EnvironmentHolder.ENVIRONMENT.snapshot()`. When the Iris
v0 API is linked, `IrisPortalShaderEnvironment.snapshot()` performs two
`Method.invoke` calls: `isShaderPackInUse` and `isRenderingShadowPass`.

Corrections to the original estimate:

- Classic and Swirl are alternatives; both do not render the same portal.
- The cost is one compatibility query and two reflective calls per visible
  portal/frame, not two compatibility queries.
- With no Iris, the environment returns a constant state and performs no
  per-frame reflection.

**Chosen solution**

- Subscribe on both nodes to `RenderFrameEvent.Pre`, which NeoForge documents
  as firing once per frame before rendering.
- Snapshot shader-pack activation there into `PortalRenderFrameState`; do not
  freeze the pass-local shadow flag in that frame snapshot.
- Resolve `isRenderingShadowPass` when the render path is consumed so an Iris
  shadow pass entered later in the same frame selects `SKIP_SURFACE`.
- Add the resolved `PortalSurfaceRenderPath` to
  `PortalVisualRenderContext` through `PortalRenderer`.
- Make Classic and Swirl consume the context value; visual renderers must not
  query Iris directly.
- Keep selection logic pure and inject a counting fake environment in tests.

**Acceptance**

- [ ] Zero visible portals: no renderer-triggered query.
- [ ] One and thirty visible portals in one frame: exactly one activation
  snapshot and one pass-local shadow query per scoped portal submission.
- [ ] A new frame refreshes the state exactly once.
- [ ] Shader-pack enable/disable takes effect on the next frame; shadow-pass
  changes take effect within the current frame.
- [ ] Iris unavailable/API-link failure still selects the documented safe path.

### 3.2 Swirl shader-pack fallback rebuilds excessive CPU geometry

**Status: confirmed, with orientation-dependent counts. Priority: P1.**

At `a3dffc7`, `FALLBACK_SURFACE_SEGMENTS` is 48. Each segment submits a
four-vertex degenerate quad. For a vertical portal:

- one layer, front and back: `48 * 4 * 2 = 384` vertices;
- surface and glow: `384 * 2 = 768` vertices;
- shared edge: `48 * 4 = 192` vertices;
- total: 960 vertices per portal/frame.

For a horizontal portal, front and back are coplanar and only one face is
submitted per layer: 384 surface/glow vertices plus 192 edge vertices, total
576.

The loop also recalculates rim `sin/cos`, creates `RimPoint` records, calculates
the same rotation `sin/cos` per vertex, and creates one or two `Uv` records per
vertex. The custom GPU path submits only 16 surface/glow vertices plus the
shared edge.

The source texture is 128 x 128, has fully transparent corners, and its
non-transparent bounds are `9,11..118,114`. This supports a quad, but rotating
quad-corner UVs leave `[0,1]`; relying on an implicit sampler mode is unsafe.

**Chosen solution**

- Replace each fallback disc face with one textured quad.
- Use an explicit `CLAMP_TO_EDGE` sampler for both fallback surface and glow.
- Preserve opposite winding and mirrored back-face rotation.
- Remove `drawFallbackDisc`, per-segment rim generation, per-vertex trig, and
  `Uv` record creation from this path.
- Keep the current low-segment/precomputed geometry approach only as a fallback
  if the quad fails the visual gate.

**Acceptance**

- [ ] Resource test asserts transparent texture border/corners before clamp is
  relied upon.
- [ ] Vertical surface + glow is at most 16 vertices; horizontal is at most 8.
- [ ] No `Math.sin`, `Math.cos`, `RimPoint`, or `Uv` allocation occurs inside
  fallback vertex submission.
- [ ] Front/back spin direction matches the custom GPU path.
- [ ] No repeated/clamped smear appears at rotated UV extremes.
- [ ] Iris screenshots pass for vertical/horizontal, mapped/unmapped,
  front/back, surface/glow, normal/max GUI scale, and 0/90/180/270-degree
  rotation samples.
- [ ] Controlled JFR shows reduced render-thread CPU/allocation for 10 and 30
  visible fallback portals.

### 3.3 `closeOwnedPortals` scans every loaded entity

**Status: confirmed. Priority: P2 release gate.**

Both node-private `PortalEntity` implementations loop through every
`ServerLevel` and every `level.getAllEntities()`, then filter by owner UUID.
Opening or explicitly closing portals therefore costs `O(all loaded entities)`.

**Chosen solution**

- Add a shared authoritative `PortalOwnerIndex`.
- Partition state by `MinecraftServer` identity, then owner UUID; store live
  `PortalEntity` references in each owner set.
- Register in `RiftLifecycle.entityJoined` and unregister in `entityLeft`.
  These hooks cover new entities, save-loaded entities, `discard`, and failed
  pair creation cleanup.
- Remove dead/stale references during lookup and clear server state from
  `serverStopped`.
- Do not retain a full-world-scan fallback in the close path.
- Keep new pair IDs excluded until the previous pair has started closing.

**Acceptance**

- [ ] Closing one owner visits only that owner's indexed portals.
- [ ] Portals owned by another player remain untouched.
- [ ] Cross-dimension pairs close together.
- [ ] Pair creation failure leaves no index entry.
- [ ] Save reload registers existing portals.
- [ ] Entity removal, player logout, respawn, and server restart clear entries.
- [ ] A counting test proves lookup work is proportional to owner portal count,
  independent of unrelated entity count.

### 3.4 The 26.1.2 message seam can crash on the client

**Status: confirmed as a current correctness bug, not merely a future hazard.
Priority: P1 release gate.**

`Msg.displayClientMessage(Player, ..., true)` casts `Player` to `ServerPlayer`
in the 26.x branch. `PortalModuleMenu.ModuleSlot.mayPickup` calls this helper for
a client-side generic player when module removal is denied, so the current path
can throw `ClassCastException`.

The 26.1.2 API already provides the correct polymorphic methods:

- `ServerPlayer.sendOverlayMessage` sends the overlay packet.
- `LocalPlayer.sendOverlayMessage` dispatches to the client overlay listener.
- Both override `Player.sendOverlayMessage`.

**Chosen solution**

- Preserve the generic `Player` helper contract.
- On 26.x, use `player.sendOverlayMessage(message)` for action-bar messages and
  `player.sendSystemMessage(message)` for chat.
- Delete the cast and direct `ClientboundSystemChatPacket` construction.

**Acceptance**

- [ ] Denied module removal displays a client overlay without exception.
- [ ] Server action-bar and chat messages still reach the player.
- [ ] 1.21.1 continues using its native `displayClientMessage` branch.
- [ ] Tests cover both action-bar values and the client-side menu call site.

### 3.5 Non-ticking destination bypass weakens transit readiness

**Status: confirmed. Priority: P1/P2 release gate.**

`PortalTransitOrchestrator` currently rejects a non-`OPEN` target only when its
chunk is entity-ticking. This fixed async destination loading, but also permits
a non-ticking target whose synced phase is stale `OPENING` or `CLOSING`.

The portal pair already stores absolute `lifecycleStartedAt` and
`closeStartedAt` values. `PortalPairClock.phase(...)` derives the correct phase
without requiring the destination entity to tick.

**Chosen solution**

- Add `PortalEntity.lifecyclePhaseAt(now)` on both node implementations.
- Resolve it through `PortalPairClock`.
- Require the derived target phase to be exactly `OPEN` before normal transit.
- Remove the `targetTicking` bypass.

**Acceptance**

- [ ] Entry to an unloaded destination completes after its clock reaches
  `OPEN`.
- [ ] `CHARGING`, `OPENING`, `CLOSING`, and `CLOSED` destinations reject transit
  whether or not their chunks tick.
- [ ] Destination entity exists before movement begins.
- [ ] One entry produces exactly one arrival and no immediate bounce/duplicate.
- [ ] Tests cover both Minecraft nodes and a real unloaded-chunk integration
  scenario.

### 3.6 Transit tick allocations

**Status: allocations confirmed; performance impact not yet established.
Priority: profile-only, not a release-blocking code change.**

Every open portal tick currently creates or materializes an inflated search
`AABB`, `PortalTransitEligibility`, entity result lists, a `HashSet<UUID>`, and
multiple `PortalPlacement` values. `PortalEntity.placement()` constructs a new
record on each call.

Do not add caching solely to reach zero allocation. First record JFR allocation
and CPU data. If this path is material, prefer tick-local reuse of one placement
and bounds before entity-lifetime caching. Entity-lifetime caching requires
correct invalidation for position, yaw, attachment, synced data, and NBT load.

- [ ] Profile 1/10/30 open portals with no touching entities.
- [ ] Profile the same scenes with entities in the search bounds.
- [ ] Record allocation contributors and `level.getEntities` cost separately.
- [ ] Open a follow-up implementation phase only if this code is a material
  contributor; document threshold and invalidation proof first.

### 3.7 Classic translucent face ordering

**Status: not confirmed as a bug; regression test only.**

Classic uses six translucent faces, no depth write, and upload sorting. Code
inspection alone does not prove incorrect per-face ordering. Do not change the
pipeline without a captured failure.

- [ ] Smoke test front, back, near-90-degree side view, and steep wall angle.
- [ ] Repeat custom/fallback and Iris on/off.
- [ ] Attach screenshots only if a reproducible artifact exists.

## 4. Staged implementation

Each phase is one independently revertible commit. Before every commit run
`git diff --check`, both node tests, and both node builds. Preserve the
Stonecutter active-node convention documented in `docs/port-26.1.2-checklist.md`.

### Phase 1 - safe message seam

- [ ] Replace the 26.x cast/packet branch with polymorphic Player methods.
- [ ] Add client and server behavior tests.
- [ ] Run module-removal denial smoke test.
- Commit: `fix(core): make player messages side-safe`

### Phase 2 - clock-derived transit readiness

- [ ] Add and test `lifecyclePhaseAt(now)`.
- [ ] Delete the non-ticking phase bypass.
- [ ] Add unit and unloaded-destination integration regressions.
- Commit: `fix(port): derive transit readiness from pair clock`

### Phase 3 - frame-local activation and pass-local shadow state

- [ ] Introduce `PortalRenderFrameState`.
- [ ] Refresh it from `RenderFrameEvent.Pre` on both nodes.
- [ ] Inject render path through `PortalVisualRenderContext`.
- [ ] Remove renderer-owned compatibility queries.
- [ ] Add activation/pass query-count tests and capture baseline/after JFR.
- Commit: `perf(render): snapshot shader state once per frame`

### Phase 4 - quad shader-pack fallback

- [ ] Add explicit clamp sampler fallback render types.
- [ ] Replace CPU discs with front/back quads.
- [ ] Add resource, vertex-budget, winding, and UV tests.
- [ ] Complete Iris visual matrix and controlled JFR comparison.
- Commit: `perf(render): replace swirl fallback discs with quads`

### Phase 5 - owner portal index

- [ ] Add server-scoped `PortalOwnerIndex`.
- [ ] Wire entity join/leave and server-stop lifecycle.
- [ ] Replace both node scans with indexed lookup.
- [ ] Add lifecycle, isolation, cross-dimension, stale-entry, and work-count
  tests.
- Commit: `perf(server): index portals by owner`

## 5. Performance evidence protocol

Keep raw recordings outside Git unless small and intentionally added. Record
commands, scenario, hardware, JVM flags, mod list, shader pack, resolution, and
result summary in the implementing commit or its follow-up note.

Use the same world and fixed camera for before/after runs:

| Scenario | Portals | Iris | Required evidence |
|---|---:|---|---|
| Baseline custom | 1 / 10 / 30 | Off | JFR CPU + allocation |
| Compatibility fallback | 1 / 10 / 30 | On | JFR CPU + allocation |
| Shadow pass | 10 / 30 | On | one activation snapshot/frame, surfaces skipped |
| Owner close | 2 owned + many unrelated entities | N/A | indexed visit count |
| Transit idle | 1 / 10 / 30 | N/A | JFR allocation profile |

Do not use FPS alone as proof. Structural counters provide deterministic gates;
JFR verifies that the structural reduction matters in the real client/server.

## 6. Verification commands

Use the repository's actual task names if they differ, but the required scope is:

```powershell
.\gradlew :1.21.1:test :26.1.2:test
.\gradlew :1.21.1:build :26.1.2:build
git diff --check
```

Manual release smoke:

- [ ] 1.21.1 client loads and each portal style in scope renders.
- [ ] 26.1.2 client loads with Iris absent.
- [ ] 26.1.2 client loads with Iris and a shader pack active.
- [ ] Normal, deferred, cross-dimension, and unloaded-destination transit work
  exactly once.
- [ ] Player logout/respawn and explicit close affect only owned portals.
- [ ] Classic view-angle matrix shows no new artifact.

## 7. Completion record

Fill this table as phases land:

| Phase | Commit | Tests/builds | JFR/visual evidence | Status |
|---|---|---|---|---|
| 1 - Msg | `ad0a8c7` | targeted tests + both node assembles pass | N/A | Automated complete; smoke pending |
| 2 - transit readiness | `a68e074` | clock/source tests + both node assembles pass | integration trace pending | Automated complete; integration pending |
| 3 - Iris frame state | `dfaf766` | query-count tests + both node assembles pass | activation snapshot/frame proven; pass-local correction applied; JFR pending | Automated correction complete; JFR pending |
| 4 - Swirl fallback | `097ac22` | resource/vertex/UV tests + both node assembles pass | 4/8 vertices per layer proven; JFR/screenshots pending | Automated complete; visual/JFR pending |
| 5 - owner index | `f04a150` | lifecycle/isolation/stale/work-count tests + both node assembles pass | two visits with 1,000 unrelated portals; server profile pending | Automated complete; profile pending |

Final automated verification on 2026-08-21:

- Both node packaging builds pass with tests excluded.
- Full suites execute 279 tests on 1.21.1 and 284 tests on 26.1.2. Each
  has the same pre-existing failure in
  `PortalVisualSelectionTest.registryExposesBuiltinsInUiOrder`; all other tests
  pass.
- `git diff --check` passes for the implementation commits.
- Manual client, Iris/shader-pack, transit, screenshots, and JFR evidence remain
  pending because this execution environment did not run an interactive game
  client.

## 8. Post-review remediation checklist

Scope remains the reviewed Msg, transit, Iris, Swirl fallback, and owner-index
work. EndFrame and GUI alignment are explicitly excluded.

- [x] Treat shader-pack activation as frame-local, but resolve Iris shadow-pass
  state at render submission time; cover a shadow transition inside one frame on
  both nodes.
- [x] Make swept projectile and swept special-entity transit use
  `lifecyclePhaseAt(now)` from the same server clock as normal transit.
- [x] Add an explicit `clamp: true` texture contract for the 1.21.1 fallback and
  verify the metadata resource.
- [x] Keep source-contract tests as boundary guards; add Msg dispatch, shader
  pass-state, pair-clock, and owner-index Store behavior tests at pure seams.
  Full unloaded-chunk/game integration remains a manual gate.
- [x] Run targeted regressions, both node suites/builds, and `git diff --check`;
  report the excluded EndFrame selection-test failure separately.

Post-review verification on 2026-08-21:

- Targeted shader, Swirl resource, and transit readiness regressions pass on both
  nodes.
- Both packaging builds pass with tests excluded; both jars contain
  `portal_surface.png.mcmeta`.
- Full suites reach the single excluded EndFrame selection failure: 279 tests on
  1.21.1 and 284 tests on 26.1.2, with all other tests passing.
- `git diff --check` passes. Interactive Iris shadow-pass and unloaded-chunk
  transit smoke tests remain manual release gates.
