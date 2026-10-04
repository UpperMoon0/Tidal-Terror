# Shardback artwork and implementation

Minecraft 1.20.1 / Forge 47.2.0. This follows the Coral Crusher artwork
workflow: native geometry and UV export, generated material artwork assembled
onto exact faces, native behavior checks, then a shader framebuffer inspection.

## Concept and geometry

`art-source/shardback/design-reference.png` preserves the supplied orthographic.
The native Java model has 51 cuboids: a stepped violet carapace, pale belly,
asymmetric cobalt coral growths, two black eyes on ivory stalks, eight
articulated legs with slate feet, and an oversized left pincer beside the
smaller right pincer. Both claws have a fixed ivory finger and hinged finger.
The collision box is 2 blocks wide and 1.2 blocks high.
The leg yaws fan away from the center rather than converging beneath the
shell, preserving four distinct legs per side in the actual side view.

`tools/create_shardback_model.py` records the cube dimensions and joint pivots.
Opposite legs alternate lifting and sweeping as actual horizontal movement
increases. Idle body breathing and slow finger motion remain subtle. All
parts reset to their baked pose every frame, avoiding shared-model pose leaks.

## Artwork

Built-in ImageGen supplied four flat pixel material panels: rich violet
carapace with lavender mottling, cobalt coral, sandy ivory underside/legs,
and slate purple joints. The prompt required a 2x2 material sheet without
text, anatomy, or lighting gradients. Generated files are retained in
`art-source/shardback/materials-v1.png` and `spawn-egg-source-v1.png`.

`ShardbackModelCheck` bakes the native model and exports positions, UVs,
normals, and part identities. `tools/shardback_art.py` samples the generated
panels into the exact native face rectangles. Black eyes and tiny ivory
highlights are assigned only to pupil cubes. The shell/palms receive violet,
the coral and palm accents receive cobalt, and claw fingers remain ivory.
Unused atlas pixels are transparent; every occupied texel is fully opaque.
UV occupancy checks reject overlap before saving the 256x256 skin.
The final native bake contains 306 faces and 3,728 occupied atlas texels.

ImageGen used the Crusher egg and Shardback sheet as references for a squat
violet egg with cobalt shards, pale crab accents, and dark eyes. Assembly
reduces it to 32x32 with nearest-neighbor sampling and binary alpha. Its
native Forge egg uses a generated item model and white tint overrides.
The exact built-in prompts are saved in `art-source/shardback/prompts.txt`.

## Habitat and behavior

Shardback has 16 health and 4 armor. Native amphibious navigation drives
supported underwater browsing, with feeding pauses on sediment. It warns
nearby survival players by raising its claws; continued crowding, damage,
and predators trigger retreat toward cover. A warning crab can pinch on
actual contact for one heart, then retreats instead of pursuing. Creative
and spectator players are excluded. Feeding, warning, and shelter states
synchronize to client poses. Quiet submerged feeding allows gradual healing.
It breathes underwater and ignores fluid pushing. See
[Reef-Ambient-AI.md](Reef-Ambient-AI.md) for current behavior and tests;
earlier captures below document the original artwork.

Natural groups contain 1–3 at weight 10 in Coral Cathedral. Placement requires
water at the feet and head, a solid sand/sandstone/gravel/clay seabed beneath,
and depth below sea level
minus four. Native entity collision checks also prevent terrain overlap.
The custom spawn egg joins Crusher, Ray, and Veilglow in the mod tab. The
initial version has no drops.

## Reproduction

Use Java 17. Model, art, gameplay, and data/build steps are:

```powershell
python tools/create_shardback_model.py
./gradlew.bat -PcoralCrusherModelTests verifyShardbackModel --offline
python tools/shardback_art.py art-source/shardback/materials-v1.png art-source/shardback/spawn-egg-source-v1.png
./gradlew.bat -PshardbackTests runGameTestServer --offline
./gradlew.bat runData build --offline
```

`tools/prepare_shardback_verification.py` creates an isolated source snapshot
for builds while a separate test client stays open. The native GameTests
cover reef-only seabed spawning, egg placement and tint, group registration,
collision, submerged walking, breathing, and peaceful behavior. The test
task requires actual completion and success markers.

`tools/prepare_shardback_preview.py` creates a separate client snapshot and
clones the audited reef only after checking its native world lock. Run
`-PshardbackPreview runClient` there to capture a naturally spawned crab from
top/front/side/rear/reef views and the creative tab with all four eggs.
Captures go to `art/shardback/runtime`; this fixture never changes the user's
live exploration save. Test and preview classes are excluded from normal jars.
After natural spawning, the photography copy clears non-colliding waterlogged
plants within three blocks of the crab so its leg and claw joints can be
inspected. Solid coral, seabed terrain, and the user's exploration world are
preserved.

## Native validation on 2026-10-04

The final model bake passed animation-vertex and pose-reset checks across 48
exported frames. Artwork assembly passed 306 faces, 3,728 occupied texels,
zero overlap, opaque used texels, transparent unused texels, and binary egg
alpha. Both required GameTests passed with the final sediment-only spawn
rule, including explicit coral-ledge rejection. The submerged walking test
moved 9.47 blocks while remaining near its original seabed height, healthy,
and without an attack target.

Data generation and the normal reobfuscated build passed. Jar inspection
verified the crab's classes, skin, egg texture/model, loot table, group spawn
data, source identity, and absence of test/preview fixtures. Artifact:
`build/libs/tidalterror-1.0.0-shardback-preview.jar`; SHA256:
`885792bf7f45fdb987b3558e867c67423429b634b79664967dd34a650c33a7e3`.

Evidence logs are in `build/shardback-verification-project-v1`:
`shardback-model.log`, `shardback-tests.log`, and `shardback-build.log`.

The final real client run obtained its crab from Minecraft's NaturalSpawner
in Coral Cathedral at `(54.5, -45.0, 17.5)`, UUID
`5568d0ba-fceb-408f-b5d0-402ea76c9020`. All six framebuffer captures passed
with Complementary active, including the native four-egg creative tab.
The top/front/side/rear/reef views were inspected for the shell, eyes,
asymmetric claws, legs, and sediment placement. Runtime evidence is in
`art/shardback/runtime/passed.txt` and
`build/shardback-preview-project-v1/shardback-client.log`.
