# Deep Cathedral spawning verification, 2026-10-09

The Forge 1.20.1 prototype now selects native spawn positions within the basin.
Endless commit `b82b3428f2e7d9cb86af6ca0307b92cabb7e6b2e` supplies the logical
lower-bound dispatch check. Tidal Terror confines all four custom species to
Cathedral water at or below the local giant-coral canopy. They are rejected in
upper water and the Wastes. Vanilla aquatic mobs can use both sides of the
canopy, with the Cathedral Drowned selection weight retained at 1.

The native fixture uses seed 0, center (-55689, 43535), a generated basin column
with floor -446 and canopy ceiling -337, and a nearby non-spectator player.
Each custom category receives 256 `NaturalSpawner.spawnCategoryForChunk`
attempts. Each vanilla aquatic/creature category receives 1,024 attempts with
the player in each vertical zone. Rare Drowned attempts stop at one deep spawn
witness, with a maximum of 131,072 attempts. Native collision, placement,
player-distance, biome selection and group spawning run; a separate refusing
extra predicate must produce no spawns. Existing pool tests cover native caps.

| Counted accepted spawns | Fresh | Cold restart |
| --- | ---: | ---: |
| Coral Crusher | 707 | 756 |
| Cathedral Ray | 988 | 1,018 |
| Veilglow | 1,000 | 1,020 |
| Shardback | 129 | 198 |
| Vanilla aquatic/creature below canopy | 3,705 | 3,819 |
| Vanilla aquatic/creature above canopy | 5,237 | 5,619 |
| Deep Drowned witness | 1 | 1 |

Both runs witnessed all six lower-water vanilla species: tropical fish,
pufferfish, squid, glow squid, dolphins and turtles. Custom upper-water and
Wastes placement rejection, Drowned weight 1, zero-light deep water, the
24-section dense core, original feature/spawn inventories, reef survival,
existing-page refusal, worker-only admission and saved player edits all pass.

Counts are acceptance evidence, not population-density or performance
benchmarks: each counted mob is immediately discarded and the fixture supplies
an accepting extra predicate except in the refusal case. It does not simulate
long-running global/local cap equilibrium or AI movement boundaries.

Reproduce in disposable audit worlds:

```text
gradlew.bat -PprovincePrototype -PdeepProvinceTests runServer --offline
gradlew.bat -PprovincePrototype -PdeepProvinceTests -PdeepReload runServer --offline
```

Local logs: `build/deep-spawn-complete-fresh.log` and
`build/deep-spawn-complete-reload.log`. Deep world generation remains a Forge
1.20.1 prototype; the shared habitat code builds for the other supported ports,
but those ports do not yet have the deep preset adapter. Both PRs remain draft
and unmerged; full new-head CI is a separate validation gate.
