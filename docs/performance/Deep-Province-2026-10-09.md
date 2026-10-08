# Deep Reef Province: measured performance, 2026-10-09

The changes improve frame p95 in both paired runs and remove the observed two-second server heartbeat gaps. They **do not improve every metric**: server tick p95 and the longest individual rendered frame regress. Both PRs remain draft and unmerged; this prototype is not ready for default-world promotion.

## Independent in-game results

Four separate Forge 1.20.1 / Java 17 integrated-client processes ran in ABBA order: baseline E, optimized E, optimized F, baseline F. Each started from an independent, identical copy of the preserved v4 exploration save. Five fixed viewpoints exercised fresh approach terrain, deep water, rim passage, Cathedral and its surface. Every run completed all five actual framebuffer captures and five active-water checks (81 sampled columns each). Optimized runs both recorded **zero server-thread admission builds**.

Times are milliseconds; lower is better. Frame statistics exclude intervals containing synchronous diagnostic/screenshot writing. Tick duration measures START to END; heartbeat measures time between consecutive START events and includes work done between ticks. This distinction catches generation stalls that tick-duration timers miss.

| Metric | Baseline E | Optimized E | Optimized F | Baseline F |
| --- | ---: | ---: | ---: | ---: |
| Measured tour duration | 126,507 | 126,515 | 126,702 | 131,057 |
| Frame p95 | 66.82 | 49.67 | 59.33 | 216.45 |
| Frame p99 | 162.11 | 144.08 | 161.08 | 438.84 |
| Longest frame | 1,029.21 | **2,761.98** | **1,654.48** | 799.56 |
| Tick duration p95 | 60.26 | **75.45** | **93.63** | 68.55 |
| Longest tick | 236.68 | **302.43** | **400.75** | 289.00 |
| Heartbeat p95 | 71.51 | 85.78 | 108.61 | 131.44 |
| Longest heartbeat gap | 2,163.60 | **302.85** | **488.80** | 2,219.18 |
| Inner Wastes teleport response | 55.87 | 14.22 | 19.91 | 129.56 |

Frame p95 falls **25.7% in pair E and 72.6% in pair F**. The mean of the two per-run p95 values falls from 141.63 to 54.50 ms (**61.5%**); this is not a pooled percentile. The worst heartbeat gap across runs falls 78.0%, from 2,219 to 489 ms. Tick p95 instead rises 31.3% on the same aggregation, and the worst frame rises from 1,029 to 2,762 ms. Heartbeat p95 has mixed paired results; its mean falls only 4.2%.

Thread CPU demand, normalized by each measured tour's elapsed time, falls 10.8% on the render thread, 11.9% on native generation workers and 5.6% on the server thread. CPU accounting includes the fixture's capture/check work; it is not an isolated engine microbenchmark. Tick totals can exceed measured thread CPU because elapsed tick time includes blocking/descheduling.

This is a two-pair result on one machine and one seed, with substantial baseline variability. It demonstrates workload-specific gains, not a universal speed multiplier or a guarantee against stalls. It does not isolate the contributions of Endless and Tidal Terror. It also does not measure multiplayer queue fairness, disk-save throughput, renderer replacements, unlimited populated depth, other Minecraft versions or a height-limit sweep. Earlier intermediate runs were rejected after revealing server-side fallback generation and premature neighbor preparation; their timings are not included here.

JFR supports the change in where work runs: baseline E sampled the old per-block `computeBlockLight` heavily on render workers and sampled deep generation 384 times on the server thread. The optimized admission counter is zero and its dominant sampled server work includes native-height queries used by spawn targeting and sparse height/network maintenance. These are statistical samples, not elapsed-time attribution. Maximum recorded GC pauses were 34.87/52.23/36.81/34.83 ms in ABBA order; they do not account for the multi-second longest frames. The remaining worst-frame cause is unresolved and is not attributed to GC or eliminated by these changes.

## Changes and ownership

**Endless** owns shared engine work: bounded section-light caching, copied palette inputs with solves outside sparse storage locks, immediate dense-write invalidation, bounded immutable snapshot reuse, bulk page admission, fair initial page delivery and budgeted periodic persistence. Explicit save/unload/shutdown retain full persistence. Skylight snapshot solves also release the sparse lock before dense halo admission; a native unloaded-halo test covers the resulting deadlock fix. Empty logical height alone does not allocate or scan all intervening sections in these paths. Populated terrain still incurs generation, lighting, storage and rendering costs.

**Tidal Terror** owns exact native-height caching and terrain preparation. A bounded 2,048-chunk cache retains the same 256 native seabed samples per chunk and computes them outside the global LRU monitor. The requested native FULL continuation prepares deep sections on generation workers, including saved protochunks already past decoration. It then admits prepared pages without snapshot serialization or terrain construction on the admission thread. Preparation is requested only for chunks becoming FULL, avoiding extra private deep terrain for decoration neighbors that never admit. The depth, seabed, bedrock and coral formulas are unchanged.

Fresh dedicated-server generation and a separate-JVM cold reload passed, preserving the player's gold-block edit. Regressions cover cross-section lighting, the dense/deep seam, enclosure/removal, immutable light snapshots, unloaded dense skylight halos, existing-page refusal and zero admission builds. Tidal tooling has 24 passing tests. Endless local validation has 212 Java cases (two existing skips) and 82 passing tooling tests; all 70 exact-head CI checks at `af33532` passed, including native renderer and live-world checks.

## Reproduction and evidence

The checked-in [raw metrics and receipts](2026-10-09-deep-province.json) include all four runs, measured thread CPU, teleport timings, identical terrain/biome probes, and hashes of the fixture, dependency, runtime sources and build configuration. Validate/recompute the summary with:

```powershell
python tools/province/report_benchmark.py build/deep-province-workspace/build/perf-results --output build/performance-summary.json
```

The script rejects changed fixtures, terrain probes, framebuffer dimensions, logical ranges, per-variant code/dependency hashes or missing zero-admission results. All 20 captured PNGs are 1920×1080. Local raw JFR recordings, run logs and five PNGs per run remain under `build/deep-province-workspace/build/perf-results/{baseline-e,optimized-e,optimized-f,baseline-f}`; large recordings are not bundled in release jars.

Benchmark runtime sources were Tidal `0f996510aa12b4001fdd7e08ad7f920bfca26854` and Endless `af3353235773b081b4951daa2c26416d76626748`. The frozen benchmark workspace called itself 0.0.3; the later 0.0.4 bump changes release metadata only. Baseline Tidal logic is reproducible by applying [baseline-before-optimization.patch](../../tools/province/baseline-before-optimization.patch) to that Tidal revision in a separate checkout; keep the measurement fixture identical. Baseline Endless is fog-only `62e5de00d2a86e3cc359e5241f942e548ea8fd80`. Both dependency jars call themselves 0.9.3; their recorded SHA-256 values distinguish them.

Build each Endless Forge dependency, install the respective unbundled jar in `.dependencies/endless`, and use independent copies of the same closed, successfully audited exploration save for every run. Set Endless logical height to `[-1024,1024)`, view distance 4, simulation distance 5, vanilla rendering, 60 FPS limit, Creative flight and Night Vision. These runs used fullscreen at an actual 1920×1080 framebuffer; the fixture's requested 1600×900 window arguments did not override saved fullscreen mode. Preserve that actual framebuffer size for reproduction. Start each independent JVM with:

```powershell
.\gradlew.bat -PprovincePrototype -PdeepProvinceExplore -PprovinceProfile "-PperformanceSave=Province Bench Baseline E" runClient --offline
```

Change only the named independent save and the baseline/optimized runtime between the four runs. `provinceProfile` enables JFR and exits after the complete tour; the ordinary exploration mode returns control to the player. Exclude startup/build time from gameplay measurements. Run games serially and avoid concurrent builds or servers on the measuring machine. Do not overwrite the original exploration or audit worlds.

Structure-height integration, cave biome preservation, full clustered garden parity, other deep loader adapters and the remaining timing regressions are still promotion gates.
