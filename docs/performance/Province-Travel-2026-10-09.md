# Province travel distance: 2026-10-09

The nearest province edge averages **51,512 blocks** from world origin across the six surveyed seeds. Use roughly **52,000 blocks** as an initial spawn-area estimate. The median is **57,826 blocks**; the sample ranges from **5,433 to 94,702 blocks**. The nearest Cathedral edge averages **53,918 blocks**; a nearest province center averages **54,951 blocks**.

**These are horizontal straight-line distances from (0,0), not measured distances from each world's actual spawn or navigable ocean routes.** Spawn placement can shift the starting point, and travel without known coordinates can be substantially longer. Six deliberately selected seeds do not establish a universal expected distance.

| Seed | Nearest province edge | Nearest Cathedral edge |
| --- | ---: | ---: |
| 0 | 65.5k | 68.1k |
| 1 | 55.3k | 57.6k |
| -1 | 5.4k | 7.7k |
| 7142026 | 94.7k | 97.1k |
| -9223372036854775808 | 27.8k | 30.4k |
| 9223372036854775807 | 60.3k | 62.7k |

All 105 accepted provinces from the [native coverage survey](Province-Coverage-2026-10-09.md) are evaluated with the unchanged production `ReefProvinceLayout`, including seeded center jitter and angular boundary warping. Sample 7,200 angular directions per candidate and minimize Euclidean distance to the warped outer and core boundaries separately. This resolves the geometric boundary numerically; report rounded distances rather than exact block coordinates.

The survey covers [-184320,196608) on both axes. Every measured nearest edge is closer than 184320 blocks, and complete footprints remain within their cells, so unexamined cells beyond the survey cannot contain a nearer province to origin. Actual spawn locations were not loaded for these six seeds. No runtime generation rules changed.

[Raw distance summary](2026-10-09-province-travel.json). Reproduce by extracting accepted seed/cell pairs from the coverage JSON, compiling `tools/province/ProvinceTravelMeasure.java` alongside `ReefProvinceLayout.java`, and passing the CSV input. The tool uses the production Java geometry directly, without a separately reimplemented hash.
