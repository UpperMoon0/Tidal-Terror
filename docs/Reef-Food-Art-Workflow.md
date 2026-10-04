# Reef mob food

Four registered mobs each have one raw and one cooked food: Coral Crusher steak,
Cathedral Ray wing, Veilglow gel, and Shardback claw. Shark skins share the steak.
All eight items appear in the Tidal Terror creative tab.

## Artwork

Generated with the built-in ImageGen tool, one call per asset. Each raw generation
received its actual textured native model preview: shark and ray multi-view sheets,
Veilglow and Shardback shader runtime side views. No untextured model or concept
sheet substituted for those inputs. Exact prompts and reference paths are retained
in `art-source/food/generation-manifest.json`; original eight transparent generated
images and the paired preview are retained beside it.

Each cooked call received its own raw source image as the edit target, with an
explicit instruction to preserve silhouette, orientation, cutouts, canvas and pose,
changing only interior colors to cooked flesh/shell. Export uses nearest-neighbor
downscaling to 64x64 RGBA. The cooked alpha is matched to the raw alpha after
downscaling, eliminating tiny generation edge changes. Only two shark edge pixels
needed filling from the closest generated cooked color. Other pairs needed none.
No background is added. The preview's dark background is presentation only.
`tools/export_food_art.py` reproduces export and validates exact paired alpha;
`art-source/food/export-checks.json` records the result.

## Gameplay

| Drop | Ordinary amount | Raw hunger | Cooked hunger |
| --- | --- | --- | --- |
| Coral Crusher steak | 2-4 | 3 | 8 |
| Cathedral Ray wing | 1-3 | 2 | 6 |
| Veilglow gel | 1-2 | 1 | 4 |
| Shardback claw | 1-2 | 2 | 5 |

Native entity loot tables apply Looting bonuses and furnace-smelt the drop when
the mob dies burning. Native cooking serializers handle furnaces (200 ticks),
smokers (100 ticks), and campfires (600 ticks), with 0.35 experience. Acquiring raw
food unlocks the corresponding recipes. No extra status effects are applied.

Source references: local decompiled Minecraft 1.20.1 `VanillaEntityLoot`,
`SmeltItemFunction`, `AbstractCookingRecipe`, and `Foods`. The smelt loot function
looks up an actual furnace recipe and preserves drop quantity.

## Verification

Prepare the isolated snapshot using `tools/prepare_food_verification.py`, then
run `gradlew.bat -PfoodTests runGameTestServer --offline` in that snapshot. The
three native tests exercise ordinary and burning deaths of all four mobs, all
12 cooking recipe lookups, hunger restoration and consumption of all eight
items, and actual ticking furnaces cooking each raw drop.

Build the snapshot without `-PfoodTests` for release so test classes and templates
are excluded. Test output is retained in `build/food-tests.txt`.

Verified: all three required native GameTests passed. The export audit confirmed
eight transparent 64x64 PNGs and identical raw/cooked alpha for every pair.
