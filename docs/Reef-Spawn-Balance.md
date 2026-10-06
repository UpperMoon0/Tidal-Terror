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

The intended coverage is approximately **5% of total ocean area**, not 5% of
land and ocean combined. All targets retain region weight 18 and the global
TerraBlender region size. A separate seeded, broad patch gate admits roughly one
in eight initial patch choices, with six zooms to keep the selected areas broad.
The shared ReefBalance constants control frequency and scale independently.
Reducing the region weight alone fragmented reefs and failed the deep-interior
terrain audit; that approach is superseded.

Only Coral Cathedral candidates are filtered. Outside selected patches, the
native parameter tree supplies the original biome, including datapack mappings.
Non-reef candidates and other mods' selected regions remain unchanged. Cold/frozen
oceans remain native. Existing generated chunks retain their terrain. Other
biome mods and TerraBlender config overrides can change coverage.

A native NeoForge 26.1.2 / TerraBlender 26.1.2.0.3 survey sampled five 32,768-block
squares centered at the origin, every 128 blocks at Y=32 (the reef terrain's biome
sampling height). There were 65,536 points per seed, 327,680 total. Coverage fell
from 42.70% to **4.36% of ocean samples**, equivalent to **1.28% of all samples**.
This estimates horizontal footprint, not 3D volume, individual biome frequency,
or exact surface area. Large patch selection increases variation between seeds.
The same settings apply to other targets, which were not separately surveyed.

| Seed | Reef share of ocean samples | Reef share of all samples |
| --- | ---: | ---: |
| 0 | 2.76% | 0.80% |
| 123456789 | 4.17% | 1.30% |
| -987654321 | 5.38% | 1.57% |
| 42 | 4.45% | 1.21% |
| 20261006 | 5.03% | 1.50% |

At 128-block resolution, a four-neighbor scan found 473 connected patches
not touching the survey boundary. The largest sampled patch covered approximately
3.3 km², with a maximum horizontal span of 3,200 blocks. Half the sampled reef
area lay in patches of at least 0.52 km². Small edge fragments still exist; the
median patch by count spans 256 blocks. This coarse scan cannot resolve narrow
connections or boundaries smaller than 128 blocks.

Repeat the survey with Java 21 and the normal toolchains:
`python tools/survey_reef_rarity.py`. It copies the current tracked/unignored
sources into an isolated build snapshot, runs the native sampler, and writes
seed counts and coverage to `build/reef-rarity-survey/result.json`. Raw logs retain
all connected-component measurements. The tool never loads development saves.

## Verification

Native verification is opt-in: `-PspawnPoolTests runGameTestServer` checks
category registration/serialization, the actual loaded biome table, the
native natural-spawner dispatch, global/local pool isolation, actual seabed-targeted crab spawn attempts, and native rarity/fallback behavior while all
other species (including vanilla aquatic creatures) are at their caps.

Validated 2026-10-04: native data generation, both native pool GameTests,
and the normal build passed. Compared full biome JSON against the previous
settings: only drowned weight and four category assignments changed. Jar
biome data matches generated data; main sources match the build snapshot;
GameTest/preview/model-check fixtures are excluded. Native tests verify the
actual category dispatch and codec and both global and local cap isolation.
Logs: `build/crusher-height-package-v1/reef-spawn-pools-*.log`.
Artifact: `build/libs/tidalterror-1.0.0-reef-spawn-pools.jar`. SHA-256: `c9ed7e4b7a2a51a35b53c4786ae16a167c552562dc1dff718343055d6912f3fb`.
