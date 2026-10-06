# Reef spawn balance

Coral Cathedral keeps the warm-ocean roster, with drowned selection weight
reduced from 5 to 1 (group size still one). This is approximately an 80 percent
reduction in selection frequency. Native drowned placement, darkness, depth
and difficulty rules remain; other biomes are unaffected.

Each mod species has its own native spawn category on every supported loader:

| Species | Serialized category | Base cap | Group size |
| --- | --- | ---: | --- |
| Coral Crusher | tidalterror_crusher | 2 | 1 |
| Cathedral Ray | tidalterror_ray | 8 | 2-3 |
| Veilglow | tidalterror_veilglow | 12 | 2-4 |
| Shardback | tidalterror_shardback | 10 | 1-3 |

Entity types and biome spawn entries use the same categories. The native
natural-spawner dispatch, global counting, local player caps and category
codec remain responsible for spawning. No manual spawn loop bypasses caps.
The categories are friendly, non-persistent spawning pools with the same
128-block despawn range as ordinary water creatures.

For each category the global cap is floor(base cap * eligible spawning chunks
/ 289), together with native local player caps. With one full 289-chunk area
the budgets are approximately those listed above. They are not per-chunk or
fixed biome-wide counts. Spawn groups can overshoot a cap, and some persistent
mobs are excluded from native counting. Squid and dolphins keep their shared
vanilla base cap of five; fish retain their separate base cap of 20.

Each custom pool has one species, so its old selection weight no longer sets
relative abundance against the other species. Caps, group sizes and valid
habitat determine the population. Existing spawn depths and the crab's
sediment requirement remain intact. Restart the client/server to load the
updated categories and biome data. Existing drowned are not removed.

Crab natural spawn attempts project the randomly sampled column onto its loaded
OCEAN_FLOOR height, when it has submerged, sturdy sediment. Other species keep
Minecraft's original height sampling. The crab's biome/depth/clearance rules,
player distances, group size and caps still run in NaturalSpawner. This improves
success on suitable seabed without increasing the crab population budget. Coral
structures and steep neighboring columns can still invalidate an attempt.

Group size is per group, not a spacing rule. Minecraft tries up to three groups
near one sampled point, so a Crusher group of one can still yield 2-3 nearby
sharks. Caps are checked around the chunk dispatch; an admitted batch can
overshoot them. Separate spawning attempts and pursuit of the same target can
also bring sharks together. Territories do not reserve exclusive space.

## Biome rarity

All targets use shared `ReefBalance.REEF_REGION_WEIGHT = 1`, reduced from 18.
The global TerraBlender region size remains its default of 3. This reduces
frequency without changing region scale or restricting reefs to small features.
The intended coverage is approximately **5% of total ocean area**, not 5% of land
and ocean combined. Five eligible ocean types can become reefs; cold/frozen oceans
remain native. Other mods and TerraBlender config overrides can change coverage.

A native NeoForge 26.1.2 / TerraBlender 26.1.2.0.3 survey sampled five 32,768-block
squares centered at the origin, every 128 blocks at Y=32 (the reef terrain's biome
sampling height). There were 65,536 points per seed, 327,680 total. Coverage fell
from 42.70% to **5.66% of ocean samples**, equivalent to **1.66% of all samples**.
This estimates horizontal footprint, not 3D volume, individual biome frequency,
or exact surface area. The same settings apply to other targets, which were not
separately surveyed. Existing generated chunks retain their terrain.

| Seed | Reef share of ocean samples | Reef share of all samples |
| --- | ---: | ---: |
| 0 | 4.89% | 1.43% |
| 123456789 | 5.06% | 1.58% |
| -987654321 | 6.49% | 1.90% |
| 42 | 5.66% | 1.53% |
| 20261006 | 6.22% | 1.85% |

A four-neighbor connected-component scan at the same coarse resolution found
1,007 patches not touching the survey boundary. Their median maximum horizontal
span was 256 blocks; the largest reached 2,048 blocks. Smaller edge fragments are
still possible: rarity does not guarantee every reef is large. The scan cannot
resolve narrow connections or boundaries smaller than 128 blocks.

## Verification

Native verification is opt-in: `-PspawnPoolTests runGameTestServer` checks
category registration/serialization, the actual loaded biome table, the
native natural-spawner dispatch, global/local pool isolation, and actual seabed-targeted crab spawn attempts while all
other species (including vanilla aquatic creatures) are at their caps.

Validated 2026-10-04: native data generation, both native pool GameTests,
and the normal build passed. Compared full biome JSON against the previous
settings: only drowned weight and four category assignments changed. Jar
biome data matches generated data; main sources match the build snapshot;
GameTest/preview/model-check fixtures are excluded. Native tests verify the
actual category dispatch and codec and both global and local cap isolation.
Logs: `build/crusher-height-package-v1/reef-spawn-pools-*.log`.
Artifact: `build/libs/tidalterror-1.0.0-reef-spawn-pools.jar`. SHA-256: `c9ed7e4b7a2a51a35b53c4786ae16a167c552562dc1dff718343055d6912f3fb`.
