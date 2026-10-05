# Fully enchanted Reef Spear balance

Reviewed 2026-10-06 against 0.0.2. Damage is in health points (two points = one heart). This mod targets Forge 1.20.1; mace and vanilla netherite spear figures below refer to 1.21.11 and were source-checked, not run inside this mod.

## Compatible maximum enchantments

Reef Spear: Sharpness V, Serration III, Hemorrhage II, Fire Aspect II, Knockback II, Looting III, Unbreaking III and Mending. Netherite sword adds Sweeping Edge III to its applicable vanilla enchantments. Trident uses Impaling V, Loyalty III, Channeling, Unbreaking III and Mending; Riptide III is an alternative mobility build, incompatible with Loyalty and Channeling.

Mace uses Density V and Wind Burst III with compatible utility enchantments for the fall-damage build. Breach IV is a separate armor-focused build, incompatible with Density, Smite and Bane of Arthropods. Netherite spear uses Sharpness V, Lunge III and compatible sword utility enchantments, excluding Sweeping Edge. Sharpness and Smite are alternative builds, not combined damage bonuses.

## Damage and roles

| Weapon | Enchanted ordinary hit | Special damage | Ordinary attack speed |
| --- | ---: | --- | ---: |
| Reef Spear, 1.20.1 | 9 | 10 bleeding over 8 seconds, fully charged hit with both entities in water | 1.1/s |
| Netherite sword, 1.20.1 | 11 | Sweeping crowd damage; Fire Aspect where burning is possible | 1.6/s |
| Trident, 1.20.1 | 9 melee | 21.5 melee against aquatic mob types with Impaling V; thrown damage is 8 or 20.5 respectively | 1.1/s melee |
| Mace, 1.21.11 | 6 | Density V smash: 25.5 at 3 blocks fallen, 48 at 8 blocks, 55 at 10 blocks, before an eligible critical multiplier | 0.6/s |
| Netherite spear, 1.21.11 | 8 jab | Charge: 8 + floor(1.2 × relative closing speed in blocks/s), with Sharpness V and full charge; 20 at 10 blocks/s, 32 at 20 blocks/s | about 0.87/s jab |

The Reef Spear has four-block survival entity reach, 250 durability and no sweeping attack. Netherite sword has 2,031 durability. The newer vanilla spear has a two-block minimum range and a 4.5-block maximum in survival, plus jab mobility and fast-moving/mounted charging. Charge examples require the damage condition and active charge window, and describe raw attack values rather than guaranteed DPS against armored targets.

A Density V critical smash can reach 38.25, 72 and 82.5 at those fall distances when normal critical conditions hold. Breach IV instead reduces armor effectiveness by 60%; Protection still matters. Wind Burst helps set up falls, but positioning and fall risk remain constraints. Neither a falling smash nor a mounted charge has a single stationary melee DPS value.

Java 1.20.1 Impaling checks aquatic mob type rather than wetness: players and drowned receive no Impaling bonus merely by being underwater. The underwater measurements exclude Fire Aspect damage and falling criticals. On land, Reef Spear cannot start its special bleed, although an existing effect continues after leaving water. Its Sharpness V falling critical is 12, compared with the sword's 15, before Fire Aspect.

## Native 1.20.1 measurements

The optional WeaponBalanceAudit calls native Player.attack with fully charged, maximally compatible enchanted items. Fourteen stationary targets are kept in water and separated to avoid sweep interference. Generic targets are drowned with base armor cleared; aquatic targets are guardians. Armored samples wear all four netherite pieces with Protection IV: 20 armor and 12 toughness. Damage comes from native health changes. Samples wait 166 ticks; sustained attacks occur from tick 0 through tick 160 at 18-tick spear/trident or 12-tick sword intervals.

| Underwater target | Reef Spear, one hit then wait | Sword, one hit | Trident, one hit |
| --- | ---: | ---: | ---: |
| Generic, unarmored | 19: 9 direct + 10 bleeding | 11 | 9 |
| Full netherite + Protection IV | about 4.481: 0.881 direct + 3.600 bleeding | about 1.140 | about 0.881 |
| Aquatic mob type, unarmored | same generic spear damage | same generic sword damage | 21.5 |

Sustained samples apply nine spear/trident hits or fourteen sword hits. Unarmored totals are **81 spear, 154 sword, 81 generic trident and 193.5 aquatic trident**. Armored totals are approximately **7.932 spear, 15.967 sword and 7.932 generic trident**. Finite-window totals include the opening hit and are not long-run DPS. Ordinary nominal direct DPS is 9.9 for Reef Spear versus 17.6 for the sword, before tick rounding and defenses.

### Bleed refresh suppresses sustained damage

Repeated same-strength effects refresh their duration to 160 ticks. The current effect pulses only when remaining duration modulo 40 equals 1. Hits every 18 ticks keep resetting the countdown before its first pulse. **The sustained spear sample therefore deals zero bleeding damage during the attack window.** Bleeding can resume after the attacker stops. Each refreshed hit does not add another ten damage: this is one non-stacking status effect.

Bleeding uses magic damage. Armor and toughness do not reduce it, while Protection does. A single maximally enchanted spear hit followed by retreat is unusually efficient against heavy armor: about 3.9 times a sword's single underwater hit after waiting eight seconds. Continuous melee is substantially weaker than the sword, and trident remains better against its Impaling targets.

## Verdict

The spear is **underpowered for continuous endgame melee**, with a useful underwater reach and hit-and-retreat niche. It is not a universal upgrade over a fully enchanted sword, trident or setup-driven mace/vanilla spear. This fits an iron-stage reef reward better than endgame equipment. The armor-bypassing bleed needs care in PvP because material progression is earlier than netherite.

Before increasing damage or enchantment strength, make bleed cadence independent of refresh so sustained combat receives its intended periodic damage. Then measure again against armor. A flat buff now would hide the cadence problem and strengthen the already effective hit-and-retreat case. This review changes no combat stats or effect behavior.

## Reproducing and sources

With Java 17, run `./gradlew.bat -PweaponBalanceTests runGameTestServer`. The optional source set and structure fixture are excluded from production. The report goes to the project's ignored `art/weapon-balance/native-1.20.1.json`; override the `tidalterror.weaponOutput` JVM property for isolated snapshots. Native framebuffer verification also confirms the 3D inventory model and resource reload without the removed 2D sprite.

Current-version values were verified against this mod and the official Forge 47.2.0 mapped Minecraft 1.20.1 classes. Newer formulas were checked in Mojang's official 1.21.11 server JAR and matching mappings: Items, Item.Properties.spear, ToolMaterial, KineticWeapon.damageEntities, Player.attack, Player.stabAttack and MaceItem.getAttackDamageBonus. Those newer formulas were source-checked rather than runtime-tested here.

Official descriptions: [Java 1.21 mace and enchantments](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21), [Java 1.21.11 spear and Lunge](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11). Official downloads and checksums are available through Mojang's [version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json). Downloads, bytecode inspection output, reports and screenshots remain ignored local evidence.
