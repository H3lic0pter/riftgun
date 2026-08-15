# Port Checklist: Minecraft 1.21.1 → 26.1.2 (NeoForge)

> **Audience**: an executing agent with no prior session context. Follow phases
> in order. Do not skip verification steps. Each phase ends with a commit.
>
> **Status**: not started. Branch: create `port/26.1.2` from `dev` first.

---

## 0. Facts you may rely on (verified 2026-08-15)

Do NOT re-research these. If something contradicts them, STOP and report.

| Fact | Value |
|---|---|
| Target Minecraft | 26.1.2 (year-based versioning; released 2026-04-09) |
| NeoForge line | `26.1.2.x` — use a stable build, minimum `26.1.2.71`. Query latest: `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml` and take the newest `26.1.2.*` without `-beta` |
| Java | 25 (already wired: `versions/26.1.2/gradle.properties` has `java_version=25`) |
| Gradle | 9.7.0 in wrapper — OK (26.1 needs ≥9.1) |
| ModDevGradle | 2.0.144 in `build.gradle.kts` — OK (26.1 needs ≥2.0.141) |
| Obfuscation | REMOVED in 26.1 — official Mojang names, including parameters |
| Migration primers | `https://docs.neoforged.net/primer/docs/26.1/` for the target; intermediate versions (1.21.2 … 1.21.11) each have `.../primer/docs/<version>/` |
| Stonecutter | 0.9.7. Node builds compile **generated** sources at `versions/<v>/build/generated` produced from the shared root `src/`, plus the node-private `versions/<v>/src`. Conditional syntax below. |

### Key API deltas (1.21.1 → 26.1.2, cumulative)

| 1.21.1 API | 26.1.2 replacement | Changed in | Hits |
|---|---|---|---|
| `ResourceLocation` | `Identifier` (code-wide rename) | 1.21.11 | shared tree, many files |
| `addAdditionalSaveData`/`readAdditionalSaveData` | `ValueInput`/`ValueOutput` (+`ProblemReporter`) | 1.21.6 | `PortalEntity`, `PortalDataStore` |
| Model-level RenderType in block/item models | removed; per-quad translucent computed from texture alpha (`force_translucent` option) | 26.1 | `portal_gun.json` etc. |
| `BlockModel` | `CuboidModel`; six built-in model types | 26.1 | model JSONs |
| `Material` / `SpriteGetter` | `SpriteId` / `MaterialBaker` | 26.1 | render code |
| Shader JSON programs | in-code `RenderPipeline` (1.21.5); `RenderType` composite → `RenderSetup` split (1.21.11) | 1.21.5 / 1.21.11 | `PortalRenderTypes`, shader JSONs |
| `EntityRenderer.render*` | `EntityRenderState` extract/create (1.21.2) + `submit*`/`SubmitNodeCollector` (1.21.9); no `MultiBufferSource` | 1.21.2/1.21.9 | `PortalRenderer`, `EntityRelocationPortalRenderer` |
| Item model: `BakedModelWrapper`, `ItemColor`, `ModelEvent.ModifyBakingResult` | Client Items JSON (`assets/<ns>/items/<id>.json`) + item-model resolver (1.21.4); items moved to separate `minecraft:items` atlas (1.21.11) | 1.21.4/1.21.11 | `PortalGunLayeredModel`, `PortalGunItemColors`, `ClientModEvents` |
| `GuiGraphics` | prepare/render split (1.21.6); class renamed `GuiGraphicsExtractor` (26.1) | 1.21.6/26.1 | 4 screen classes |
| Recipe serializers | codec records | 26.1 | `recipe/*` |
| `net.minecraft.nbt.CompoundTag` | **still exists** — do not preemptively change NBT usages; only save/load signatures changed | — | — |

### Stonecutter conditional syntax (exact)

```java
// closed scope
//? if >=1.21.11 {
Identifier id = Identifier.of("riftgun", "path");
//?} else {
ResourceLocation id = ResourceLocation.fromNamespaceAndPath("riftgun", "path");
//?}

// line scope (affects next non-empty line only)
//? if >=1.21.11
Identifier id = ...;

// local swap on one line
//$ if >=1.21.11 'Identifier' else 'ResourceLocation'
ResourceLocation id = ...;

// build-script swap (declare in stonecutter {} block):
//   swaps["identifier_type"] = when { current.parsed >= "1.21.11" -> "Identifier" else -> "ResourceLocation" }
```

Version expressions: `>=1.21.11`, `<1.21.11`, `1.21.1`, and `26.1.2 > 1.21.11`
(major component comparison — year-based 26.x sorts above 1.x).

---

## 1. Global rules (read twice before starting)

1. **Never modify `versions/1.21.1/src`** except when a file moves OUT of the
   shared tree (then a 1.21.1 copy MUST land in `versions/1.21.1/src`).
2. **File-move rule**: if a shared-tree file needs version-specific changes on
   more than ~3 lines, move it: one copy per node (`versions/1.21.1/src` AND
   `versions/26.1.2/src`), delete from shared `src/`. Never leave the same FQCN
   in two source roots — `SharedSourceBoundaryTest.nodeSourcesDoNotDuplicateSharedClasses`
   enforces this.
3. **Small-diff rule**: ≤3 version-specific lines → stonecutter conditional in
   the shared file. Prefer a build-script swap when the same token repeats.
4. **After every phase**: (a) active-node build green, (b) 1.21.1 build green,
   (c) `./gradlew test` green, (d) commit with message `port(26.1.2): <phase>`.
5. **Verify against the real jar**, not the root `build/` dir — node jars are
   at `versions/<v>/build/libs/`.
6. **Do not guess APIs.** For every changed class, open the primer page for the
   version listed in the table above before writing code.
7. **Stop and ask the user** if: dependency resolution for NeoForge 26.1.2.x
   fails; a required external mod (JEI, RyoamicLights) has no 26.1.2 build; or
   a change requires a design decision (behavior change, config break, save
   format change).
8. RunClient quirk on this machine: early window must stay disabled — keep
   `run/fml.toml` (`earlyWindowControl=false`) for the 26.1.2 node's run dir
   too if the same crash appears (`FileSystemNotFoundException` in
   `DisplayWindow.setupMinecraftWindow`).

Build commands (active version = the one in `stonecutter.gradle.kts`
`stonecutter active "..."`):

```bash
./gradlew :<node>:build          # build ONE node, e.g. :26.1.2:build
./gradlew :1.21.1:build :26.1.2:build   # build ALL nodes (Phase 0-verified; no chiseled task in 0.9.7)
./gradlew :<node>:test
```

Stonecutter 0.9.7 workflow facts (verified in Phase 0):

- **The active node compiles the shared tree ON DISK directly.** Non-active
  nodes compile preprocessed copies at `versions/<v>/build/generated/stonecutter/{main,test}/java`.
- Conditional state is materialized on disk (inactive branches wrapped in
  `/* */`, markers preserved — reversible).
- After editing conditionals, run the task **`Refresh active project`** to
  re-materialize the active state.
- Switch versions with the task **`Set active project to <version>`**.
- **Run `Reset active project` (back to 1.21.1) before every commit** — this
  is the tool's own stated convention and keeps the committed shared tree in
  the 1.21.1 state.
- Conditional smoke test passed: `//? if >=1.21.11` resolved to the new branch
  in the 26.1.2 generated copy and the old branch on disk (1.21.1 active).

---

## 2. Phase list (execute in order)

| # | Phase | Exit criteria |
|---|---|---|
| 0 | Recon & node enablement | both nodes configure; chiseled build task identified |
| 1 | Compile skeleton (no feature porting) | `:26.1.2:compileJava` fails ONLY on missing symbols, not resolution |
| 2 | NBT migration | node `PortalEntity`/`PortalDataStore` compile |
| 3 | Shared-tree mechanical pass | `:26.1.2:compileJava` green for shared sources |
| 4 | Node bootstrap & registries | mod loads on `runServer` (26.1.2) to main menu/state |
| 5 | Fluids & fuel | fluid registration compiles; scoop logic compiles |
| 6 | Item model system | gun item renders in inventory (client smoke) |
| 7 | Shaders & render types | portal surfaces compile + render |
| 8 | Entity renderers | portal entities render |
| 9 | GUI screens | all 4 screens open |
| 10 | Particles / lights / compat | compile + graceful no-mod fallback |
| 11 | Resources & data pass | datapack loads; tags resolve |
| 12 | Full verification | both nodes build+test green; runClient+runServer smoke; jar diff |

---

## 3. Detailed phases

### Phase 0 — Recon & node enablement (0.5 day)

- [ ] `git checkout dev && git checkout -b port/26.1.2`
- [ ] Edit `settings.gradle.kts`: `versions("1.21.1", "26.1.2")`.
- [ ] Fill `versions/26.1.2/gradle.properties`:
      `minecraft_version=26.1.2`, `neoforge_version=<latest stable 26.1.2.x>`.
      Remove the placeholder comment.
- [ ] Create `versions/26.1.2/src/main/java/` (empty).
- [ ] `./gradlew tasks --all | grep -i chiseled` — record the build-all task
      name in this file (expected something like `chiseledBuild`; verify, don't
      guess). Record it here: `________________`
- [x] Verify generated-source layout: `versions/<v>/build/generated/stonecutter/...`
      exists for NON-active nodes; the ACTIVE node compiles the on-disk tree.
- [x] Conditional smoke test: verified `>=1.21.11` resolves correctly per node
      (26.1.2 generated copy gets the new branch; active 1.21.1 disk state the
      old branch). Remember: run `Refresh active project` after editing
      conditionals. Smoke test removed.
- [ ] Switching versions: to work on 26.1.2 set `stonecutter active "26.1.2"`
      in `stonecutter.gradle.kts`; switch back to `"1.21.1"` before committing
      is NOT required — pick one, note it in the commit body if ambiguous.
- [ ] Commit.

### Phase 1 — Compile skeleton (0.5–1 day)

Goal: dependency resolution + toolchain proven. Copy the 1.21.1 node-private
files as the starting point — they will NOT compile; that's expected.

- [ ] Copy `versions/1.21.1/src/main/java/dev/riftgun/` → `versions/26.1.2/src/main/java/dev/riftgun/` (bootstrap files only at first: `RiftGun.java`, `config/`, `data/PortalDataStore.java`, `network/NeoForgeNetworkAdapter.java`, `portal/PortalEntity.java`).
- [ ] `./gradlew build` with active=26.1.2. Accept: compile errors (missing
      symbols: `ResourceLocation`, `ModConfigSpec`, etc.). Reject/STOP:
      dependency-resolution errors, toolchain errors (Java 25 not found —
      install Temurin 25 and ensure Gradle can auto-provision).
- [ ] Commit (compiling state not required at this phase; failing compile of
      the port node is fine as long as 1.21.1 still builds via chiseled task
      — if the chiseled task fails the whole build, build nodes individually:
      `./gradlew :1.21.1:build`).

### Phase 2 — NBT migration (1 day)

Primer: 1.21.6 (`ValueInput`/`ValueOutput`) + 26.1 page.

- [ ] `versions/26.1.2/.../portal/PortalEntity.java`: rewrite
      `addAdditionalSaveData`/`readAdditionalSaveData` to the new
      input/output API. Keep the same NBT keys (save-format compatibility).
- [ ] Same for `data/PortalDataStore.java`.
- [ ] Do NOT touch shared files that merely use `CompoundTag` (still exists).
- [ ] `:26.1.2:compileJava` — those two files no longer error on NBT.
- [ ] Commit.

### Phase 3 — Shared-tree mechanical pass (1–2 days)

Goal: shared `src/` compiles for BOTH nodes.

- [ ] Set active=26.1.2, run build, collect every shared-file error.
- [ ] For each `ResourceLocation` error apply the identifier conditional —
      prefer ONE build-script swap (`identifier_type` and, if needed,
      `identifier_of` for the factory call difference) over per-file blocks.
- [ ] Common constructors differ too: 1.21.1 `fromNamespaceAndPath(ns, path)`
      vs 26.x `Identifier.of(ns, path)` — handle with a swap or a tiny static
      helper behind a conditional.
- [ ] Any shared file needing deeper changes → apply the file-move rule
      (Rule 2; copy to BOTH nodes).
- [ ] `SharedSourceBoundaryTest` stays green.
- [ ] Both nodes: `:26.1.2:compileJava` green AND `:1.21.1:build` green.
- [ ] Commit.

### Phase 4 — Node bootstrap & registries (1–2 days)

Port, using the 26.1 primer + a fresh MDG 26.1 template project as reference:

- [ ] `RiftGun.java`: mod constructor, `DeferredRegister` usage, creative tab
      hook, config registration (`ModConfig` API — verify names).
- [ ] `config/{ClientConfig,ServerConfig}.java`: `ModConfigSpec` — verify it
      still exists in NeoForge 26.1; if renamed, port accordingly.
- [ ] `network/NeoForgeNetworkAdapter.java`: payload registration — payload
      API is expected to be stable since 1.21.2; verify, then fix compile.
- [ ] `CommonEvents.java` (shared!): if NeoForge event class names changed,
      either conditionals (small) or move to nodes (large) per Rules 2–3.
- [ ] Smoke: `./gradlew :26.1.2:runServer` reaches "Done" state (world loads,
      riftgun mod in list, no crash).
- [ ] Commit.

### Phase 5 — Fluids & fuel (0.5–1 day)

- [ ] Phase 1 finding: `FluidUtil`, `IFluidHandler`, `FluidActionResult`,
      `FluidHandlerItemStack` are **deprecated with removal planned** in
      NeoForge 26.1.2 (compiler shows `[removal]` warnings). Find the
      replacement API in the NeoForge changelog before porting.
- [ ] `fuel/*` are SHARED NeoForge-API files (`FluidStack`, `FluidType`,
      `BaseFlowingFluid`, `SimpleFluidContent`, `FluidHandlerItemStack`,
      `IFluidHandler`). Verify each against the 26.1 NeoForge changelog;
      apply Rules 2–3 per file.
- [ ] `:26.1.2:compileJava` green for fuel package.
- [ ] Commit.

### Phase 6 — Item model system (2–3 days) — hardest visual phase

- [ ] Read 1.21.4 primer "Client Items" + 26.1 primer model pipeline sections
      BEFORE coding.
- [ ] Create `assets/riftgun/items/portal_gun.json` (26.1.2 node resources;
      node resources root: `versions/26.1.2/src/main/resources`).
- [ ] Port `PortalGunLayeredModel`'s 16-variant filtering to the 26.x
      item-model resolver mechanism. Keep the semantic core: it consumes
      `PortalGunVisualSnapshot` (shared) — do not duplicate that logic.
- [ ] `PortalGunItemColors` tint indices 2–10 contract is version-neutral:
      reimplement the registration on the new tint hook.
- [ ] 26.1 removed model-level RenderType: review `portal_gun.json` — the
      translucent glass/fuel behavior now comes from texture alpha per quad;
      verify visually (gun in inventory + hand, rotate camera — the beta.4
      see-through bug must NOT regress).
- [ ] Items atlas (1.21.11 split): texture paths may need moving/declaring.
- [ ] Smoke: runClient → creative inventory → Rift Gun item visible & correct.
- [ ] Commit.

### Phase 7 — Shaders & render types (1–2 days)

- [ ] Port `PortalRenderTypes` + the two shader programs
      (`rendertype_rift_portal`, `rendertype_rift_portal_swirl`) to in-code
      `RenderPipeline` (1.21.5) and the 26.x `RenderSetup` form.
- [ ] Shader JSON files under `assets/.../shaders/` are replaced by code —
      delete node copies of the JSONs if the new pipeline doesn't read them.
- [ ] Iris compat (`IrisPortalShaderEnvironment`): reflective calls target a
      different Iris API on 26.x. If Iris for 26.1.2 isn't installed locally,
      ensure the fallback path compiles and runs (compat is optional).
- [ ] Smoke: place two portals; surfaces render.
- [ ] Commit.

### Phase 8 — Entity renderers (1–2 days)

- [ ] `PortalRenderer` + `EntityRelocationPortalRenderer`: create/extract
      `EntityRenderState` (1.21.2) and `submit*` methods (1.21.9); no
      `MultiBufferSource`.
- [ ] `ClassicPortalVisualRenderer` / `SwirlPortalVisualRenderer`: port vertex
      submission to the 26.x API.
- [ ] Smoke: portals + relocation gate animate.
- [ ] Commit.

### Phase 9 — GUI screens (1–2 days)

- [ ] Port `PortalModuleScreen`, `PortalConfigScreen`, `PrivacyTerminalScreen`,
      `PrivacyPermissionDetailScreen` to the prepare/render-split GUI API
      (1.21.6) and 26.1 rename. `drawString` semantics changed (opaque ARGB).
- [ ] Every `_on`/`_off` icon pair and alignment rule from `AGENTS.md` still
      applies — render coordinates must not drift.
- [ ] Smoke: open each screen in-game.
- [ ] Commit.

### Phase 10 — Particles / lights / compat (0.5–1 day)

- [ ] `TintableSplashParticle`: particle engine split (1.21.9) — port to the
      new provider signature.
- [ ] RyoamicLights compat: check Modrinth for a 26.1.2 build; if absent,
      keep the code compiling via reflection-guards and treat as
      no-op (report to user; do NOT delete).
- [ ] JEI plugin: check JEI 26.1.2 existence; same no-op rule.
- [ ] Commit.

### Phase 11 — Resources & data pass (0.5 day)

- [ ] `pack.mcmeta`: update `pack_format` for 26.1.2 (resource 84 / data 101.1
      — verify on the version page).
- [ ] Model JSONs: 26.1 block-model pipeline (six built-in types). Our item
      models are element-based; validate against the 26.1 model docs.
- [ ] Datapack: tags, recipes (codec records), advancements load without
      errors in runServer log (`grep -i "riftgun" logs/latest.log`).
- [ ] Commit.

### Phase 12 — Full verification (0.5 day)

- [ ] Chiseled/both-node build + tests green.
- [ ] `:1.21.1:runClient` still loads (regression check).
- [ ] `:26.1.2:runClient`: gun renders, portals place/transit, GUI opens,
      relocation works (manual, 10 minutes).
- [ ] `:26.1.2:runServer`: "Done", no riftgun errors in log.
- [ ] Jar diff: `versions/26.1.2/build/libs/*.jar` class count ≈ 1.21.1 jar's
      431 (allow small deltas from adapters), assets/data equal.
- [ ] Update README "Known limitations" (1.21.1-only line) + this checklist
      status line.
- [ ] Final commit; report to user with phase durations and known gaps.

---

## 4. Report template (fill per phase)

```
Phase N — <name>: DONE | BLOCKED
- changed: <files>
- verified: <command + result>
- deviations from checklist: <none | ...>
- next: Phase N+1
```
