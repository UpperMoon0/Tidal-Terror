# Balanced province frequency and Reef Compass: 2026-10-09

New Reef Province worlds average **17,576 blocks from the native spawn-search hint to the nearest province edge**, within the intended 10,000-20,000-block average. The earlier layout averaged 51,512 blocks from origin. The new origin-to-edge average is 17,488 blocks, **66.1% lower** than that like-for-like original reference. All Cathedral/rim/Wastes radii, angular warping and giant-coral heights are retained.

## Placement and saved worlds

Placement version 2 uses **8,192-block cells**, **256-block seeded center jitter**, an ocean center and at least **two-thirds ocean probes within the expanded Cathedral core**. The maximum warped radius remains below 3,789 blocks; candidate centers remain at least 3,840 blocks from every cell edge. Complete rings fit their cells and cannot overlap. Outer rings still do not require ocean coverage.

The prototype presets explicitly select version 2. Older serialized biome sources without `placement_version` default to **version 1**, retaining 12,288-block spacing, 1,024-block jitter, 75% core eligibility and their original seeded centers. The field is persisted through the source codec. Existing worlds are not silently converted to the denser layout; create a new prototype world for the new balance. Saved terrain and the former performance fixtures remain intact.

## Six-seed measurement

| Seed | Nearest province edge | Nearest Cathedral edge | Nearest center |
| --- | ---: | ---: | ---: |
| 0 | 33.8k | 36.3k | 37.4k |
| 1 | 27.8k | 30.2k | 31.2k |
| -1 | 2.4k | 5.0k | 6.1k |
| 7142026 | 17.6k | 20.1k | 21.2k |
| -9223372036854775808 | 9.8k | 12.1k | 13.1k |
| 9223372036854775807 | 14.1k | 16.4k | 17.4k |
| **Mean** | **17.6k** | **20.0k** | **21.1k** |

Distances are horizontal straight lines, not navigable routes. Native `Climate.Sampler.findSpawnPosition` supplies the starting hints; vanilla subsequently chooses a surface position within an 11x11-chunk local search. The final surface spawn and the first player spawn spread are not independently generated for all six seeds. The loaded seed-0 client starts at (-21.5,68,5.5), close to its measured native hint (-31,6). Hint-to-edge results span **2.4k-33.8k blocks**; this is an average target, not a per-seed maximum or a guaranteed proximity to spawn.

Native ocean-area sampling uses the same six seeds and 961 cells each, now covering [-122880,131072) per axis. This is the same cell-coordinate range as the old survey but a smaller physical region due to changed spacing. Of **5,766 candidates, 236 qualify (4.09%)**. The full province occupies **2.746% of sampled vanilla ocean area** and **2.240% of total map area**. Cathedral alone intersects **0.531% of vanilla ocean area**. Approximately **63.70% of province footprint overlies non-ocean delegate biomes**; the core-only gate still permits substantial coastal/non-ocean Wastes. These are biome-footprint estimates at Y32, not physical water-column counts.

Use the real native biome sampler and unchanged seeded geometry, 1,476,096 coarse area samples and independent 128x128 strata within accepted cells. Mean distances are computed against every accepted cell, including the warped boundary. The nearest measured provinces are well inside the surveyed region, so unsurveyed cells cannot hide a nearer candidate. Six preselected seeds are not a guarantee across every seed.

## Reef Compass

Craft **one vanilla Compass + three Amethyst Shards + one Nautilus Shell** in the prototype preset. Obtaining a compass unlocks its recipe. The item is also in the Forge Tidal Terror creative tab.

Use the compass to attune it to the **nearest accepted Cathedral center within 65,536 blocks**. The needle then follows that saved target, with vanilla compass wobble and wrong-dimension spinning. Use it again after travelling to retune. The search evaluates candidate centers in distance order on a single bounded server worker, without generating distant chunks. Cooldowns and one pending search per player limit repeated requests; a full queue reports busy. Delivery checks the original player, dimension, stack ownership and request token before changing the item. Delivery guards reject stale ownership/dimension/token results, and the worker is interrupted when its server stops.

Targets survive native item serialization and do not require a lodestone block at the destination. Moving an item into a different-seed world clears its stale target. Worlds without a province-capable source report no resonance. The compass follows each saved world's placement version, including legacy worlds. This first deep-world locator belongs to **Tidal Terror's Forge 1.20.1 prototype**; the other release ports do not yet have the deep province adapter or this locator recipe. Endless requires no changes for this feature.

## Generated artwork and validation

The dial and needle were generated separately with the built-in image generation tool, with transparent backgrounds. The resource packer resizes them for the game and produces 32 rotating 64x64 RGBA frames. [Original art, exact prompts and packer](../assets/reef-compass/README.md) are saved in the repository. [Native screenshot](../assets/reef-compass/compass-pose-0.png) shows the real atlas/model in the inventory.

- Native fresh generation and separate-process reload preserve all six garden families, all five coral colors, giant crown fans, deep spawns, reduced Drowned weight, bedrock/water seams, lighting and a player edit. A newly located province uses a larger 80x80 garden witness; all-color preservation is not assumed within every 48x48 patch. Its 2,456 living coral blocks, 485 boulder blocks, 925 plants, 844 fans, 422 seagrass and 220 pickles remain stable after ticking/reload.
- Native locator search returns seed-0 center (-4317,37119), with unchanged loaded-chunk count. Recipe output, native target serialization, native inventory ticking without a lodestone, foreign-seed clearing, unbound tooltip/target access and both placement codec versions are checked.
- The real client uses the item asynchronously, receives the server-owned target through normal inventory synchronization, verifies four 90-degree bearings and native frame selection, and captures all four final framebuffers. Initial unbound tag handling was corrected after this native test caught vanilla's non-null tag precondition.
- Pure nearest-target regressions cover distance ordering, empty search, cancellation, negative coordinates, a bounded search and legacy centers. Layout/deep-bedrock invariants retain the full Cathedral and complete nonoverlapping rings. Normal/prototype builds and artifact guards pass, excluding the native fixtures. All four additional ports compile locally; all 24 tooling tests pass. The final versioned native survey exactly reproduces every acceptance decision, sample count and spawn hint from the selected balance trial.

[Raw native coverage](2026-10-09-province-balanced-coverage.json), [weighted coverage summary](2026-10-09-province-balanced-summary.json), [spawn-hint distance results](2026-10-09-province-balanced-travel.json), and [build/native receipts](2026-10-09-province-balanced-receipts.json). The discarded 50% core trial averaged only about 7.2k blocks to an edge and was not selected. New placement uses two-thirds core ocean instead.

Version remains **0.0.4 unreleased** in draft PR #3; no merge, tag or publication. The prior optimization benchmark measures the older placement runtime and is preserved as historical evidence, not relabelled as a performance benchmark of these new locations.
