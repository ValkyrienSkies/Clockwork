import argparse
import array
import pathlib
import subprocess


def convert(source, destination, start=0.0, end=None):
    decoded = subprocess.run([
        "ffmpeg", "-v", "error", "-i", str(source), "-ac", "1", "-ar", "48000",
        "-af", "highpass=f=30", "-f", "f32le", "pipe:1"
    ], check=True, capture_output=True).stdout
    samples = array.array("f", decoded)
    samples = samples[int(start * 48000):int(end * 48000) if end is not None else None]
    overlap = min(5760, len(samples) // 4)
    blend = array.array("f", (
        samples[-overlap + i] * (1 - i / (overlap - 1)) + samples[i] * i / (overlap - 1)
        for i in range(overlap)
    ))
    loop = samples[overlap:-overlap] + blend
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run([
        "ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", "48000", "-ac", "1",
        "-i", "pipe:0", "-c:a", "libvorbis", "-q:a", "6", str(destination)
    ], input=loop.tobytes(), check=True)
    print(f"{destination.name}: {len(loop) / 48000:.3f}s, mono 48 kHz; 120 ms circular crossfade")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("hold", type=pathlib.Path)
    parser.add_argument("overload", type=pathlib.Path)
    parser.add_argument("--one-shots", type=pathlib.Path)
    args = parser.parse_args()
    output = pathlib.Path(__file__).resolve().parents[1] / "common/src/main/resources/assets/vs_clockwork/sounds"
    convert(args.hold, output / "clampotron_hold_loop.ogg")
    convert(args.overload, output / "clampotron_overload.ogg", 0.75, 13.5)
    if args.one_shots:
        for name in ("grab", "release", "snap_1", "snap_2", "power_on", "power_off"):
            filename = "clampotron_" + name
            subprocess.run([
                "ffmpeg", "-v", "error", "-y", "-i", str(args.one_shots / (filename + ".wav")),
                "-ac", "1", "-ar", "48000", "-c:a", "libvorbis", "-q:a", "6", str(output / (filename + ".ogg"))
            ], check=True)
            print(f"{filename}.ogg: mono 48 kHz; full one-shot preserved")
