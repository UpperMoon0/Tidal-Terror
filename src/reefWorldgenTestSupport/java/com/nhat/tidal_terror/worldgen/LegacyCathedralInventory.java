package com.nhat.tidal_terror.worldgen;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** Frozen published 0.0.3 inventory (133c112): native features, carvers and every spawn table. */
final class LegacyCathedralInventory {
    private static final String EXPECTED="""
{
  "features": [
    [
      "tidalterror:reef_basin"
    ],
    [
      "minecraft:lake_lava_underground",
      "minecraft:lake_lava_surface"
    ],
    [],
    [
      "minecraft:monster_room",
      "minecraft:monster_room_deep"
    ],
    [],
    [],
    [
      "minecraft:ore_dirt",
      "minecraft:ore_gravel",
      "minecraft:ore_granite_upper",
      "minecraft:ore_granite_lower",
      "minecraft:ore_diorite_upper",
      "minecraft:ore_diorite_lower",
      "minecraft:ore_andesite_upper",
      "minecraft:ore_andesite_lower",
      "minecraft:ore_tuff",
      "minecraft:ore_coal_upper",
      "minecraft:ore_coal_lower",
      "minecraft:ore_iron_upper",
      "minecraft:ore_iron_middle",
      "minecraft:ore_iron_small",
      "minecraft:ore_gold",
      "minecraft:ore_gold_lower",
      "minecraft:ore_redstone",
      "minecraft:ore_redstone_lower",
      "minecraft:ore_diamond",
      "minecraft:ore_diamond_large",
      "minecraft:ore_diamond_buried",
      "minecraft:ore_lapis",
      "minecraft:ore_lapis_buried",
      "minecraft:ore_copper",
      "minecraft:disk_sand",
      "minecraft:disk_clay",
      "minecraft:disk_gravel"
    ],
    [],
    [
      "minecraft:spring_water",
      "minecraft:spring_lava"
    ],
    [
      "minecraft:trees_water",
      "minecraft:flower_default",
      "minecraft:patch_grass_badlands",
      "minecraft:brown_mushroom_normal",
      "minecraft:red_mushroom_normal",
      "minecraft:patch_sugar_cane",
      "minecraft:patch_pumpkin",
      "tidalterror:reef_garden"
    ],
    [
      "minecraft:freeze_top_layer",
      "tidalterror:giant_coral"
    ]
  ],
  "carvers": {
    "air": [
      "minecraft:cave",
      "minecraft:cave_extra_underground",
      "minecraft:canyon"
    ]
  },
  "spawners": {
    "ambient": [
      {
        "type": "minecraft:bat",
        "maxCount": 8,
        "minCount": 8,
        "weight": 10
      }
    ],
    "axolotls": [],
    "creature": [
      {
        "type": "minecraft:turtle",
        "maxCount": 2,
        "minCount": 1,
        "weight": 3
      }
    ],
    "misc": [],
    "monster": [
      {
        "type": "minecraft:drowned",
        "maxCount": 1,
        "minCount": 1,
        "weight": 1
      },
      {
        "type": "minecraft:spider",
        "maxCount": 4,
        "minCount": 4,
        "weight": 100
      },
      {
        "type": "minecraft:zombie",
        "maxCount": 4,
        "minCount": 4,
        "weight": 95
      },
      {
        "type": "minecraft:zombie_villager",
        "maxCount": 1,
        "minCount": 1,
        "weight": 5
      },
      {
        "type": "minecraft:skeleton",
        "maxCount": 4,
        "minCount": 4,
        "weight": 100
      },
      {
        "type": "minecraft:creeper",
        "maxCount": 4,
        "minCount": 4,
        "weight": 100
      },
      {
        "type": "minecraft:slime",
        "maxCount": 4,
        "minCount": 4,
        "weight": 100
      },
      {
        "type": "minecraft:enderman",
        "maxCount": 4,
        "minCount": 1,
        "weight": 10
      },
      {
        "type": "minecraft:witch",
        "maxCount": 1,
        "minCount": 1,
        "weight": 5
      }
    ],
    "tidalterror_crusher": [
      {
        "type": "tidalterror:coral_crusher",
        "maxCount": 1,
        "minCount": 1,
        "weight": 2
      }
    ],
    "tidalterror_ray": [
      {
        "type": "tidalterror:cathedral_ray",
        "maxCount": 3,
        "minCount": 2,
        "weight": 6
      }
    ],
    "tidalterror_shardback": [
      {
        "type": "tidalterror:shardback",
        "maxCount": 3,
        "minCount": 1,
        "weight": 10
      }
    ],
    "tidalterror_veilglow": [
      {
        "type": "tidalterror:veilglow",
        "maxCount": 4,
        "minCount": 2,
        "weight": 8
      }
    ],
    "underground_water_creature": [
      {
        "type": "minecraft:glow_squid",
        "maxCount": 6,
        "minCount": 4,
        "weight": 10
      }
    ],
    "water_ambient": [
      {
        "type": "minecraft:tropical_fish",
        "maxCount": 8,
        "minCount": 8,
        "weight": 25
      },
      {
        "type": "minecraft:pufferfish",
        "maxCount": 3,
        "minCount": 1,
        "weight": 15
      }
    ],
    "water_creature": [
      {
        "type": "minecraft:squid",
        "maxCount": 4,
        "minCount": 4,
        "weight": 10
      },
      {
        "type": "minecraft:dolphin",
        "maxCount": 2,
        "minCount": 1,
        "weight": 2
      }
    ]
  }
}
""";
    static void verify(ServerLevel level) {
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(ReefWorldgen.BIOME).value();
        var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var actual=Biome.DIRECT_CODEC.encodeStart(ops,biome).result().orElseThrow().getAsJsonObject();
        var expected=JsonParser.parseString(EXPECTED).getAsJsonObject();
        for(String key:java.util.List.of("features","carvers","spawners"))
            if(!expected.get(key).equals(actual.get(key)))throw new AssertionError("Legacy Cathedral inventory changed: "+key);
        System.out.println("DEEP_LEGACY_INVENTORY_PASS reference=133c112 features/carvers/spawners");
    }
}
