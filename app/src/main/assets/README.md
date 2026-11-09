# Bundled mud material

Asset: `mud-puddle.png` — transparent wet-mud ground decal, generated with the built-in image-generation tool on 2026-11-08. The original alpha is preserved. The app decodes it at half resolution and maps it onto the curved road with a perspective transform. Ripples and impact splashes are animated in code. No downloads occur during gameplay.

## Exact generation prompt

Use case: photorealistic-natural. Asset type: transparent game ground decal for an Android third-person runner. One isolated shallow irregular puddle of wet chocolate-brown mud, top-down orthographic view straight from overhead, centered, nearly circular uneven organic footprint, dark muddy water, glossy soft golden sky reflections, viscous muddy swirls, damp clods, fine silt, a few tiny stones embedded around the irregular muddy bank. Realistic photographic material texture, game-ready decal. Puddle fills about 80 percent of a square image. Edges gradually break up into sparse wet splatters fading into genuinely transparent alpha. No surrounding landscape, no rectangular ground tile, no text, no labels, no objects, no tire, no people, no grass, no cast shadow outside puddle. Transparent background. Warm earthy brown, restrained highlights, not a black hole. Output one clean usable asset.


## Original game audio

The WAV files in `app/src/main/res/raw/` are synthesized by `scripts/compose-audio.py` using an original melody and procedural percussion, with no imported songs or sampled recordings. The 34.29-second, 112 BPM loop wraps note tails across its boundary for continuous playback. The seven short cues cover coins, jumping, skill points, mission completion, village warnings, stumbles and the end of a run. All files are mono 22,050 Hz, 16-bit PCM. Regenerate them with `python3 scripts/compose-audio.py`; the script also checks PCM bounds and the loop boundary.
