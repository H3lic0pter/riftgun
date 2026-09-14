"""Synthesize RiftGun's original arcane portal/transit cues and preview the sound set.

Run with the yolov11 Conda Python (NumPy, SciPy) and FFmpeg on PATH.
OGG assets go into resources; WAV previews and measurements go into build/audio.
The user-supplied staff_cast.ogg and star_cast.ogg are previewed, never regenerated.
"""

from pathlib import Path
import argparse
import json
import subprocess

import numpy as np
from scipy import signal
from scipy.io import wavfile


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftgun/sounds/arcane"
PREVIEWS = ROOT / "build/audio/arcane"
RATE = 48000


def timeline(seconds):
    return np.arange(round(seconds * RATE)) / RATE


def noise(t, rng, low, high):
    filtered = signal.sosfilt(signal.butter(3, [low, high], btype="bandpass",
                                          fs=RATE, output="sos"), rng.normal(size=t.size))
    return filtered / max(np.std(filtered), 1e-9)


def struck(t, attack, decay, onset=0):
    age = np.maximum(0, t - onset)
    return (1 - np.exp(-age / attack)) * np.exp(-age / decay)


def swell(t, peak, width):
    return np.exp(-0.5 * ((t - peak) / width) ** 2)


def resonance(t, frequency, drift=0, rate=5):
    # Small pitch relaxation, without the large sweep of an electronic laser.
    phase = 2 * np.pi * (frequency * t + drift * (1 - np.exp(-rate * t)) / rate)
    return np.sin(phase + 0.025 * np.sin(2 * np.pi * 3.7 * t))


def diffuse(dry, rng, wet, tail):
    result = dry.copy()
    softened = signal.sosfilt(signal.butter(2, 2800, fs=RATE, output="sos"), dry)
    for delay in np.linspace(0.027, tail, 19) + rng.uniform(-0.005, 0.005, 19):
        offset = round(delay * RATE)
        if offset < len(dry):
            gain = wet * rng.choice([-1, 1]) * np.exp(-3 * delay / tail) / 4
            result[offset:] += softened[:-offset] * gain
    return result


def finish(samples, target_db):
    samples = signal.sosfilt(signal.butter(2, 65, btype="highpass", fs=RATE,
                                         output="sos"), samples)
    # Fade both boundaries to prevent clicks, without delaying the initial gesture.
    samples[:240] *= np.sin(np.linspace(0, np.pi / 2, 240)) ** 2
    samples[-2400:] *= np.cos(np.linspace(0, np.pi / 2, 2400)) ** 2
    rms = np.sqrt(np.mean(samples ** 2))
    gain = min(10 ** (target_db / 20) / rms, 0.62 / np.max(np.abs(samples)))
    return samples * gain


def synthesize():
    rng = np.random.default_rng(14092026)
    # Advance past the original shot's random draws to preserve the approved other cues.
    rng.normal(size=2 * round(0.56 * RATE))
    rng.uniform(-0.005, 0.005, 19)
    rng.choice([-1, 1], size=19)

    t = timeline(1.35)
    opening_air = (0.24 * noise(t, rng, 160, 900) * swell(t, 0.18, 0.13)
                   + 0.15 * noise(t, rng, 550, 2700) * swell(t, 0.32, 0.19)
                   + 0.033 * noise(t, rng, 1700, 4800) * swell(t, 0.43, 0.18))
    bloom = sum(gain * resonance(t, hz, -2, 3) for hz, gain in
                [(147, 0.14), (221, 0.10), (331, 0.055), (518, 0.02)])
    opening = finish(diffuse(opening_air + bloom * struck(t, 0.09, 0.24),
                             rng, 0.55, 0.58), -25)

    t = timeline(0.78)
    inward = (0.25 * noise(t, rng, 220, 1700) + 0.035 * noise(t, rng, 1600, 4000))
    inward *= swell(t, 0.19, 0.07)
    seal = (0.24 * resonance(t, 123, 8, 10) + 0.08 * resonance(t, 185))
    seal *= struck(t, 0.008, 0.085, 0.24)
    closing = finish(diffuse(inward + seal, rng, 0.24, 0.28), -25)

    t = timeline(0.70)
    passing = (0.20 * noise(t, rng, 180, 1000) * swell(t, 0.14, 0.09)
               + 0.09 * noise(t, rng, 900, 3200) * swell(t, 0.22, 0.075))
    trace = (0.065 * resonance(t, 221) + 0.022 * resonance(t, 443))
    trace *= struck(t, 0.025, 0.10, 0.14)
    transit = finish(diffuse(passing + trace, rng, 0.38, 0.32), -25)
    return {"open": opening, "close": closing, "transit": transit}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--only", choices=("open", "close", "transit"),
                        help="Replace only this asset; validate and preview the complete set")
    parser.add_argument("--preview-only", action="store_true",
                        help="Validate shipped assets and refresh previews without replacing audio")
    args = parser.parse_args()
    ASSETS.mkdir(parents=True, exist_ok=True)
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    decoded = {}
    report = {}
    imported = {"shot": "staff_cast.ogg", "star_shot": "star_cast.ogg"}
    for name, samples in {**dict.fromkeys(imported), **synthesize()}.items():
        source = PREVIEWS / f"{name}.wav"
        destination = ASSETS / imported.get(name, f"{name}.ogg")
        if samples is not None and not args.preview_only and (args.only is None or args.only == name):
            wavfile.write(source, RATE, samples.astype(np.float32))
            subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                            "-i", str(source), "-map_metadata", "-1", "-c:a", "libvorbis",
                            "-q:a", "5", "-ac", "1", str(destination)], check=True)
        probe = json.loads(subprocess.check_output([
            "ffprobe", "-v", "error", "-show_streams", "-of", "json", str(destination)]))
        stream = probe["streams"][0]
        assert stream["codec_name"] == "vorbis" and stream["channels"] == 1
        assert int(stream["sample_rate"]) == RATE
        raw = subprocess.check_output(["ffmpeg", "-v", "error", "-i", str(destination),
                                       "-f", "f32le", "-c:a", "pcm_f32le", "-"])
        audio = np.frombuffer(raw, dtype="<f4")
        assert np.all(np.isfinite(audio)) and 0.01 < np.max(np.abs(audio)) < 0.95
        if samples is not None:
            assert abs(len(audio) / RATE - len(samples) / RATE) < 0.02
        assert np.sqrt(np.mean(audio[-240:] ** 2)) < 0.001
        decoded[name] = audio
        report[name] = {"seconds": round(len(audio) / RATE, 3),
                        "peak_dbfs": round(float(20 * np.log10(np.max(np.abs(audio)))), 2),
                        "rms_dbfs": round(float(20 * np.log10(np.sqrt(np.mean(audio ** 2)))), 2),
                        "bytes": destination.stat().st_size}

    # Match registry playback volumes; preview is decoded from the shipped OGG files.
    gains = {"shot": 0.75, "star_shot": 0.75, "open": 0.70, "close": 0.65, "transit": 0.65}
    preview = np.concatenate([np.concatenate([decoded[name] * gains[name], np.zeros(RATE // 2)])
                              for name in decoded])
    wavfile.write(PREVIEWS / "arcane-preview.wav", RATE, (preview * 32767).astype(np.int16))
    # An opening shot may coincide with both ends of a portal opening nearby.
    for shot in imported:
        overlap = np.zeros(max(map(len, decoded.values())))
        for name, multiplier in [(shot, 1), ("open", 2)]:
            overlap[:len(decoded[name])] += decoded[name] * gains[name] * multiplier
        assert np.max(np.abs(overlap)) < 0.95
        report[f"{shot}_plus_two_openings_peak_dbfs"] = round(float(20 * np.log10(np.max(np.abs(overlap)))), 2)
    (PREVIEWS / "measurements.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
