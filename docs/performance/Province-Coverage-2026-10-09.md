# Reef Province ocean coverage: 2026-10-09

The current Deep Reef Province placement replaces approximately **0.57% of sampled vanilla ocean biome area**, including the Cathedral, Rim and Wastes. The six-seed results range from **0.23% to 0.88%**. Cathedral alone accounts for **0.11% of vanilla ocean area** in the pooled survey.

Only **105 of 5,766 candidates qualify (1.82%)**. The complete province footprint covers **0.443% of all map area** in the surveyed regions. The earlier 24.3% figure was the theoretical footprint if every candidate qualified; it was not measured ocean coverage.

## Results by seed

| Seed | Accepted candidates | Vanilla ocean covered by province | Vanilla ocean covered by Cathedral |
| --- | ---: | ---: | ---: |
| 0 | 9 / 961 | 0.268% | 0.059% |
| 1 | 8 / 961 | 0.231% | 0.052% |
| -1 | 28 / 961 | 0.857% | 0.180% |
| 7142026 | 16 / 961 | 0.542% | 0.102% |
| -9223372036854775808 | 19 / 961 | 0.605% | 0.119% |
| 9223372036854775807 | 25 / 961 | 0.875% | 0.164% |
| **Pooled** | **105 / 5,766** | **0.567%** | **0.113%** |

The pooled result is area-weighted: sum province/ocean intersection estimates and divide by total sampled vanilla ocean area. It is not an unweighted average of the six percentages. Seeds were selected before measurement: 0, 1, -1, 7142026 and both signed-long limits. These are six reproducible examples, not a random survey of all world seeds.

## Which ocean denominator?

The main result answers **what fraction of the original vanilla ocean biome footprint lies inside an accepted province?** Vanilla ocean means the delegate biome source returns the `minecraft:is_ocean` tag at quart Y=8 (block Y=32), exactly matching the current placement eligibility test. It includes all native ocean biome variants and uses neither a province radius approximation nor a hand-written ocean noise threshold.

A second useful denominator is the **union of vanilla ocean and accepted province footprints**. Provinces occupy **1.487% of that combined domain**. This includes non-ocean terrain inside province rings; it is a biome-footprint estimate, not an exact count of surface water blocks. It must not be reported as measured physical sea-surface coverage. Outer-edge native-height blending can preserve high terrain, and neither river area outside provinces nor actual water block columns were evaluated.

Approximately **62.24% of accepted province area overlies vanilla non-ocean biomes**. This follows from current eligibility: the center must be ocean and 75% of the expanded core probes must be ocean, but the much larger outer rings have no ocean requirement. The measurement confirms that the core-only rule often admits substantial non-ocean terrain in the Wastes. It does not change this placement policy.

## Native measurement method

- Runtime placement source: Tidal Terror `3ac2c342228250c5558bab6c4e17a441ea566afb` (runtime unchanged from `dee2e49`), Endless source pin `f506758830f25041b9a05f932dbca4ea47d92861`, Forge 47.2.0 / Minecraft 1.20.1 / Java 17.0.20.1+1. Survey code is opt-in development tooling; no generation rules changed.
- Bootstrap the real deep preset in a separate local server directory. Read the loaded native registries and generator noise settings, then construct the native `RandomState` sampler for each seed. Call the actual `ReefProvinceBiomeSource.province` at every candidate center, retaining its complete cached eligibility decision.
- Survey cell coordinates [-15,15] on both axes, including negative coordinates. Each seed covers the same 380,928 x 380,928-block region, from -184,320 inclusive to 196,608 exclusive: 145,106,141,184 blocks squared per seed.
- Use one deterministic randomly jittered point per equal-area 16 x 16 stratum in every cell: **1,476,096 global area samples** across six seeds. Classify each with the native delegate biome sampler and original seeded warped layout.
- Independently refine every accepted cell with 128 x 128 jittered strata (96-block stratum width). This evaluates **1,720,320 geometric sample positions** across the 105 accepted cells; **418,537** positions are inside provinces and receive an additional native ocean classification. Rejected cells contribute exactly zero province area. Use equal cell-area weights, not equal weights for the two sampling densities.
- The first coarse survey found 0.562% ocean coverage; the refined result is 0.567%. Both runs agree exactly on every acceptance decision and every coarse count. The refined total-map footprint is 0.443036%, versus 0.442940% from the exact angular-area integral of the current warped boundary and measured accepted-cell count.
- Both native runs complete successfully and stop normally. No survey-position chunks are generated: the native biome/placement sampler determines the province footprint directly. Only the separate server's bootstrap spawn chunks are generated. The user's client and saves remain intact.

This estimates spatial area in the chosen regions. It does not exhaust all world coordinates, establish a confidence interval across world seeds, or measure physical water columns. Native acceptance counts are exact for these cells; ocean intersections are stratified spatial estimates.

## Reproduction and evidence

```powershell
.\gradlew.bat -PprovincePrototype -PprovinceCoverageTests -PcoverageOutput=build/province-coverage-new -I tools/province/coverage.init.gradle runServer --offline --console=plain
python tools/province/summarize_coverage.py build/province-coverage-new/coverage.json --output build/province-coverage-new/summary.json
```

Use a fresh output directory. The helper refuses an existing directory and activates only through its init script and explicit survey property. Rebuild normal/prototype packaging without that init script before producing a jar; the survey class must be absent from release artifacts.

[Raw per-cell counts](2026-10-09-province-coverage.json), [weighted summary](2026-10-09-province-coverage-summary.json) and [native log/source receipts](2026-10-09-province-coverage-receipts.json) preserve the evidence. The coarse raw output and both disposable bootstrap worlds remain under `build/`. No PR was merged or release published.
