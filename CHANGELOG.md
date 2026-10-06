# Changelog

## 0.0.3 — Unreleased

- Fix ray seabed/coral escape clearance and whole-wing navigation shortcuts across all targets; add native obstacle regressions.
- Include JEI in every port development environment, with client-loading checks and no production dependency.

- Fix 26.1.2 Reef Armor's native head/hat hierarchy crash and restore the special spear's hand-centering transform; construct armor models in client regression checks.

- Fix the NeoForge 26.1.2 normal-world generation crash caused by reversed pumpkin/sugar-cane feature ordering; add native Overworld biome-graph regression coverage.
- Share identical 1.20.1 gameplay/rendering classes, the shark behavior algorithm and modern resources; use authoritative shared equipment tuning without changing balance.
- Restrict port reef water repair to unfinished generation or saved pending work, preserving player-built drained rooms and bubble elevators on revisit.
- Added Fabric 1.20.1, Fabric/NeoForge 1.21.1 and NeoForge 26.1.2 ports with shared reef gameplay and unchanged equipment balance.
- Adapted native entity/chunk persistence, modern enchantments/recipes and 26.1.2 rendering.
- Added native port gameplay and client-loading checks plus verified five-target release packaging.

## 0.0.2

- Added the native 3D Reef Spear: Crusher tooth, coral skeleton shaft and sandy hide bindings; 6 damage, 1.1 attack speed, 250 durability and +1 block main-hand entity reach.
- Fully charged underwater spear hits apply 2 bleeding damage over 4 seconds, refreshing without stacking.
- Full Reef Armor grants 25% less bleeding damage on land and in water; partial sets do not. Protection stacks normally, and equipping/removing pieces updates the bonus immediately.
- Armor upgrades preserve input enchantments, name, wear, repair cost, trims and other serialized item data; dedicated shaped recipes synchronize normally.
- Bleeding preserves attacker UUID through victim saves and attributes lethal magic damage for native Looting/XP rewards; weaker applications cannot steal a stronger bleed.
- Corrected public equipment documentation and added save/cure damage-source diagnostics.
- Iron-stage balance: armor matches iron protection (15 total) with 20% full-set seabed grip; bleeding enchantments conflict with Sharpness/Smite/Bane, and the spear excludes Fire Aspect.
- Tooltips state bleeding damage every two seconds and enchanted duration; Hemorrhage books show only added duration. Fang Arrow remains a non-stacking two-damage bonus.
- Added four pieces of Shardback Reef Armor with violet shell plates, cobalt coral growths, ivory supports and slate joints; 15 armor, no toughness and iron durability.
- Each worn armor piece reduces knockback by 5% while grounded underwater, up to 20%.
- Added tooth/plate loot alongside seafood, peaceful Shardback molts, iron-and-coral recipes, recipe unlocks and material repairs.
- Added native equipment gameplay/model checks, exact-face texture tooling and isolated client previews.
- Revised armor icons to vanilla silhouettes and material icons to their native mob appearance using referenced ImageGen sprites.
- Added custom blood droplets/plumes with no potion swirls, and spear-only Serration I–III and Hemorrhage I–II enchantments for stronger/longer bleeding.
- Added optional development-client JEI and native table/anvil/enchanted bleeding regressions.
- Coral Crusher bites apply base bleeding; held spears point forward in both hands.
- All five spear enchantment book levels appear beside the spear with level-specific bonus tooltips.
- Added Fang Arrows: base bleeding on land/water, native bow/crossbow ammunition, four crafted from a Crusher Tooth, stick and feather.
- Armor recipes now consume matching iron armor, three plates and dead coral; gear tooltips explain mechanics and material repairs.
- Bleeding books show general bonuses without a spear-only line; ranged gear cannot receive bleeding enchantments.

- Repeated bleeding hits refresh duration without resetting the independent two-second pulse cooldown; pulse timing survives saves and resets for a new effect after curing.

## 0.0.1 â€” 2026-10-04

- Added Coral Cathedral: a deep ocean biome with giant branching, chalice, and sea-fan corals, irregular smaller colonies, dense coral gardens, sandy floors, and sandstone beneath.
- Added animated Coral Crushers, Cathedral Rays, Veilglows, and Shardbacks, with habitat-specific natural spawning and separate population pools.
- Added blue and sandy Coral Crusher skins; natural spawns choose by seabed clearance, while spawn eggs choose randomly.
- Added predator pursuit, telegraphed bites and charges, moving recovery, low-health escape, and safe regeneration for Coral Crushers.
- Added peaceful schooling and curiosity for rays, pulsing jellyfish groups and contact stings, and crab foraging, warnings, defensive pinches, and escape.
- Added four raw seafood drops, four cooked foods, furnace/smoker/campfire recipes, and custom transparent spawn egg artwork.
- Improved reef boundaries and flooding, removed unwanted reef bubble columns, and adjusted new shipwreck/ruined-portal placement to the seabed.
- Added deep-water spawning support for dolphins, turtles, and tropical fish in Coral Cathedral, and reduced drowned selection weight there.
- Added native behavior, navigation, spawning, structure, food, and model regression fixtures, plus documented art workflows and preview tools.

World-generation fixes affect newly generated chunks. Existing terrain and structures are not automatically regenerated or moved.
