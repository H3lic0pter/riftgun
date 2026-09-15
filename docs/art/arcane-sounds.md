# Arcane staff sounds

**Arcane / 奥术** casting uses the user-supplied `游戏魔法音效_耳聆网_[声音ID：12609].wav`,
converted to `staff_cast.ogg` (mono 48 kHz Vorbis quality 5) at its original pitch,
duration, with `+13.62 dB` gain and a 10 ms tail fade. Its in-game loudness matches
the Aperture-ish shot without clipping.
Source WAV SHA-256: `53F35FC51D811D330EB4D2D93371FA437CD16AFB3004BCB44A3FEDD0192DD2F1`.

The independent **Stars / 星星** shot choice uses `闪光发光魔法音_耳聆网_[声音ID：44404].mp3`,
pitched down four semitones with Rubber Band (`pitch=0.793700526`, `tempo=1`),
then trimmed to the first 1.80 seconds with a 30 ms fade-out starting at 1.77 s.
It is converted to `star_cast.ogg` (mono 48 kHz Vorbis quality 5).
Source MP3 SHA-256: `10D21940A3BF78D16DD30FBF2244C5112398C7552AACD36D7028C1BB62705FAF`.

The other three cues are original procedural audio without third-party samples.
Generator: `tools/generate_arcane_sounds.py` (fixed random seed, NumPy/SciPy + FFmpeg).
Run with the yolov11 Conda Python. Generation is an offline authoring step;
Minecraft only loads the five OGG files, with no new runtime dependency.

| Event | Duration | Character |
| --- | --- | --- |
| Arcane shot | 0.68 s | User-selected magic casting sample, Ear0 sound ID 12609 |
| Stars shot | 1.80 s | Ear0 sound ID 44404, pitched down four semitones and trimmed |
| Open | 1.35 s | Air widening into a diffuse, gently decaying resonance |
| Close | 0.78 s | Air drawing inward, followed by a soft low seal |
| Transit | 0.70 s | Brief passing air with a quiet resonant trail |

Assets: `assets/riftgun/sounds/arcane/{staff_cast,star_cast,open,close,transit}.ogg`.
Format: mono Vorbis, 48 kHz. Mono permits Minecraft's positional playback.
There is no charge-up delay or continuous idle loop. Setting a pairing marker
remains silent. Real firing, portal opening/closing and transit use existing
playback events. Opening and closing share one selectable portal theme.

The generator writes individual WAV sources, a decoded-OGG listening reel
(`build/audio/arcane/arcane-preview.wav`, arcane shot/stars shot/open/close/transit with 0.5 s gaps),
and measurements under `build/audio/arcane`. It checks the encoded files' format,
duration, finite samples, peak headroom and quiet tails. It also checks overlap
of a shot and two opening sounds using the registry's playback volumes.
These measurements do not substitute for subjective in-game listening.
The generator never overwrites either imported casting sample. Use `--preview-only`
to refresh the listening reel without regenerating any asset, or `--only open`,
`--only close`, `--only transit` to regenerate one procedural cue.

## Using the preset

All three sound selectors offer **Arcane / 奥术**, ID `riftgun:arcane`.
The shot selector additionally offers **Stars / 星星**, ID `riftgun:star`.
Fresh client configs recommend Arcane sounds and the End Frame visual for the staff.
Existing local recommendation
values and saved gun settings are deliberately preserved.

To update an existing local staff recommendation, edit these entries in
`riftgun-client.toml` while the game is closed (retain the other fields):

```toml
[appearance.presets.arcane_rift_staff]
shotSound = "riftgun:arcane"
portalSound = "riftgun:arcane"
transitSound = "riftgun:arcane"
portalVisual = "riftgun:endframe"
```

Then reapply the staff skin with recommended sounds and portal visuals enabled. Alternatively,
select Arcane directly for the current gun's shot, portal and transit sounds.
Already saved guns do not follow subsequent preset edits.
