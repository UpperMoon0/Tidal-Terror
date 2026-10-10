# Coral Crusher AI repair (Minecraft 1.20.1 / Forge 47.2.0)

Depth control: automatic swim-controller lift and target-dependent sinking
are removed; navigation controls underwater height. See the depth control
revision in CoralCrusher-Predator-AI.md.

Current AI: see CoralCrusher-Predator-AI.md for the patrol/investigate/circle/
windup/charge/recover controller, retreat, regeneration, and current tests.
The notes below record the original swimming-control repair and its historical
verification; the melee/random-swim goal combination has since been replaced.

Source-confirmed defects
- CoralCrusherEntity.aiStep replaced delta movement with its random tx/ty/tz
  after vanilla AI processing. MeleeAttackGoal controls PathNavigation, so
  pursuit steering could not control the resulting swimming velocity.
- The random goal was always eligible and declared no movement flags. Vanilla
  GoalSelector arbitrates only the flags declared by goals, so the random
  movement and melee goal could run concurrently.
- WaterBoundPathNavigation supplies three-dimensional water paths, but the
  inherited ground MoveControl steers horizontal movement and requests jumps
  for positive height differences rather than setting swimming vertical input.

References inspected
D:/Workspaces/Minecraft Projects/MC Mods/MC-Modding-Src/1.20.1/
  net/minecraft/world/entity/animal/Dolphin.java
    constructor:86-87; goals:167-175; navigation:182-185; travel:351-361
  net/minecraft/world/entity/ai/control/SmoothSwimmingMoveControl.java:37-70
  net/minecraft/world/entity/ai/control/MoveControl.java
  net/minecraft/world/entity/ai/goal/MeleeAttackGoal.java
  net/minecraft/world/entity/ai/goal/RandomSwimmingGoal.java
  net/minecraft/world/entity/ai/goal/RandomStrollGoal.java
  net/minecraft/world/entity/ai/goal/GoalSelector.java
The local 1.20.1 decompilation uses obfuscated member names. The project's
47.2.0 mapped Minecraft bytecode was also inspected to confirm mapped APIs,
attribute inheritance, and the native player fixture API.

Repair
Use Dolphin's vanilla swimming controls and water travel implementation.
Use RandomSwimmingGoal at priority 3, below melee at priority 2; both now
share water navigation and MOVE arbitration. Register player acquisition in
targetSelector, following vanilla's target/action selector separation.
Use Dolphin's melee speed multiplier 1.2 to match the reference controller
configuration. The initial integration with multiplier 5 showed repeated
circles outside melee reach and no damage over 300 ticks. The verified final
configuration combines multiplier 1.2 with the pitch fix described below;
multiplier 5 with that pitch fix has not been tested.
Retain wandering interval 50, health 120, damage
10, knockback 2, follow range 20, and inherited movement attribute default.
The old random velocity overrides and private movement vector API are gone.

Regression command
  ./gradlew.bat runGameTestServer -PcoralCrusherTests
Tests and pool resources are opt-in and absent from normal release builds.
The test server writes only to build/coral-crusher-gametest, not run/saves.
Coverage: natural player acquisition, upward/downward pursuit and native melee
health loss, creative/spectator exclusion, and navigation-driven wandering.

Scope
Water-only navigation remains. This does not add land pursuit, amphibious
behavior, or certification for other Minecraft versions or mod combinations.

Additional control interaction
MeleeAttackGoal updates LookControl with the target entity every tick.
LookControl uses LivingEntity eye height; SmoothSwimmingLookControl then sets
X rotation after SmoothSwimmingMoveControl has computed path-based steering.
The observed upward-pursuit failure with unmodified Dolphin look control
hovered above intermediate waypoints and never dealt damage over 300 ticks.
PathNavigation requires a height difference under one block to advance a node.
During an active water path, the crusher's look controller now leaves pitch
with the movement controller; horizontal looking and idle pitch remain vanilla.

Verification
The final implementation passed all four native Forge 47.2.0 GameTests.
Detailed position/path traces: build/ai-audit/fixed-pitch-tests.txt.
The server used an isolated test world and real ServerPlayer damage/targeting.
The test player has a packet sink and is registered directly in ServerLevel,
because Forge 47.2.0's vanilla mock-player login helper fails on a null Netty
channel before the test can begin. Player physics are ticked explicitly.
No packaged-client visual check was performed.
Original-code negative control
The same four tests against the original source failed both vertical melee
pursuits and navigation-driven wandering; creative/spectator exclusion passed.
Log: build/ai-audit/original-regression-tests.txt.
The fixed source was restored and its hash checked before packaging.
Normal build verification
./gradlew.bat build --offline succeeded, including reobfJar.
Artifact: build/libs/examplemod-1.0.0.jar (existing project archive name).
The release archive was inspected: no GameTest classes, pool fixture, or
obsolete CoralCrusherRandomSwimGoal class remained.
