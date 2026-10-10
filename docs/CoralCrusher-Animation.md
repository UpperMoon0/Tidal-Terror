# Coral Crusher model animation (Minecraft 1.20.1)

The exported setupAnim method was empty, lowerjaw contained no cubes, and
head/body/tail were independent root parts. Head and tail now belong to the
body, with geometry rebased around the neck, jaw hinges, and tail attachment.
The lower-mouth cubes were moved from upperjaw into lowerjaw. The native
vertex comparison confirms the unchanged rest geometry and texture UVs.

Motion
- Gentle idle tail movement, mouth breathing, and whole-body bobbing.
- Stronger tail strokes and paired fin movement as swimming speed rises.
- Body pitch follows the renderer's interpolated entity pitch underwater;
  limited head yaw follows the renderer's relative look direction.
- Jaw opening/closing and slight head recoil use EntityModel.attackTime.
  The predator goal calls swing(MAIN_HAND) once for its committed bite;
  LivingEntityRenderer fills attackTime from the synchronized attack swing.
- The synchronized WINDUP behavior strengthens the tail stroke and slightly
  opens the jaw/poses the head before the charge.
- Faster tail and body movement out of water, following CodModel's pattern.
- Reset every baked ModelPart before posing, to prevent cumulative transforms
  and leakage between entities sharing the renderer's model instance.

Decompiled source references
D:/Workspaces/Minecraft Projects/MC Mods/MC-Modding-Src/1.20.1/
  net/minecraft/client/model/CodModel.java (tail sine and land frequency)
  net/minecraft/client/model/DolphinModel.java (body pitch and swim motion)
  net/minecraft/client/model/RavagerModel.java (mouth joint animation)
  net/minecraft/client/renderer/entity/LivingEntityRenderer.java
  net/minecraft/world/entity/ai/goal/MeleeAttackGoal.java
  net/minecraft/world/entity/LivingEntity.java
The motion amplitudes are artistic choices for this model, not copied vanilla
constants. The animation lifecycle and swing source follow native Minecraft.

Verification
./gradlew.bat verifyCoralCrusherAnimation -PcoralCrusherModelTests --offline
Native ModelPart baking and renderToBuffer vertex capture passed checks for
rest geometry/UV preservation, moving swim/bite/land vertices, and pose reset.
48 rendered vertex frames were exported and rasterized with the actual texture
for build/animation-preview/coral-crusher-animation.gif. This is a software
preview of native model geometry, not an in-game framebuffer capture.
The swimming-control implementation is retained. The encounter controller is
now documented in CoralCrusher-Predator-AI.md.
Test sources reside in src/modelTest and are excluded from the release jar.
Normal ./gradlew.bat build --offline passed, including reobfJar.
The historical release archive inspection confirmed no model checks, baseline
model, or GameTest fixtures in the then-named examplemod-1.0.0.jar. The current
release filename is build/libs/tidalterror-0.0.1.jar.
