# Veilglow and Shardback AI

The first versions had only a random swim/stroll goal. The revised creatures
use a single movement-owning goal per species, with server-owned behavior
states synchronized to clients for visible animation cues.

## Veilglow

- **DRIFT:** slow water routes with a resting/coasting period in each cycle.
- **PULSE:** a 35-tick contraction/swimming phase in each staggered 140-tick
  cycle, with an upward-biased destination and a stronger bell contraction.
- **BLOOM:** loose groups around a nearby lower-ID leader, with a 3.5-block
  offset and vertical staggering. Leaders never reciprocally follow followers.
- **FLEE:** damage, contact stinging, or a visible Crusher within nine blocks
  interrupts drift. A short escape pulse routes away from the danger; no
  attack target or pursuit goal is assigned. Danger memory lasts 140 ticks
  after the last nearby predator observation or damage/contact reaction.
- **RECOVER:** a stranded jelly attempts a small flop toward nearby clear
  water while native suffocation continues. Underwater healing starts only
  after 260 quiet ticks, at one health per 100 additional quiet ticks.

The contact sting remains one heart with a 40-tick cooldown and native player
invulnerability. Creative/spectator players are excluded. After stinging,
Veilglow escapes rather than following the victim. Swimming input is scaled
once, with enough acceleration to survive the native tiny-motion cutoff;
the original controller/travel combination barely moved during autonomous drift. The synchronized pulse
and flee states tighten the bell and draw the ribbons inward.

## Shardback

- **WANDER:** routes between submerged, supported patches using native
  amphibious navigation. Whole paths must remain grounded and underwater.
- **FORAGE:** an 80-tick feeding pause on sediment after walking, with
  alternating digging/claw motion. It does not break blocks or create items.
- **THREATEN:** a visible submerged survival player within three blocks makes
  the crab stop, look toward them, raise both claws, and open its pincers.
  Sixty ticks of continued crowding causes retreat.
- **FLEE:** damage, a nearby visible Crusher, or a defensive pinch interrupts
  browsing. Routes increase distance from the threat and prefer nearby cover.
- **SHELTER:** on reaching cover at least four blocks from danger, it tucks
  its legs and waits. A threat moving too close causes another retreat.
- **RECOVER:** amphibious navigation seeks nearby supported water when on
  land. Quiet submerged feeding can restore one health after 300 safe ticks
  and 100 feeding ticks; danger, crowding, and stranding reset recovery.

A pinch applies only while warning, in actual bounding-box contact, to a
survival player without native damage immunity. It deals one heart, has a
60-tick cooldown, and immediately switches to escape. It never sets an
attack target or chases players. Creative/spectator presence does not warn
or pinch. Natural spawning stays restricted to the biome's sediment seabed.

## Navigation and cost

`ReefNavigation` rejects unloaded chunks, air, terrain intersections,
unreachable destinations, and paths longer than 32 nodes. Grounded routes
also require support beneath every path node. Each route search uses at most
8–12 local candidates and waits 30–40 ticks before retrying. Nearby-entity
scans occur every 20 ticks. Cached entities are checked for liveness, mode,
distance, and dimension before being reused. No chunk is force-loaded.

## Verification

```powershell
python tools/prepare_reef_life_verification.py
# In the printed snapshot directory, using Java 17:
./gradlew.bat -PreefLifeTests runGameTestServer --offline
./gradlew.bat -PcoralCrusherModelTests verifyVeilglowModel verifyShardbackModel --offline
./gradlew.bat runData build --offline
```

The suite includes the five original habitat/egg/navigation/sting tests plus
eight native AI scenarios: pulse/group drifting, predator escape, interrupted
and safe healing, a solid escape barrier, sediment feeding, cover seeking,
warning/contact pinch/retreat, and creative/spectator exclusions.
Native model checks export the behavior poses and verify moving vertices and
resetting the shared model after every pose. All fixtures remain opt-in and
are excluded from ordinary jars.

## Validated 2026-10-04

- Fresh native GameTest world: all 13 required tests passed. A reused-world
  run had a spawn-obstruction failure; the fresh world passed. The movement
  failures discovered earlier were fixed in swimming control/travel,
  without weakening the movement assertions.
- Both native model checks passed, including animated behavior vertices and
  shared-pose resets. Inspected `art/reef-life-ai/behavior-poses.png`, rendered
  from the actual baked model vertices. These are model renders, not new
  shader framebuffer captures.
- Normal `runData build` passed. Packaged main sources match the working
  tree, generated data matches semantically, and all four mobs, biome spawns,
  and illustrated eggs are present. Test/preview fixtures are excluded.
- Preview jar: `build/libs/tidalterror-1.0.0-reef-life-ai-preview.jar`
- SHA-256: `772dfc16a22bdd1b03dc6e4e93cb2d960913d2106a1e544de5e4a0129914bb4e`

## CI regression corrections

Native ambient fixtures use a dedicated 48 x 18 x 48 template: their 24-block pools and 15-block-high water setup exceeded the older 20 x 12 x 20 template. The native GameTest batch runner spaces fixtures from template bounds, so the larger bounds keep setup and roaming creatures isolated. The cover fixture allows native ticks to initialize the stationary predator's water flags before starting crab AI, preserving the intended initial encounter geometry. Foraging movement is measured across the observation window, so a crab returning close to its starting point is not misreported as stationary. Escape tests still require movement away from danger, entry into shelter, water and collision safety. Native path traces showed vanilla amphibious evaluation selecting swimming nodes one block above the sediment; the grounded route validator correctly rejected them. Shardback now uses the native amphibious evaluator with supported-foot neighbors and zero water-border penalty. It still uses native collision/pathfinding, can traverse supported land while recovering, and retains all route water/ground checks and the 32-node bound. Exact route targets remain unchanged.

A new native unsupported-water-gap regression requires the crab to reject the route and remain on the supported side, submerged and collision-free. The combined ambient suite now contains 14 required tests.
