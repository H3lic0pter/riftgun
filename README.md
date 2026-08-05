# Rift Gun

Minecraft 1.21.1 / NeoForge portal-gun prototype. Java 21. Runtime dependency: NeoForge only.

## Current interaction

- Press `G` while carrying a Portal Gun to open the destination/configuration GUI.
- Select a saved destination by clicking its row. The GUI button always opens a floating portal in front of the player.
- Right-click with the gun to use its current `SMART`, `FRONT`, or `SURFACE` placement mode. `V` cycles the mode; direct front/surface keys are unbound by default.
- In bucket mode, right-click scoops a supported full fluid source and never falls back to opening a portal.
- Opening a new pair closes the previous pair owned by that player. Portals are bidirectional, preserve riders and momentum, and close three seconds after fully opening.

## Portal fuel

The gun holds one fluid type at a time and has a nominal capacity of 8000 mB.

- Unstable Portal Fluid (`#9A9A90`): same-dimension travel, 50–100 mB per pair.
- Portal Fluid (`#58BFFF`): same-dimension travel, 5–8 mB per pair.
- Dimensional Portal Fluid (`#A8F0B6`): same- and cross-dimension travel, 5–8 mB per pair.

Costs are server-configurable. Random consumption can be disabled, in which case the minimum is used. The dedicated world-scoop policy accepts a whole 1000 mB source while the tank is below nominal capacity, so the stored amount may reach at most 8999 mB; normal fluid capabilities remain strict at 8000 mB.

## Run

```powershell
.\gradlew.bat runClient
```
