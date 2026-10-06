package com.nhat.tidal_terror.gametest;

import com.nhat.tidal_terror.items.ModEquipment;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Opt-in native damage measurements; deliberately does not change combat balance. */
@GameTestHolder("tidalterror") @PrefixGameTestTemplate(false)
public final class WeaponBalanceAudit {
    private record Sample(String weapon, boolean armor, boolean aquatic, boolean sustained, Player player, Mob target, Vec3 position, float start, double[] direct, int interval) {}
    @GameTest(template="reef_life_pool",timeoutTicks=200)
    public static void measureFullyEnchantedUnderwaterWeapons(GameTestHelper h) {
        for(int x=0;x<=23;x++)for(int z=0;z<=23;z++)for(int y=3;y<=8;y++)h.setBlock(x,y,z,y==3?Blocks.SANDSTONE:Blocks.WATER);
        List<Sample> samples=new ArrayList<>();
        for(var item:List.of(ModEquipment.REEF_SPEAR.get(),Items.IRON_SWORD,Items.NETHERITE_SWORD,Items.TRIDENT))
            for(boolean armor:List.of(false,true))for(boolean sustained:List.of(false,true))samples.add(sample(h,item,armor,false,sustained,samples.size()));
        samples.add(sample(h,Items.TRIDENT,false,true,false,samples.size()));samples.add(sample(h,Items.TRIDENT,false,true,true,samples.size()));
        for(var s:samples)attack(s);
        for(int tick=1;tick<=165;tick++) {int now=tick;h.runAtTickTime(now,()->{
            for(var s:samples) {s.target.setPos(s.position);s.target.setDeltaMovement(Vec3.ZERO);
                if(s.sustained&&now<=160&&now%s.interval==0)attack(s);}
        });}
        h.runAtTickTime(166,()->{
            List<Map<String,Object>> rows=new ArrayList<>();
            for(var s:samples) {
                double total=s.start-s.target.getHealth();Map<String,Object> row=new LinkedHashMap<>();
                row.put("weapon",s.weapon);row.put("full_netherite_protection_iv",s.armor);row.put("aquatic_mob_type",s.aquatic);row.put("sustained",s.sustained);
                row.put("armor_points",s.target.getArmorValue());row.put("armor_toughness",s.target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));row.put("direct_damage",s.direct[0]);row.put("total_damage",total);row.put("extra_damage",total-s.direct[0]);row.put("attack_interval_ticks",s.interval);
                rows.add(row);System.out.println("WEAPON_BALANCE "+row);s.target.discard();
            }
            try {var out=Path.of(System.getProperty("tidalterror.weaponOutput","../../art/weapon-balance/native-1.20.1.json"));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));}
            catch(Exception e){throw new RuntimeException(e);}h.succeed();
        });
    }
    private static Sample sample(GameTestHelper h,Item item,boolean armor,boolean aquatic,boolean sustained,int index) {
        var p=h.makeMockSurvivalPlayer();int x=3+(index%4)*5,z=3+(index/4)*5;var pos=h.absolutePos(new BlockPos(x,4,z));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);p.setNoGravity(true);
        var stack=new ItemStack(item);stack.enchant(Enchantments.UNBREAKING,3);stack.enchant(Enchantments.MENDING,1);
        if(item==Items.TRIDENT){stack.enchant(Enchantments.IMPALING,5);stack.enchant(Enchantments.LOYALTY,3);stack.enchant(Enchantments.CHANNELING,1);}
        else {stack.enchant(Enchantments.MOB_LOOTING,3);stack.enchant(Enchantments.KNOCKBACK,2);
            if(item instanceof SwordItem){stack.enchant(Enchantments.SHARPNESS,5);stack.enchant(Enchantments.FIRE_ASPECT,2);stack.enchant(Enchantments.SWEEPING_EDGE,3);}
            else {stack.enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.SERRATION.get(),3);stack.enchant(com.nhat.tidal_terror.enchantments.ModEnchantments.HEMORRHAGE.get(),2);}}
        p.setItemSlot(EquipmentSlot.MAINHAND,stack);
        Mob target=aquatic?h.spawn(EntityType.GUARDIAN,x+2,4,z):h.spawn(EntityType.DROWNED,x+2,4,z);target.setNoAi(true);target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.getAttribute(Attributes.ARMOR).setBaseValue(0);target.setHealth(1000);
        if(armor) {var gear=new Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS};var slots=new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
            for(int i=0;i<4;i++){var piece=new ItemStack(gear[i]);piece.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);target.setItemSlot(slots[i],piece);}}
        target.tick();return new Sample(item==Items.TRIDENT?"Trident Impaling V":item==Items.NETHERITE_SWORD?"Netherite Sword Sharpness V":item==Items.IRON_SWORD?"Iron Sword Sharpness V":"Reef Spear Serration III Hemorrhage II",armor,aquatic,sustained,p,target,target.position(),target.getHealth(),new double[]{0},item instanceof SwordItem?12:18);
    }
    private static void attack(Sample s) {
        for(int i=0;i<25;i++){s.player.setDeltaMovement(Vec3.ZERO);s.player.tick();}
        float before=s.target.getHealth();s.player.attack(s.target);s.direct[0]+=before-s.target.getHealth();
    }
}
