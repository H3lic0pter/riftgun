# Portability Refactor Blueprint

Status: accepted for implementation on `codex/portability-refactor`

Target release line: `0.1.0-beta.4`

Baseline: `0db3da5` (`dev` after Beta 3)

## Purpose

This refactor has two primary goals:

1. Replace the duplicated normal-portal and Entity Relocation passenger-tree
   implementations with one explicit best-effort transfer module.
2. Isolate NeoForge lifecycle and extension mechanisms behind narrow platform
   interfaces so a future Fabric port and the Minecraft 26.1 rendering migration
   replace adapters instead of rewriting portal behavior.

The work prepares the architecture for another loader. It does not add a Fabric
build in this release.

## Accepted constraints

- Common code may depend on the JDK, Mojang libraries, and `net.minecraft.*`.
- Common code must not depend on `net.neoforged.*`.
- Minecraft domain objects such as `Entity`, `ItemStack`, `Fluid`, and
  `ResourceLocation` are not wrapped merely to hide Minecraft.
- Existing TOML paths, keys, defaults, and ranges remain compatible.
- Existing saved data, data-component IDs/codecs, payload IDs, and network
  envelope keys remain compatible.
- Existing public extension interfaces may change; no compatibility facade is
  required because no external integrations currently use them.
- Third-party portal fuels become easier to define through datapacks, with a
  Java resolver escape hatch for dynamic or component-sensitive behavior.
- No reflection-based registration, dependency-injection container, service
  loader, or second event bus is introduced.
- New gameplay features are frozen until this branch is complete.
- A small NeoForge GameTest suite covers only the highest-risk integration paths;
  it is not the focus of the refactor.

## Dependency architecture

```text
NeoForge adapters ---> platform interfaces ---> Minecraft types
         |                    ^
         v                    |
   immutable RiftRuntime ---> core modules ---> Minecraft types
```

The intended package layout is:

```text
dev.riftgun.core.*
dev.riftgun.core.transit.*
dev.riftgun.core.relocation.*
dev.riftgun.core.fuel.*
dev.riftgun.platform.*
dev.riftgun.client.visual.*
dev.riftgun.neoforge.*
dev.riftgun.neoforge.client.*
```

This release remains a single Gradle project. An architecture test enforces:

- no `net.neoforged` import under `core` or `platform`;
- no dependency from `core` or `platform` to `neoforge` packages;
- no reflection, `ServiceLoader`, or custom event bus in platform bootstrap;
- client renderer types do not leak into common visual-state interfaces.

When a Fabric implementation is started, `core` and `platform` can move into a
physical Gradle common module without redesigning their interfaces.

## Passenger-tree transfer module

### Interface

The transfer seam is a deep module. Callers provide tree access and the
single-node transit policy; the module owns topology capture, validation,
detach order, transfer order, restoration, remount, synchronization, and the
complete result.

```java
public interface PassengerTreeAccess<N, K> {
    K identity(N node);
    List<N> passengers(N node);
    void detachPassengers(N node);
    boolean attach(N passenger, N vehicle);
    void synchronizeRoot(N root);
}

@FunctionalInterface
public interface NodeTransfer<N> {
    @Nullable N transfer(N node, NodeRole role);
}
```

Two representations deliberately have different lifetimes:

- `PassengerTreeShape<K>` stores only identities and topology. Entity Relocation
  can retain it safely while a destination chunk prepares.
- `BoundPassengerTree<N, K>` binds a shape to current live nodes immediately
  before transfer and rejects changed membership or seat order.

Normal portal transit captures and binds immediately. Entity Relocation captures
the shape when the session begins and binds it again just before transfer.

### Result and failure semantics

```java
public enum TransferOutcome {
    SUCCESS,
    PARTIAL,
    FAILED
}

public enum TransferStage {
    VALIDATION,
    TRANSFER,
    REMOUNT,
    SYNCHRONIZE
}

public record TreeTransferResult<N, K>(
    TransferOutcome outcome,
    @Nullable N movedRoot,
    Map<K, N> moved,
    List<TreeTransferFailure<K>> failures
) {}
```

The algorithm is explicitly best effort rather than falsely atomic:

1. Bind and validate the whole live topology.
2. Detach the whole tree once.
3. Transfer nodes in stable preorder.
4. If the root fails before any commit, restore the original tree and return
   `FAILED`.
5. Once the root succeeds, never attempt a cross-dimension rollback.
6. Continue transferring remaining nodes and collect failures.
7. Remount every available parent-child pair in original order.
8. Synchronize the root after remount completes.
9. Return `PARTIAL` when any passenger transfer, remount, or synchronization
   operation fails.

Entity Relocation treats `PARTIAL` as a committed transfer:

- consume the full quoted fuel cost;
- finalize moved members, including fall guard, projectile state, and exit
  immunity;
- close portals and settle reservations normally;
- notify the owner that some members failed;
- log exact failed identities and stages when diagnostics are enabled.

The module is O(n). One topology snapshot and one result are allocated per
transfer; no tree work occurs during unrelated ticks.

## Entity Relocation session module

`EntityRelocationManager` becomes a small external interface backed by explicit
sessions:

```java
public interface EntityRelocationModule {
    BeginResult begin(RelocationRequest request);
    void tick(MinecraftServer server);
    void cancelAll(MinecraftServer server);
}
```

The internal state machine is:

```text
Preparing -> Opening -> Transferring -> Completed
    |           |             |
    +-----------+-------------+-> Failed / Cancelled
```

Every `RelocationSession` owns:

- owner, target, and gun reference;
- resolved destination and privacy reservations;
- passenger-tree shape;
- fuel quote;
- the immutable relocation config captured at session start;
- chunk ticket, registry reservation, source visual, and exit handle;
- diagnostics identity and timing data.

All terminal paths use one idempotent cleanup owner. Chunk tickets, privacy
grants, registry reservations, and portal handles are released or settled
exactly once. Existing animation timing, privacy behavior, destination
readiness, and normal success UX remain unchanged.

## Immutable configuration

Common code reads grouped immutable records:

```text
RiftConfig
|- PortalConfig
|- FuelConfig
|- RelocationConfig
|- PrivacyConfig
|- CrisisConfig
|- PredictionConfig
+- DiagnosticsConfig
```

The NeoForge adapter retains `ModConfigSpec` and all current TOML names. Load or
reload builds a complete `RiftConfig` and atomically publishes it. Hot paths read
ordinary record fields rather than calling `ModConfigSpec.Value#get()`.

Long-lived operations capture the relevant sub-config at start, so a reload does
not alter a transaction halfway through preparation or transfer.

## Lifecycle and event adapter

Common lifecycle entry points are explicit methods, not another event bus:

```java
RiftLifecycle.serverStarting(server);
RiftLifecycle.serverTick(server);
RiftLifecycle.playerJoined(player);
RiftLifecycle.playerLeft(player);
RiftLifecycle.playerCloned(original, replacement);
RiftLifecycle.entityJoined(entity);
RiftLifecycle.entityLeft(entity);
RiftLifecycle.projectileImpact(projectile, hit);
RiftLifecycle.serverStopping(server);
RiftLifecycle.serverStopped(server);
```

NeoForge event subscribers extract Minecraft objects, invoke one lifecycle
method, and translate its result to event cancellation or result state. A future
Fabric adapter connects Fabric callbacks to the same methods.

## Runtime composition

Mutable `PortalServices` fields are replaced by an immutable, once-installed
runtime:

```java
RiftRuntime.current().portals().open(request);
RiftRuntime.current().transit().transfer(request);
RiftRuntime.current().relocation().begin(request);
```

Pure modules receive dependencies through constructors. Fixed Minecraft entry
points such as items and entities enter through `RiftRuntime.current()`. Tests
construct modules directly and do not replace global fields. Runtime bootstrap
may occur once; server shutdown clears owned state but does not swap the runtime
graph.

## Registry interface

Common code accesses registered values through immutable refs:

```java
public interface RegistryRef<T> {
    ResourceLocation id();
    T get();
}
```

`RiftContentDefinitions` describes vanilla registry IDs, factories, and
dependencies. The NeoForge registration adapter translates them to
`DeferredRegister` and installs refs once. Access before installation or a
second installation fails immediately.

NeoForge-only concepts such as `FluidType` and capability providers remain in
the NeoForge adapter rather than being represented as hypothetical common
registries.

## Network transport

Payload records, codecs, IDs, and business handlers remain common Minecraft
code. Loader transport is narrow:

```java
public interface NetworkTransport {
    void sendToServer(CustomPacketPayload payload);
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
```

The NeoForge adapter owns payload handler registration, `PacketDistributor`,
`IPayloadContext`, and enqueue semantics. Wire formats remain unchanged.

## Fuel storage and third-party fuel registry

The common fuel value contains the actual Minecraft fluid, never an internal
enum:

```java
public record PortalFluidContent(Fluid fluid, int amount) {}

public interface PortalGunFuelStore {
    PortalFluidContent content();
    int capacity();
    int fill(PortalFluidContent input, boolean simulate);
    PortalFluidContent drain(int amount, boolean simulate);
}
```

The NeoForge adapter continues storing the current `SimpleFluidContent` data
component and exposes `FluidStack`/`IFluidHandler` capability behavior. It
converts at the store seam, preserving existing saved guns.

Portal fuels are datapack-first. Definitions select a fluid ID or tag and
provide color, cross-dimension permission, and consumption policy. Reload builds
an immutable O(1) fluid-to-profile index, reports conflicts, and rebuilds after
tag reload. A Java resolver remains available for dynamic or
component-sensitive integrations.

Datapack definitions live at
`data/<namespace>/riftgun/portal_fuels/<name>.json`; the file ID becomes the
profile ID. Exactly one selector is required:

```json
{
  "fluid": "example:bright_fluid",
  "color": "#12ABEF",
  "cross_dimension": true,
  "minimum_consumption": 3,
  "maximum_consumption": 7
}
```

Replace `fluid` with `tag` to expand a fluid tag. Definitions are processed in
stable ID order; if selectors overlap, the first profile wins and the conflict
is logged. Datapack profiles take precedence over builtin profiles and Java
resolvers. The existing builtin fuels remain config-backed fallbacks, so the
current TOML fuel ranges keep their behavior.

## Client rendering interface

Shared client semantics include:

- synchronized `PortalGunVisualState`;
- liquid-level quantization and geometry key;
- liquid and core colors;
- portal animation parameters and geometry mathematics.

They must not expose `BakedModel`, `BakedQuad`, `ItemOverrides`, `RenderType`,
`PoseStack`, `VertexConsumer`, or loader model events.

The 1.21.1 NeoForge adapter keeps the current cached `BakedModelWrapper`
implementation. A future 26.1 adapter uses the new item-model and render-state
interfaces while consuming the same visual semantics. Entity renderer,
shader registration, item model registration, and resource reload hooks remain
adapter responsibilities.

## Test strategy

Tests exercise public seams rather than private implementation details.

Core tests cover:

- topology capture and validation;
- stable transfer and seat order;
- root failure restoration;
- passenger transfer and remount partial outcomes;
- relocation state transitions and exactly-once cleanup;
- config snapshot stability;
- datapack fuel indexing, tags, and conflicts;
- visual-state and geometry-key behavior.

Architecture tests enforce package dependency rules. A small NeoForge GameTest
suite targets boat/passenger transfer, a nested tree, relocation completion,
unloaded-destination ticket cleanup, and third-party fuel capability behavior.
Manual smoke tests remain responsible for client visuals, shader compatibility,
and frame-time verification.

## Performance merge gates

- No new steady-state collection, config wrapper, or adapter allocation per
  server tick.
- Passenger-tree operations remain O(n).
- Disabled diagnostics do not build detail strings, member lists, or timings.
- Registry and event adapters perform direct calls without reflection or runtime
  scans.
- Item rendering retains synchronized state and prebuilt variants.
- Held Portal Gun frame time must not regress by more than five percent in the
  established profiler scene.
- A seam that requires wrapper allocation inside a hot inner loop must move to
  the operation entry point.

## Implementation slices

Each slice follows red -> green -> cleanup and must leave the project buildable.

1. `refactor(transit): unify passenger tree transfer`
2. `refactor(relocation): introduce session state machine`
3. `refactor(config): publish immutable snapshots`
4. `refactor(events): add loader-neutral lifecycle`
5. `refactor(registry): add immutable content refs`
6. `refactor(network): isolate transport adapter`
7. `refactor(fuel): isolate storage and fuel registry`
8. `refactor(rendering): isolate visual semantics`
9. `test(platform): add architecture and game tests`
10. `refactor(platform): finalize package isolation`

The migration pattern for every seam is:

```text
failing parity test
-> minimal new interface/module
-> migrate callers
-> delete replaced implementation
-> build and architecture check
```

Passenger-tree semantics are committed separately from loader isolation so any
behavioral regression can be reverted without discarding platform work.

## Definition of done

- Normal portal and Entity Relocation both use one passenger-tree transfer
  implementation.
- Partial transfer has explicit committed semantics and diagnostics.
- `EntityRelocationManager` no longer owns every lifecycle concern in one static
  class; sessions own their state and cleanup.
- Common business/config/lifecycle/registry/network/fuel/render semantics contain
  no NeoForge imports.
- Existing config, saved guns/worlds, and network protocol remain compatible.
- Datapacks can define third-party portal fuels, with an advanced Java escape
  hatch.
- Unit, architecture, selected GameTests, full build, manual smoke tests, and
  performance gates pass.
- The branch is ready to merge into `dev` without requiring a Fabric module.
