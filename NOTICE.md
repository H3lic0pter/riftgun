# Third-party notices

The portal surface vertex and fragment shaders are adapted from Tempad's
`rendertype_timedoor` shaders by Terrarium Earth:

- Repository: https://github.com/terrarium-earth/Tempad
- Reviewed commit: `e3816ad55f69d159b3e04b47bae4971f5f0a09ca`
- Tempad source and shader files are MIT-licensed under Terrarium License v1.
- Tempad non-code assets are All Rights Reserved. No Tempad PNG, OGG, Aseprite, or other non-code asset is included here.

The complete upstream license text is stored in `THIRD_PARTY_LICENSES/Tempad-LICENSE.md`.

The portal frame texture (`textures/entity/portal_frame.png`) is adapted from
Portal Gun by MeowMC:

- Repository: https://github.com/iPortalTeam/PortalGun
- Branch reviewed: `1.20.1`
- Source file: `src/main/resources/assets/portalgun/textures/entity/overlay_frame.png`
- Portal Gun is MIT-licensed (Copyright (c) 2021 MeowMC).

The complete upstream license text is stored in `THIRD_PARTY_LICENSES/PortalGun-LICENSE.md`.

The `endframe` portal visual's star surface uses Minecraft's own end-portal
rendering (`RenderType.endPortal`/`RenderTypes.endPortal`); no end-portal
texture is copied into this project, and resource and shader packs that
override the end portal keep applying.

The Portal Gun item model is self-authored by H3lic0pter and is covered by
Rift Gun's MIT License.
