import math, random, wave, struct
from pathlib import Path

OUT = Path("/workspaces/MagicBall/app/src/main/res/raw")
OUT.mkdir(parents=True, exist_ok=True)
RATE = 22050
rng = random.Random(2026)

def save(name, samples):
    peak = max(1.0, max(abs(s) for s in samples))
    with wave.open(str(OUT / name), 'wb') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(b''.join(struct.pack('<h', int(max(-1,min(1,s / peak * 0.82))*32767)) for s in samples))

# Smooth shaking sound: 6 rattles in exactly 1 second

duration = 1.0
n = int(duration * RATE)
shake = [0.0] * n

rattle_times = [0.05, 0.21, 0.37, 0.53, 0.69, 0.85]
rattle_length = 0.14

# Local random generator for repeatable sound
smooth_rng = random.Random(2026)

for center in rattle_times:
    start = int(center * RATE)
    samples = int(rattle_length * RATE)

    # Filter state for softer noise
    filtered_noise = 0.0

    for j in range(samples):
        i = start + j

        if i >= n:
            break

        t = j / RATE
        progress = j / max(1, samples - 1)

        # Smooth fade-in and fade-out
        envelope = math.sin(math.pi * progress) ** 2

        # Low-pass filter: removes harsh high frequencies
        white_noise = smooth_rng.uniform(-1.0, 1.0)
        filtered_noise = (
            0.90 * filtered_noise
            + 0.10 * white_noise
        )

        # Gentle liquid-like tones
        tone = (
            0.55 * math.sin(2 * math.pi * 115 * t)
            + 0.25 * math.sin(2 * math.pi * 175 * t)
        )

        # Blend filtered noise and tones
        sample = envelope * (
            0.35 * filtered_noise
            + 0.65 * tone
        )

        shake[i] += sample

save("shake.wav", shake)

# 0.85 seconds: C6, E6, G6, C7 chiming partials with a soft attack and decay
n = int(.85*RATE)
notes = [(1046.5,0.0),(1318.5,0.11),(1568.0,0.22),(2093.0,0.34)]
answer = []
for i in range(n):
    t = i/RATE
    s = 0.0
    for freq, onset in notes:
        dt = t-onset
        if dt >= 0:
            env = (1-math.exp(-dt*100))*math.exp(-dt*7.5)
            s += env*(math.sin(2*math.pi*freq*dt) + 0.22*math.sin(2*math.pi*freq*2.01*dt))
    answer.append(s)
save('answer.wav', answer)
print('Created:', OUT/'shake.wav', OUT/'answer.wav')
