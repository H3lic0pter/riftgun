# Unreleased

Changes after the published Minecraft 1.21.1 and 26.1.2 `0.2.1-r1` releases.

- Network protocol: `3`
- Skin identity is stored per gun. Resource packs can supply models and dynamic
  fuel and mode colors, including the new arcane staff appearance.
- First-person shot animation supports Off, Recoil, Classic swing, and Lower and
  return, with configurable recoil parameters.
- Portal visual type, shot animation mode, and sound choices are saved per gun.
  Detailed visual and recoil parameters remain client settings.
- New guns copy the owner's local skin recommendations on creation, or first use
  when no owner was available. Applying a skin copies only enabled recommendation
  categories; disabled categories and `CUSTOM` entries preserve that gun's choices.
- Existing guns without presentation data migrate on first use from the player's
  previously effective visual/animation choices and saved sounds. The old client
  preference fields remain readable for this migration.
- Saved guns no longer follow recommendation edits or a new owner's preferences.
  Open portals retain the visual and sound choices captured when opened.

Protocol `3` adds pairing mode and per-gun presentation synchronization, replacing
the player-wide sound-selection request. Protocol `2` and older peers are rejected. Both client
and server must use the same development build. Keep a save backup before upgrading:
older builds do not understand the new item component, so downgrading and saving
can discard per-gun presentation choices.

The published `0.2.1-r1` artifacts still use protocol `2`. Their release dates,
checksums, and compatibility notes describe those artifacts, not this branch.
