# Tidal Terror

A Minecraft 1.20.1 Forge mod featuring Coral Crushers, Cathedral Rays, Veilglows, and Shardbacks in Coral Cathedral, a deep ocean biome with giant coral formations, sandstone seabeds, and lush coral gardens.

Coral Crushers share one entity and animated model, with sandy skins near the seabed and blue skins higher in the water. They spawn naturally in the Coral Cathedral. The mod creative tab includes a custom Coral Crusher spawn egg.

Cathedral Rays glide peacefully through the reef in spawn groups of 2–3. Their stepped indigo wings have violet edges, branching mint markings, a cream underside, and softly emissive eyes and spots. Both mobs have custom illustrated spawn eggs in the Tidal Terror tab.

Veilglows drift in groups of 2–4 through deep reef water. A translucent lavender bell surrounds a glowing mint bloom, with six animated ribbon tentacles. They sting on contact but never pursue players. All three mobs have custom illustrated eggs in the mod tab.

Shardbacks walk peacefully along the seabed in groups of 1–3. Their violet shells carry cobalt coral, with eight jointed ivory legs, black eye stalks, and an oversized claw. All four creatures have custom eggs in the Tidal Terror tab.

## Development

Requires Java 17. The Gradle wrapper downloads the Forge development dependencies on the first build.

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

The release jar is written to `build/libs`. On Linux or macOS, use `./gradlew` instead.

Generate biome and feature data with `./gradlew.bat runData`. Generated registry JSON is tracked; runtime worlds and generator caches are ignored.

## Verification and previews

```powershell
./gradlew.bat -PcoralCrusherTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyCoralCrusherAnimation
./gradlew.bat -PcathedralRayTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyCathedralRayModel
./gradlew.bat -PveilglowTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyVeilglowModel
./gradlew.bat -PshardbackTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyShardbackModel
./gradlew.bat -PreefTests runServer
```

Development shaders are optional: run `python tools/install_dev_shaders.py` to install the pinned Oculus, Embeddium, and Complementary development dependencies. Downloaded dependencies are excluded from Git and the release jar.

After the native reef audit, `python tools/launch_reef_explorer.py` opens an independent copy of the audited reef with shaders enabled. Preview capture and packaging tools are in `tools`; generated galleries remain local under `art`.

See `docs/Coral-Cathedral.txt` and the Coral Crusher notes in `docs` for source references, behavior, and validation details.

The [Coral Crusher art workflow](docs/CoralCrusher-Art-Workflow.md) records the rig repair, animation, native UV export, ImageGen texture reconstruction, sandy variant, spawn egg, and validation process.

The [Cathedral Ray art workflow](docs/Cathedral-Ray-Art-Workflow.md) records its design, reproducible model/texture assembly, spawning, egg, and isolated native verification.

The [Cathedral Ray AI notes](docs/Cathedral-Ray-AI.md) describe peaceful cruising, loose schooling, player curiosity, predator avoidance, and safe recovery.

The [Veilglow art workflow](docs/Veilglow-Art-Workflow.md) records its hollow translucent bell, bloom core, ribbon animation, contact sting, artwork assembly, and native verification.

The [Shardback art workflow](docs/Shardback-Art-Workflow.md) records its shell, coral growths, leg/pincer rig, generated materials, seabed navigation, egg, and native verification.

The [predator AI notes](docs/CoralCrusher-Predator-AI.md) describe the encounter cycle, low-health escape, safe regeneration, and native regression coverage.

The [ambient AI notes](docs/Reef-Ambient-AI.md) describe Veilglow blooms and escape pulses, Shardback feeding and defensive warnings, safe recovery, and native regression tests.

Each reef mob drops its own food: Coral Crusher steak, Cathedral Ray wing, Veilglow gel, or Shardback claw. All have cooked versions obtainable in a furnace, smoker, or campfire and appear in the mod creative tab. The [food art workflow](docs/Reef-Food-Art-Workflow.md) records the textured model references, ImageGen prompts, paired 64x64 transparent exports, drop amounts, and native verification.

The [spawn egg art workflow](docs/Spawn-Egg-Art-Workflow.md) records the shared style reference, textured mob previews, ImageGen prompts, and transparent 64x64 exports for all four eggs.

The [spawn balance notes](docs/Reef-Spawn-Balance.md) describe separate population pools for each mod creature and reduced drowned spawning.
