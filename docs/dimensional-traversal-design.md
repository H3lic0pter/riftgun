# Dimensional Traversal Module Design

## Scope

Add a standalone **Dimensional Traversal Module / 维度穿梭模块**. It unlocks one
new header entry in the Portal Gun destination GUI. The entry opens the
**Dimensional Navigation / 维度导航** screen for either saving an exact
cross-dimensional destination or automatically searching for a safe destination
and opening a portal.

The existing Coordinate Override Module, `+ COORD` action, and bottom-right random
dice button keep their behavior and layout from commit `8c7a9a3`.

## Module and availability

- Registry name: `dimensional_traversal_module`.
- Maximum active count: one per gun.
- Independent of the Coordinate Override Module.
- Shaped recipe: `ECE / OAO / ELE`, where `E` is Eye of Ender, `C` is End
  Crystal, `O` is Crying Obsidian, `A` is Advanced Basic Module, and `L` is
  Lodestone.
- Present in the creative inventory, JEI, and the `#riftgun:module` item tag.
- The Creative Module grants its capability like every other module.
- Normal tooltip:
  - zh-CN: `解锁跨维度坐标搜索。`
  - en-US: `Unlocks cross-dimensional coordinate search.`
- A server config switch, enabled by default, controls the complete feature:
  `dimensionalTraversal.enabled`.
- When disabled, the header entry is hidden and all related requests are rejected.
  The item remains registered and installable for save compatibility, and its
  tooltip gains a red warning:
  - zh-CN: `服务器已禁用维度穿梭功能`
  - en-US: `Dimensional traversal is disabled by the server`
- Disabling the feature or removing the module does not invalidate destinations
  that were already saved. Existing destinations, shared coordinates, and map
  integrations retain their normal opening behavior.

## Entry and navigation

- The module adds exactly one new entry: a 19 x 18 icon button in the main GUI's
  upper-right header, immediately left of the existing close-portals button.
- No footer entry, quick action, settings-page entry, or keybind is added.
- The main Dimensional Navigation screen returns to the Portal Gun GUI via Back or
  Escape.
- Its detailed dimension-selection screen returns to Dimensional Navigation via
  Back or Escape and preserves the current form state.

## Persistent state

The selected target dimension and selected mode are stored per Portal Gun in its
module settings and synchronized immediately whenever either value changes.

- First use defaults to the player's current dimension and `EXACT_COORDINATES`.
- If the saved dimension no longer exists, fall back to the player's current
  dimension.
- Unsaved name and coordinate drafts survive mode switches and visits to the
  detailed dimension page during the current GUI session only. Closing the GUI
  discards the draft.

## Main screen layout

Both modes use identical window bounds and keep their primary action button at the
same position.

1. Target dimension selector.
   - Uses the same wide field plus dropdown-button visual language as the existing
     group and portal-visual selectors.
   - The selected value is a concise friendly name such as `Overworld`.
   - Hovering shows the full dimension ID.
   - Duplicate friendly names are disambiguated inside the dropdown as
     `Moon - mod_a`, `Moon - mod_b`.
2. A full-width button opens the detailed dimension-selection page.
3. Two equal-width segmented mode buttons:
   - `指定坐标 / Exact Coordinates`
   - `自动搜索 / Automatic Search`
4. Mode-specific content.
5. A primary action button at one fixed bottom position.

The current dimension remains a valid target.

## Detailed dimension-selection page

- Contains a search box followed by a scrollable list of dimensions available on
  the server.
- Rows show full IDs such as `minecraft:overworld`.
- Clicking a row immediately selects it and returns to the main Dimensional
  Navigation screen; there is no separate confirmation button.
- Built-in dimensions sort first, followed by modded dimensions in stable ID order.

## Exact Coordinates mode

The form order is Name, X/Y/Z/Yaw, then the bottom `保存坐标 / Save Coordinates`
button.

- Name may be empty and then uses the existing `Location N / 地点 N` automatic
  naming rule.
- Initial X/Z values are the player's position mapped by source and target
  dimension coordinate scales. Initial Y and Yaw use the player's current values.
- Changing dimension updates untouched default coordinate values. Once the player
  edits a coordinate field, dimension changes do not overwrite the draft.
- Blank or `~` X/Z values are relative to the mapped target-dimension position.
  Blank or `~` Y and Yaw values are relative to current height and facing.
- Parsing, input length, finite-number checks, and bounds validation reuse the
  existing `+ COORD` rules. Bounds are checked against the selected target
  dimension. Saving does not load the target chunk or perform safety/collision
  checks; normal portal opening performs those checks later.
- No extra client-side red validation or new parsing rules are added.
- The new destination is saved into the editable group selected in the main GUI.
  Player, Shared, Xaero, JourneyMap, or otherwise invalid/read-only selections fall
  back to Default.
- The save request keeps the page and draft until the server replies. Success
  returns to the main GUI and selects/views the new destination; failure preserves
  the form and shows the server error.
- Either the Coordinate Override Module or Dimensional Traversal Module permits
  coordinate-number editing in the existing destination editor. That editor never
  changes a destination's dimension.
- Saving does not consume portal fluid. Opening the saved destination later uses
  normal portal-fluid rules.

## Automatic Search mode

- The content region beneath the mode selector intentionally remains empty.
- The bottom green action is `开启传送门 / Open Portal`.
- Clicking sends the request and immediately closes the entire GUI, matching the
  existing bottom-right random dice interaction. Immediate or later failures are
  reported through the existing player-message path.
- Search center is the player's request-time position mapped by dimension
  coordinate scales.
- Search radius, maximum attempts, cooldown, concurrent-search limit, preparation
  timeout, world-border handling, and safety inspection reuse the existing random
  rift configuration and implementation.
- The found destination is temporary and is not added to any destination group.
- On success the entry portal is forced to `IN_FRONT`; no placement-mode setting is
  consulted.
- Search start does not consume fluid. Fluid is consumed only when the portal is
  actually opened successfully, using normal cross-dimensional fuel rules.
- The gun, module, feature switch, target dimension, and fuel eligibility are
  validated server-side. Removing the module, losing the referenced gun, or
  changing the player's source dimension cancels an active search.
- If the server disables random search, the Automatic Search segmented button stays
  visible but disabled with a server-disabled tooltip. Exact Coordinates remains
  usable while the dimensional-traversal master switch is enabled.

## Visual assets

- Create independent 16 x 16 transparent PNGs for the module item and header icon.
- The artwork must not reuse the dice or compass motif.
- Both source PNG paths are stable so the artwork can be replaced without code or
  model changes.
- Header rendering follows the repository's centered-icon contract and is checked
  at normal and maximum Minecraft GUI scale.

## Security and compatibility

- Every request is authorized on the server; client visibility is never treated as
  permission.
- Search authorization is rechecked while asynchronous work is active.
- Dimension IDs are resolved only against currently available server levels.
- Invalid or removed dimensions are rejected without generating arbitrary chunks.
- Both Minecraft 1.21.1 and 26.1.2 implementations must expose identical behavior.
