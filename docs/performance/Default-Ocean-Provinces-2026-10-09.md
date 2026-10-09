# Default ocean provinces — native validation, 2026-10-09

Version 0.0.4 makes the province source the default `minecraft:normal` Overworld on Forge 1.20.1, Fabric 1.20.1/1.21.1 and NeoForge 1.21.1/26.1.2. TerraBlender has no build, runtime, entrypoint, mixin or publication dependency. No legacy TerraBlender save migration is provided.

The shared placement engine owns the bounded climate admission cache and delegates background biome selection through the original source. Shared seed binding, province geometry, terrain, gardens and Wastes landmarks support both ordinary and deep profiles. Thin source adapters handle Minecraft's Codec/MapCodec API difference. Version-specific Wastes data handles the carver format change in 26.1.2. Cathedral radius stays 1,024 model blocks; all bands and versioned placement remain unchanged.

| Check | Result |
|---|---|
| Forge normal, no Endless or TerraBlender | Passed fresh world, codec, biome feature graph, frozen Cathedral inventory, spawn tables, compass and bedrock |
| Forge normal with Endless | Passed identical shallow profile and target |
| Normal world copied from Endless run, addon removed | Passed cold load and same target |
| Deep Forge fresh generation and cold reload | Passed native-height equivalence (768 columns), sparse joins/bedrock, gardens, active coral survival, habitat/spawns, compass and lighting |
| Detailed normal-height terrain audit | Passed 16,236 live coral blocks; tallest column 103; 5,594 garden blocks; all 12,544 sampled deep columns retained sediment; no air, magma or bubbles |
| Outer terrain seam | Eight generated boundary checks; maximum target-vs-native height error 0 |
| Native structures and saved pieces | Ten cases: eight rotated wrecks and two portals, including reload |
| Fabric 1.20.1 / 1.21.1 | 23 + 23 native tests passed |
| NeoForge 1.21.1 / 26.1.2 | 23 + 22 native tests passed |
| Production archives | All five built and passed dependency/resource/fixture guards |
| Tooling | 26 tests passed, including rejection of legacy dependency and vanilla-only default |

At seed 0 the locator returned X -4,317, Z 37,119 in all three normal dependency cases, with 2,209 loaded chunks before and after lookup. Detailed normal terrain used seed 7,142,026 at X 20,667, Z -4,103. Native spawning stress checks produced both crusher skins plus dolphin, turtle and tropical fish below Y -15; those stress counts do not establish natural population rates. Deep custom fauna restrictions and vanilla marine life above/below the canopy retained their existing native regression coverage, with drowned weight 1.

The old terrain audit treated the Cathedral edge as a vanilla boundary. It now checks the full province's outer edge, across the Wastes. Admission still permits coastal land within the complete footprint; passing boundary checks does not mean coast preservation inside a province. Installing Endless does not switch normal terrain to the deep profile. Its supported deep world type remains Forge 1.20.1 only, with the configured minimum at or below -512.

Port native tests exercise default-preset registration, full biome feature graph, source codec, seed-stable ocean admission and land-only rejection alongside the existing gameplay suite. Detailed fresh noise-world terrain, active-tick and cold-reload checks were performed on Forge; they are not claims of a full terrain survey on every port. No new performance comparison is claimed by this dependency/default-world change.

Archive hashes and local log paths are recorded in [the native receipt](2026-10-09-default-province-native.json). The PR remains draft and unmerged.
