# Current deep province versus vanilla chunk benchmark

The outer basin transition has a reproduced first-generation regression: **66.82 seconds versus 4.40 seconds in stock vanilla**, approximately **15.2 times slower**, for each workload of 16 explicitly requested chunks. Loading the same saved transition chunks in a new JVM takes **0.77 seconds versus 0.74 seconds**. The previous frame benchmark did not measure this workload and preceded full feature recovery.

Measured runtime: Tidal Terror `f52267dfbb305aea48ccd69a918a4174352d00f6`, Endless `b82b3428f2e7d9cb86af6ca0307b92cabb7e6b2e`, Forge 47.2.0 / Minecraft 1.20.1. This commit adds measurement tools and evidence; it does not implement a new runtime optimization. Both related PRs remain draft and unmerged.

## Results

All values below are seconds, averaging two independent runs. Each cell covers four serial 2 x 2 chunk requests, followed by an entity-ready check for all four chunks. Vanilla's required generation halo also runs; **16 is the requested footprint, not the total number of chunks generated**.

| Fresh generation workload | Stock vanilla | Mods, Default terrain | Mods, deep province | Deep / vanilla |
| --- | ---: | ---: | ---: | ---: |
| Outside province | 4.477 | 6.641 | 5.111 | 1.14x |
| Cathedral core | 5.059 | 4.785 | 8.666 | 1.71x |
| Inner Wastes | 3.534 | 4.043 | 6.714 | 1.90x |
| Outer blending slope | 4.398 | 4.397 | **66.824** | **15.19x** |

| Saved-chunk restart workload | Stock vanilla | Mods, Default terrain | Mods, deep province |
| --- | ---: | ---: | ---: |
| Outside province | 0.885 | 0.918 | 1.259 |
| Cathedral core | 0.608 | 0.863 | 0.607 |
| Inner Wastes | 0.606 | 0.621 | 0.626 |
| Outer blending slope | 0.737 | 0.542 | 0.767 |

The deep transition's fresh runs independently took **67.395 / 66.253 seconds**. Their restart runs took **0.768 / 0.767 seconds**. Vanilla's fresh pair took **4.164 / 4.632 seconds**. This large transition penalty reproduced in both runs. Smaller differences, particularly the outside first approach, vary enough that these two runs do not establish a general engine penalty.

## Source and profile findings

The transition is the main optimization target in **Tidal Terror**:

- `ReefTerrain.calculateFloor` requests `originalFloor(x,z,true)` throughout the outer blending band. The `true` argument bypasses the already captured original chunk heightmap. The bounded cache saves repeated requests, but the first visit still calculates each of the chunk's 256 columns separately.
- Each cache miss calls native `NoiseBasedChunkGenerator.getBaseHeight`. Exact cached 1.20.1 sources show `iterateNoiseColumn` constructing a fresh `NoiseChunk`, preparing density functions and interpolating a column. Doing this per block column repeats expensive work that normal chunk terrain generation shares across columns.
- Both transition profiles show substantially more worker noise execution than the controls: approximately 5,000 execution samples per transition versus about 500 for vanilla/Default. Noise synthesis, density-function construction, hashing and cache lookup are prominent. Stack examples link native height calculation to Tidal Terror's `originalFloor` caller. These are statistical CPU samples, not elapsed-time percentages; default JFR stack-depth truncation hides the caller in many deep noise stacks.
- The transition's recorded GC pauses total **0.480 / 0.482 seconds**, with the longest pauses **41 / 40 milliseconds**. They do not explain a roughly 67-second wait. Saved chunks avoid the generation path and load quickly in both runs.

These observations strongly support repeated native height reconstruction as the leading cause. They do not quantify every millisecond of the delay or prove that Endless has no other bottlenecks. The modded Default control combines Forge, Endless and Tidal Terror; it is not a Forge-only control. No logical-height sweep was performed.

The next change should share the native height/noise work across a chunk while preserving the exact seabed and restart determinism. Simply substituting a post-carver heightmap can change the terrain, so that shortcut needs geometry verification. Cathedral decoration and populated deep sections still have real generation costs; increasing empty logical space and actually filling deeper terrain are different workloads.

## Method and limits

- Twelve dedicated-server JVMs, serially: vanilla, modded Default, deep, deep, modded Default, vanilla; each case first generates a fresh independent save and then restarts that save in another JVM. No concurrent game client or build.
- Same Adoptium **17.0.20.1+1**, 2 GiB initial/maximum heap, `ActiveProcessorCount=4` (three native workers), seed 0, structures enabled, view distance 4 and simulation distance 5. Both modded cases use logical range [-1024,1024); native dense terrain remains [-64,320). Vanilla retains its native 384-block range.
- Same full recovered current prototype classes and required Endless dependency. Optional JEI, Embeddium, Oculus and TerraBlender are absent. Mob spawning and daylight progression are disabled after startup to isolate terrain work.
- An ordinary-terrain warmup precedes every measurement. Deep-specific first-use costs remain included. Coordinates before chunk alignment: outside `(0,2048)`, core `(-55689,43535)`, Wastes `(-53755,43535)`, transition `(-52689,43535)`.
- The command acknowledgement records native FULL completion separately. The reported time continues until `/execute if loaded` passes for all four chunks, which checks ENTITY_TICKING and loaded entities in exact 1.20.1 source. RCON uses TCP_NODELAY; entity-ready polling may add a server tick. Startup, warmup and final flush/shutdown are excluded.
- JFR profile recording is enabled identically in all cases. Profiles are filtered to the recorded command-to-completion intervals, excluding startup and shutdown. Stock vanilla frames retain Mojang's obfuscated names.
- "Restart" means a fresh JVM reading persisted chunks. Windows filesystem caches were not flushed; this is not a cold physical-disk test.
- This measures server generation/loading, **not client meshing, packet transfer or frame stalls**. It covers one known province/seed on one machine. The user's latest tested save has a different seed; it was inspected read-only and was not used or modified by this benchmark. All benchmark saves are separate children of the output directory.

## Evidence and reproduction

[Raw timings](2026-10-09-chunk-loading.json), [interval-filtered profile summaries](2026-10-09-chunk-profiles.json) and [local recording/log hash receipts](2026-10-09-chunk-receipts.json) are committed. Large JFR recordings and disposable worlds remain under `build/chunk-bench-2026-10-09-matched/`; they are not published or bundled in the mod.

Run from the repository root with the measured source revisions and pinned Endless dependency installed. Do not clean the root build directory: it also contains preserved audit and exploration worlds. The benchmark deliberately refuses to reuse its output directory.

1. Install Python `psutil` for the launch capture helper. Prepare a separate probe save once:

   ```powershell
   python -c "import sys; from pathlib import Path; sys.path.insert(0,'tools/province'); import benchmark_chunks as b; b.prepare(Path('build/chunk-bench-probe'),'deep',25680,25681)"
   .\gradlew.bat -PprovincePrototype --init-script tools/province/chunk_benchmark.init.gradle runServer --console=plain
   ```

2. In another terminal, find the Java process listening on **the probe's exact local port**, then capture its real Forge launch. This avoids exporting the incomplete lazy ForgeGradle task configuration. Only the required `MOD_CLASSES` environment entry is exported.

   ```powershell
   $benchServerPid = (Get-NetTCPConnection -LocalPort 25680 -State Listen).OwningProcess
   python tools/province/capture_chunk_benchmark_launch.py --pid $benchServerPid --output build/chunk-bench-launch.json
   ```

3. Stop the probe normally using `stop` in its server console. Close game clients, then run the serial benchmark. Keep the classes/resources/dependency jars unchanged until it completes. Override `--java` and `--vanilla` when your matching Java 17 installation or official stock 1.20.1 server jar is elsewhere.

   ```powershell
   python -u tools/province/benchmark_chunks.py --output build/chunk-bench-new-comparison
   python tools/province/profile_chunk_benchmark.py build/chunk-bench-new-comparison
   ```

The profiling helper accepts `--jfr` for the matching JDK tool. Raw JSON retains every batch, completion response, timestamp, FULL acknowledgement, stronger entity-ready duration, startup time and artifact/source receipt. Reproduction on other seeds and actual player travel is needed before claiming universal performance.
