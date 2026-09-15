# Rift Gun notices

Copyright (c) 2026 H3lic0pter

## Project licensing

Except for the third-party work identified below, Rift Gun's original source
code, shaders, resource metadata, translations, configuration, data files,
documentation, models, textures, icons, and audio are distributed under the
Apache License, Version 2.0 in `LICENSE`.

The Apache-2.0 attribution notice is:

    Rift Gun
    Copyright (c) 2026 H3lic0pter

## Effective version boundary

This Apache-2.0 licensing statement applies to repository revisions and
distributions strictly after commit `cdf00ac`. The `0.2.1-r1` release itself,
identified by tags `mc1.21.1-v0.2.1-r1` and `mc26.1.2-v0.2.1-r1` at that
commit, is excluded and remains under the MIT License distributed with it.
Earlier releases likewise remain under their distributed license terms.

`src/main/resources/assets/riftgun/textures/entity/immersive_portal_frame.png`
is excluded from that grant and remains under its upstream MIT License, as
described below.

## Third-party notices

The portal surface vertex and fragment shaders are adapted from Tempad's
`rendertype_timedoor` shaders by Terrarium Earth:

- Repository: https://github.com/terrarium-earth/Tempad
- Reviewed commit: `e3816ad55f69d159b3e04b47bae4971f5f0a09ca`
- Tempad source and shader files are MIT-licensed under Terrarium License v1.
- Tempad non-code assets are All Rights Reserved. No Tempad PNG, OGG, Aseprite, or other non-code asset is included here.

The complete upstream license text is stored in `THIRD_PARTY_LICENSES/Tempad-LICENSE.md`.

The Immersive portal frame texture is cropped from Portal Gun Mod's
`overlay_frame.png` asset:

- Repository: https://github.com/Jozef-Steinhubl/portal-gun-mod
- Copyright (c) 2021 MeowMC; Copyright (c) 2025 Jozef Steinhübl
- License: MIT

The complete upstream license text is stored in
`THIRD_PARTY_LICENSES/PortalGunMod-LICENSE.md`.

The `endframe` portal frame texture (`textures/entity/portal_frame.png`) is a
self-authored rift-liquid artwork by H3lic0pter and is covered by Apache-2.0.

The `endframe` portal visual's star surface uses Minecraft's own end-portal
rendering (`RenderType.endPortal`/`RenderTypes.endPortal`); no end-portal
texture is copied into this project, and resource and shader packs that
override the end portal keep applying.

The Portal Gun item model is self-authored by H3lic0pter and is covered by
Apache-2.0.
