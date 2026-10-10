# Coral Crusher skins — Minecraft 1.20.1 / Forge 47.2.0

Both skins use tidalterror:coral_crusher, with the same model, animation,
attributes, attacks and navigation. No new entity type or spawn-list entry.

Spawn eggs choose blue or sandy randomly with equal probability, independent
of depth, through vanilla SPAWN_EGG finalization (including dispenser use).

At natural finalizeSpawn, sharks within 24 blocks above the reef's actual sand seabed
receive the sandy skin. Sharks higher in the water receive the existing blue
skin. The skin stays fixed as the shark swims and is saved as Skin=sandy or
Skin=blue. Old saves without Skin and unknown skin values default to blue.
SynchedEntityData carries the skin to clients. Explicit summon NBT can select
Skin:"sandy" or Skin:"blue". Natural spawning is still Coral Cathedral only.

OCEAN_FLOOR includes the giant coral structures. The lookup starts at that
heightmap and descends to the real SAND layer, so a high coral crown cannot
turn an upper-water shark sandy. It only reads the spawning column. This is
a spawn habitat choice; it does not restrict pursuit or wandering afterward.

Native references inspected locally:
MC-Modding-Src/1.20.1/net/minecraft/world/entity/animal/Fox.java
  synchronized variant definition:112,157; spawn initialization:294-317;
  getter/setter:346-352; NBT save/load:371-405.
MC-Modding-Src/1.20.1/net/minecraft/world/level/NaturalSpawner.java:164-168
  moves the mob to the accepted native spawn position before finalizeSpawn,
  then adds it to the level. Mapped Fox APIs confirmed against the project's
  Forge 47.2.0 / Parchment jar. Local decompiled member names are obfuscated.

Artwork was generated with the built-in image_gen tool. Exact prompt and
original result are preserved in art/coral-crusher-sandy-v1/. The image tool
shifted some atlas edges; assemble_sandy.py samples generated materials and
fits them onto all 132 native UV faces. The exact used-texel mask, orientation,
mouth/teeth and anatomical landmark placements are preserved. Gill color is
sampled from generated artwork. Every used texel is opaque; unused space clear.
The blue texture is unchanged. Both game textures are 256x256 RGBA.

Asset:
src/main/resources/assets/tidalterror/textures/entity/coral_crusher/coral_crusher.sandy-v1.png
Model/UV preview:
art/coral-crusher-sandy-v1/textured-model-preview.png
Reassembly:
  python art/coral-crusher-sandy-v1/assemble_sandy.py
Native regression:
  gradlew.bat -PcoralCrusherTests runGameTestServer --offline
Native shader photo fixture (three sandy shark views, isolated preview save):
  gradlew.bat -PreefPreview -PcrusherSkinPreview runClient --offline
All fixture sources are excluded from ordinary release builds.
