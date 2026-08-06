# Module chip concept provenance

- Generator: built-in `image_gen`
- Generated source: `docs/art/module-chip-concept.png`
- Runtime assets: `assets/riftgun/textures/item/modules/*.png`
- Note: the generated sheet is design reference only. Runtime sprites were reconstructed as deterministic 16×16 hard-pixel PNGs by `tools/generate_module_icons.ps1` to keep silhouettes and alpha crisp in Minecraft.

## Prompt

```text
Use case: stylized-concept
Asset type: Minecraft Java Edition mod item-sprite concept sheet
Primary request: Design six distinct 16x16-style pixel-art upgrade module icons arranged in one clean 3x2 grid, with wide gaps and no labels.
Subjects, in order: cyan coordinate crosshair chip; blue fluid reservoir/tank chip; green pig-face transit chip; orange-red zombie-face transit chip; purple Ender Dragon head transit chip; gold expanding range-wave chip.
Style/medium: authentic crisp Minecraft inventory pixel art, dark graphite square technology-chip housing shared by all six, simple readable silhouettes, hard pixel edges, restrained highlights, consistent construction but clearly different symbols.
Composition/framing: each icon centered in its own equal cell, generous separation, straight-on orthographic view, no overlap.
Scene/backdrop: perfectly flat solid #ff00ff chroma-key background for background removal.
Color palette: near-black graphite frames, cool gray edge details, the specified distinct accent color for each icon.
Constraints: no text, no numbers, no gradients, no blur, no antialiasing, no shadows, no perspective, no watermark, no extra objects; background must be one uniform #ff00ff with no texture or lighting variation; do not use #ff00ff in the icons.
```
