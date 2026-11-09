#!/usr/bin/env python3
"""Compose original offline music and cues. No downloaded samples or melodies."""
import array
import math
from pathlib import Path
import random
import struct
import wave

RATE = 22050
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(231)

def note(track, at, midi, duration, volume, voice='pluck', pan=0):
    frequency = 440 * 2 ** ((midi - 69) / 12)
    for i in range(int(duration * RATE)):
        t = i / RATE
        attack = min(1, t / .008)
        release = min(1, (duration-t) / .04)
        phase = math.tau * frequency * t
        if voice == 'flute':
            envelope = attack * release * math.exp(-t * 1.5)
            value = math.sin(phase + .018*math.sin(math.tau*5*t)) + .12*math.sin(phase*2)
        elif voice == 'bass':
            envelope = attack * release * math.exp(-t * 4)
            value = math.sin(phase) + .16*math.sin(phase*2)
        else:
            envelope = attack * release
            value = (math.sin(phase)*math.exp(-t*7) + .32*math.sin(phase*2)*math.exp(-t*13)
                     + .12*math.sin(phase*3.98)*math.exp(-t*23))
        track[(int(at*RATE)+i) % len(track)] += volume * envelope * value

def drum(track, at, voice, volume):
    duration = .16 if voice == 'kick' else .08
    previous = 0
    for i in range(int(duration*RATE)):
        t = i/RATE
        noise = rng.uniform(-1, 1)
        if voice == 'kick':
            value = math.sin(math.tau*(53*t + 2.8*(1-math.exp(-t*25)))) * math.exp(-t*25)
        elif voice == 'rim':
            value = (.7*math.sin(math.tau*240*t)+.3*noise)*math.exp(-t*65)
        else:
            value = (noise-previous)*.5*math.exp(-t*55)
        previous = noise
        track[(int(at*RATE)+i) % len(track)] += value*volume*min(1,t/.002)

def write(name, track, peak=.78):
    maximum = max(abs(v) for v in track) or 1
    pcm = array.array('h', (int(max(-1,min(1,v/maximum*peak))*32767) for v in track))
    with wave.open(str(OUT / (name+'.wav')), 'wb') as audio:
        audio.setnchannels(1); audio.setsampwidth(2); audio.setframerate(RATE); audio.writeframes(pcm.tobytes())
    rms = math.sqrt(sum((v/32768)**2 for v in pcm)/len(pcm))
    print(f'{name}: {len(track)/RATE:.2f}s, peak {peak:.2f}, RMS {rms:.3f}')

beat = 60/112
music = [0.0] * round(64*beat*RATE)
phrases = [
    [74,None,78,81,78,None,76,74], [71,None,74,76,78,76,74,None],
    [74,78,None,81,83,None,81,78], [76,None,74,71,69,None,None,None],
    [74,None,76,78,81,None,78,76], [71,74,None,78,76,74,71,None],
    [78,None,81,83,81,78,76,None], [74,None,76,71,74,None,None,None],
]
for bar in range(16):
    root = [38,35,43,45][bar%4]
    for pulse in [0,1.5,2,3.5]:
        note(music,(bar*4+pulse)*beat,root,.32,.19,'bass')
    for step in range(8):
        at=(bar*4+step*.5)*beat
        drum(music,at,'shaker',.025 if step%2==0 else .04)
        midi=phrases[bar%8][step]
        if midi is not None:note(music,at,midi,.5,.12 if bar<8 else .105)
    for pulse in [0,2]:drum(music,(bar*4+pulse)*beat,'kick',.13)
    for pulse in [1,3]:drum(music,(bar*4+pulse)*beat,'rim',.075)
    if bar%4==3:
        for pulse,midi in [(2,81),(2.5,78),(3,76),(3.5,74)]:
            note(music,(bar*4+pulse)*beat,midi,.24,.052,'flute')
write('village_loop',music,.76)

cues = {
    'coin':[(0,86,.14,.4),(.07,93,.2,.35)],
    'jump':[(0,62,.1,.25),(.045,69,.1,.3),(.09,74,.16,.3)],
    'skill':[(0,78,.18,.3),(.07,83,.2,.35),(.14,86,.24,.32)],
    'mission':[(0,74,.2,.28),(.1,78,.2,.3),(.2,81,.2,.32),(.3,86,.5,.35)],
    'warning':[(0,83,.13,.35),(.19,83,.13,.35),(.38,90,.24,.3)],
    'stumble':[(0,57,.12,.3),(.06,50,.18,.3)],
    'run_end':[(0,74,.2,.3),(.15,71,.2,.3),(.3,69,.25,.3),(.5,62,.38,.32)],
}
for name,notes in cues.items():
    track=[0.0]*int((max(t+d for t,m,d,v in notes)+.06)*RATE)
    for at,midi,duration,volume in notes:note(track,at,midi,duration,volume)
    write(name,track,.65)

# The loop wraps ringing notes into its first samples; verify no clipped PCM or sharp seam.
with wave.open(str(OUT/'village_loop.wav'),'rb') as audio:
    pcm=struct.unpack('<'+'h'*audio.getnframes(),audio.readframes(audio.getnframes()))
    assert max(map(abs,pcm)) < 32767
    assert abs(pcm[-1]-pcm[0]) < 2500, 'Loop boundary click'
print('PASS: music is bounded PCM with a continuous loop boundary.')
