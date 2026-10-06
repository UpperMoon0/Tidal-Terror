# Cathedral Ray behavior

The ray remains a peaceful water creature. One `CathedralRaySwimGoal` owns
movement and looking; it never acquires an attack target or deals damage.
Native `WaterBoundPathNavigation` through `RayWaterNavigation` through `RayWaterNavigation`, `SwimNodeEvaluator`, and smooth swimming
controls still move the entity. The implementation was checked against the
local decompiled Minecraft 1.20.1 `FollowFlockLeaderGoal`, `AvoidEntityGoal`,
`WaterBoundPathNavigation`, `SwimNodeEvaluator`, and `Path` sources.

## Priorities and movement

- **Flee:** any accepted damage, or a living underwater Coral Crusher within
  18 blocks, interrupts all social behavior. Remembered danger lasts ten
  seconds; a nearby attacker within 24 blocks refreshes it. Escape destinations
  must increase distance from danger and have a reachable native water path.
  Escape movement uses speed 1.3. Environmental damage also triggers retreat.
- **Curiosity:** a living survival/adventure swimmer within ten blocks, visible
  and moving below 0.1 blocks/tick, attracts a short approach at speed 0.65.
  The ray keeps about six blocks of space, observes for at most six seconds,
  then has a thirty-second cooldown. Creative/spectator players are excluded.
- **School:** nearby rays within twenty blocks loosely follow the lowest
  available entity ID, so following cannot form cycles. Followers aim behind
  and beside the leader, with additional separation from neighbors within six
  blocks. This is local loose grouping, not a persistent flock or synchronized
  formation. Fleeing rays are excluded as leaders.
- **Cruise:** solitary/leaving rays choose water routes six or more blocks
  away, with modest vertical variation. Routes are held for up to nine seconds
  rather than being replaced every tick. School and cruise speed is 0.85.

Native swim paths address the lower corner of a mob's footprint. Destinations
compensate for `Path#getEntityPosAtNode`'s width offset. Every path segment is
sampled at quarter-block intervals, checking loaded water throughout the entire
occupied collision box. No extra block of water below or inflated collision
margin is required: a ray resting against coral or the seabed can leave.
Native center-line shortcuts are replaced with whole-wing swept checks, and
waypoints use a 0.4-block horizontal tolerance rather than half the wingspan. This protects the 4.75-block wingspan against narrow coral openings.
Unreachable routes are rejected; the ray is never teleported through obstacles.
If trapped with no safe route, it may remain still until a route becomes free.

## Recovery

Recovery requires actual water, no nearby predator, and expired damage/danger
memory. After ten additional seconds continuously safe, heal one health point
every four seconds. The first point comes approximately fourteen seconds after
safety begins. New damage or danger resets both timers. A fleeing ray resumes
ordinary behavior after ten safe seconds. It cannot heal on land.

Behavior and recovery timers are transient: loading a ray restarts the quiet
delay. Health uses native entity persistence. No attack, spawn, skin, or model
geometry changes are part of this behavior update.

## Verification

`src/rayTest` is an opt-in native GameTest suite. Two original tests cover
habitat/egg/collision and navigation. Ten behavior tests cover autonomous
cruising, actual school movement and spacing, curiosity approach/expiry,
creative exclusion, predator escape, damage interrupting curiosity, slow
recovery and reset, no dry healing, rejection of a three-block coral gap, seabed escape, and wing-safe shortcuts.
Behavior tests use separate batches to prevent nearby fixture mobs from
influencing another scenario. Fake network connections discard packets, but
the curiosity/damage fixtures use real native `ServerPlayer` entities.

Run `python tools/prepare_ray_verification.py`, then run these sequentially
inside its isolated source copy:

```powershell
./gradlew.bat -PcathedralRayTests runGameTestServer --offline
./gradlew.bat -PcoralCrusherModelTests verifyCathedralRayModel build --offline
```

The test task checks native completion and all twelve required success markers.
These controlled tests do not measure schooling density in a large generated
reef or certify the final appearance of moving schools with shaders.

### Completed native verification (2026-10-04)

`build/ray-behavior-tests-verified.txt` records all ten required GameTests
passing on the final controller and fixtures. The predator scenario measured
escape from five blocks to more than ten blocks, then kept the nearby shark
present for thirty seconds to verify that regeneration stayed disabled.
The first run caught insufficient escape movement; correcting the native
wide-mob destination offset resolved it. The final controller source hash was
checked against the isolated verification copy.

`build/ray-behavior-model-release.txt` records the native model check passing
(animated vertices, pose reset, 48 exported frames) and the mod build succeeding.
The reobfuscated jar was inspected for the new controller and behavior enum,
with no GameTest, model-check, or preview classes. Local review artifact:
`build/libs/tidalterror-1.0.0-cathedral-ray-ai.jar`.
