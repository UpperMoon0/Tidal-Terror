# Reef equipment — Forge 1.20.1

The spear and armor implement the approved orthographic design as native Minecraft
models. The material references are the actual sandy Coral Crusher and Shardback
renders, alongside the supplied Shardback concept. Armor uses violet/lavender
carapace, cobalt attached coral, ivory structural segments and slate joints. The
spear has an ivory tooth, sandy hide bindings and a pale porous coral shaft.

## Native geometry and artwork

`tools/create_reef_equipment_models.py` generates 54 armor cuboids in four separate
slot layers and 15 spear cuboids. Broad stepped shell plates, asymmetrical shoulder
coral, ivory limb segments and a single serrated tooth preserve the source mobs'
shapes. Joint roots match native humanoid models. Individual slot models prevent
leggings and boots from drawing one another's geometry. Very small distinct cube
inflations separate intentionally layered shell surfaces to prevent z-fighting.

`EquipmentModelCheck` bakes those exact production layers. It exports rendered
positions, UV coordinates, normals and material-tagged part paths, verifies that
head/arm/leg movement carries the equipment, and verifies a clean pose reset.
These native exports, rather than inferred rectangles from the concept, are the
texture source of truth.

Built-in ImageGen supplied six flat pixel material tiles. Its full prompt is
retained in [reef-equipment-material-prompt.md](reef-equipment-material-prompt.md)
and its source image in the optional local `docs/assets/reef-equipment-materials.png`.
The material sheet contains violet, cobalt, ivory, slate, sandy hide and porous
coral in two columns and three rows. It contains no painted anatomy or directional
lighting. The concept and original full-resolution generations remain in the local
`art-source/reef-equipment` folder; reproducible inputs are retained here in docs.

`tools/equipment_art.py` samples those tile interiors and assigns them by material
name to every native face. The armor atlas contains 324 faces and 5,284 occupied
texels; the spear atlas contains 90 faces and 438 occupied texels. Both 256×256
atlases have zero overlapping UV islands, fully opaque occupied texels and
transparent unused texels. The spear inventory icon renders the baked geometry.
Armor and material icons are now independently generated sprites that follow
vanilla item silhouettes and the actual worn armor/mob materials; see
[the sprite and bleeding revision](Reef-Sprites-and-Bleeding.md).

The native spear model renders in inventory, in hand, on the ground and in item
frames. The unused flat spear texture has been removed. The armor uses
the native humanoid armor hooks and inherits the wearer's pose. Model caches reset
after resource reload. Client renderer classes are initialized only on the client.

## Acquisition and progression

Crushers drop 1–2 teeth; Shardbacks drop 1–2 plates, in addition to their existing
seafood. Looting adds material yields, and burning deaths still drop raw crafting
materials. Living Shardbacks can shed a plate on submerged sediment while foraging
after 6,000 loaded underwater ticks; following cooldowns are 6,000–7,200 ticks.
Dry time does not advance that timer. Threatened/fleeing crabs wait until they can
forage again. Cooldowns persist through NBT saves and molting respects `doMobLoot`.

The spear recipe uses iron, dead coral and leather. Each armor recipe consumes
the matching iron armor piece, three Shardback Plates and a dead coral block. Acquiring the relevant mob
material unlocks its recipe-book entries. This is an iron-to-diamond specialist
branch; ordinary diamond armor retains greater protection and toughness.

| Item | Stats and behavior |
| --- | --- |
| Reef Spear | 6 damage, 1.1 attack speed, 250 durability, +1 main-hand entity reach. Charged successful hits apply one 80-tick bleed when both fighters are in water. Two pulses deal 1 damage each at approximately 2 and 4 seconds; equal-strength hits refresh duration. Ordinary magic-damage defenses and immunity still apply. Repairs with teeth; damage or bleeding enchantment builds; no Fire Aspect or Sweeping Edge. |
| Reef Armor | Defense 2/6/5/2 (15 total) for helmet/chest/legs/boots, zero toughness and iron durability. Native knockback is multiplied by `1 - 0.05 × worn pieces` only when the wearer is in water and grounded. Knockback works with mixed sets; all four pieces additionally reduce bleeding damage by 25% on land and underwater. Repairs with plates. Armor upgrades preserve the iron input’s saved data and damage. |

## Reproduction and verification

Use Java 17. Run test properties separately; fixtures never belong in release jars.

```powershell
python tools/create_reef_equipment_models.py
./gradlew.bat -PcoralCrusherModelTests verifyReefEquipmentModel --offline
python tools/equipment_art.py docs/assets/reef-equipment-materials.png
python tools/create_equipment_resources.py
./gradlew.bat -PequipmentTests runGameTestServer --offline
./gradlew.bat -PfoodTests runGameTestServer --offline
```

Accept the EULA in each isolated test directory before running the native suites.
For a clean production build alongside a running preview, use
`python tools/prepare_equipment_verification.py` and run `clean build` in its printed
project directory. The production packaging verifier also scans the calling source
tree to reject leaked fixture classes.
The six equipment tests exercise real player attacks and charge gates, Forge's
server entity reach, bleed refresh/timing, real recipe matching and assembly,
repair/enchant compatibility, armor attributes and native conditional knockback,
actual deaths, peaceful molting, persisted cooldowns and the loot gamerule.

`tools/prepare_equipment_preview.py` copies sources into a separate preview project
with the existing optional shader dependencies. Run its printed client command.
It creates a fresh private flat world and a small underwater test area; it never
reads or modifies an existing development or player world. The preview captures
the actual final framebuffer for front/side/back worn armor and held spear,
underwater first person in both hands with Complementary shaders, native creative-tab entries,
and a complete resource reload followed by enchantment glint. It requires all five
captures and a success marker before passing.

Validate includes the equipment suite and native model audit; release packaging
also checks that every production resource is present and fixtures are excluded.

## Verified implementation

On 2026-10-05, all six required native equipment GameTests and all three food
GameTests passed. The equipment bake/texture audit and all 13 release-tooling
tests passed. A clean ordinary 0.0.2 jar passed the production resource, metadata
and fixture-exclusion checks. Five final client framebuffer captures passed and
were visually inspected; both hands show the spear tip inside the viewport, and
the third-person spear is upright. This is local verification; PR CI independently
checks the complete native matrix before merge.

Artwork sources, concept references and runtime screenshots are optional local files ignored by Git. The release builds from committed game-ready textures; recreating artwork requires supplying those local source images.

The spear uses the native 3D model in every item display context, including inventory and JEI. There is no separate 2D spear texture. Its particle sprite uses the model UV atlas.
