# Tidal Terror

![Tidal Terror banner](https://raw.githubusercontent.com/UpperMoon0/Tidal-Terror/main/docs/assets/tidal-terror-banner.png)

**Version 0.0.4:** Explore provinces of **Coral Cathedral** and the adjoining **Sunken Wastes**, with towering coral formations, sandstone basins, and four animated marine creatures. New normal worlds on all five ports generate full-sized Cathedral provinces and their wide Wastes rings at ordinary heights. The optional Forge 1.20.1 Deep Reef Province world type extends the same terrain below vanilla bedrock using Endless.

## Meet the reef's creatures

- **Coral Crusher:** a large shark that investigates swimmers, circles prey, and attacks with close bites or committed charges. It hunts drowned as well as fish, prioritizes players, and retreats when badly injured. Sandy sharks spawn closer to the seabed; blue sharks spawn higher in the water. Both are skins of the same creature.
- **Cathedral Ray:** a peaceful ray with broad indigo wings and branching mint markings. It glides in loose schools, investigates nearby swimmers, and flees predators and attackers.
- **Veilglow:** a drifting jellyfish with a translucent lavender bell, mint glow, and animated ribbons. It pulses through the water in loose groups. Keep your distance: contact can sting, though it does not chase players.
- **Shardback:** a seabed crab with a violet shell, cobalt coral growths, and an oversized claw. It wanders and forages on sediment, warns swimmers who crowd it, and can deliver a defensive pinch before retreating.

All four creatures have animated models and custom spawn eggs in the **Tidal Terror** creative tab. The Coral Crusher egg randomly chooses its blue or sandy skin.

## Explore, survive, and cook

The reef combines branching coral groves, open chalices, lace-like sea fans, boulders, dense coral plants, seagrass, and sea pickles using vanilla blocks. Dolphins, turtles, and tropical fish can spawn in its deep water. Newly generated shipwrecks and ruined portals are placed against the seabed in this biome.

Each creature drops its own seafood: **Coral Crusher Steak**, **Cathedral Ray Wing**, **Veilglow Gel**, or **Shardback Claw**. Cook them in a furnace, smoker, or campfire for better hunger restoration.

## Craft the reef's equipment (0.0.2)

Hunt Coral Crushers for **Crusher Teeth**, or gather **Shardback Plates** from crab drops and peaceful molts. Shardbacks shed plates after several minutes underwater when they can forage safely on sediment. Combine materials with the matching iron armor piece and dead coral for armor upgrades; the spear uses an iron ingot, dead coral and leather. Collecting materials unlocks the crafting recipes.

- **Reef Spear:** an ivory tooth on a coral shaft with sandy bindings. Deals 6 attack damage at 1.1 attack speed, has 250 durability and one extra block of main-hand entity reach. Fully charged hits cause 2 bleeding damage over 4 seconds when both fighters are in water; repeated hits refresh the effect.
- **Reef Armor:** native violet shell plates, cobalt coral growths, ivory supports and slate joints, matching Shardback. The full set provides 15 armor, no toughness, and iron durability. Each piece reduces knockback by 5% while grounded underwater, up to 20%. Wearing all four pieces reduces bleeding damage by 25%, on land or underwater.

Armor upgrades retain the iron piece's enchantments, name, damage, repair cost, trim and other saved item data. Serration and Hemorrhage combine with each other, but conflict with Sharpness, Smite and Bane; the spear excludes Fire Aspect and Sweeping Edge.

This is a specialist branch between iron and diamond. Teeth repair the spear; plates repair the armor. Equipment and crafting materials appear in the Tidal Terror creative tab.

## Requirements and installation

Choose a file for your exact Minecraft version and loader:

| Minecraft | Loader | Java | Companion mods |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.2+ | 17 | None |
| 1.20.1 | Fabric 0.16.14+ | 17 | Fabric API, Architectury API 9.2.14+ |
| 1.21.1 | Fabric 0.16.14+ | 21 | Fabric API, Architectury API 13.0.8+ |
| 1.21.1 | NeoForge 21.1.228+ | 21 | Architectury API 13.0.8+ |
| 26.1.2 | NeoForge 26.1.2.99+ | 25 | Architectury API 20.1.16+ |

Place the matching Tidal Terror jar and required companion mods in your instance's `mods` folder. Multiplayer needs them on the server and every client. Use companion builds for the same Minecraft version and loader. Fabric 1.20.1 bundles Reach Entity Attributes for the spear; newer versions use native reach. Fabric 1.21.1 declares Loader 0.16.14 as its metadata minimum and is built and tested with Loader 0.17.2. TerraBlender is no longer required.

Shaders are optional. Development previews use Oculus, Embeddium, and Complementary Reimagined; these are separate downloads and are not bundled with Tidal Terror.

## Finding Coral Cathedral and Sunken Wastes

Explore newly generated ocean chunks; with commands enabled, locate either the Cathedral or Sunken Wastes biome. Existing chunks retain their terrain and structures. Older Cathedral rarity estimates do not describe the full new province footprint.

**Forge 1.20.1 only:** A craftable **Reef Compass** (Compass, three Amethyst Shards, Nautilus Shell) locates an accepted Cathedral without loading remote chunks. The separate, experimental **Deep Reef Province (Endless)** preset requires Endless 0.9.3 (supported range: 0.9.3 to below 0.10), its Architectury API 9.2.14+ dependency on both client and server, and a minimum build height at or below -512 configured before creating a new world. Ordinary worlds need no Endless; the other four ports do not include this compass or deep world type.

**Compatibility warning:** Version 0.0.4 replaces vanilla **minecraft:normal** world-preset JSON, conflicting with other mods/datapacks that replace the same preset. The highest-precedence pack wins; generators are not automatically combined. Use a deliberately merged or separate custom preset and test a fresh world. No older TerraBlender-save migration is supplied. See the [worldgen design and historical validation notes](https://github.com/UpperMoon0/Tidal-Terror/blob/main/docs/Sunken-Wastes-Province.md).

**Experimental deep-world limitations:** Cave and structure integration remains incomplete; the native deep audit disables structures. Preset-collision checks do not establish compatibility with every modpack. Worst-case populated burst-load timings and the recorded client/tick timing regressions remain unresolved. Historical benchmark data is retained as measured and does not establish a general performance improvement. Evaluate deep generation in a disposable new world and keep backups.

Coral Crushers ignore Creative and Spectator players. Switch to Survival or Adventure to experience their hunting behavior.

## Feedback

Created by **NsTut**.

Licensed under the [MIT License](https://github.com/UpperMoon0/Tidal-Terror/blob/main/LICENSE.txt).

Report bugs on the [issue tracker](https://github.com/UpperMoon0/Tidal-Terror/issues). Include your Minecraft, loader, companion mod, and Tidal Terror versions, other installed mods, steps to reproduce, and the relevant log or crash report. For generation issues, also include the world seed and coordinates.

[Source and development notes](https://github.com/UpperMoon0/Tidal-Terror) � [Changelog](https://github.com/UpperMoon0/Tidal-Terror/blob/main/CHANGELOG.md)
