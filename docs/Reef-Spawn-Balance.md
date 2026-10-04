# Reef spawn balance

Coral Cathedral keeps the warm-ocean roster, with drowned selection weight
reduced from 5 to 1 (group size still one). This is approximately an 80 percent
reduction in selection frequency. Native drowned placement, darkness, depth
and difficulty rules remain; other biomes are unaffected.

Each mod species now has its own native Forge spawn category:

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

Native verification is opt-in: `-PspawnPoolTests runGameTestServer` checks
category registration/serialization, the actual loaded biome table, the
native natural-spawner dispatch, and global/local pool isolation while all
other species (including vanilla aquatic creatures) are at their caps.

Validated 2026-10-04: native data generation, both native pool GameTests,
and the normal build passed. Compared full biome JSON against the previous
settings: only drowned weight and four category assignments changed. Jar
biome data matches generated data; main sources match the build snapshot;
GameTest/preview/model-check fixtures are excluded. Native tests verify the
actual category dispatch and codec and both global and local cap isolation.
Logs: `build/crusher-height-package-v1/reef-spawn-pools-*.log`.
Artifact: `build/libs/tidalterror-1.0.0-reef-spawn-pools.jar`. SHA-256: `c9ed7e4b7a2a51a35b53c4786ae16a167c552562dc1dff718343055d6912f3fb`.
