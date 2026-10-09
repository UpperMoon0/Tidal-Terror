# Chunk generation optimization: Tidal Terror and Endless

The basin transition's entity-ready generation time fell from **66.82 to 8.69 seconds** for 16 requested chunks plus their native generation halo: **87.0% lower**, or about **7.7 times faster**. Current stock vanilla took **3.89 seconds**. This is a substantial transition improvement, not a claim that every workload or client rendering now matches vanilla.

Measured final runtime: Tidal Terror `dee2e4959a351a6a20e0032db15926bb17a8a501`, pinned Endless `f506758830f25041b9a05f932dbca4ea47d92861`. Both remain unreleased draft PRs. Documentation commits after these revisions do not change the measured runtime.

## What changed and which mod owns it

**Tidal Terror** owns exact native seabed evaluation. Instead of reconstructing a noise graph separately for each of 256 columns, the Forge prototype now creates one graph for the chunk, shares its X interpolation slices, fills each vertical cell once, and walks unresolved columns downward until native OCEAN_FLOOR_WG finds solid terrain. Use the same noise settings, empty blender, structure-free native marker, fluid picker and stopping predicate as scalar `getBaseHeight`; cache all 256 exact results together. Unsupported horizontal noise settings retain scalar evaluation. Other loader/version adapters keep their existing path.

**Endless** owns reusable native sparse-section construction. `VerticalSectionFactory.uniform` creates independently mutable native block/biome palettes without 4,096 block writes. Its counter accessors are used only on these newly constructed worker-local sections. Counts match individual native writes exactly, including water: native palette recalculation by itself double-counts nonempty water and initializes fluid counters differently. Tidal Terror selects uniform water/rock sections; sediment, bedrock, partial province chunks and decorated boundaries keep their original per-block logic.

The Cathedral's radius, giant height, seabed/bedrock formulas, seeded geometry, complete garden/giant decorators, biome filling and mob tables are unchanged. These changes do not expand dense arrays or replace saved FULL terrain. Two Forge access-transformer entries expose native marker types; prototype-only mixins activate the batched evaluator.

## Independent generation results

Seconds averaged across two fresh JVMs per variant. All results include the stronger entity-ready guard, rather than merely ticket submission or FULL acknowledgement.

| Area | Before: deep | Final: deep | Final: stock vanilla | Final: same mods, Default |
| --- | ---: | ---: | ---: | ---: |
| Outside province | 5.111 | 4.773 | 3.572 | 5.458 |
| Cathedral | 8.666 | 8.172 | 3.984 | 5.973 |
| Inner Wastes | 6.714 | 6.558 | 4.477 | 4.352 |
| Outer transition | **66.824** | **8.693** | **3.885** | **5.823** |

The final transition pair was **8.870 / 8.516 seconds**, versus baseline **67.395 / 66.253 seconds**. The final transition is approximately **2.24x stock vanilla** and **1.49x the current same-mods Default control**. Cathedral and Wastes improvements are small relative to run variation; the evidence does not support a large improvement in those areas. The same-mods control combines Forge, Endless and Tidal Terror, so its difference from vanilla does not isolate an Endless penalty.

An intermediate implementation, `4e71dee` with the same Endless API, shared setup per native cell. It averaged **11.122 seconds** at the transition. Profiling justified sharing the graph across the entire chunk; the final revision reduced that by another **21.8%**. Preserve that intermediate dataset rather than conflating it with the final result.

FULL acknowledgement is a different boundary: the transition's final FULL average is **4.725 seconds**, versus baseline **26.940 seconds**, stock vanilla **2.316 seconds**, and same-mods Default **3.540 seconds**. The additional entity-ready time includes native neighbour completion and entity loading. The requested footprint is 16 chunks, not the total generated footprint.

## Saved-world loading: keep the variability visible

| Restart scenario, entity-ready seconds | Before: deep | Final: deep | Final: stock vanilla |
| --- | ---: | ---: | ---: |
| Outside province | 1.259 | 1.047 | 0.773 |
| Cathedral | 0.607 | 0.541 | 0.475 |
| Inner Wastes | 0.626 | 0.447 | 0.470 |
| Outer transition | **0.767** | **1.180** | **0.684** |

The final transition restart pair, **0.951 / 1.408 seconds**, was slower than the earlier baseline and more variable. Its FULL acknowledgement average nevertheless improved from **0.443 to 0.337 seconds**. One final batch acknowledged FULL immediately but waited another approximately **0.93 seconds** for the all-chunks entity-ready condition. No GC pause was recorded in either final transition restart interval. Some native height evaluation was sampled during one restart interval; the whole restart scenario should not be described as a pure disk-read-only test.

To investigate, clone the already-restarted old and new generated saves and run both under the **same final runtime** in serial **old/new/new/old** order. Original saves are never changed. The repeated results were:

| Copied saved terrain | Transition entity-ready pair | Mean entity-ready | Mean FULL acknowledgement |
| --- | --- | ---: | ---: |
| Old generated terrain | 0.499 / 0.711 s | **0.605 s** | 0.437 s |
| New generated terrain | 0.639 / 0.630 s | **0.635 s** | 0.335 s |

The large restart regression did not reproduce in that controlled comparison. The approximately 30 ms difference between means is within the old pair's variation and below one server tick. This supports a large fresh-generation improvement and faster FULL loading of the new representation, while leaving entity-ready restart timing variable. It does not establish zero reload regression on every seed or on first restart: these follow-up clones have already completed the original restart scenario.

## Profiles and native section microbenchmark

Interval-filtered JFR transition execution samples fell from **4,952 / 4,979** before to **803 / 846** final. Original profiles were dominated by repeated noise synthesis, graph wrapping and hashing; final profiles are closer to ordinary terrain's mix of aquifer/noise/surface work. Final transition GC pauses total **0.110 / 0.118 seconds**, versus approximately **0.480 / 0.482 seconds** before. These are sampling counts and recorded pauses, not precise elapsed-time percentages. Truncated stacks limit attribution.

Endless's factory also has an independent, serial ABBA native microbenchmark: warm both paths 64 times, then construct 256 water sections per timed group, retain each result through a volatile sink, and compare original native writes with the uniform factory.

| Native fixture JVM | Individual-write mean | Uniform-factory mean | Sections per timed group |
| --- | ---: | ---: | ---: |
| Final fresh fixture | 32.840 ms | 0.249 ms | 256 |
| Final restart fixture | 42.441 ms | 0.229 ms | 256 |

This measures section construction only, with the fixture JVM's own settings. It is not a claim that Endless, chunk generation or frame rate improves by that ratio. Biome filling, decoration, native terrain generation and page installation remain separate costs.

## Validation and reproduction

- Fresh generation and separate-JVM restart passed on the final whole-chunk algorithm. All **768 native heights** matched scalar vanilla evaluation in outside and both sides of the blending band, including negative coordinates.
- AIR, WATER, DEEPSLATE and living coral sections matched exact individual-write counters; codec round trips preserved every block; modifying one section did not affect another.
- Frozen 0.0.3 features/carvers/spawn inventory, all six restored garden families and all five coral colors passed. The garden remained 730 living coral blocks, 435 boulder blocks, 373 plants, 377 fans, 189 seagrass and 102 sea pickles after active/forced ticking and restart.
- All four custom species, vanilla aquatic species above/below the canopy, deep Drowned weight 1 and rejected custom upper/Wastes positions passed native dispatch checks. Bedrock continuity, ordinary terrain, player-edit preservation, both lighting seams and zero admission builds passed.
- Cache regression passed point/batch reuse, eviction, independent-chunk concurrency, malformed-batch refusal and retry. All 24 tooling tests passed. Final Forge normal/prototype packaging passed; fixtures and bundled Endless classes are absent from the prototype jar. Shared Endless 1.20.1/1.21.1 compilation and common unit suites passed. Final additional-port compilation is recorded in the accompanying receipts.
- Runtime CI passed **32 Tidal Terror checks** and **70 Endless checks**. One NeoForge 1.21.1 extreme-height same-JVM rejoin test timed out while closing its world; an unchanged failed-job retry passed. Retain draft status and existing promotion gates; these results are not merge/publication authorization.

The original [benchmark method and commands](Chunk-Loading-2026-10-09.md) apply unchanged: stock Minecraft 1.20.1 / Forge 47.2.0, identical Java 17.0.20.1+1, 2 GiB heap, four active processors, seed 0, structures enabled, view distance 4, simulation distance 5, ordinary-terrain warmup and no concurrent client/build. Both modded variants use logical range [-1024,1024). Each final/intermediate revision ran twelve serial JVMs in reversed variant order. The saved-world follow-up adds four serial JVMs. Windows filesystem caches are not flushed. This is one-seed server-side evidence, not client meshing/network/frame-time evidence or a sweep of logical heights.

```powershell
python -u tools/province/benchmark_chunks.py --launch build/chunk-bench-optimized-launch.json --output build/chunk-bench-new-final
python tools/province/profile_chunk_benchmark.py build/chunk-bench-new-final
python -u tools/province/benchmark_reload_pair.py --old build/chunk-bench-2026-10-09-matched/3-deep --new build/chunk-bench-2026-10-09-whole-chunk/3-deep --launch build/chunk-bench-optimized-launch.json --output build/chunk-reload-new-pair
```

Never overwrite output directories or clean the root build directory. Use the capture helper from the original report to obtain an actual production-source-set Forge launch, and stop that probe before measuring. Native fixture commands add `-PdeepProvinceTestDirectory=build/deep-province-opt-chunk` to the original fresh/restart commands; that directory remains separate from user saves and earlier audit worlds.

Evidence: [final timings](2026-10-09-chunk-whole-batch.json), [final profiles](2026-10-09-chunk-whole-profiles.json), [intermediate timings](2026-10-09-chunk-cell-batch.json), [intermediate deep profiles](2026-10-09-chunk-cell-profiles.json), [paired saved-world loading](2026-10-09-chunk-reload-pair.json), [native check/microbenchmark receipts](2026-10-09-chunk-optimization-checks.json) and [runtime jar/JFR/log hashes](2026-10-09-chunk-optimization-receipts.json). Large recordings and disposable worlds remain local under `build/`.
