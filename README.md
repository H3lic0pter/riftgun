# Rift Gun

Rift Gun is a portal-gun mod for Minecraft that creates linked, walk-through portals for local and cross-dimensional travel. It combines a destination manager, configurable portal placement, three tiers of portal fuel, per-gun modules, player-to-player portals, and client-side visual and sound themes.


## Requirements

| Component | Version | Required |
| --- | --- | --- |
| Minecraft | `1.21.1` | Yes |
| NeoForge | `21.1.140` or newer for Minecraft 1.21.1 | Yes |
| Java | `21` | Yes |
| RyoamicLights | `0.2.11+` | No; adds portal light to nearby blocks |

Rift Gun has no required runtime dependency besides NeoForge. 

## Installation

1. Install Minecraft 1.21.1 with a compatible NeoForge 21.1 release.
2. Put the Rift Gun JAR in the instance's `mods` directory.
3. Install the same Rift Gun version on both the server and every connecting client.
4. Optionally install JEI and/or RyoamicLights on the client.

Back up the world before changing mod versions. Rift Gun stores destinations and privacy preferences as server-side player data, while visual preferences remain client-local.

## Getting started

- Craft a Portal Gun and produce a supported portal fluid. Recipes are available in the vanilla recipe book; JEI also documents fluid transmutation.
- Carry a Portal Gun and press `G` to open its destination and configuration screen.
- Save the current position or select an existing destination. The GUI's **Open Portal** action always creates the entrance in front of the player.
- Right-click the gun to open a portal using its current `SMART`, `FRONT`, or `SURFACE` placement mode.
- Press `V` to cycle placement modes. Direct front placement, direct surface placement, and close-portals key mappings are unbound by default.
- Walk into the portal to travel. Opening another portal pair closes the previous pair owned by that player.

Destinations support shared groups, pinning, remembered sorting, coordinate entry when the required module is installed, and same- or cross-dimensional targets. Safety inspection warns through the action bar but does not move the destination, break blocks, or prevent a portal from opening.

## Gun behavior

### Portal placement and behavior

- `SMART` uses surface placement within the configured smart distance and front placement beyond it.
- `FRONT` creates a floating vertical portal. Looking steeply downward may create a horizontal downshot portal.
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

The gun always exposes its standard item fluid capability to pipes, tanks, and machine GUIs. Bucket mode controls only direct player interaction and prioritizes extraction from a clicked tank before insertion. A direct world scoop may accept one complete `1000 mB` source while the gun is still below nominal capacity, allowing at most `capacity + 999 mB`. Once over capacity, further filling is rejected. Standard fluid capabilities used by pipes, tanks, and other mods always stop at nominal capacity.

Portal fluids are created by dropping all recipe ingredients into one independent water source block. Once the complete ingredient set is present, the water converts immediately into a `1000 mB` portal-fluid source. JEI provides an in-game view of these recipes.

### Modules

The gun starts with nine module slots. Each Module Bay Expansion adds three slots; up to six expansions unlock the full 27-slot, three-row bay. Open the gun GUI and use the module-bay button to install or remove modules.

| Module | Function | Default limit |
| --- | --- | --- |
| Coordinate Override | Unlocks coordinate-created destinations and coordinate editing | 1 |
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
| Fall Guard | Clears accumulated pre-portal fall distance after transit | 1 |

Module limits and numerical bonuses marked as defaults may be changed by the server configuration. Removing a Reservoir Expansion discards fluid above the reduced capacity. A Module Bay Expansion cannot be removed while slots that depend on it are occupied.

### Player targets and privacy

The Player Target Module can open an exit near an online player. The Privacy Terminal controls two independent policies:

- **Target privacy:** `PUBLIC`, `REQUEST`, or `PRIVATE` controls whether others may open a player-target portal beside you.
- **Transit privacy:** optionally prevents other players from arriving through portals whose destination belongs to you.

`REQUEST` mode provides chat actions for Allow Once, Deny Once, and Always Deny. Requests, one-time grants, and denial cooldowns expire independently. Their default timeouts are server-configurable.

### Unstable-fluid crises

Fluids tagged as unstable use a configurable weighted crisis registry. The built-in events are High-Altitude Fall, Lava Hazard, Spatial Tear, Weakness, and Nausea. The default weights total 100 out of 1000, giving a 10% aggregate crisis roll before eligibility and environment checks.

The system first checks whether the traveler can reasonably survive an event, then performs the weighted roll, and only searches the world if that event is selected. If a valid scenario cannot be constructed, that crisis is cancelled; it is never replaced with an unsafe fallback. Operators can adjust fluid stability, crisis weights, search bounds, effects, cooldowns, and the per-portal crisis limit in server configuration.

### Visuals, sounds, and shaders

- `Swirl` is the default portal visual; `Classic` is also available.
- Visual selection and swirl animation settings are client-local, so different players may see the same portal differently.
- Shot, open/close, and transit sounds can be selected independently. The Rift theme is the default; Ender is also available for transit. Splash sound is off by default.
- Portal colors and splash particles follow the active fuel.
- When a supported shader environment is detected, Rift Gun uses a visible fallback surface and skips the portal surface during shadow passes. Shader-pack-specific appearance may still vary.
- RyoamicLights integration is optional. Without it, portals render normally but do not illuminate nearby blocks.

## Configuration

Player-facing options are available from the Portal Gun GUI. NeoForge also generates:

- `config/riftgun-client.toml` for client-local visuals and optional dynamic-light brightness.
- `<world>/serverconfig/riftgun-server.toml` for destination limits, fuel costs, module limits, portal duration, privacy timeouts, shortcut gun lookup, prediction tuning, unstable-fluid classification, and crisis behavior.

Keyboard shortcuts use `shortcuts.gunLookupMode`. Its default, `HELD_HANDS`, operates on the main-hand Portal Gun or falls back to the offhand. `REGISTERED_LOCATORS` restores inventory-wide lookup and includes third-party gun locator extensions. The Close Portals shortcut never requires a gun.

Server configuration changes should be made while the server is stopped and take effect after restart. Existing per-gun modules and fluid contents are not automatically replaced when limits change; reducing capacity may truncate excess fluid when the affected module is removed.

## Known limitations

- Only Minecraft 1.21.1 with NeoForge is currently supported; Fabric and Forge builds are not provided.
- Portal placement never breaks or moves blocks, so cramped destinations may be difficult or impossible to enter.
- Cross-dimensional destinations that are not loaded use a deferred exit path; the exit may appear immediately after the first traveler reaches the destination.
- Shader compatibility uses a conservative fallback. Unlisted shader packs may render portal brightness or transparency differently.
- Lava Hazard performs a bounded search and may cancel when it cannot construct a valid natural-lava scenario.
- Public extension seams exist for portal fuels, visuals, sounds, crises, and gun locators, but API stability is not guaranteed before `1.0.0`.

## Development

```powershell
.\gradlew.bat runClient
.\gradlew.bat test
.\gradlew.bat build
```

The project targets Java 21. Source code and issue tracking are hosted at [github.com/H3lic0pter/riftgun](https://github.com/H3lic0pter/riftgun).

## License and attribution

Rift Gun is distributed under the [MIT License](LICENSE). Third-party notices and bundled-resource attribution are recorded in [`META-INF/NOTICE.md`](src/main/resources/META-INF/NOTICE.md) and [`META-INF/THIRD_PARTY_LICENSES`](src/main/resources/META-INF/THIRD_PARTY_LICENSES).
