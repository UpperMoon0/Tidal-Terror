# Multi-version ports

Version 0.0.3 retains Forge 1.20.1 and adds Fabric 1.20.1, Fabric/NeoForge 1.21.1 and NeoForge 26.1.2. The layout follows Endless: Architectury common/versioned sources, Fabric Loom targets, and native NeoForge ModDev targets. Each target has separate output and development worlds.

## Source ownership

- `src/main` and `src/generated` remain the original Forge entry point, Forge adapters and authoritative artwork/1.20.1 data.
- `common` owns authoritative Java-only equipment tuning: spear durability/damage/speed, bleed interval/duration/damage and the armor bleed multiplier. Every runtime and spear tooltip calls these values.
- `common-1201` owns the identical Forge/Fabric 1.20.1 models, renderers, entities, armor material, arrow item, upgrade recipe and turtle placement mixin. Model generators write into this shared root.
- `common-universal` owns reef geometry, navigation helpers, shared shark/crab/jelly goals and structure/water postprocessing hooks that compile across all five runtimes. The complete shark behavior algorithm lives here; `CoralCrusherRuntime` adapters contain only damage-call, collision-box, water-check, drowned-name and minimum-height API differences.
- `common-versioned` owns the remaining identical Minecraft 1.20.1/1.21.1 renderer and feature sources, including the ray.
- `common-ports` owns identical Architectury particle registration, spawn categories, attack-charge access and chunk-finish hooks across the four ports.
- `common-1201-1211` owns identical 1.20.1/1.21.1 port adapters and registration. `common-modern` owns 1.21.1/26.1.2 spear enchantment support, turtle placement and supported-seabed navigation, plus 44 identical modern JSON resources compiled into both versions. Different recipe/component/item/equipment formats remain in their native version folders. Archive verification checks that every shared resource is packaged byte-for-byte.
- `common-1.20.1` and `common-1.21.1` own version-specific entities, equipment, codecs and render APIs. The two 1.21.1 loaders compile the same gameplay sources.
- `fabric-*` and `neoforge-*` own loader initialization, events, metadata and launch configuration. The substantial 26.1.2 API changes keep its remaining entity serialization, data components, render states and special-item renderer in the 26.1.2 target.

Game-ready mob/item textures are copied from the original assets. Modern recipe/loot/registry folders and 26.1.2 equipment/item definitions are committed for their native formats. The original Forge data remains unchanged. Port MobCategory codec identifiers are namespaced (`tidalterror:crusher`, etc.); each platform's registered category matches its packaged biome data.

Gameplay balance is unchanged: iron protection/durability, 20% full-set grounded underwater knockback reduction, 25% full-set bleeding reduction, and the existing spear/arrow bleeding rules. Native modern enchantment definitions exclude Sharpness/Smite/Bane from bleeding enchantments. Spears support native melee enchantments but exclude Fire Aspect and Sweeping Edge; bows/crossbows never accept bleeding enchantments. Upgrade recipes preserve the input armor's saved data/components rather than returning fresh gear.

Bleeding tracks its own two-second countdown and attacker UUID. Entity save/reload uses the version's native persistence format. NeoForge 26.1.2 persists unfinished reef-water work as a serialized chunk attachment; older versions use the native chunk serializer hooks. Work resumes only after the chunk reaches block-ticking readiness.

## Build and run

Run multi-version Gradle with **Java 21**. Toolchains select Java 17 for Minecraft 1.20.1, Java 21 for 1.21.1, and Java 25 for 26.1.2. Toolchain auto-download is enabled through Foojay. Gradle 8.14 is shared with the original Forge build.

```powershell
./gradlew.bat clean build
./gradlew.bat -Pmultiversion clean buildAll
./gradlew.bat -Pmultiversion :fabric-1.20.1:runClient
./gradlew.bat -Pmultiversion :fabric-1.21.1:runServer
./gradlew.bat -Pmultiversion :neoforge-1.21.1:runClient
./gradlew.bat -Pmultiversion :neoforge-26.1.2:runClient
```

Qualified target tasks configure only the required common/version modules. `buildAll` configures every port. Keep each loader in its own instance and use the corresponding TerraBlender/Architectury/Fabric API builds. Development dependencies, caches, worlds, evidence and downloaded libraries are ignored by Git.

## Verification

```powershell
python -m unittest discover -s tools -p "test_*.py" -v
python tools/ports.py test fabric-1.20.1
python tools/ports.py test fabric-1.21.1
python tools/ports.py test neoforge-1.21.1
python tools/ports.py test neoforge-26.1.2
python tools/ports.py client neoforge-26.1.2
```

Tests are opt-in through `-PportTests`. Fabric 1.20.1 and both 1.21.1 targets run 19 native GameTests: ten crab/jelly AI fixtures, two category/cap fixtures, five equipment fixtures, and two chunk-generation/revisit regressions. NeoForge 26.1.2 runs 17 native tests: ten crab/jelly AI fixtures, five equipment fixtures, and the same two chunk regressions. Equipment checks exercise independent bleed timing/save/reload, arrow bleeding on land, armor resistance, repairs and enchantment restrictions. The modern equipment fixture additionally decodes the real armor-upgrade JSON and verifies names, wear, repair costs and enchantments survive crafting. The original Forge suite retains its broader 18 equipment regressions, including lethal bleed Looting/XP credit.

Water repair is queued only when an unfinished `ProtoChunk` becomes a full chunk, or when loading a saved unfinished-repair marker. Full chunks loaded from disk and repeated native `postProcessGeneration` calls never start a new repair. Native regression fixtures round-trip full chunks through Minecraft chunk serialization, preserve air/magma/soul-sand/bubble-column constructions on revisit, and verify pending generation repairs survive reload.

Port test launchers must report every required native test passed. Fabric and 26.1.2 also require an XML report with the expected number of unique successful cases. NeoForge 1.21.1 uses its native completion log because its server accepts no `--report` option. A zero process exit after a startup exception is rejected.

`-PclientSmoke` checks completed native resource loading, particle/renderer registration and baking of all nine mob/equipment layers, then exits its own client. Run `python tools/ports.py client TARGET`; CI supplies Xvfb/Mesa on Linux. This is a loading check, not an assertion about final in-world screenshots or shader compatibility.

After tests or client checks, **clean the target before a production build**:

```powershell
./gradlew.bat -Pmultiversion :neoforge-26.1.2:clean :neoforge-26.1.2:build
python tools/ports.py jar neoforge-26.1.2 --jar neoforge-26.1.2/build/libs/tidalterror-neoforge-26.1.2-0.0.3.jar --version 0.0.3
```

The archive gate verifies loader/Minecraft/Java metadata, required companion mods, mixin classes/refmaps, category names, gear recipes, loot, artwork and modern enchantment data. Test/preview classes and obsolete spear icons must be absent. Evidence is preserved under `build/port-evidence` before clean builds.

## Release matrix

`python tools/release.py package --ports --output build/release` verifies and packages all five normal jars plus one immutable source/version manifest and checksum list. Missing targets, wrong loader/Java metadata, fixture leaks or altered artifacts stop packaging.

The version-driven release workflow retains draft/tag reservation and resumable CurseForge receipts. It reserves the complete immutable bundle, publishes the original Forge jar, then uploads each port with its exact loader, Minecraft version, Java version and required dependencies. The GitHub release remains a draft until all five upload receipts match their artifacts and source commit. PR CI never publishes. Merge/publication is a separate action.
