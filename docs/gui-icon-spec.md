# GUI icon artwork specification

## Is texture-based GUI rendering standard?

Yes. These icons use Minecraft 1.21's GUI sprite convention:

```text
assets/riftgun/textures/gui/sprites/icons/<name>.png
```

Code references them as `riftgun:icons/<name>` and renders them with `GuiGraphics.blitSprite`.
Minecraft discovers and stitches these resources into the GUI atlas. This also lets a resource pack
replace the artwork without changing Java code. Screen layout, hitboxes, tooltips, and state selection
remain in Java; PNG files contain presentation only.

## Editing rules

- Canvas: exactly **16 x 16 px**.
- Format: PNG, RGBA, transparent background.
- Keep the existing filename and directory.
- Use hard pixel edges and disable anti-aliasing/resampling. Minecraft scales these with nearest-neighbor
  filtering, so fractional or blurred pixels will look poor at GUI scale 2-4.
- Artwork may use the full canvas, but leave one or two transparent pixels at an edge when the shape
  should not visually touch its button border.
- Do not encode hover, click regions, tooltip text, or server state in the image.
- `_on` and `_off` are deliberately separate files. Edit both when changing a stateful control.
- Test both normal and maximum GUI scale after replacing art.

The bootstrap script [`tools/generate_gui_icons.ps1`](../tools/generate_gui_icons.ps1) recreates the
current code-drawn artwork. **It overwrites every exported icon**, so do not run it after making manual
changes unless you intend to reset them.

## Runtime icon directory

[`src/main/resources/assets/riftgun/textures/gui/sprites/icons`](../src/main/resources/assets/riftgun/textures/gui/sprites/icons)

| Area | Files |
|---|---|
| Main controls | `bucket_on/off`, `drain_on/off`, `placement_smart/front/surface`, `prediction_on/off` |
| Gun and modules | `configure_gun`, `module_bay`, `portal_duration`, `smart_distance`, `surface_range`, `entity_access`, `aperture_on/off` |
| Entity access | `passive_transit_on/off`, `hostile_transit_on/off`, `boss_transit_on/off` |
| Visual settings | `visuals`, `swirl`, `reset_on/off` |
| Navigation | `back`, `module_back`, `dropdown`, `group_expanded/collapsed` |
| Destination list | `drag_handle`, `destination_dot_on/off`, `star_on/off`, `edit`, `delete` |

The reference sheet [`docs/art/gui-icons-reference.png`](art/gui-icons-reference.png) is for browsing
only; the game loads the individual PNG files above.

## Intentionally still code-rendered

Continuous or layout-dependent graphics remain procedural: button/panel backgrounds, outlines,
selection highlights, scrollbars, the fuel fill gauge, and the inactive-module-slot warning overlay.
They are not standalone icons and should not be baked into these PNGs.
