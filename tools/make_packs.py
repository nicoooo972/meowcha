#!/usr/bin/env python3
"""
Construit les packs de contenu téléchargeables de Meowcha Café.

Pour chaque dossier packs/<id>/ contenant un manifest.json :
  - génère la musique et les bruitages (synthèse maison, aucune ressource externe),
  - crée dist/<id>-v<version>.zip,
  - écrit dist/index.json (liste des packs, taille, sha256) lu par l'application.

Usage : python3 tools/make_packs.py [dossier_sortie]
"""
import hashlib
import json
import sys
import wave
import zipfile
from pathlib import Path

import numpy as np

SR = 44100
ROOT = Path(__file__).resolve().parent.parent
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "dist"


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def tone(freq, dur, kind="musicbox", vol=0.3):
    t = np.arange(int(SR * dur)) / SR
    if kind == "musicbox":
        w = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 2 * t) + 0.12 * np.sin(2 * np.pi * freq * 3.01 * t)
        env = np.exp(-t * 3.2) * np.minimum(1, t * 400)
    elif kind == "pad":
        w = sum(np.sin(2 * np.pi * freq * d * t) for d in (1, 1.003, 0.997)) / 3
        a = np.minimum(1, t / 0.4)
        r = np.minimum(1, (dur - t) / 0.5).clip(0, 1)
        env = a * r
    elif kind == "bass":
        w = np.sin(2 * np.pi * freq * t) + 0.2 * np.sin(2 * np.pi * freq * 2 * t)
        env = np.exp(-t * 2.0) * np.minimum(1, t * 200)
    else:  # pluck
        w = np.sign(np.sin(2 * np.pi * freq * t)) * 0.3 + np.sin(2 * np.pi * freq * t)
        env = np.exp(-t * 9)
    return w * env * vol


def place(buf, sig, start):
    i = int(start * SR)
    end = min(len(buf), i + len(sig))
    if i < len(buf):
        buf[i:end] += sig[: end - i]


def song(seconds, bpm, root, progression, seed, swing=0.0):
    """Petite musique de boîte à musique façon café lofi, en boucle."""
    rng = np.random.default_rng(seed)
    buf = np.zeros(int(SR * seconds))
    beat = 60 / bpm
    bar = beat * 4
    major = [0, 2, 4, 7, 9, 12, 14, 16]  # pentatonique majeure
    bars = int(seconds / bar)
    note_idx = 3
    for b in range(bars):
        chord = progression[b % len(progression)]
        t0 = b * bar
        # nappe d'accord
        for iv in chord:
            place(buf, tone(midi(root + iv), bar + 0.4, "pad", 0.06), t0)
        # basse
        place(buf, tone(midi(root - 12 + chord[0]), beat * 2, "bass", 0.22), t0)
        place(buf, tone(midi(root - 12 + chord[0] + 7), beat * 2, "bass", 0.16), t0 + beat * 2)
        # mélodie boîte à musique (marche aléatoire douce)
        for s in range(8):
            if rng.random() < (0.35 if b % 8 == 7 else 0.7):
                note_idx = int(np.clip(note_idx + rng.integers(-2, 3), 0, len(major) - 1))
                off = (beat / 2) * s + (swing * beat / 2 if s % 2 else 0)
                place(buf, tone(midi(root + 12 + major[note_idx]), 1.4, "musicbox", 0.16), t0 + off)
        # petit "tic" de hi-hat doux
        for s in range(4):
            n = rng.normal(0, 1, int(SR * 0.03)) * np.exp(-np.arange(int(SR * 0.03)) / SR * 150) * 0.03
            place(buf, n, t0 + s * beat + beat / 2)
    # fondu pour une boucle propre
    fade = int(SR * 1.5)
    buf[:fade] *= np.linspace(0, 1, fade)
    buf[-fade:] *= np.linspace(1, 0, fade)
    return buf


def band(sig, lo, hi):
    """Filtre passe-bande par FFT (suffisant pour des bruitages)."""
    spec = np.fft.rfft(sig)
    f = np.fft.rfftfreq(len(sig), 1 / SR)
    spec[(f < lo) | (f > hi)] = 0
    return np.fft.irfft(spec, len(sig))


def noise(dur, seed):
    return np.random.default_rng(seed).normal(0, 1, int(SR * dur))


def env_ar(n, attack, release):
    t = np.arange(n) / SR
    dur = n / SR
    return np.minimum(1, t / max(attack, 1e-4)) * np.clip((dur - t) / max(release, 1e-4), 0, 1)


def meow(dur, f_start, f_peak, f_end, seed, bright=1.0):
    """Miaulement : glissando de hauteur + harmoniques façonnées par deux formants (i-a-ou)."""
    n = int(SR * dur)
    t = np.arange(n) / SR
    x = t / dur
    f0 = np.where(x < 0.35, f_start + (f_peak - f_start) * (x / 0.35), f_peak + (f_end - f_peak) * ((x - 0.35) / 0.65))
    f0 = f0 * (1 + 0.012 * np.sin(2 * np.pi * 6 * t))  # léger vibrato
    phase = 2 * np.pi * np.cumsum(f0) / SR
    formant1 = 700 + 500 * np.sin(np.pi * x)          # la bouche s'ouvre puis se ferme
    formant2 = 1900 + 700 * np.sin(np.pi * x) * bright
    out = np.zeros(n)
    for h in range(1, 14):
        fh = f0 * h
        gain = np.exp(-((fh - formant1) / 350) ** 2) + 0.6 * np.exp(-((fh - formant2) / 500) ** 2) + 0.05
        out += np.sin(phase * h) * gain / h ** 0.3
    breath = band(noise(dur, seed), 2000, 6000) * 0.04
    return (out + breath) * env_ar(n, 0.04, 0.25)


def glugs(dur, seed, lo, hi, rate, depth):
    """Liquide versé : écoulement filtré + petites bulles (glou-glou)."""
    rng = np.random.default_rng(seed)
    n = int(SR * dur)
    flow = band(noise(dur, seed), lo, hi) * 0.5
    out = flow * env_ar(n, 0.08, 0.3)
    t = 0.05
    while t < dur - 0.15:
        f = rng.uniform(250, 700) * depth
        k = int(SR * 0.06)
        tt = np.arange(k) / SR
        bub = np.sin(2 * np.pi * np.cumsum(f * (1 + 2.5 * tt / 0.06)) / SR) * np.exp(-tt * 45) * 0.5
        place(out, bub, t)
        t += rng.uniform(0.5, 1.5) / rate
    return out


def sfx(name):
    if name == "meow":
        return meow(0.75, 520, 780, 430, 1)
    if name == "meow_happy":
        out = np.zeros(int(SR * 0.9))
        place(out, meow(0.32, 700, 950, 800, 2, 1.3), 0)
        place(out, meow(0.4, 750, 1000, 650, 3, 1.3), 0.38)
        return out
    if name == "meow_sad":
        return meow(1.0, 600, 650, 330, 4, 0.6)
    if name == "pour_water":
        return glugs(1.1, 5, 300, 3500, 12, 1.2)
    if name == "pour_milk":
        return glugs(1.1, 6, 150, 1800, 7, 0.8)
    if name == "syrup":
        return glugs(0.9, 7, 100, 900, 4, 0.5)
    if name == "espresso":
        n = int(SR * 1.4)
        t = np.arange(n) / SR
        pump = (np.sin(2 * np.pi * 50 * t) + 0.5 * np.sign(np.sin(2 * np.pi * 100 * t))) * 0.15
        hiss = band(noise(1.4, 8), 1500, 7000) * 0.35 * np.minimum(1, t / 0.5)
        drip = glugs(1.4, 9, 400, 2500, 6, 1.0) * 0.5
        return (pump + hiss + drip) * env_ar(n, 0.05, 0.3)
    if name == "steam":
        n = int(SR * 1.2)
        t = np.arange(n) / SR
        s_ = band(noise(1.2, 10), 2500, 9000) * (0.3 + 0.7 * np.sin(np.pi * t / 1.2))
        gurgle = band(noise(1.2, 11), 200, 900) * 0.4 * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t))
        return (s_ + gurgle) * env_ar(n, 0.1, 0.3)
    if name == "whisk":
        out = np.zeros(int(SR * 0.9))
        for i in range(9):
            place(out, band(noise(0.07, 20 + i), 1500, 6000) * env_ar(int(SR * 0.07), 0.01, 0.05), i * 0.09)
        return out
    if name == "ice":
        out = np.zeros(int(SR * 0.7))
        rng = np.random.default_rng(12)
        for i in range(4):
            f = rng.uniform(2200, 3800)
            k = int(SR * 0.25)
            tt = np.arange(k) / SR
            clink = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 2.76 * tt)) * np.exp(-tt * 28)
            place(out, clink * 0.4, i * 0.11 + rng.uniform(0, 0.04))
        return out
    if name == "cream":
        n = int(SR * 0.9)
        t = np.arange(n) / SR
        return band(noise(0.9, 13), 3000, 10000) * (1 - t / 0.9) * env_ar(n, 0.02, 0.1) * 0.8
    if name == "sprinkle":
        out = np.zeros(int(SR * 0.7))
        rng = np.random.default_rng(14)
        for i in range(14):
            k = int(SR * 0.02)
            place(out, band(noise(0.02, 30 + i), 4000, 10000) * np.exp(-np.arange(k) / SR * 200) * rng.uniform(0.3, 0.8), rng.uniform(0, 0.6))
        return out
    if name == "bell":
        out = np.zeros(int(SR * 1.5))
        for i, n_ in enumerate([88, 84]):
            k = int(SR * 1.2)
            tt = np.arange(k) / SR
            f = midi(n_)
            place(out, (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2.4 * tt)) * np.exp(-tt * 4) * 0.4, i * 0.18)
        return out
    if name == "trash":
        n = int(SR * 0.5)
        t = np.arange(n) / SR
        return band(noise(0.5, 15), 300, 3000) * np.sin(np.pi * t / 0.5) * 0.7
    if name == "pop":
        t = np.arange(int(SR * 0.12)) / SR
        f = 500 + 900 * np.exp(-t * 40)
        return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 30) * 0.5
    if name == "perfect":
        out = np.zeros(int(SR * 1.2))
        for i, n in enumerate([72, 76, 79, 84]):
            place(out, tone(midi(n), 0.9, "musicbox", 0.3), i * 0.09)
        return out
    if name == "coin":
        out = np.zeros(int(SR * 0.5))
        place(out, tone(midi(88), 0.3, "pluck", 0.25), 0)
        place(out, tone(midi(93), 0.4, "pluck", 0.25), 0.07)
        return out
    if name == "purr":
        t = np.arange(int(SR * 1.2)) / SR
        purr_noise = np.random.default_rng(3).normal(0, 1, len(t))
        # bruit filtré grossièrement + modulation à ~25 Hz = ronronnement
        smooth = np.convolve(purr_noise, np.ones(60) / 60, mode="same")
        return smooth * (0.5 + 0.5 * np.sin(2 * np.pi * 25 * t)) * np.minimum(1, t * 8) * np.minimum(1, (1.2 - t) * 4) * 2.5
    if name == "sad":
        out = np.zeros(int(SR * 1.0))
        for i, n in enumerate([76, 72, 67]):
            place(out, tone(midi(n), 0.6, "musicbox", 0.25), i * 0.18)
        return out
    raise ValueError(name)


def write_wav(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = data / max(1e-9, np.max(np.abs(data))) * 0.85
    pcm = (data * 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


C, AM, F, G, EM, DM = [0, 4, 7], [-3, 0, 4], [-7, -3, 0], [-5, -1, 2], [-8, -5, -1], [-10, -7, -3]


def build_audio(manifest, workdir):
    for key, spec in manifest.get("generate", {}).items():
        target = workdir / spec["file"]
        if spec["type"] == "song":
            prog = [globals()[c] for c in spec["progression"]]
            write_wav(target, song(spec["seconds"], spec["bpm"], spec["root"], prog, spec["seed"], spec.get("swing", 0)))
        else:
            write_wav(target, sfx(spec["type"]))
        print(f"  ♪ {spec['file']} ({target.stat().st_size / 1e6:.1f} Mo)")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    index = {"format": 1, "packs": []}
    for pack_dir in sorted((ROOT / "packs").iterdir()):
        mf = pack_dir / "manifest.json"
        if not mf.exists():
            continue
        manifest = json.loads(mf.read_text(encoding="utf-8"))
        print(f"Pack {manifest['id']} v{manifest['version']}")
        work = OUT / "work" / manifest["id"]
        build_audio(manifest, work)
        zip_name = f"{manifest['id']}-v{manifest['version']}.zip"
        zip_path = OUT / zip_name
        with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr("manifest.json", json.dumps({k: v for k, v in manifest.items() if k != "generate"}, ensure_ascii=False, indent=1))
            for f in sorted(work.rglob("*")):
                if f.is_file():
                    z.write(f, f.relative_to(work).as_posix())
        data = zip_path.read_bytes()
        index["packs"].append({
            "id": manifest["id"],
            "version": manifest["version"],
            "name": manifest["name"],
            "description": manifest.get("description", ""),
            "file": zip_name,
            "size": len(data),
            "sha256": hashlib.sha256(data).hexdigest(),
        })
        print(f"  → {zip_name} : {len(data) / 1e6:.1f} Mo")
    (OUT / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    main()
