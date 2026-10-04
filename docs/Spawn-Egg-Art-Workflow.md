# Spawn egg artwork

All four eggs were regenerated with the built-in ImageGen tool using two images per request: the user's egg style sheet and the mob's actual textured model preview. The direction follows the supplied sheet's compact mob-themed egg shapes, coarse pixel shading, colored outlines, and recognizable facial or body markings.

The Coral Crusher uses a blue shell, cream jaw, teeth, and small fins. Cathedral Ray uses an indigo shell, wing accents, and mint branching markings. Veilglow uses a lavender bell motif, mint glow markings, and short tentacle accents. Shardback uses a purple shell, eye stalks, and pale claw motifs.

## Reproducible references and exports

- `art-source/spawn-eggs-v2/style-reference.png`: the supplied style sheet.
- `art-source/food/model-reference-*.png`: the actual textured mob previews passed to ImageGen, identified in the generation manifest.
- `art-source/spawn-eggs-v2/generation-manifest.json`: exact prompts and generated source locations.
- `art-source/spawn-eggs-v2/*-source.png`: retained original generated images.
- `art-source/spawn-eggs-v2/*-previous.png`: previous textures retained for comparison.

Run `python tools/export_spawn_egg_art.py` to reproduce the resource exports from retained sources. The exporter crops transparent padding, downsizes with nearest-neighbor sampling to a maximum visible dimension of 48 pixels, and centers each sprite on a transparent 64x64 RGBA canvas. Alpha is normalized to transparent or opaque to keep edges crisp. The gray background in `spawn-eggs-preview.png` is only for presentation; the four item textures have transparent backgrounds.

## Validation

The isolated release build succeeded; see `build/spawn-eggs-v2-build.txt`. All four textures inside `build/libs/tidalterror-1.0.0-spawn-eggs-v2.jar` match the source resource bytes, have transparent 64x64 RGBA images, and are referenced by their existing item models. Export and package results are recorded in `export-checks.json` and `package-checks.json` beside the retained sources. This change replaces artwork only; spawn behavior and creative-tab registration remain as implemented.
