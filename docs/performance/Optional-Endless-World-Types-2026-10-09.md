# Optional Endless world types — 2026-10-09

Forge 1.20.1 uses one production jar. TerraBlender remains required for the previous default Cathedral distribution. Endless 0.9.3 is optional and adds the separate Deep Reef Province world type. Installing it does not replace a normal world's generator.

The biome resource, configured feature graph, spawn tables, terrain queries, coral and garden decorators are shared. Only the sparse terrain adapter depends on Endless. The default path retains its previous habitat rules; the deep path retains canopy restrictions for the four custom species and vanilla aquatic spawning above and below it. Both use the same Reef Compass recipe, server worker and persisted target, with world-specific locator strategies.

## Native results

| Case | Result |
| --- | --- |
| Normal world without Endless | Passed; legacy generation, bedrock, feature/carver/spawn inventory, compass and recipe |
| Normal world with Endless, logical range −1024 to 1024 | Passed; normal generator and 24-section dense core retained |
| Copied normal world reloaded after removing Endless | Passed; saved normal generator and Cathedral location retained |
| Fresh deep world with both companion mods | Passed; deep bedrock/water, coral/gardens, spawning, lighting and worker admission |
| Separate-process deep reload | Passed; player edit, living coral and saved pages retained |

All three normal cases located the same seed-0 Cathedral at X 2304, Z 6016. Locator calls left the loaded chunk count at 2209. Deep lookup retained the balanced center at X −4317, Z 37119; it also preserves the version-1 codec default and old center eligibility.

The deep 80×80 garden witness retained 2456 colony blocks, 485 boulder blocks, 844 fans, 925 plants, 422 seagrass and 220 pickles through ticking and restart. The frozen `133c112` Cathedral feature, carver and spawn inventory passed in both generation paths. Exact native height batching passed for 768 columns. Missing-Endless deep decoding fails with an explicit dependency error, while shallow saved sources still decode.

The production build and 24 tooling tests passed. Jar checks confirm optional Endless metadata, the conditional world-type pack, all 32 compass frames, and exclusion of fixtures and the Endless API itself. [Raw receipts and jar checksum](2026-10-09-optional-endless-native.json).

## Reproduce

Prepare the pinned Endless compile dependency described in [setup](../Sunken-Wastes-Province.md), then run:

```powershell
.\gradlew.bat -PoptionalEndlessTests runServer --offline
.\gradlew.bat -PoptionalEndlessTests -PwithEndless runServer --offline
.\gradlew.bat -PprovincePrototype -PdeepProvinceTests runServer --offline
.\gradlew.bat -PprovincePrototype -PdeepProvinceTests -PdeepReload runServer --offline
.\gradlew.bat build --offline
python -m unittest discover -s tools -p 'test_*.py' -v
```

The optional-dependency CI matrix repeats normal-world loads and reloads a copy of the Endless-enabled normal save without Endless. Deep checks run in a separate fresh world and then a second JVM. Local deep checks used `-PdeepProvinceTestDirectory=build/deep-province-optional-endless-2026-10-09` to preserve earlier worlds. The former `provincePrototype` flag is now only a development runtime alias for `withEndless`; it does not select a different production jar or globally disable legacy generation.
