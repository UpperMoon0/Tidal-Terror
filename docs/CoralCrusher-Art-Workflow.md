# Coral Crusher modeling, animation, and texture workflow

Recorded from the current model, native export fixture, and saved artwork assembly scripts on 2026-10-04. This describes the process actually used for the blue shark, sandy variant, and spawn egg.

## Starting model and joint repair

The starting mesh was an existing Blockbench Java export, identified in its header as Blockbench 4.8.3, exported for Minecraft 1.17+ with Mojang mappings. We retained its shark shape and UV layout. This work repaired its rig and added animation; it did not create the original geometry from scratch or generate a new mesh with ImageGen.

The original `setupAnim` was empty. Head, body, and tail were separate root parts, and the lower-jaw part had no cubes. The repaired hierarchy is:

```text
root
└─ body
   ├─ head
   │  ├─ upperjaw
   │  └─ lowerjaw
   ├─ tail
   └─ paired front/rear fins and their rotated cubes
```

Reparent the head and tail to the body. Put pivots at the neck, jaw hinges, and tail attachment, then rebase cube coordinates to compensate for the new parent/pivot transforms. Move the lower-mouth cubes from the upper jaw into the lower jaw. Retain each cube's texture offsets and its position at rest.

For a translated joint, compensating the local cube position by the opposite pivot translation preserves its world position. With rotated parents, use the full inverse parent transform rather than subtracting offsets blindly. Verify the result through native model baking and rendered vertices.

The baseline is preserved in `src/modelTest/java/com/nhat/tidal_terror/entities/coral_crusher/OriginalCoralCrusherModel.java`. The working rig is `src/main/java/com/nhat/tidal_terror/entities/coral_crusher/CoralCrusherModel.java`. `CoralCrusherAnimationCheck` compares sorted rendered position/UV signatures at rest against the baseline. That comparison passed: rig repair preserved the rest shape and UVs.

## Procedural animation

Animation lives in Java `setupAnim` / `animatePose`, using baked `ModelPart` transforms. No keyframe animation file or additional animation library was introduced.

Every frame first resets all parts to their baked pose. Minecraft shares the renderer's model between entities, so skipping this reset causes accumulating transforms or another shark's pose to leak into the next render.

The current motion combines:

- Idle tail movement, mouth breathing, and a small body bob.
- Stronger tail yaw and paired fin movement as actual movement speed increases.
- Body pitch from the renderer's interpolated entity pitch, with limited relative head yaw.
- Bite motion from `EntityModel.attackTime`: the lower jaw opens, the upper jaw moves slightly, and the head recoils.
- Faster, stronger movement on land.

The implementation uses `swimAmount = clamp(speed * 4, 0, 1)`. The stroke phase is `ageInTicks * 0.25` underwater and `ageInTicks * 0.6` on land. Underwater tail strength is `0.08 + 0.30 * swimAmount`; land strength is `0.45`. Bite opening uses `sin(clamp(attackProgress, 0, 1) * PI)`. These amplitudes are artistic choices for this shark.

Native references inspected locally were `CodModel` for tail/land movement, `DolphinModel` for swim pitch, `RavagerModel` for mouth animation, and `MeleeAttackGoal`, `LivingEntity`, and `LivingEntityRenderer` for the synchronized attack-swing lifecycle. The original native melee goal supplied the hand swing; the current predator goal calls that same native swing explicitly for its single bite attempt. The renderer supplies interpolated `attackTime`. A separate client bite timer was unnecessary. The current synchronized windup state also adds a visible tail/jaw/head cue; see `CoralCrusher-Predator-AI.md`.

The native model fixture checks moving swim/bite vertices, land motion, and pose reset. It also exports 48 rendered frames. The GIF made from these vertices is a software preview of the real baked model, not an in-game screenshot.

## Exporting a trustworthy texture map

The source of truth is the baked Minecraft model, rather than guessing the rectangles from an atlas screenshot.

`CoralCrusherAnimationCheck` visits each native cube and captures its four vertices per face through `VertexConsumer`, after applying the baked part hierarchy. Each record contains part path, cube index, face index, transformed XYZ position, UV, and transformed normal. The export is `build/texture-audit/native-uv-vertices.txt`.

The texture pipeline groups each four-vertex face and converts normalized UVs into texels on the 256×256 atlas. For this model, the export contained **132 faces**, with **zero overlapping occupied face pixels**. The occupied region fits in the upper-left 144×144 area. No UV repacking was needed.

The guides were:

- `uv-labelled.png`: numbered cube/face rectangles with dominant normal directions such as X+ and X−.
- `model-face-reference.png`: four views of the model, using the same numbered colors and a part legend.
- `atlas-edit-target.png`: the actual atlas artwork, cropped to the occupied 144×144 area and enlarged 8× with nearest-neighbor sampling to 1152×1152.
- `uv-mask.png` and `uv-faces.json`: exact occupied texels and native face identities/bounds for reconstruction.

Use the artwork atlas as the edit target. Supply the labels and colored model views as supporting references. This gives the image tool a visual correspondence between a rectangle and a model part, but the native face data is still required to enforce that correspondence reliably.

## Blue texture: generation and correction

The built-in ImageGen tool generated pixel-art skin materials from the atlas and model references. The first pass produced a useful slate-blue/ivory palette, but placed eyes and mouths on body panels and shifted some island boundaries. It was rejected as a finished game texture.

The historical registration script compared a generated occupancy mask with the native mask, searching scale and XY offsets by intersection-over-union. The recorded best alignment was **0.9906 IoU at 8.625 generated pixels per native texel**. Each registered texel was area-averaged into a 256×256 candidate. Good mask alignment did not fix the anatomical mistakes.

A second pass generated skin with facial landmarks removed. It still did not fully preserve back/belly assignments. Final assembly therefore sampled generated material swatches from island interiors and assigned them to exact native faces:

- Dorsal faces received back material; ventral faces received pale belly material.
- Fin and rotated tail-fin faces received back material.
- Other side faces received flank material.
- Inward upper/lower jaw faces received mouth material. Mouth patches were not applied to the body or fins.

Use transformed normals and part identities to classify faces. In this export, negative Y normals identify the dorsal direction and positive Y normals identify the belly; inward jaw tests use the corresponding upper/lower part identity. Do not transfer these signs to a different export without checking its coordinate convention.

Flank orientation comes from the correlation between vertex UV-V and transformed world Y. A negative correlation causes a vertical flip. Texture V cannot be assumed to mean "down" on every rotated or mirrored cube face.

Eyes and gills were cropped from the first generated candidate, resized with nearest-neighbor sampling, and relocated to the actual lateral head/neck faces. The eye patch is 2×3 texels and the gill patch is 5×8 texels. Placement uses a world-to-UV transform:

```text
Given a face's world-space vertices P0, P1, P3 and a desired landmark point P:
solve [P1-P0, P3-P0] * [a, b] = P-P0 by least squares
UV(P) = UV0 + a*(UV1-UV0) + b*(UV3-UV0)
multiply normalized UV by 256, then clamp the patch inside that face's rectangle
```

This is how the correct eye/gill placement was recovered. The tool supplied artwork; anatomical placement was enforced from native geometry. Current recorded landmark rectangles, with exclusive upper bounds, are:

| Landmark | Face side | Atlas rectangle `(x0,y0,x1,y1)` |
| --- | --- | --- |
| Gills | X− | `(60,10,65,18)` |
| Gills | X+ | `(82,10,87,18)` |
| Eye | X− | `(82,28,84,31)` |
| Eye | X+ | `(96,28,98,31)` |

These coordinates are specific to this rig. Re-export and recalculate if geometry or UVs change.

Final assembly writes a transparent 256×256 RGBA image, forces every used texel opaque, and clears every unused texel. Both properties are asserted against the native mask. The output is `src/main/resources/assets/tidalterror/textures/entity/coral_crusher/coral_crusher.generated-v1.png`. The original `coral_crusher.png` remains as a rollback asset.

Four textured model views were rendered from native exported vertices to inspect both sides, mouth surfaces, fins, and tail. The software renderer uses depth testing, barycentric UV interpolation, and nearest texture sampling. It is useful for fast iteration; final in-game captures establish appearance under the actual renderer and shaders.

## Sandy variant

ImageGen edited the blue atlas toward sandy taupe, sandstone beige, a darker mottled back, and a creamy underside. The existing textured model was the supporting reference. The returned atlas still shifted island edges, so it was not used directly.

`assemble_sandy.py` samples back, belly, and flank materials from the generated atlas and reuses the exact blue face assignments and UV orientations. It copies existing mouth material, preserves dark eye pixels, and recolors only the dark gill pixels using color sampled from the generated artwork. This avoids copying blue skin around the gills into the sandy variant.

The same occupied-mask/opacity assertions apply. The result is `coral_crusher.sandy-v1.png`, also 256×256 RGBA. Geometry, animation, AI, and entity type are shared with the blue shark. The renderer selects the texture from synchronized skin data. Natural spawning selects sandy within 24 blocks of the sand seabed and blue above; ordinary spawn eggs choose either skin with equal probability. The skin persists when the shark swims or is saved/reloaded.

## Mob-themed spawn egg

The user's sheet of illustrated spawn eggs supplied style; the sandy shark's four-view model preview supplied the subject. ImageGen was asked for one transparent, coarse pixel-art egg with shark fins, sandy colors, eyes, gills, and a restrained tooth/mouth cue.

Although the requested sprite grid was 32×32 enlarged to 1024×1024, the returned artwork was 1254×1254. It was fitted to 32×32 using nearest-neighbor sampling, then alpha was snapped to 0/255 with threshold 128 to remove translucent colored edge artifacts.

The resulting `textures/item/coral_crusher_spawn_egg.png` is used by a generated item model. `CoralCrusherSpawnEggItem` returns white for tint layers so Minecraft does not recolor this artwork. Native egg behavior is retained, and the item is registered in the mod creative tab. A native creative-inventory capture verified the actual item rendering.

## Repeating and checking the process

Run from the project root with Java 17. Native export/model checks need the optional model fixture:

```powershell
./gradlew.bat -PcoralCrusherModelTests verifyCoralCrusherAnimation --offline
./gradlew.bat -PcoralCrusherTests runGameTestServer --offline
./gradlew.bat build --offline
```

Use `--offline` only when dependencies are already cached. The model fixture writes native UV records and animation frames under `build`. Python assembly/preview scripts use Pillow and NumPy.

Historical local scripts and results are under `art/coral-crusher-texture-v1`, `art/coral-crusher-sandy-v1`, and `build/texture-audit`. The blue `finalize_texture.py` still expects its working inputs under `build/texture-audit`; it cannot be run from a clean checkout without regenerating/restoring those inputs. Generated artwork and build folders are deliberately ignored by Git. The process and available exact prompts are preserved in this document so they remain available from the repository even without those local files.

For a future model: retain a baseline, repair pivots, verify rest vertices/UVs, export native faces, make labelled guides, generate materials, reconstruct exact face assignments, relocate landmarks with world-to-UV mapping, assert mask/opacity, inspect multiple model views and animation frames, then capture the real in-game result. If UV faces overlap, resolve the shared-face constraints before painting independent features.

For native sandy shader photographs, after installing the dev shader stack and preparing the audited reef world:

```powershell
python tools/install_dev_shaders.py
./gradlew.bat -PreefPreview -PcrusherSkinPreview runClient --offline
```

The saved captures used Oculus 1.8.0, Embeddium 0.3.31, and Complementary Reimagined r5.9.3. Preview/model/GameTest sources are opt-in and excluded from the ordinary release jar. Restart an already running dev client to load changed textures or model code.

## Preserved ImageGen prompts

The exact original blue-generation prompts were not found in the inspected saved artifact files. The description above records their observed inputs, two passes, and assembly behavior; it is not an invented verbatim prompt. The following sandy and spawn-egg prompts were saved and are copied verbatim below.

### Sandy atlas edit

```text
Built-in image_gen edit; image 1 is blue-atlas-edit-target.png and image 2 is
../coral-crusher-texture-v1/textured-model-preview.png. Transparent output.

Use case: precise-object-edit. Image 1 is the exact UV texture atlas EDIT TARGET for an existing Minecraft shark, enlarged 8x from a 144x144 active texture region. Image 2 is ONLY a reference of the same shark model wearing that atlas. Produce ONLY an edited atlas in exactly image 1 layout, square 1152x1152. Preserve every island position, boundaries, orientation, existing dark eye pixels, dark gill slits, muted pink mouth and ivory teeth. Replace the blue dorsal skin with natural warm sandy taupe and sandstone beige, darker muted brown on the back and fins, subtly mottled sand camouflage, transitioning to pale creamy ivory on underside. Palette should harmonize with a sand and sandstone deep reef floor. Keep restrained low resolution Minecraft pixel-art detail and existing 8x pixel grid. No drawing of a whole shark, no labels, no new features, no rearranged islands, no decorations, no lighting/shadows beyond existing texture shading. Transparent unused space. Same mob anatomy, only a sandy skin variant.
```

### Spawn egg

```text
Built-in image_gen. Style reference: user's third image (mob-themed pixel spawn eggs).
Subject reference: art/coral-crusher-sandy-v1/textured-model-preview.png.

Create one original Minecraft inventory spawn egg sprite on a truly transparent background. Reference image 1 is STYLE ONLY: those mob-themed eggs use coarse, crisp low resolution pixel art, chunky shaded egg silhouettes, a few distinctive anatomy cues, no surrounding scene. Reference image 2 is SUBJECT reference: the sandy Coral Crusher shark, taupe sandy brown back, ivory underside, dark small eyes and gill slits, pink mouth, tall angular dorsal fins. Design a sandy shark-themed EGG, upright oval egg body with a pointed angular dorsal fin rising on top and small triangular pectoral fins on both sides; small dark eyes near the upper-middle, three dark gill marks on each side, creamy ivory lower third, restrained dark mouth line with 2 ivory tooth pixels. It must read first as a mob-themed spawn egg in reference 1's style, not a swimming shark portrait or a chicken egg illustration. Pixel sprite occupying central 80% of a square transparent canvas, no grid, no border, no text, no shadows outside the sprite, no watermark. Design on a strict 32x32 pixel grid enlarged evenly to 1024x1024 with nearest-neighbor hard pixel edges. Limited warm sand, taupe, ivory, dark brown palette; recognizable simple Minecraft silhouette. Produce only ONE icon.

The returned 1254x1254 RGBA art was fitted to 32x32 with nearest-neighbor sampling.
Alpha is snapped to 0/255 at threshold 128 to remove translucent colored edge artifacts.
Game asset: src/main/resources/assets/tidalterror/textures/item/coral_crusher_spawn_egg.png.
Original generated art is preserved in generated.png; enlarged icon in icon-preview.png.
```
