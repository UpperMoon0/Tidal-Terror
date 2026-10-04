# Cathedral Ray

Minecraft 1.20.1 / Forge 47.2.0. The supplied orthographic design is preserved
in `art-source/cathedral-ray/design-reference.png`.

## Geometry and rig

The new model follows the design's stepped diamond wings, raised central back,
paired cephalic fins, narrow tapering tail, indigo back, lavender edges, cream
underside, mint eyes, branching markings, and paired ventral gills. It is built
from 46 native Minecraft cubes, with a 76-model-unit (4.75 block) wingspan.
The collision box covers that width and is 0.5 blocks tall. The rectangular
collision footprint is intentionally conservative around solid coral.

`tools/create_ray_model.py` reconstructs the Java geometry and allocates a fresh
standard unfolded cube UV region for every cube in a 512x512 atlas. Three
nested joints per wing create a delayed wave from root to tip. Four nested
tail joints create a restrained trailing sway. Cephalic fins attach to the
body. Animation resets the entire baked hierarchy before each pose, follows
actual swimming speed and interpolated entity pitch, and uses stronger wing
movement out of water.

This is newly constructed geometry, not an ImageGen mesh. The supplied sheet is
concept art: the silhouette and texture motifs are reproduced using native
cuboids rather than treating inconsistent illustrated viewpoints as exact CAD.

## Artwork and native UV assembly

Following `CoralCrusher-Art-Workflow.md`, Minecraft's baked model is the source
of truth. `CathedralRayModelCheck` uses `ModelPart.visit` and cube compilation
to export native positions, UVs, and normals per face. It checks that animation
changes rendered vertices and that resetting a pose prevents shared-model
state from leaking. It also exports 48 animated native vertex frames.

Built-in ImageGen produced four flat pixel material panels: indigo back,
lavender edge, sandy cream belly, and seafoam marking material. The retained
input is `art-source/cathedral-ray/materials-v1.png`. ImageGen was used for
artwork, not anatomical placement or UV packing.

`tools/ray_art.py` samples the interiors of those panels into 16x16 materials.
It maps each exact occupied texel back into native model coordinates using
the baked face's UV-to-position transform. Negative-Y faces are dorsal in this
export; positive-Y faces are ventral. Wing boundary pixels receive violet,
undersides receive cream, and world-coordinate branching markings remain
continuous across wing strips. Eyes are confined to the actual body side
faces; five paired gills and the mouth are confined to ventral body faces.
Only eyes and sparse dorsal spots enter the separate emissive atlas. Coral
veins remain ordinary texture pixels, keeping the glow restrained.

Assembly asserts no occupied UV overlaps, full opacity on used skin texels,
transparency on unused skin texels, and no glow outside the native mask.
The revised model exports 276 faces and occupies 6,736 atlas texels.
Software orthographic views and a GIF use the exported native vertices and
nearest-neighbor texture sampling; these are distinct from actual game captures.

## Spawn egg

Built-in ImageGen used the existing Coral Crusher spawn egg as the style
reference and the ray sheet as its subject reference. An initial broad ray
silhouette was rejected in favor of an upright egg with short side fins, two
cephalic horns, mint eyes/markings, indigo upper shell, and cream lower shell.
The final generated source is retained as
`art-source/cathedral-ray/spawn-egg-source-v1.png`.

Assembly fits it to 32x32 with nearest-neighbor sampling and thresholds alpha
at 128 into 0/255. The custom generated item model and white tint override
follow Coral Crusher's egg. Vanilla/Forge egg behavior is retained and both
eggs appear in the Tidal Terror creative tab.

## Habitat and behavior

`tidalterror:cathedral_ray` is a peaceful `WATER_CREATURE`, with 24 health,
native water navigation and smooth swimming controls. It has no attack or
target goals. Spawn groups contain 2–3 rays, weight 6. Cruising, loose schooling,
player curiosity, threat avoidance, and safe recovery are described in
`Cathedral-Ray-AI.md`. Sand resting is not implemented.

Both the generated biome JSON and data generator list rays only in Coral
Cathedral. The registered placement predicate requires that biome, source
water at the position and above/below it, and at least four blocks below sea
level. Native spawning additionally checks collision clearance. Spawn eggs
can create rays outside the biome, as with ordinary Minecraft eggs.

## Rebuild and verify

Use Java 17 and cached Gradle dependencies for `--offline`.

```powershell
python tools/create_ray_model.py
./gradlew.bat -PcoralCrusherModelTests verifyCathedralRayModel --offline
python tools/ray_art.py art-source/cathedral-ray/materials-v1.png art-source/cathedral-ray/spawn-egg-source-v1.png
./gradlew.bat -PcathedralRayTests runGameTestServer --offline
./gradlew.bat build --offline
```

The two original ray GameTests cover deep-water habitat, vanilla biome exclusion,
surface/blocked-water rejection, biome group table, full-width collision,
native egg creation/tints, and observable navigation-driven swimming.
Eight additional behavior tests are described in `Cathedral-Ray-AI.md`.
The ray test task requires native GameTest completion and success markers so a native server
startup failure cannot be mistaken for passing tests merely from exit status.

When other chats are building the same checkout, use
`tools/prepare_ray_verification.py` and run those Gradle checks sequentially
inside its independent source copy. Source/classpath replacement during a
running development client can otherwise cause missing-class errors.

`tools/prepare_ray_preview.py` prepares a separate source copy and save from
the completed reef audit. It checks the original save lock before copying and
does not modify the user's exploration save. Run `-PrayPreview runClient`
from the returned directory. The optional fixture obtains its photographed
ray through `NaturalSpawner.spawnCategoryForPosition`, then holds it still
for top/front/side/belly/reef photographs and captures the native creative
tab. Native attempts are accelerated only in this fixture. It requires the
existing Oculus/Embeddium development dependencies and Complementary shader
pack. Runtime captures are saved under `art/cathedral-ray/runtime`.

Model, GameTest, and preview fixture sources are opt-in and must be absent
from the normal release jar. The normal jar contains the entity, renderer,
model, egg, skin and emissive atlases, language keys, and biome spawn entry.

## Completed validation (2026-10-04)

- Native model verification passed for the final 46-cube geometry, with
  animated vertices and shared-pose reset checks.
- Exact native texture assembly passed for 276 faces / 6,736 occupied texels,
  with zero UV overlaps. Spawn egg conversion passed binary-alpha assertions.
- Both isolated ray GameTests passed. The navigation fixture measured five
  blocks of movement. Native GameTest completion markers confirmed 2/2 passed.
- The shader client obtained its subject through the actual native spawner
  in Coral Cathedral, then produced six real framebuffer captures: top,
  front, side, underside, reef context, and both eggs in the creative tab.
  The source audit's accelerated fauna population was cleared only in this
  independent photography save. Shaders were confirmed active at capture.
- `runData build --offline` passed in the independent verification checkout.
  Its regenerated biome exactly matches the committed-path JSON.
- The reobfuscated jar was inspected for ray classes, both atlases, egg
  artwork, and the 2–3 group spawn entry; no test or preview fixtures remain.
  Local review artifact: `build/libs/tidalterror-1.0.0-cathedral-ray-preview.jar`.

An earlier combined 15-test run passed both ray tests but failed the existing
Coral Crusher `cannotRegenerateOutOfWater` test because its subject was in
water. The dedicated ray checks do not certify the separate, concurrently
edited Coral Crusher behavior. No release was published.
