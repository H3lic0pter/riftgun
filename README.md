# Rift Gun

Rift Gun is a NeoForge portal-gun mod for Minecraft 1.21.1 and 26.1.2. It
creates linked, walk-through portals for local and cross-dimensional travel,
with saved destinations, modular gun upgrades, configurable placement, player
privacy, three portal-fluid tiers, and client-local visual and sound themes.

The project is currently in release-candidate testing on both supported versions. The 26.1.2 port
is newer and has seen less testing than the 1.21.1 build. Back up worlds
before updating and expect configuration or save-data migration requirements
before `1.0.0`.

## Requirements

| Component | 1.21.1 build | 26.1.2 build |
| --- | --- | --- |
| Minecraft | `1.21.1` | `26.1.2` |
| NeoForge | `21.1.140` or newer | `26.1.2.95` or newer |
| Java | `21` | `25` |
| JEI (optional) | `19.21.1.248+` | `29.29.0.76+` |
| LambDynamicLights | `4.8.10+` | `4.11.1+` |
| JourneyMap (optional) | `1.21.1-6.0.0-beta.1+` | `26.1-6.0.0-beta.1+` |
| Xaero's Minimap (optional) | `26.4.2` through `26.4.x` | `26.4.2` through `26.4.x` |

Rift Gun has no required runtime dependency besides NeoForge. LambDynamicLights is
optional; it adds portal light to nearby blocks on both supported builds.

## Installation

1. Install the matching Minecraft version with a compatible NeoForge release.
2. Put the Rift Gun JAR for that Minecraft version in the instance's `mods` directory.
3. Install the same Rift Gun version on both the server and every connecting client. The current network protocol is `2`; older protocol-`1` builds are intentionally rejected.
4. Optionally install JEI and/or LambDynamicLights on the client.

Back up the world before changing mod versions. Rift Gun stores destinations and privacy preferences as server-side player data, while visual preferences remain client-local.

## Getting started

- Craft a Portal Gun and produce a supported portal fluid. Recipes are available in the vanilla recipe book; JEI also documents fluid transmutation.
- Hold a Portal Gun in either hand and press `G` to open its destination and configuration screen under the default server shortcut policy.
- Save the current position or select an existing destination. The GUI's **Open Portal** action always creates the entrance in front of the player.
- Right-click the gun to open a portal using its current `SMART`, `FRONT`, or `SURFACE` placement mode.
- Press `V` to cycle placement modes. Direct front placement, direct surface placement, and close-portals key mappings are unbound by default.
- Walk into the portal to travel. Opening another portal pair closes the previous pair owned by that player.

Destinations support shared groups, pinning, remembered sorting, coordinate entry when the required module is installed, and same- or cross-dimensional targets. Safety inspection warns through the action bar but does not move the destination, break blocks, or prevent a portal from opening.

## Gun behavior

### Portal placement and behavior

- `SMART` uses surface placement within the configured smart distance and front placement beyond it.
- `FRONT` creates a floating vertical portal. Looking steeply down or up creates a horizontal top or bottom portal.
- `REMOTE` projects a fixed floating portal along the view ray and requires the independent Remote Module.
- `SURFACE` attaches a portal to the targeted block face and is limited by the gun's surface range.
- Motion prediction can be disabled or configured from the gun GUI. Projection mode is the default.
- Standard portals accept players, dropped items, and vehicles. Mob categories require their corresponding transit modules.
- Portals are bidirectional, preserve vehicle/passenger trees and momentum, and close after the configured duration. A new portal pair closes the owner's old pair.
- A standard portal remains fully open for three seconds by default, and its gun can select up to 15 seconds. Duration modules extend that limit, while the Eternal Duration Module removes automatic closure.

Placement is intentionally non-destructive. If no valid entrance geometry can be placed, the action fails instead of modifying the world.

### Portal fuel

Each Portal Gun stores one fluid type at a time and has a base nominal capacity of `8000 mB`.

| Fluid | Theme color | Travel | Default cost per portal pair |
| --- | --- | --- | --- |
| Unstable Portal Fluid | Purple `#A855D4` | Same dimension | `50–100 mB` |
| Portal Fluid | Blue `#58BFFF` | Same dimension | `5–8 mB` |
| Dimensional Portal Fluid | Green `#4FCB72` | Same or cross dimension | `5–8 mB` |

Consumption ranges and random consumption are server-configurable. When randomness is disabled, every use consumes the configured minimum.

The Zero-Point Fuel Module makes the gun's currently loaded portal fluid
unlimited. If the gun is empty, it provides unlimited Dimensional Portal Fluid
behavior instead. Installing it does not force the tank gauge to full, replace
loaded fluid, or change that fluid's color and cross-dimensional capability.

The gun always exposes its standard item fluid capability to pipes, tanks, and machine GUIs. Bucket mode controls only direct player interaction and prioritizes extraction from a clicked tank before insertion. A direct world scoop may accept one complete `1000 mB` source while the gun is still below nominal capacity, allowing at most `capacity + 999 mB`. Once over capacity, further filling is rejected. Standard fluid capabilities used by pipes, tanks, and other mods always stop at nominal capacity.

Portal fluids are created by dropping all recipe ingredients into one independent water source block. Once the complete ingredient set is present, the water converts immediately into a `1000 mB` portal-fluid source. JEI provides an in-game view of these recipes.

Datapacks can register third-party fuels under
`data/<namespace>/riftgun/portal_fuels/<name>.json`. Each definition selects
exactly one fluid or fluid tag and supplies its color, travel capability, and
consumption range:

```json
{
  "tag": "example:portal_fuels",
  "color": "#4FCB72",
  "cross_dimension": true,
  "minimum_consumption": 5,
  "maximum_consumption": 8
}
```

### Modules

The gun starts with nine module slots. Each Module Bay Expansion adds three slots; up to six expansions unlock the full 27-slot, three-row bay. Open the gun GUI and use the module-bay button to install or remove modules.

| Module | Function | Default limit |
| --- | --- | --- |
| Coordinate Override | Unlocks coordinate-created destinations and coordinate editing | 1 |
| Dimensional Traversal | Adds exact cross-dimensional destination creation and bounded automatic destination search | 1 |
| Reservoir Expansion | Adds `8000 mB` nominal capacity | 2 |
| Passive Transit | Allows passive and friendly mobs when enabled | 1 |
| Hostile Transit | Allows hostile mobs when enabled | 1 |
| Boss Transit | Allows entities in NeoForge's boss entity tag when enabled | 1 |
| Surface Range Amplifier | Adds 16 blocks of surface-placement range | 3 |
| Portal Aperture | Prefers `2×2` portals when clearance and support rules allow | 1 |
| Module Bay Expansion | Adds three module slots | 6 |
| Player Target | Adds online players as portal destinations | 1 |
| Duration | Adds 45 seconds to the selectable duration limit | 1 |
| Eternal Duration | Keeps opened portals active until closed or replaced; incompatible with Duration | 1 |
| Fall Guard | Clears accumulated pre-portal fall distance after transit; can also protect eligible entities | 1 |
| Matter Anchor | Prevents a dropped Portal Gun from despawning or being destroyed by fire, lava, or explosions | 1 |
| Projectile Transit | Allows eligible projectiles to cross portals while preserving transformed velocity and orientation | 1 |
| Entity Relocation | Opens a short-lived visual gate around a targeted entity and sends it to the selected destination | 1 |
| Remote | Unlocks fixed-distance `REMOTE` floating placement and the optional SMART fallback | 1 |
| Precision Placement | Adds a face/orientation radial for exact portal placement | 1 |
| Portal Pairing | Adds manual A/B endpoint placement and the Coordinate Travel / Portal Pairing function switch | 1 |
| Zero-Point Fuel | Makes the loaded portal-fluid profile unlimited; supplies Dimensional Portal Fluid behavior when empty | 1 |
| Creative | Grants every module function at its configured maximum and unlocks all module slots | 1 |

Module limits and numerical bonuses marked as defaults may be changed by the server configuration. Removing a Reservoir Expansion discards fluid above the reduced capacity. A Module Bay Expansion cannot be removed while slots that depend on it are occupied.

The Advanced Basic Module is a non-installable endgame crafting component used
by the Eternal Duration and Zero-Point Fuel modules. Its recipe returns the
bucket containers and does not consume the Dragon Egg. The Creative Module has
no survival recipe and is intended for Creative mode or administrators.

Matter Anchor always protects a dropped gun from fire, lava, and explosions.
Its no-despawn behavior is enabled by default and can be disabled independently
with the server setting `modules.matterAnchorPreventsDespawn`.

Entity Relocation has independent enable and SMART-routing controls. Its fuel
cost is calculated once when the transfer starts: projectiles use the base
cost, passive entities default to `1.5×`, hostile entities and players to
`3×`, and boss-tagged entities to `10×`, rounded down. Server administrators
may change all multipliers, including setting them to zero.

With Portal Pairing enabled, right-click places endpoint B and sneak-right-click
places endpoint A. The first endpoint is stored invisibly; placing the other
connects the pair and consumes one ordinary pair charge. Connected endpoints
use identical normal portal visuals. Replacing either endpoint rebuilds the
pair, consumes one charge, and resets the shared duration. The dedicated
endpoint and function-switch keys are unbound by default.

Pairing does not grant `REMOTE`. Pairing SMART routing falls back to `REMOTE`
only when the same gun has both the Portal Pairing Module and Remote Module;
otherwise it safely falls back to `FRONT` without overwriting the saved choice.

### Player targets and privacy

The Player Target Module can open an exit near an online player. The Privacy
Terminal provides global defaults plus per-player overrides for ordinary
player-target exits, Entity Relocation destinations and subjects, and whether
foreign exit portals may carry the player.

- **Target privacy:** `PUBLIC`, `REQUEST`, or `PRIVATE` controls whether others may open a player-target portal beside you.
- **Transit privacy:** optionally prevents other players from arriving through portals whose destination belongs to you.

`REQUEST` mode provides chat actions for Allow Once, Deny Once, and Always
Deny. Requests, one-time grants, and denial cooldowns expire independently.
Their default timeouts are server-configurable.

### Unstable-fluid crises

Fluids tagged as unstable use a configurable weighted crisis registry. The built-in events are High-Altitude Fall, Lava Hazard, Spatial Tear, Weakness, and Nausea. The default weights total 100 out of 1000, giving a 10% aggregate crisis roll before eligibility and environment checks.

The system first checks whether the traveler can reasonably survive an event, then performs the weighted roll, and only searches the world if that event is selected. If a valid scenario cannot be constructed, that crisis is cancelled; it is never replaced with an unsafe fallback. Operators can adjust fluid stability, crisis weights, search bounds, effects, cooldowns, and the per-portal crisis limit in server configuration.

### Visuals, sounds, and shaders

- `Swirl` is the default portal visual; `Classic` is also available.
- Visual selection and swirl animation settings are client-local, so different players may see the same portal differently.
- Shot, open/close, and transit sounds can be selected independently. The Rift theme is the default; Ender is also available for transit. Splash sound is off by default.
- Portal colors and splash particles follow the active fuel.
- Pending Pairing endpoints use client-rendered, world-oriented white wireframes with colored `I`/`II` strokes. They keep the original portal orientation, use world depth so blocks occlude them, and render as opaque geometry rather than allowing the background to show through.
- Pairing markers are batched without marker entities or per-marker buffer flushes. Their frame/number geometry stays in world space while the shader expands strokes to a fixed screen-space width, so camera movement cannot rescale the stored marker shape.
- When a supported shader environment is detected, Rift Gun uses a visible fallback surface and skips the portal surface during shadow passes. Complementary Reimagined and Complementary Unbound r5.x also receive the registered Endframe central-surface path; unregistered packs keep the conservative fallback.
- LambDynamicLights integration is optional. Without it, portals render normally but do not illuminate nearby blocks.

### Optional integrations

- JEI displays Rift Gun crafting and portal-fluid transmutation recipes.
- JourneyMap and Xaero's Minimap contribute read-only waypoint groups to the
  destination GUI. Their client data is copied into the current GUI session;
  changing server or closing the session clears it, and refresh removes stale
  selections. Opening a selected waypoint still sends only a bounded request:
  the server rejects unknown dimensions, non-finite/out-of-range coordinates,
  and overlong fields instead of trusting client waypoint state.
- LambDynamicLights adds nearby block illumination for real portals only. Pair
  preview markers never register as dynamic lights.
- On 1.21.1, Create adds optional Mechanical Mixer recipes and Immersive
  Portals provides its node-specific integration. Neither integration is
  included in the 26.1.2 artifact.

## Configuration

Player-facing options are available from the Portal Gun GUI. NeoForge also generates:

- `config/riftgun-client.toml` for client-local visuals and optional dynamic-light brightness.
- `config/riftgun-common.toml` on 1.21.1 for the optional Create Mechanical Mixer recipe switch. This file is not generated by the 26.1.2 node, which does not include Create integration.
- `<world>/serverconfig/riftgun-server.toml` for destination limits, fuel costs, module limits, portal duration, passenger-tree transit and relocation, special-entity swept collision, destination-readiness timeouts, privacy timeouts, shortcut gun lookup, prediction tuning, unstable-fluid classification, crisis behavior, and opt-in transit diagnostics.

Entity Relocation can move eligible vehicles, dropped items, and complete passenger trees. Servers can independently disable passenger-tree relocation, cap each tree's member count, tune utility-entity fuel cost, and set the temporary immunity that prevents relocated non-player entities from immediately triggering normal portal exits. Transit diagnostics remain disabled by default under `debug.enableTransitDiagnostics`.

Falling blocks, primed TNT, and experience orbs are enabled for both ordinary
portal transit and Entity Relocation by built-in entity-type tags. Falling
blocks and primed TNT additionally use swept collision so fast movement cannot
skip a portal face; this runtime check can be disabled with
`specialEntityTransit.enableSweptCollision` without disabling ordinary
trigger-box transit.

Modpacks can extend or restrict the feature with datapack entity-type tags.
Deny tags take precedence over built-in behavior and allow tags, while portal
and relocation policy remain independent:

- `#riftgun:portal_transit_allowed`
- `#riftgun:portal_transit_denied`
- `#riftgun:portal_transit_swept`
- `#riftgun:entity_relocation_allowed`
- `#riftgun:entity_relocation_denied`

Tagged mod entities use Minecraft's generic entity teleportation, preserving
vanilla-managed entity data and timers. Entity Relocation charges unfamiliar
tag-only entities at the configurable utility multiplier. Compatibility still
depends on the entity supporting Minecraft's normal cross-dimension teleport
contract.

Keyboard shortcuts use `shortcuts.gunLookupMode`. Its default, `HELD_HANDS`, operates on the main-hand Portal Gun or falls back to the offhand. `REGISTERED_LOCATORS` restores inventory-wide lookup and includes third-party gun locator extensions. The Close Portals shortcut never requires a gun.

Server configuration changes should normally be made while the server is stopped and take effect after restart. The transit diagnostics switch is read at runtime. Existing per-gun modules and fluid contents are not automatically replaced when limits change; reducing capacity may truncate excess fluid when the affected module is removed.

## Known limitations

- Minecraft 1.21.1 and 26.1.2 with NeoForge are supported; Fabric and Forge builds are not provided.
- The 26.1.2 build is newer and receives the same shared fixes as the 1.21.1 build, but has seen less real-world testing.
- A faint light rim can appear around the portal surface on both builds; this is a known cosmetic issue.
- Portal placement never breaks or moves blocks, so cramped destinations may be difficult or impossible to enter.
- Cross-dimensional destinations that are not loaded use a deferred exit path; the exit may appear immediately after the first traveler reaches the destination.
- Shader compatibility uses a conservative fallback. Unlisted shader packs may render portal brightness or transparency differently.
- Lava Hazard performs a bounded search and may cancel when it cannot construct a valid natural-lava scenario.
- Public extension seams exist for portal fuels, visuals, sounds, crises, gun locators, coordinate notes, dimension labels, portal-open policies, transit authorization, and portal opening. API stability is not guaranteed before `1.0.0`.

## Development

The project uses a Stonecutter multi-version layout: the root source tree is
shared by every supported Minecraft version, and each version is a node under
`versions/`. Node builds are independent; see `settings.gradle.kts` for the
registered versions.

```powershell
.\gradlew.bat :1.21.1:runClient    # runs the 1.21.1 client
.\gradlew.bat :26.1.2:runClient    # runs the 26.1.2 client
.\gradlew.bat :1.21.1:test         # runs the 1.21.1 test suite
.\gradlew.bat :1.21.1:build :26.1.2:build   # builds both jars
```

Built jars land under `versions/<version>/build/libs/`. The project targets
Java 21 for Minecraft 1.21.1 and Java 25 for Minecraft 26.1.2. Source code and
issue tracking are hosted at
[github.com/H3lic0pter/riftgun](https://github.com/H3lic0pter/riftgun).

Each node also publishes `-api.jar` and `-api-sources.jar` Addon API artifacts.
Addon callbacks are isolated by provider ID: a failed dimension-label provider
is skipped so later providers or the built-in label can answer, while a failed
portal-open policy denies the request. Each failing provider is warned only once
per process to prevent log spam. The removed destination-provider draft API is
not part of the `1.2.0` public artifact.

## License and attribution

Rift Gun's original code and assets are distributed under the
[MIT License](LICENSE). Third-party material remains covered by its respective
copyright and license: the portal surface vertex and fragment shaders are
adapted from Tempad's MIT-licensed `rendertype_timedoor` shaders in the
[Terrarium Earth Tempad repository](https://github.com/terrarium-earth/Tempad),
and the Immersive portal frame texture is cropped from Portal Gun Mod's
MIT-licensed `overlay_frame.png` asset.

The complete attribution and upstream license text are included in
[NOTICE.md](NOTICE.md), [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES), and the
distributed JAR under `META-INF`.
