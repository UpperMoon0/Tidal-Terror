# Tidal Terror

A Minecraft 1.20.1 Forge mod featuring the Coral Crusher and the Coral Cathedral, a deep ocean biome with giant coral formations, sandstone seabeds, and lush coral gardens.

Coral Crushers share one entity and animated model, with sandy skins near the seabed and blue skins higher in the water. They spawn naturally in the Coral Cathedral. The mod creative tab includes a custom Coral Crusher spawn egg.

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
./gradlew.bat -PreefTests runServer
```

Development shaders are optional: run `python tools/install_dev_shaders.py` to install the pinned Oculus, Embeddium, and Complementary development dependencies. Downloaded dependencies are excluded from Git and the release jar.

After the native reef audit, `python tools/launch_reef_explorer.py` opens an independent copy of the audited reef with shaders enabled. Preview capture and packaging tools are in `tools`; generated galleries remain local under `art`.

See `docs/Coral-Cathedral.txt` and the Coral Crusher notes in `docs` for source references, behavior, and validation details.
