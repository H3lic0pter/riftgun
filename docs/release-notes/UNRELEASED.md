# Unreleased

Changes after the published Minecraft 1.21.1 and 26.1.2 `0.2.1-r1` releases.

- Network protocol: `3`
- Skin identity is stored per gun. Resource packs can supply models and dynamic
  fuel and mode colors, including the new arcane staff appearance.
- First-person shot animation supports Off, Recoil, Classic swing, and Lower and
  return, with configurable recoil parameters.
- Skin recommendations can apply sounds, portal visuals, and shot animation
  while retaining the player's custom preferences.

Protocol `3` adds pairing mode to synchronized gun visuals. Protocol `2` and older
peers are rejected. Both client and server must use the same development build,
including support for the acknowledged sound-selection request.

The published `0.2.1-r1` artifacts still use protocol `2`. Their release dates,
checksums, and compatibility notes describe those artifacts, not this branch.
