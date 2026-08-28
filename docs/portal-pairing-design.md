# Portal Pairing design

Status: approved design baseline (2026-08-27)

## 1. Goal

Add an installable Portal Gun capability that lets a player place both ends of
a linked portal pair directly. The capability is named **Portal Pairing**
(`配对传送`), because it does not imply a short distance or a special lifetime.

The feature must reuse the existing portal lifecycle, transit, fuel, aperture,
privacy, crisis, chunk-loading, sound, and visual systems wherever their
semantics already match. It must not create a second implementation of normal
portal transit.

## 2. Terms and mode model

### 2.1 Function mode (per Portal Gun)

- `COORDINATE_TRAVEL` (`坐标传送` / `Coordinate Travel`)
- `PORTAL_PAIRING` (`配对传送` / `Portal Pairing`)

The selected function is persisted on the individual Portal Gun. Portal
Pairing is available only while that gun has an active Portal Pairing Module.
Removing the module closes pairing-owned world state and makes Coordinate
Travel effective, but retains the stored pairing preferences for a future
reinstall.

### 2.2 Concrete mode (per player, existing ownership)

- `SMART`
- `FRONT`
- `REMOTE` (`远端浮空` / `Remote Placement`)
- `SURFACE`
- `ENTITY_RELOCATION`

Concrete placement and prediction modes remain player settings for save
compatibility and consistent handling across guns. `REMOTE` is visible and
selectable only when the resolved gun has Portal Pairing capability. The
Creative Module grants that capability through the existing maximum-capability
path.

Function and concrete modes are orthogonal. Entity Relocation observes the
function mode to choose its destination source, but otherwise remains a
concrete mode.

### 2.3 Per-gun Portal Pairing settings

Add two independent SMART floating-fallback values:

- Coordinate Travel SMART fallback: `FRONT` or `REMOTE`; default `FRONT`.
- Portal Pairing SMART fallback: `FRONT` or `REMOTE`; default `FRONT`.

Persist them in the existing per-gun module-settings component. Model them as
an enum even if the first GUI presents each as a two-state control. When the
module is absent, keep the values but hide their settings entry.

## 3. Module and item

- Item: **Portal Pairing Module** (`传送门配对模块`).
- Maximum active count per gun: 1.
- No module incompatibilities.
- Ordinary mid-game module based on `riftgun:basic_module`.
- Recipe theme: Ender Pearl, Compass, Redstone, and Quartz. The exact shaped
  arrangement may follow the surrounding module recipe conventions.
- Add the item to the Rift creative tab and recipe/JEI discovery paths in the
  same way as other modules.
- Item and GUI artwork must be authored as PNG. GUI sprites use the repository's
  16 x 16 canvas/alignment contract.

## 4. Input behavior

### 4.1 Portal operation

While the effective function is Portal Pairing and the concrete mode places
portals:

| Input | Action |
| --- | --- |
| Right-click | Place or replace endpoint B |
| Shift + right-click | Place or replace endpoint A |
| Portal Pairing operation key | Place or replace endpoint B |
| Shift + operation key | Place or replace endpoint A |

The new operation key is unbound by default. Right-click always uses the held
gun. The operation key follows the same server-side held/inventory gun lookup
policy as the existing GUI shortcut. Bucket mode keeps priority over portal
placement.

Endpoints A and B are equal and bidirectional. Either may be placed first, and
each placement uses the concrete mode selected at that moment; one end may be
SURFACE while the other is REMOTE, for example.

### 4.2 Function switching

- Add a dedicated function-mode key, unbound by default.
- It follows the existing shortcut gun lookup policy.
- Successful switching reports the new function in the action bar.
- If Portal Pairing is unavailable, a request to enter it fails without
  changing stored or effective state.

### 4.3 Radial selector

- Hover continues to select a concrete mode; releasing the radial key commits.
- Right-click continues to switch between placement and prediction pages.
- Left-click toggles Coordinate Travel / Portal Pairing locally.
- Releasing the radial key atomically submits the previewed function and
  concrete/prediction selection.
- Escape cancels every previewed change.
- Coordinate Travel uses the current cyan-blue theme.
- Portal Pairing uses an amber-orange theme.
- The center label, selected text, ring highlight, and center icon all change,
  so the distinction never depends on color alone.

The normal concrete-mode cycle keeps the function unchanged and skips modes
whose modules are unavailable.

## 5. Placement

### 5.1 REMOTE

REMOTE projects along the player's view ray using the existing maximum surface
range. It does not attach to a block and stores no surface anchor.

- If the ray hits a block, begin at a floating point immediately before the hit
  rather than attaching to the face.
- If the ray misses, begin at maximum range.
- If the candidate is obstructed, search back along the ray toward the player
  for the nearest valid floating placement.
- If no valid placement exists, fail without changing an existing portal.
- A vertical remote portal faces the player.
- Looking steeply down creates a horizontal `TOP` portal.
- Looking steeply up creates a horizontal `BOTTOM` portal.
- Use the same configurable absolute pitch threshold in both directions.

The symmetric TOP/BOTTOM behavior also applies to FRONT. Existing source,
configuration, localization, tooltip, README, and comment text that describes
only a downshot/downward door must be updated to describe horizontal doors in
both directions.

### 5.2 Prediction

Motion prediction applies only to FRONT. REMOTE is fixed to the previewed view
ray; SURFACE is fixed to its hit face. SMART uses prediction only when its
resolved fallback is FRONT.

### 5.3 SMART routing

1. A block face hit within `smartDistance` routes to SURFACE.
2. If that near surface is invalid or obstructed, use the configured floating
   fallback.
3. A miss, or a hit beyond `smartDistance`, uses the per-function floating
   fallback (`FRONT` or `REMOTE`).

### 5.4 Client preview

Portal Pairing does not render a projected placement preview. The server
recomputes and validates every placement when the operation is requested.

## 6. Pair state and lifecycle

The server is authoritative. A pairing session is associated with its owner
and the gun instance used to mutate it. The owner's existing one-normal-pair
rule remains in force: Coordinate Travel, Portal Pairing, and a fixed pairing
relocation target cannot coexist. A successful creation in one route closes
the old route. Merely changing a mode does not close world state.

### 6.1 State transitions

| Current state | Successful operation | Result |
| --- | --- | --- |
| No endpoints | Place A or B | One dormant endpoint; start full portal duration |
| One endpoint | Place other endpoint | Charge once; connect; reset both to full duration |
| One endpoint | Replace same endpoint | Replace it; restart its full duration; no charge |
| Connected | Replace A or B | Charge once; atomically replace pair; reset both |
| Any | Close portals | Close endpoints and fixed relocation target |
| Any | Open a coordinate pair | Close pairing state after the coordinate open succeeds |

Opening or replacement failures preserve the previously valid state, consume
no fuel, and do not reset its timer. When both ends are linked they share one
lifecycle clock and close together. Duration Extension and Eternal Duration
continue to apply through the existing duration resolution.

### 6.2 Fuel and dimensions

- Creating a dormant endpoint requires a recognized current portal-fluid
  profile, or the existing Zero-Point Fuel behavior, but does not consume or
  reserve fluid.
- The operation that first forms a connected pair resolves current fuel,
  validates dimensions and affordability, and consumes one complete normal
  pair cost.
- Replacing either end of a connected pair repeats that complete transaction.
- A fluid change while only one endpoint exists is allowed. On connection,
  both endpoints receive the color and properties of the fluid actually used.
- Same-/cross-dimensional support is determined by that resolved fuel profile,
  exactly as for Coordinate Travel. Zero-Point Fuel keeps its existing empty-gun
  dimensional fallback.

## 7. Entity Relocation combinations

### 7.1 Coordinate Travel + Entity Relocation

Keep current behavior: relocate the aimed eligible entity/passenger tree to the
selected saved or player destination.

### 7.2 Portal Pairing + Entity Relocation

| Input | Action |
| --- | --- |
| Shift + right-click / Shift + operation key | Set or replace a fixed relocation target at the view ray |
| Right-click / operation key | Relocate the aimed eligible entity to that target |

The target uses a surface-safe exit when a face is hit; otherwise it uses the
REMOTE floating resolver and its vertical/TOP/BOTTOM orientation rule. It is a
fixed target rather than a normal walk-through portal.

- Setting the target requires recognizable fuel but does not consume it.
- The target starts the gun's normal portal duration.
- Each successful relocation uses the existing entity-relocation fuel and
  permission policies, then resets the target to its full duration.
- Failure consumes nothing and does not reset the target.
- No valid target means an explicit failure; do not silently route to a saved
  coordinate.
- Existing optional SMART entity routing observes the active function: it uses
  the selected coordinate in Coordinate Travel and the fixed target in Portal
  Pairing. An aimed eligible entity with no fixed target reports that the target
  must first be set.

The fixed target occupies the owner's one portal group. Successfully setting it
closes a Coordinate or Portal Pairing pair; successfully opening either normal
pair closes the target.

## 8. Visual and feedback rules

- Portal-fluid color keeps its current meaning and never identifies A versus B.
- Dormant endpoints and fixed entity targets are not rendered and cannot be
  traversed.
- Connected endpoints use the selected normal portal visual and animation.
- A and B have no marker, label, color, geometry, or animation difference.
- Other players and entities traverse connected paired portals under exactly
  the normal portal transit/module rules.
- Player camera rotation is independent from momentum transformation. If either
  endpoint is TOP or BOTTOM, preserve the complete incoming yaw and pitch.
  VERTICAL-to-VERTICAL traversal retains the existing mirrored view behavior.
- Camera correction is instantaneous and does not change exit position or
  velocity.
- Portal Gun details and the configuration GUI show the current function mode.
  Do not recolor the gun body, whose color represents fluid.

New or changed GUI icons must use shared centered-icon helpers and matching PNG
bounds as required by `AGENTS.md`.

## 9. GUI

- With an active pairing capability, the primary configuration GUI gains a
  19 x 19 function-mode icon immediately left of the prediction icon.
- The per-gun settings area gains a Portal Pairing second-level page containing
  the two SMART fallback selectors.
- Without the capability, both controls and the settings-page entry are absent,
  matching other module-owned settings.
- The destination list remains editable in Portal Pairing mode.
- The GUI's explicit Open Portal action remains a one-shot Coordinate Travel
  request and does not mutate the gun's saved function. A successful open still
  replaces the owner's active pairing state.
- Detail text must use the existing overflow scrolling behavior.

## 10. Data and compatibility

- Extend `PortalGunModuleSettings` with a grouping owned by Portal Pairing:
  saved function plus both SMART fallback values. Defaults decode old guns as
  Coordinate Travel with FRONT/FRONT fallbacks.
- Keep concrete placement/prediction in `PortalPlayerSettings`; add a
  backward-compatible parse default for the new REMOTE enum value.
- Synchronize the effective per-gun function/capability/settings required by
  the client through the existing Portal Gun snapshot path.
- Give endpoint state explicit serialized fields: role A/B, linked state, owner,
  gun instance, partner reference, shared lifecycle start, and active fuel
  visual/profile data. Loading must reject or close incomplete/cross-linked
  pairs safely rather than pairing by proximity.
- Treat all packets as intent only. The server resolves the gun again, checks
  module availability, recomputes placement, validates ownership and current
  state, and performs fuel/state mutation atomically.

Both supported nodes, Minecraft 1.21.1 and 26.1.2, receive equivalent behavior.

## 11. Implementation seams

Prefer these separations rather than adding route conditionals throughout
screens and entity ticks:

1. `PortalFunctionMode` and pairing settings: pure shared data/codec layer.
2. `PortalPairingStateMachine`: pure transition decisions for A/B, clocks,
   charge/reset requirements, and replacement failure behavior.
3. `PortalPairingManager`: server ownership/index lookup and atomic world/fuel
   orchestration.
4. REMOTE placement intent/resolver: shared placement service used by ordinary
   pairing and entity target placement.
5. Function-aware Entity Relocation destination resolver: selected coordinate
   versus fixed pairing target.
6. Client presentation: snapshots, input, radial theme, and GUI page.

The existing `PortalEntity` transit implementation remains the single normal
portal runtime. A dormant endpoint should be a deliberate lifecycle/link state,
not a parallel entity type unless version constraints make that unavoidable.

## 12. Verification

### Shared automated coverage

- Old setting codecs decode to Coordinate Travel and FRONT/FRONT.
- Function settings remain per gun; concrete/prediction settings remain per
  player.
- Module removal makes Coordinate Travel effective without deleting settings.
- A-first and B-first state transitions.
- One-end expiry, connection reset, replacement reset, and failed-operation
  non-reset.
- One initial connection charge and one charge per successful linked-end
  replacement.
- Fuel changes before connection and cross-dimensional profile checks.
- Atomic preservation of an old connected pair on placement/fuel failure.
- SMART near-surface/far/miss/invalid-surface routing for both fallback values.
- REMOTE ray range, backward clearance search, vertical/TOP/BOTTOM threshold,
  and no motion prediction.
- Function-aware explicit and SMART Entity Relocation destinations.
- One-owner-group replacement rules across Coordinate, Pairing, and fixed
  relocation targets.

### Client and integration coverage

- Module-gated concrete modes, cycle order, GUI entry, and key requests.
- Radial left/right/Escape/release semantics and atomic commit payload.
- Cyan/amber text and ring themes at normal and maximum GUI scale.
- No projected placement particles or endpoint helper rendering.
- Dormant and fixed-target entities are invisible; connected A/B visuals match.
- Function icons remain centered at normal and maximum GUI scale.
- 1.21.1 radial render-order regression remains covered so background blur
  cannot blur the wheel itself.
- English and Simplified Chinese localization completeness.
- `git diff --check`.
- `./gradlew.bat :1.21.1:build :26.1.2:build`.

## 13. Suggested implementation sequence

1. Data model, codec migration, module registration, recipe, and tests.
2. Symmetric FRONT horizontal placement plus REMOTE resolver and geometry tests.
3. Pairing state machine, owner indexing, lifecycle, fuel transaction, and
   server tests.
4. Function-aware Entity Relocation target flow and tests.
5. Network actions, right-click/key input, cycle behavior, and snapshots.
6. Radial/GUI presentation and configuration page.
7. Function-mode PNG assets and visual QA.
8. Localization, README updates, both builds, and controlled in-game smoke
   tests.
