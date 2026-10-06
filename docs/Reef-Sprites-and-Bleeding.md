# Reef sprites and bleeding

The seven item sprites were generated individually with built-in ImageGen. The
exact prompts and retained source paths are in
[`tools/reef_sprite_sources.json`](../tools/reef_sprite_sources.json).
`python tools/export_reef_sprites.py` exports the sources to transparent 64×64
item textures using 32×32 logical pixels and nearest-neighbor sampling.

References were actual native-game Shardback and Crusher images, the actual
front/side/back worn armor capture, and the user's vanilla armor silhouette
reference. Purple segmented carapace and cobalt coral come from the Shardback;
ivory plates and slate joints match the worn armor. The Crusher tooth is a
single ivory tapered tooth. Helmet, chestplate, leggings, and paired boots keep
vanilla's general item silhouettes instead of rendering a mannequin or cuboid.

## Bleeding

The custom blood particle has its own registered type, provider, and three
native 16×16 frames. Fresh droplets disperse, expand, and fade into a small red
plume underwater; droplets fall in air. Native translucent particle rendering
integrates with the existing Oculus shader pipeline and also works without a
shader pack. Server emission keeps nearby multiplayer clients in sync; normal
particle distance and quality settings apply. Blood remains local to the wound,
with small ongoing trails and a burst on each damage pulse. Potion swirls are
disabled; a dedicated blood-drop status icon remains visible.

## Spear enchantments

Both are available through enchanting tables and enchanted books/anvils, only
on the Reef Spear. They are compatible with each other, but conflict with
Sharpness, Smite and Bane of Arthropods. Looting, Knockback, Unbreaking and Mending
remain supported. The spear excludes Fire Aspect and Sweeping Edge. They retain the charged underwater-hit gate and refresh one
effect rather than stacking independent bleeds.

| Enchantment | Bonus |
| --- | --- |
| Serration I–III | Adds 0.5 damage per level to each two-second bleed pulse. Over four seconds, total damage becomes 3/4/5 instead of 2. |
| Hemorrhage I–II | Extends duration to six/eight seconds, adding one/two pulses. |

Together at maximum level, four 2.5-damage pulses deal 10 damage over eight
seconds. The spear tooltip displays enchanted damage every two seconds and duration.
A weaker hit cannot downgrade an active stronger effect. Enchantment values
from edited NBT are clamped to the supported levels.

Thirteen native equipment GameTests cover the original six equipment regressions
plus enhanced bleed timing, stronger-effect preservation, suppressed potion
particles, native enchanting-table candidates, actual anvil/book assembly, successful versus rejected Crusher bites, and all five creative book levels with native tooltip events.

## Development JEI

JEI 15.20.0.118 loads through `localRuntime` in development clients. It follows
the [official JEI setup for Minecraft 1.20.1](https://github.com/mezz/JustEnoughItems/wiki/Getting-Started-%5BMinecraft-1.18.2-to-1.20.1%5D).
It is excluded from native server test runs and is not declared as a required
mod dependency or bundled into the released JAR. All six shaped equipment
recipes use vanilla crafting and are discovered by JEI without a custom plugin.

## Verified native client

On 2026-10-05, the isolated client completed seven actual framebuffer captures:
native worn armor, first-person spear in both hands, the creative tab, resource
reload with glint, bleeding with Complementary/Oculus, and the JEI inventory.
The blood check verified a living visible target, 41 server-emitted custom blood
particles in the native client particle engine, and the registered provider
after full resource reload. The production JAR and all 13 release-tooling tests
also passed locally. Eight equipment GameTests passed locally and in CI.

## Forward spear and book discovery (2026-10-06)

The held spear points forward in third person, with an angled forward first-person hold in either hand. Successful Coral Crusher bites apply the base four-second bleed (two damage), including custom blood particles. Rejected hits do not apply it.

The Tidal Terror tab includes Serration I, II, III and Hemorrhage I, II immediately after the spear. Native enchanted-book tooltips state the exact level bonus without restricting the description to a specific weapon. Vanilla enchanted-book/search behavior remains compatible.

The updated isolated client passed eight framebuffer captures, including the forward spear in both hands and the actual book tooltips. All ten equipment GameTests passed locally.

## Fang arrows and iron armor upgrades

Fang Arrow is an ArrowItem in minecraft:arrows, with a registered AbstractArrow projectile and native renderer. Exact Forge 1.20.1 / 47.2.0 bytecode confirms both BowItem and CrossbowItem call the ammunition createArrow factory. The projectile adds base bleeding only through native doPostHurtEffects, after damage succeeds. It needs no water and preserves normal flight, piercing, saves and pickup. Special arrows are consumed even with Infinity, matching native special-ammunition behavior.

Compatibility follows the standard ArrowItem factory and arrow tag; weapons that bypass that factory or replace the projectile with a separate ammunition system need a specific adapter. A registered third-party-style BowItem fixture covers inherited bow behavior. Universal behavior for every modded bow is not claimed.

Book tooltips describe only their general bleeding bonus. Spear tooltips explain full charge, both fighters in water, nonstacking refresh and Crusher Tooth repair. Armor tooltips explain 5% knockback reduction per piece while grounded underwater, 20% full set, the full-set 25% bleeding-damage reduction on land and underwater, and Shardback Plate repair. Armor crafting consumes the matching iron armor piece, three plates and one dead coral block, preserving the input armor’s saved item data and damage.

The 2026-10-06 revision passed all thirteen equipment GameTests, nine native client framebuffer captures, thirteen release-tooling tests and clean release JAR guards. Native firing tests cover both vanilla ranged weapons and a registered Forge-style bow subclass; arrow tests cover dry-land bleeding, rejected hits, saved piercing and pickup.


Artwork sources, concept references and runtime screenshots are optional local files ignored by Git. The release builds from committed game-ready textures; recreating artwork requires supplying those local source images.

## Independent bleeding pulse clock

Repeated hits now refresh effect duration without postponing the two-second damage pulse. Raising power or duration preserves that same clock. The server saves the remaining pulse ticks with the entity; legacy saves retain their prior pulse phase. A cured/removed effect starts a fresh clock when reapplied. Native damage immunity and all damage/enchantment values are unchanged. Two regression tests cover repeated native spear hits, power changes, expiry, milk curing and entity save/reload.

## Compact spear tooltip

The spear shows `Bleed: 1 damage every 2s for 4s` normally, `2.5 damage every 2s for 4s` with Serration III, and `2.5 damage every 2s for 8s` with Serration III + Hemorrhage II. The condition line is `Fully charged; both in water. Refreshes bleed.` Repair stays on its own short line. Hover text reads the current item enchantments and shares power/duration lookup with the actual hit effect. Native tooltip checks cover all twelve level combinations, including fractional totals and removing unnecessary decimal zeros.


## Armor data and lethal bleeding credit

Armor upgrade recipes retain the iron piece's serialized NBT/capability data instead of returning a fresh item. Damage is preserved because the new armor uses the same durability as iron. A dedicated shaped serializer keeps ordinary crafting layout, recipe-book and network behavior.

Spear hits, Fang Arrows and Crusher bites save the bleeding attacker UUID on the victim. Each damage event resolves that owner and uses the same magic damage type with the attacker attached, allowing native Looting and player XP credit. The UUID survives victim saves. Online players can be resolved after dimension changes; unresolved/offline owners fall back to ownerless magic rather than fabricated credit. Weaker applications cannot take ownership of an active stronger bleed. Fresh effects after curing and natural expiry clear old ownership. No bleed damage, cadence or balance value changes are introduced by this fix.
