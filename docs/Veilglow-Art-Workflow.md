# Veilglow artwork and implementation

Minecraft 1.20.1 / Forge 47.2.0. This follows the Coral Crusher and Cathedral
Ray workflows: native baked geometry determines UV placement, ImageGen supplies
material artwork, exact face assembly produces game textures, and native
GameTests plus real shader framebuffer captures establish behavior/appearance.

## Model

The user's orthographic sheet is retained in
`art-source/veilglow/design-reference.png`. `tools/create_veilglow_model.py`
constructs 78 native cuboids: a hollow stepped octagonal lavender bell, eight
violet rim sections, a mint bloom core, and six ribbon chains with six joints
each. Alternating ribbon lengths and tapered final sections match the concept.
The six attachment spokes are offset by 15 degrees so opposing ribbons do
not collapse onto the same silhouette in the front and side views.
The native collision box is 1.5 blocks wide and 2.4 blocks high.

The bell pulses in width and height, the core breathes subtly, and each ribbon
has delayed pitch/roll motion along its chain. Actual swim speed strengthens
the sway. Every animation starts by resetting the full baked hierarchy.
Body pitch is kept upright rather than copying a fish's pitching posture.

## Texture and transparency

Built-in ImageGen generated four low-contrast pixel material panels: pale
lavender bell, muted violet rim, medium lavender ribbons, and mint core.
The generated material sheet and egg art remain in `art-source/veilglow`,
so reconstruction does not depend on temporary files or an ignored gallery.

`VeilglowModelCheck` bakes the Minecraft model and exports actual vertex,
normal, and UV records for every face. It verifies moving animation vertices
and pose reset, then exports 48 frames. `tools/veilglow_art.py` maps the
generated materials onto these native UV faces, placing mint rim flecks and
tip/core glow by part identity. The 256x256 atlas has 468 faces and 4,168
occupied texels with zero overlapping occupied pixels. The final ribbon ends
fit entirely inside the collision height.

Unused texels are transparent. Used bell texels have alpha 78/255; opaque
core/rim/ribbon texels have alpha 255. The separate glow atlas includes only
the core, restrained rim flecks, and mint ribbon tips. Assembly checks UV
overlap, occupied alpha, unused transparency, and glow-mask containment.

The renderer draws the opaque core, rim, and ribbons, supplies a full-bright
glow layer, then submits the bell to native `entityTranslucent`. This keeps
the core visible through an actual hollow shell instead of painting an
opaque glowing flower on a solid bell. Invisible entities omit both extra
layers. The software preview intentionally shows the glass opaque for shape
inspection; it is not evidence of the final transparency. Real game captures
are required to evaluate transparency with Oculus/Complementary.

## Egg

Built-in ImageGen used the Coral Crusher spawn egg as style reference and the
Veilglow orthographic sheet as subject reference. The inventory design is a
lavender oval egg with a violet bell cap, mint bloom, and short tassels.
Assembly downsamples to 32x32 with nearest-neighbor sampling and thresholds
alpha to 0/255. The Forge egg retains native interactions, uses a custom
generated item model with white tint overrides, and joins Crusher and Ray
eggs in the Tidal Terror creative tab.

## Behavior and spawning
Veilglow has 12 health. A single movement goal alternates slow drifting,
swimming pulses, loose grouping, and predator escape using native swimming
navigation. Accepted damage or a contact sting triggers escape. Safe quiet
water allows gradual healing; stranding triggers a limited recovery flop.
It has no attack or pursuit goals. Contact stings deal one heart with a
40-tick cooldown, ordinary damage immunity, and creative/spectator exclusions.
Synchronized behavior states contract its bell and ribbons during pulses.
See [Reef-Ambient-AI.md](Reef-Ambient-AI.md) for the current state machine
and verification. Earlier captures below document the original artwork.

Natural groups contain 2–4, weight 8, only in Coral Cathedral. Placement
requires source water in the column from one block below through three above
the feet, below sea level minus eight. Native collision checks additionally
exclude solid coral/terrain. Spawn eggs retain ordinary placement behavior
outside the biome. There are no drops in this first version.

## Reproduction

Use Java 17. With cached dependencies:

```powershell
python tools/create_veilglow_model.py
./gradlew.bat -PcoralCrusherModelTests verifyVeilglowModel --offline
python tools/veilglow_art.py art-source/veilglow/materials-v1.png art-source/veilglow/spawn-egg-source-v1.png
./gradlew.bat -PveilglowTests runGameTestServer --offline
./gradlew.bat runData build --offline
```

The three GameTests cover habitat/vanilla exclusion/group table/collision/egg,
observable navigation-driven swimming, and contact damage/cooldown/player
mode exclusions/no pursuit. The task requires native completion and success
markers, preventing server startup failure from becoming a false pass.

For shared-checkout builds, `tools/prepare_veilglow_verification.py` creates an
independent source copy; run checks sequentially in that directory.
`tools/prepare_veilglow_preview.py` prepares a separate source copy and cloned
reef save after checking the audit save's lock. Run `-PveilglowPreview runClient`
in its returned directory. It uses the existing Oculus/Embeddium dependencies
and Complementary pack, obtains its photographed Veilglow through Minecraft's
native spawner, and captures front/side/top/underside/reef/creative-tab views.
The accelerated audit fauna is cleared only in this photography copy.
Captures go to `art/veilglow/runtime`. All fixtures are opt-in and excluded
from the normal release jar. The user's exploration save is not changed.

## Verified 2026-10-04

- Final native model bake passed vertex animation and pose-reset checks across
  48 exported frames. Texture assembly passed all 468 faces, 4,168 occupied
  texels, zero overlap, shell alpha, and glow containment checks.
- All three required native GameTests passed, including one-heart contact
  damage, immediate repeat suppression, sting recovery after 40 ticks,
  creative/spectator exclusions, and no damage without contact. Navigation
  moved the living submerged entity 5.0 blocks in the test pool.
- Minecraft's actual NaturalSpawner produced the photographed Veilglow in
  `tidalterror:coral_cathedral` at `(56.5, -10.0, 22.5)`, UUID
  `64ca3484-d69b-4f8c-96ae-e5fb9618863f`. Six real framebuffer captures
  passed with Complementary active, including the native creative tab with
  all three custom eggs. Front, side, top, underside, and reef captures were
  visually inspected for the transparent shell, visible core, and ribbons.
- Data generation and the normal reobfuscated build passed. Package inspection
  confirmed the Veilglow classes, textures, egg, loot table, and group spawn
  data, byte-matched the implemented Veilglow sources and registrations, and
  excluded all opt-in test/preview fixtures. Preview artifact:
  `build/libs/tidalterror-1.0.0-veilglow-preview.jar`, SHA256
  `bc2225704fb6fdb9f850dce26589d3f8a55751999801893c68a13c162b721e05`.

Evidence: `build/veilglow-verification-project-v1/veilglow-model.log`,
`build/veilglow-verification-project-v1/veilglow-test.log`,
`build/veilglow-verification-project-v1/veilglow-build.log`,
`build/veilglow-preview-project-v1/veilglow-client.log`, and
`art/veilglow/runtime/passed.txt`.
