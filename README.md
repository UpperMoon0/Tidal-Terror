# Tidal Terror

![Tidal Terror banner](docs/assets/tidal-terror-banner.png)

Explore **Coral Cathedral** and the surrounding **Sunken Wastes** in new ocean provinces. The full-size Cathedral retains its giant coral formations, reef gardens and four animated marine creatures; broad adjoining Wastes rings contain sand and rock landmarks. Normal worlds use ordinary terrain heights. An optional Forge 1.20.1 world type extends the same provinces deep beneath the world with Endless.

## Creatures and food

| Creature | What to expect |
| --- | --- |
| Coral Crusher | A shark that investigates, circles, bites, and charges at prey. It prioritizes players over drowned and fish, swims during attack recovery, and flees at low health before slowly healing when safe. Sandy skins spawn nearer the seabed; blue skins spawn higher up. |
| Cathedral Ray | A peaceful ray that glides in loose schools, approaches swimmers out of curiosity, and escapes predators. |
| Veilglow | A glowing jellyfish that drifts and pulses with nearby companions. Contact can sting; it does not chase players. |
| Shardback | A seabed crab that wanders, forages, warns nearby swimmers, and can pinch defensively before retreating. |

All four have custom spawn eggs in the **Tidal Terror** creative tab. The Crusher egg randomly chooses either skin. Each creature drops its own raw seafood, with a cooked version obtainable in a furnace, smoker, or campfire. Steak, ray wing, jellyfish gel, and crab claw restore more hunger after cooking.

## Reef equipment (0.0.2)

Coral Cathedral supplies an **iron-to-diamond specialist equipment branch**. Coral Crushers drop 1�?"2 **Crusher Teeth**, and Shardbacks drop 1�?"2 **Shardback Plates**, alongside seafood. Looting can increase the material drops. Living Shardbacks also shed a plate while safely foraging on submerged sediment after 5�?"6 minutes of loaded underwater time; keep a respectful distance so they can forage. The initial molt takes five minutes, and cooldowns persist across saves.

Craft the spear with a Crusher Tooth, **iron ingot, dead coral block and leather**. Upgrade each matching iron armor piece with **three Shardback Plates and one dead coral block** at a crafting table. Any of the five vanilla dead coral block varieties works. Isolate a coral block from adjacent water to let it die, then mine the dead block with a pickaxe; Silk Touch is not required for the dead block. Collecting teeth or plates unlocks their recipes in the recipe book. Teeth repair the spear; plates repair the armor.

| Equipment | Strength and specialty |
| --- | --- |
| Reef Spear | 6 attack damage, 1.1 attack speed, 250 durability, and +1 block of entity reach in the main hand. Fully charged hits apply 2 bleeding damage over 4 seconds when both attacker and target are in water. Repeated hits refresh one bleeding effect. Supports damage or bleeding builds; Fire Aspect and Sweeping Edge are excluded. |
| Fang Arrow | Native arrow damage plus 2 bleeding damage over 4 seconds on land or in water. Craft four with a Crusher Tooth, stick and feather. Supports bows, crossbows and modded weapons using standard arrow ammunition. Serration and Hemorrhage cannot enchant arrows or ranged weapons. |
| Reef Armor | Helmet/chestplate/leggings/boots give 2/6/5/2 armor (15 total), zero toughness, and iron-equivalent durability. Each piece reduces knockback by 5% while standing on submerged ground, up to 20%. Knockback reduction works with mixed equipment. The full set reduces bleeding damage by 25% on land and underwater; ordinary protection remains iron-tier. |

The equipment inherits the mobs' materials: an ivory tooth and sandy bindings for the spear; violet Shardback carapace, cobalt coral, ivory segments and slate joints for armor. Diamond remains the stronger general defensive tier. See [equipment artwork and verification](docs/Reef-Equipment-Art-Workflow.md).

**Serration I�?"III** adds 0.5 damage per level every two seconds. **Hemorrhage I�?"II** extends bleeding to six/eight seconds. Both are spear-only enchantments available through tables and books/anvils, and can be combined with each other. Both conflict with Sharpness, Smite and Bane of Arthropods, creating a choice between immediate damage and stronger bleeding. All five book levels appear beside the spear in the Tidal Terror creative tab; book tooltips explain their level-specific bleeding bonuses. The spear tooltip shows enchanted damage every two seconds and duration. Coral Crusher bites also cause the base bleed: two damage over four seconds, with the same custom blood particles. The spear points forward in both hands. Bleeding produces custom blood droplets and dispersing underwater plumes with a dedicated status icon. See [sprite references, bleeding and enchantment details](docs/Reef-Sprites-and-Bleeding.md).

JEI loads automatically in the development client (`runClient`) for recipe inspection. It is optional development tooling and is not bundled into the release.

## Install and explore

Choose the jar matching your Minecraft version and loader. Version **0.0.4** supports five loader/version targets and changes default new-world ocean generation:

| Minecraft | Loader | Java | Required companion mods |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.2+ | 17 | None |
| 1.20.1 | Fabric Loader 0.16.14+ | 17 | Fabric API, Architectury API 9.2.14+ |
| 1.21.1 | Fabric Loader 0.17.2+ | 21 | Fabric API, Architectury API 13.0.8+ |
| 1.21.1 | NeoForge 21.1.228+ | 21 | Architectury API 13.0.8+ |
| 26.1.2 | NeoForge 26.1.2.99+ | 25 | Architectury API 20.1.16+ |

Install companion mods for the **same Minecraft version and loader** on both the server and clients. Fabric 1.20.1 includes Reach Entity Attributes 2.4.0 for the spear's extra reach; the newer versions use Minecraft's native reach attribute. Architectury API and Fabric API are separate downloads; TerraBlender is no longer required.

New normal worlds on all five ports contain Cathedral-centered ocean provinces surrounded by Sunken Wastes instead of the previous rare standalone Cathedral placement. The Cathedral retains its 1,024-block nominal radius; complete Wastes rings may reshape coastlines inside their footprint. With commands enabled, locate either the Cathedral or Sunken Wastes biome. Already generated chunks are not regenerated.

**Forge 1.20.1 only:** Craft a **Reef Compass** from a Compass, three Amethyst Shards and a Nautilus Shell to find a nearby accepted Cathedral without forcing distant chunk loads. The optional **Deep Reef Province (Endless)** world type requires Endless 0.9.3 on both client and server, configured with a minimum build height at or below -512 before creating the world. Other loaders have neither the Reef Compass nor this deep world type; ordinary worlds do not require Endless.

**World-preset compatibility warning:** This draft replaces the vanilla **minecraft:normal** world preset JSON. Other mods or datapacks replacing that same preset do not merge automatically: whichever pack wins takes ownership of the normal-world generator/dimensions. Use a deliberately merged preset or a separate custom world type; test in a new disposable world first. There is no migration for older TerraBlender saves. Details are in [Sunken Wastes design and limitations](docs/Sunken-Wastes-Province.md).

Coral Crushers ignore Creative and Spectator players. Use Survival or Adventure to try their hunting behavior. Shaders are optional and are not required to play.

Downloads are available from [GitHub Releases](https://github.com/UpperMoon0/Tidal-Terror/releases) and [CurseForge](https://www.curseforge.com/minecraft/mc-mods/tidal-terror). Check the file's Minecraft version and loader. See the [changelog](CHANGELOG.md) for release changes.

## Feedback and license

Report problems on the [issue tracker](https://github.com/UpperMoon0/Tidal-Terror/issues), with mod/loader versions, reproduction steps, and the relevant log or crash report. Include the seed and coordinates for generation problems.

Author: **NsTut**. Licensed under the [MIT License](LICENSE.txt). The original Forge MDK notice is preserved separately in [LICENSE-Forge-MDK.txt](LICENSE-Forge-MDK.txt).

## Development

The original Forge development commands use Java 17. The Gradle wrapper downloads development dependencies on the first build. Ports use Java 21 to run Gradle, with Java 17, 21 or 25 compilation/runtime toolchains selected for each target.

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

The Forge release jar is written to `build/libs`. Port jars are written to each target module's `build/libs`. On Linux or macOS, use `./gradlew` instead.

```powershell
./gradlew.bat -Pmultiversion buildAll
./gradlew.bat -Pmultiversion :fabric-1.20.1:runClient
./gradlew.bat -Pmultiversion :fabric-1.21.1:runClient
./gradlew.bat -Pmultiversion :neoforge-1.21.1:runClient
./gradlew.bat -Pmultiversion :neoforge-26.1.2:runClient
```

See [multi-version architecture and verification](docs/Multi-Version-Ports.md) for source ownership, runtime checks and release packaging.

To publish a new version, change `mod_version` in `gradle.properties`, add `changelogs/vVERSION.txt`, and push to `main`. CI runs native regression checks and packaging verification, then uploads all five verified jars to CurseForge and creates the matching GitHub release only after every upload has a matching receipt. Publication requires the repository secret `CURSEFORGE_API_TOKEN`.

Generate biome and feature data with `./gradlew.bat runData`. Generated registry JSON is tracked; runtime worlds and generator caches are ignored.

## Verification

```powershell
./gradlew.bat -PcoralCrusherTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyCoralCrusherAnimation
./gradlew.bat -PcathedralRayTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyCathedralRayModel
./gradlew.bat -PveilglowTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyVeilglowModel
./gradlew.bat -PshardbackTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyShardbackModel
./gradlew.bat -PreefLifeTests runGameTestServer
./gradlew.bat -PfoodTests runGameTestServer
./gradlew.bat -PspawnPoolTests runGameTestServer
./gradlew.bat -PequipmentTests runGameTestServer
./gradlew.bat -PcoralCrusherModelTests verifyReefEquipmentModel
```

Run each test property separately; test fixtures are excluded from normal release builds. The Validate workflow also runs the terrain audit in an isolated server with its documented seed and required server settings.

For equipment screenshots, prepare the optional development shaders, run `python tools/prepare_equipment_preview.py`, then use its printed client command. It opens a separate fresh preview world, captures native worn/held models, underwater first person in both hands, the creative tab and resource reload/glint, then exits. It does not open the ordinary development world.

Development shaders are optional: run `python tools/install_dev_shaders.py` to install the pinned Oculus, Embeddium, and Complementary development dependencies. Downloaded dependencies are excluded from Git and the release jar.

Reef Armor recipes upgrade the matching iron armor piece with three Shardback Plates and one dead coral block, preserving enchantments, name, damage, repair cost, trim and saved item data. Repair the spear with Crusher Teeth and the armor with Shardback Plates in an anvil. Gear tooltips explain the spear charge/water gate, bleed refresh, armor grounding bonus and repair materials.

The Reef Spear uses its 3D model in inventory and JEI as well as in hand. See the [fully enchanted weapon comparison](docs/Reef-Weapon-Balance.md) and [repository file policy](docs/Repository-Hygiene.md). Artwork sources and screenshots are local optional files; the release uses committed game-ready assets.

Repeated bleeding hits refresh duration while preserving the independent two-second pulse cooldown, including across entity saves.

Bleeding saves the attacker UUID and attributes lethal damage to that owner when resolvable, preserving native Looting and XP credit through victim save/reload.
