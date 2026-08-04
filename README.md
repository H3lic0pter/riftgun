# Rift Gun

Minecraft 1.21.1 / NeoForge portal-gun prototype. Java 21. Runtime dependency: NeoForge only.

## Current interaction

- Hold the Portal Gun and right-click while aiming at a block within 96 blocks.
- Entry opens two blocks in front of the player; exit opens at the hit point.
- Opening another pair visibly closes the previous pair owned by that player.
- Portals are bidirectional and preserve horizontal momentum.

## Run

```powershell
.\gradlew.bat runClient
```

Logic decision source: branch `prototype/portal-state-machine`, commit `b73d4f6`.

