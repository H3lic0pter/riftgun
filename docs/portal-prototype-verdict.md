# Portal lifecycle verdict

Question: can replacement, animation, and bounce prevention stay independent from Tempad's energy, saved-location, permission, upgrade, UI, and compatibility systems?

Verdict: yes. Each pair owns a five-state lifecycle: `CHARGING`, `OPENING`, `OPEN`, `CLOSING`, `CLOSED`. Replacing a pair closes the old pair for five ticks while the new pair charges for six ticks. Only `OPEN` portals teleport. Each portal tracks entities currently inside its trigger; an arrival is blocked from returning until it fully leaves and enters again. Firing has no cooldown.

Primary source: Git branch `prototype/portal-state-machine`, commit `b73d4f6`.

## Splash effect freeze

The accepted splash effect is frozen. Do not change particle count, perimeter sampling, phase timing, velocity, gravity, lifetime, or orientation behavior. Side, TOP, and BOTTOM portals all retain vanilla horizontal splash motion without rotation or inversion.

Color is the only supported customization seam. `PortalVisualStyleProvider` resolves a per-portal 24-bit RGB value; alpha remains controlled by the vanilla splash texture. Color is snapshotted from the consumed fuel profile: unstable/Nether purple `#A855D4`, portal blue `#58BFFF`, and dimensional emerald `#4FCB72`. Tint-neutral copies of the four vanilla splash sprites may preserve their alpha silhouettes, but must not alter animation geometry or physics.

## Resolved portal corrections

1. **Tighter teleport trigger volumes.** Side portals now use a narrow portal-local depth instead of their visual bounds. TOP/BOTTOM portals use a height below half a slab and an inset X/Z footprint, so brushing the supporting block or its outer edge does not trigger travel.
2. **Deterministic side-placement priority.** Candidate ordering is: more solid backing blocks first, then portal-center distance to the player's body center, then the lower candidate on an exact tie. This keeps a portal beside the standing player instead of unnecessarily floating above them while still preferring real backing over air.
3. **Back-entry travel direction.** The portal basis transform makes the destination-normal component point away from the exit for entry from either side. The occupancy gate still requires the entity to leave the destination trigger before it can return.

These rules have focused regression tests in `PortalTriggerShapeTest`, `SidePortalCandidateSelectorTest`, and `PortalTransformTest`.

## Fuel and cross-dimension boundary

`PortalFuelProfile` is the single behavior snapshot used when opening a pair: fluid identity, 24-bit RGB, dimension capability, and consumption range. `PortalFuelProfileResolver` is a code-level registration seam for future fuel families; the current build registers only the three built-ins and rejects every other fluid. A pair snapshots the profile once, consumes fuel only after both portal entities are accepted, and gives both ends the same visual color. Failed placement, unavailable dimensions, incompatible fuel, or failed entity creation leave the existing pair and fuel unchanged.

The tank accepts one portal-fluid family at a time. Standard `IFluidHandler` filling is capped at 8000 mB. World source pickup is isolated behind `WorldFluidOverflowPolicy`; its current implementation accepts one complete 1000 mB source while stored volume is below 8000 mB, allowing at most 8999 mB. Replacing that policy with `StrictWorldFluidPolicy` removes the special overflow without touching GUI, portal, or capability code.

Both endpoint chunks receive temporary region tickets owned by their portal entities. Tickets are released on close or any other entity removal, keeping cross-dimension return travel available for the full pair lifetime.

Opening is coordinated by one latest-request-wins transaction per player. A provisional destination ticket is added without blocking the server thread and polled until the destination becomes entity-ticking (maximum 100 ticks). Fuel and the old pair remain untouched until placement revalidation, safety inspection, and both new entities succeed. Surface requests preserve their original hit while front requests are recalculated at completion. Replacing a destination, moving the referenced gun, changing source dimension, dying, logging out, or timing out releases the provisional ticket. Unsafe GUI confirmation retains a prepared ticket for at most 15 seconds.

Both endpoints derive `CHARGING`/`OPENING`/`OPEN`/`CLOSING` from the same absolute overworld game time, so lifecycle progress does not depend on which endpoint ticked first. Cross-dimension non-player travel uses the entity returned by `changeDimension`, then rebuilds the eligible vehicle/passenger tree around the replacement references.

## Known limitations

- `lava_hazard` forces crisis selection but does not guarantee scenario creation. Its current bounded random search samples only loaded chunks near the destination, so it can miss valid nearby lava and retain the forced test override. Replace this with a deterministic, bounded candidate search before treating the crisis as stable.

## Deferred progression

- Recipes and world generation for all three portal fluids.
- Additional unstable-fluid crises and richer malfunction presentation.
- Tank upgrades and a future multi-fluid module. The base gun deliberately remains single-fluid.
- Fluid production cost remains the intended distinction between blue and green fuel even though their current consumption ranges match.
