# Coral Cathedral - deep reef revision

The biome has a sandy deep basin, open water between giant corals, smaller seabed gardens,
rounded sandstone boulders, seagrass, coral fans, pickles and vanilla warm-ocean fauna.
All building materials are vanilla blocks.

Terrain:
- Ocean-centered provinces contain a full-size Cathedral core and proportionally scaled Sunken Wastes rings. The Cathedral model radius is 1,024 blocks; outside the full footprint, terrain stays native.
- ReefTerrain samples the native biome source without requesting distant chunks.
- The reef interior floor undulates around Y -49; sea level is 63.
  The surrounding Wastes include a raised rim, lower inner ring and outer approach.
  The outermost 569 model blocks blend into the actual original native terrain.
  Forge 1.20.1 also supports the optional Endless deep world type, whose core is near Y -448.
  Original heightmaps are captured before excavation; sparse noise-height lookups anchor
  cross-chunk giant corals deterministically. Both shared caches are bounded.
- ReefBasinFeature operates only on the decorating chunk's reef columns at RAW_GENERATION.
  Each floor has 6-8 solid sand layers, with sandstone beneath; existing bedrock is retained.
- Changes apply to newly generated chunks. Existing saved terrain is not regenerated.
- Flooded quart-biome cells use the reef biome through Minecraft's native biome resolver.
  Native cave biomes are retained beneath the seabed. This prevents cave vegetation from
  being placed inside the excavated ocean and later losing its supporting neighbours.

Coral:
- CoralGeometry makes exact, deterministic voxel plans. Three giant forms:
  branching staghorn groves with root arches and forked twigs;
  fluted open chalices with scalloped lips;
  ribbed sea fans with lace holes.
- Anchors are spaced on an 88-block lattice with seed-dependent offsets.
  Giant widths extend across multiple chunks. Heights vary, with some crowns just below the surface.
- Each chunk writes only its own slice. There are no far-chunk decoration writes.
  A bounded cache shares immutable geometry plans across generation threads.
- Giant coral is placed at TOP_LAYER_MODIFICATION, after native vegetation.
  Sandstone interiors and retained adjacent water prevent living coral from suffocating.
- Seabed gardens contain smaller branching, layered and folded coral, separately from the giants.
  ReefGardenLayout samples seeded world-coordinate density fields and thins nearby anchors.
  There is no fixed colony count or centre offset per chunk. Full shapes include neighbouring
  anchors, with each chunk writing its own slice. Heights, rotation, aspect and forms vary.
  Native one-block coral plants, fans, seagrass and pickles form much denser seabed patches.
- Vanilla amethyst geodes, kelp_warm and warm_ocean_vegetation are omitted in this biome.
  Geodes could otherwise appear suspended in the excavated basin; our gardens control colony density.
  Other inherited features retain their vanilla ordering.
- Native underwater_magma is omitted to avoid bubble columns. Native glow_lichen is omitted:
  MultifaceBlock.updateShape returns AIR after its last support face is removed, including
  waterlogged lichen. Unsupported waterlogged decorations are replaced with source water
  in the final flooded-volume pass. Seagrass and pickles are rooted by the custom garden
  instead of the cross-chunk seagrass_warm and sea_pickle placements.
- ReefWaterFinish queues only newly generated server chunks. At the end of a native level
  tick it uses getChunkNow and waits for BLOCK_TICKING, without loading neighbouring chunks,
  to finish the flooded volume after native postprocessing. FULL alone is too early:
  ChunkMap.prepareTickingChunk invokes LevelChunk.postProcessGeneration later.
  Unfinished new chunks retain a small NBT marker across saves and resume this one-time pass.
  Existing unmarked chunks and player-built spaces are not scanned on ordinary loads.
  This also covers decoration spilling from adjacent cave biomes.
- ReefWaterPostprocessMixin preserves source water when the native
  Block.updateFromNeighbourShapes call inside LevelChunk.postProcessGeneration
  discards a waterlogged block in the reef. All other biome results remain native.
  The development fixtures trace these native replacements and inspect live water
  around every screenshot camera, after actual generation and ticking.
- Native UnderwaterMagmaFeature applies its radius without checking the biome of
  every written block. Both final passes replace magma/soul-sand intrusions with
  sandstone in new reef columns and restore any resulting bubble columns to water.

Source references inspected locally for Minecraft 1.20.1:
net/minecraft/world/level/chunk/ChunkGenerator.java (feature order and biome feature selection)
net/minecraft/server/level/WorldGenRegion.java (write bounds and heightmaps)
net/minecraft/world/level/levelgen/GenerationStep.java (generation order)
net/minecraft/world/level/block/CoralBlock.java (six-neighbour water survival)
net/minecraft/world/level/block/MultifaceBlock.java (support loss returns air)
net/minecraft/world/level/block/MagmaBlock.java (scheduled bubble-column creation)
net/minecraft/world/level/levelgen/feature/UnderwaterMagmaFeature.java (radius writes and placement checks)
net/minecraft/world/level/chunk/LevelChunk.java (postProcessGeneration neighbour-shape updates)
net/minecraft/world/level/chunk/ChunkAccess.java (native fillBiomesFromNoise)
Forge ChunkEvent.Load (new-chunk flag and warning about callbacks before FULL promotion)
net/minecraft/world/level/levelgen/feature/CoralFeature.java (native coral decorations)
net/minecraft/data/worldgen/placement/AquaticPlacements.java (native vegetation density)
net/minecraft/world/level/NaturalSpawner.java (weighted spawning, distance and collision checks)

Coral Crusher:
The spawn table and IN_WATER placement predicate both restrict natural spawns to
tidalterror:coral_cathedral. Deep water is eligible; the surface and all vanilla biomes are rejected.
The native NaturalSpawner trigger used in previews accelerates attempts only in the fixture.
Production spawn caps/rates are unchanged.

Verified version 4 fresh normal Overworld, seed 7142026:
112 x 112 block sample at x/z -48..63: water depth up to 112 m; giant coral vertical span 100 m.
14,554 coral blocks and 6,378 plant/decorative blocks (previous revision: 1,378 decorations).
Accelerated native attempts created Coral Crushers, dolphins, turtles and tropical fish below Y -15.
All sampled living coral survived the native CoralBlock tick, including chunk seams.
All 64 registered vanilla biomes rejected Coral Crusher natural placement.
The sand/strata checks found no stone or deepslate below the sampled deep floor.
The version 4 ticking fresh-world scan found zero air pockets, magma blocks and bubble columns.
All twelve live shader views found zero air pockets and bubble columns.
The preceding water-fix revision also passed cold reload
and persisted unfinished-chunk-marker checks in native saved NBT.
Six native GameTests passed, including original animal predicates across all 64 vanilla biomes,
deep reef turtle placement/obstruction and rejection of solid collisions.
Eight radial native-boundary checks differed from the reference seabed by at most one block.
A coarse native climate scan found a connected reef patch spanning approximately 960 x 1,280 m.
This is one tested seed/sample, not an all-seed coverage claim.

Preview workflow:
1. gradlew.bat runData --offline
2. gradlew.bat runServer -PreefTests --offline (fresh isolated audit world; requires prepared EULA/properties)
3. gradlew.bat runClient -PreefPreview -PreefLushPreview --offline (private copied world; thirteen screenshots including creative egg; automatic exit)
   python tools/package_reef_previews.py --lush (gallery, PNG pack, native validation and checksums)
4. python tools/launch_reef_explorer.py (independent project snapshot/client; private copied world; stays open)

Structure workshop:
art/coral-structure-study/index.html has orbit, zoom and cutaway controls for the exact voxel geometry.
See its README for the standalone Java export and image-render commands.
These studies have simplified colours; art/coral-cathedral-v4 contains actual shader framebuffer captures.

Deep reef fauna preserves vanilla spawn categories, rates/caps and AI. Dolphin and tropical-fish
predicates OR in the deep reef water route. Turtles also get a reef-only exception to native
ON_GROUND placement and Mob's liquid-obstruction gate; actual collision and world border checks remain.
Turtle entries use CREATURE, so native passive-animal spawning cadence/caps still apply.
No breathing or reproduction changes: dolphins still surface for air and turtles retain vanilla breeding.
The native reference paths are WaterAnimal.java:81-85, TropicalFish.java:190-192,
Turtle.java:180-187, NaturalSpawner.java (native positioning/placement), and Mob.checkSpawnObstruction.
Mapped Forge SpawnPlacementRegisterEvent and ForgeEventFactory.checkSpawnPosition bytecode were inspected.

The Tidal Terror creative tab contains the native Coral Crusher spawn egg with a custom untinted
32x32 image-generated sandy-shark icon. Template example_item, example_block, Config and greeting
logs were removed. Event classes are CoralCrusherClientEvents and CoralCrusherEntityEvents.
Project metadata and release filename use Tidal Terror / tidalterror.
