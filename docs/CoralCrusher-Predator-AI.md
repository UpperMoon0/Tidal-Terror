# Coral Crusher predator AI

The current controller replaces the original melee/random-swim combination with one `CoralCrusherHuntGoal` owning both MOVE and LOOK. Target acquisition remains in Minecraft's target selector. Native `WaterBoundPathNavigation`, Dolphin-style swimming controls, and navigation-driven water travel are retained.

## Encounter cycle

| State | Behavior and transition |
| --- | --- |
| Patrol | Swim between water waypoints around a remembered home territory. Keep routes long enough to finish turning and moving. Sandy sharks prefer waypoints roughly eight blocks above the actual sand layer, with smaller vertical variation; blue sharks patrol around their original higher spawn altitude. |
| Investigate | Approach at speed 0.95, or 1.1 when retaliating. Normal encounters investigate for 30 ticks before circling; retaliation selects an attack after ten ticks once within 12 blocks. Long pursuits have a 900-tick limit. |
| Circle | Choose moving orbit waypoints about six blocks from the target. An injured or actively moving target prompts a windup after 60 ticks; a still, healthy target takes 90. Require proximity and line of sight. Disengage if the approach remains blocked for 200 ticks. |
| Windup | Stop navigation for 20 ticks and slow residual movement. Synchronized behavior state drives stronger tail movement, a slightly open jaw, and a small head pose cue. |
| Charge | Commit to the target's position at the end of the windup at speed 1.5, with an endpoint six blocks beyond it when reachable. Reject blocked or partial water paths. Do not continuously track a dodging target. Allow 60–160 ticks, calculated as distance × 6 plus 20 turning ticks and clamped to those limits, rather than the previous fixed 40-tick cutoff. Make one native bite attempt within vanilla melee reach and line of sight. |
| Melee windup | When already within native reach, with no more than 1.5 blocks of vertical separation and line of sight, telegraph for 20 ticks before one close bite. If the target moves out of range, reposition instead of striking through empty space. |
| Recover | Swim through the full 60-tick damage cooldown at speed 0.85. Continue the charge's committed endpoint when its route remains; a close bite starts a pass beyond the target. When a route ends, choose another reachable waypoint along the pass heading, with alternative angles and lateral turns around the floor/surface. Choose a close bite if appropriate after recovery, otherwise reposition for another charge. There are no bite checks during recovery. |
| Reposition | Find a reachable water route roughly eight blocks from the target, including alternative angles around obstacles. After at least 30 ticks, wind up a new charge when spaced and vertically aligned. Reassess after 140 ticks if the preferred route stays blocked. Close bites alternate with repositioning rather than becoming continuous contact damage. |
| Flee | Interrupt hunting at low health, clear the attack target, suppress acquisition, and navigate to reachable water positions farther from the threat. A directly overhead threat also permits lateral escape rather than driving the shark into the floor. |

Player targets must be alive, in water, and neither creative nor spectator. Native target-selector priority is player, drowned, then fish. Passive players are detected within 32 blocks; drowned within 48 blocks in all three axes. Fish remain occasional nearby prey. The follow-range attribute is 64 blocks for long paths/pursuit, and the home limit is 96 blocks. Disengagement imposes a 200-tick acquisition cooldown and returns it toward home in reachable steps.

Accepted damage above the retreat threshold immediately targets a valid underwater attacker and clears disengagement cooldown. Retaliation remembers the attacker for 600 ticks and can pursue up to 64 blocks, including brief loss of sight. Damage from an NPC cannot replace a living player target. Switching targets cancels the old attack route and investigates the new target. Retreat still overrides retaliation; attacking a fleeing shark does not force it back into combat.

The path endpoint compensates for the native `SwimNodeEvaluator` footprint and `Path#getEntityPosAtNode` width offset. This avoids steering beside the requested target. `SmoothSwimmingMoveControl` changes pitch by only five degrees per tick; a steep charge needs time to turn before covering its vertical distance.

Charges use native `swing(MAIN_HAND)` and `doHurtTarget`, preserving synchronized bite animation, damage, armor handling, and knockback. Repeated contact does not cause repeated damage during a pass. Existing max health 120, attack damage 10, and knockback 2 remain.

## Retreat and regeneration

- Retreat starts at **30% health or below**: 36 HP with the current 120 HP maximum.
- Retreat remains active until **60% health or above**: 72 HP. The shark must also satisfy the quiet/safety delay before resuming patrol. Crossing 36 HP alone does not re-enable hunting.
- Safety requires being in water, no valid combat target, no living survival/adventure player within 24 blocks, and no remembered living attacker within that distance. Creative/spectator players are excluded. Safety does not assume a wall makes a nearby threat harmless.
- A continuous **200-tick quiet period** is required before the regeneration timer advances. Regeneration then restores **1 HP every 80 ticks**. The first point arrives after roughly 14 seconds of uninterrupted safety; subsequent points arrive every four seconds at 20 TPS.
- New accepted damage, nearby threats, an active combat target, or leaving water resets the quiet and regeneration timers. These timers measure simulation ticks, so wall-clock healing slows with low TPS.
- Safe injured sharks can regenerate while patrolling too; retreating sharks remain nonaggressive until the higher health threshold is reached.
- NBT preserves the home territory and retreat latch. Reloads restart the quiet/healing delay instead of bypassing it. Transient attack routes and phase timers restart rather than resuming an old charge.

Both skins share this controller and entity type. Skin assignment remains fixed after spawning; swimming deeper or shallower does not recolor a shark.

## Source references and checks

The local Minecraft 1.20.1 decompiled sources inspected for this rewrite include `MeleeAttackGoal` (reach, native swing/damage), `AvoidEntityGoal` (escape destinations farther from the threat and native paths), `NearestAttackableTargetGoal` (target predicates and selector separation), and the swimming-control/navigation references listed in `CoralCrusher-AI.md`. Mapped project APIs were validated through compilation and native GameTests.

The test fixtures exercise actual server entities, water navigation, synchronized behavior, player targeting, and native damage. Player fixture physics are ticked and their positions held where the test requires a stationary target. The dry-healing fixture explicitly clears its air volume and holds the shark there so escape movement cannot invalidate the condition being tested.

```powershell
./gradlew.bat -PcoralCrusherTests runGameTestServer --offline
./gradlew.bat -PcoralCrusherModelTests verifyCoralCrusherAnimation --offline
./gradlew.bat build --offline
```

For concurrent development, `python tools/prepare_crusher_verification.py`
prepares an independent source copy. Run those checks sequentially inside the
printed directory, rather than replacing a running client's classes. Do not
refresh that copy while its checks are running.

The AI tests cover vertical investigation/pursuit before biting; required encounter phases; actual circling movement; full windup; one bite and recovery; patrol movement; creative/spectator exclusion; low-health escape and blocked healing near a threat beyond the healing delay; damage interrupting windup; escape from an overhead threat; slow healing and its reset after new damage; no healing on land; retreat persistence; return to patrol only after safe recovery; native fish acquisition followed by actual bite damage; and escaped-target reacquisition cooldown. The fish test accelerates native random acquisition checks with a fixed seed, while leaving the hunting sequence to actual AI ticks.

The original verified native run on 2026-10-04 passed all 16 required tests, including the existing skin and deep-fauna placement tests. Its historical log is `build/reef-predator-tests-verified.txt`. Six additional combat regressions cover steep upward/downward charging over a 22-block initial height difference, retaliation beyond passive player detection, distant drowned hunting with large vertical separation, player priority over drowned/NPC retaliation, and close bite followed by repositioning and charging. Player acquisition has no random chance gate.

Tests live under `src/gameTest`; model checks live under `src/modelTest`. Both are opt-in and excluded from the ordinary release jar. Detailed predator test logs are local under `build/reef-predator-tests*.txt`. Automated server/model checks do not substitute for a packaged-client visual review of the encounter.

The native model checks and normal release build also passed on 2026-10-04
(`build/reef-predator-model-release.txt`). Release inspection confirmed the new
controller and synchronized behavior enum are included, while GameTest classes,
the animation-check fixture, and the baseline model are excluded.

### Combat revision verification

`build/crusher-combat-tests-final.txt` records all **22 required native tests
passing** on the final combat controller. The two steep-charge tests start
with 22 blocks of vertical separation, measure at least four blocks of actual
vertical movement while charging, and require the **first** charge to bite a
stationary survival player. Detection of a drowned 30 blocks horizontally and
19 blocks vertically away is followed by actual bite damage. Separate tests
prove immediate retaliation outside passive player range, player preemption
over drowned (including NPC retaliation), and melee → recovery → reposition
→ windup → charge.

The wider hunt exposed cross-fixture interactions in the old tests. Completed
subjects are now removed, combat/escape encounters use separate batches, and
the skin-depth test clears its column through sea level instead of assuming
all terrain above its artificial floor is empty. Native damage is scheduled
after player water-state updates, matching a real underwater attacker.

The exploration launcher starts in Creative. Change to Survival or Adventure
to test aggression; Creative and Spectator remain excluded. A running source
snapshot must be refreshed and relaunched to use this revision. Native tests
do not certify the appearance of a live encounter with shaders.

`build/crusher-combat-model-final.txt` records passing native animation checks
and the final mod build. The verified shark entity/controller/model sources
match the project exactly. Jar inspection confirms the controller is packaged
and GameTest/animation fixtures are excluded. Local built artifact:
`build/libs/tidalterror-1.0.0-crusher-combat.jar`.

### Depth control revision

`SmoothSwimmingMoveControl` remains the three-axis navigation controller, but
its final `applyGravity` argument is now false. In Minecraft 1.20.1 that flag
adds +0.005 Y velocity every underwater tick, even with no navigation command.
The old travel code added -0.005 only without an attack target, so automatic
vertical motion changed when the shark acquired or lost prey. Both biases
are removed. Underwater travel now consumes AI steering and damps momentum;
patrol, pursuit, charge and escape routes choose height. Ordinary out-of-water
physics, collisions, currents and knockback remain active.

Two native depth-holding tests retain effective AI, native movement control
and travel while withholding navigation commands. They check both target
states over 80 ticks; NoAI would bypass the defect. These join the existing
22 tests, including upward/downward pursuit and the first steep charge bite.

Validation on 2026-10-04: baseline depth drift was +0.40 blocks without a
target and +3.55 blocks with a target over 80 ticks. Only the two new height
tests failed in the 24-test baseline. With both biases removed, all **24
required tests passed**, including the existing vertical attack tests. Logs:
`build/crusher-height-verification-v1/crusher-height-baseline.log` and
`crusher-height-fixed.log`. Normal build passed. Tested, packaged and current
main sources match; no test/preview fixtures are included in the jar.
Artifact: `build/libs/tidalterror-1.0.0-crusher-height-fix.jar`. SHA-256: `e857ee8998264ff7e3ef7d9c754f7dccd19e076591922c26aa1c1c523dc53bdd`.

### Recovery movement and state audit

The previous close bite called `change(RECOVER)`, which stopped navigation.
Recovery issued no replacement movement commands for 60 ticks. After momentum
decayed, the shark stood still between attacks. Charge recovery preserved its
route but also preserved speed 1.5 until that route ended, at which point it
stopped too. These were controller defects, not vanilla water-navigation rules.

Recovery now swims at 0.85 throughout its cooldown when reachable water is
available. A charge retains its original committed endpoint and lowers the
navigation multiplier; a close bite supplies a forward pass route. Completed
routes renew in the same general heading with alternate angles around solid
reef, and lateral alternatives when a steep pass reaches the surface or floor.
Failed routes are retried at ten-tick intervals. It never forces velocity
through blocks or performs additional bites during recovery. The intentional
20-tick windups still brake movement to telegraph an attack.

| Movement state | Navigation multiplier |
| --- | ---: |
| Patrol | 0.80 |
| Investigate | 0.95 |
| Retaliation pursuit | 1.10 |
| Circle | 0.90 (0.80 fallback approach) |
| Charge | 1.50 |
| Recovery | 0.85 |
| Reposition | 1.00 |
| Flee | 1.25 |
| Charge/close bite windup | Brake; no active route |

These are steering multipliers, not speeds in blocks per second. The inspected
Minecraft 1.20.1 `SmoothSwimmingMoveControl` combines the multiplier with the
movement attribute and the aquatic acceleration factor; pitch, turns, drag,
currents and collisions affect the resulting displacement. The controller
already used multiple speeds before this fix. Recovery lacked a distinct speed
and, after a close bite, lacked a route altogether.

`CoralCrusherStateAuditTests` adds five native regression cases: sustained
swimming after a close bite, a solid-wall detour, slower steering after a charge,
the full encounter through actual movement and retreat, and pursuit after a
swimmer leaves a close bite's reach with a vertical dodge. The cycle audit checks
telegraph/cooldown durations, damage only from attack states, movement in every
swimming state, native controller speed application, and retreat interruption.
The movement regressions measure actual traveled distance, late-cooldown movement,
active route ticks and longest idle gap. Test traces record ticks, distances and
average displacement for each observed state. The original depth, target priority,
vertical charge, healing, skin and deep-fauna regressions remain in the suite.

The final isolated comparison logs are `build/crusher-state-baseline-audit.txt`
and `build/crusher-state-audit-passed.txt`. On 2026-10-04, the old controller
failed four of the 29 tests: both recovery movement cases, recovery speed, and
the cycle's native speed audit. The revised controller passed **all 29 required
tests**. Old melee recovery traveled zero blocks across 55 ticks, had no active
route, and accumulated a 50-tick idle streak. Revised open-water melee recovery
traveled 2.38 blocks, including 0.90 blocks in the last 25 sampled ticks, with
55 active-route ticks and a longest idle streak of three ticks. The solid-wall
case traveled 2.48 blocks with no extended idle gap or solid-block collision.
Charge recovery's native multiplier changed from 1.50 to 0.85.

The audit uses an eight-block charge run-up and waits for real repositioning
movement before injecting a retreat injury. The existing injury test now
interrupts either attack windup before the first bite, rather than assuming
every encounter chooses a charge first. Failed audit samples remove their
subjects before later batches. Earlier iteration logs, including
`crusher-state-baseline.txt` with its incorrect pool-edge placement, are not
the final validation evidence. Server checks measure behavior and movement;
they do not certify the appearance of the encounter in a shader client.

The animation check and normal release build also passed
(`build/crusher-state-model-build.txt`). All main/generated sources match the
tested snapshot, and the release jar excludes GameTest/model-test fixtures.
The packaged spawn egg textures match the latest project resources.
Artifact: `build/libs/tidalterror-1.0.0-crusher-state-refinement.jar`.
The baseline failures, recovery measurements, source comparison and artifact
SHA-256 are recorded in `build/crusher-state-audit-report.json`.
