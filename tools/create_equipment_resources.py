"""Reproducible acquisition, recipes, recipe unlocks, language and item model resources."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT/'src/main/resources'


def write(relative, value):
    path = RES/relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2)+'\n', encoding='utf-8')


write('data/tidalterror/tags/items/coral_skeletons.json', dict(replace=False, values=[
    'minecraft:dead_'+kind+'_coral_block' for kind in ('tube','brain','bubble','fire','horn')]))
for mob, material in [('coral_crusher','crusher_tooth'), ('shardback','shardback_plate')]:
    path=RES/f'data/tidalterror/loot_tables/entities/{mob}.json'
    table=json.loads(path.read_text())
    table['pools']=[p for p in table['pools'] if not any(e.get('name')==f'tidalterror:{material}' for e in p.get('entries',[]))]
    table['pools'].append(dict(rolls=1, entries=[dict(type='minecraft:item', name=f'tidalterror:{material}', functions=[
        {'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}},
        {'function':'minecraft:looting_enchant','count':{'type':'minecraft:uniform','min':0,'max':1}}])]))
    write(path.relative_to(RES),table)

recipes={
    'reef_spear':([' T ',' LI',' C '], 'crusher_tooth'),
    'reef_helmet':([' P ','PAP',' C '], 'shardback_plate'),
    'reef_chestplate':([' P ','PAP',' C '], 'shardback_plate'),
    'reef_leggings':([' P ','PAP',' C '], 'shardback_plate'),
    'reef_boots':([' P ','PAP',' C '], 'shardback_plate'),
}
ingredients={'T':{'item':'tidalterror:crusher_tooth'},'P':{'item':'tidalterror:shardback_plate'},
             'C':{'tag':'tidalterror:coral_skeletons'},'I':{'item':'minecraft:iron_ingot'},'L':{'item':'minecraft:leather'}}
for name,(pattern,unlock) in recipes.items():
    symbols=set(''.join(pattern)) - {' '}
    if name != 'reef_spear': ingredients['A']={'item':'minecraft:iron_'+name.removeprefix('reef_')}
    write(f'data/tidalterror/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'equipment',
        'pattern':pattern,'key':{s:ingredients[s] for s in sorted(symbols)},'result':{'item':'tidalterror:'+name}})
    write(f'data/tidalterror/advancements/recipes/equipment/{name}.json',{
        'parent':'minecraft:recipes/root','criteria':{
            'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['tidalterror:'+unlock]}]}},
            'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'tidalterror:'+name}}},
        'requirements':[['has_material','has_the_recipe']], 'rewards':{'recipes':['tidalterror:'+name]}})

write('data/tidalterror/recipes/fang_arrow.json', {'type':'minecraft:crafting_shaped','category':'equipment','pattern':['T','S','F'],'key':{'T':{'item':'tidalterror:crusher_tooth'},'S':{'item':'minecraft:stick'},'F':{'item':'minecraft:feather'}},'result':{'item':'tidalterror:fang_arrow','count':4}})
write('data/minecraft/tags/items/arrows.json',{'replace':False,'values':['tidalterror:fang_arrow']})
write('data/tidalterror/advancements/recipes/equipment/fang_arrow.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['tidalterror:crusher_tooth']}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'tidalterror:fang_arrow'}}},'requirements':[['has_material','has_the_recipe']],'rewards':{'recipes':['tidalterror:fang_arrow']}})

for name in ('fang_arrow','crusher_tooth','shardback_plate','reef_helmet','reef_chestplate','reef_leggings','reef_boots'):
    write(f'assets/tidalterror/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'tidalterror:item/'+name}})
# The native spear model renders in every context, including GUI and hotbar.
write('assets/tidalterror/models/item/reef_spear.json',{
    'parent':'builtin/entity','gui_light':'front','textures':{'particle':'tidalterror:item/reef_spear_model'},'display':{
        'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
        'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
        'firstperson_righthand':{'rotation':[-60,15,5],'translation':[0,2,0],'scale':[.45,.45,.45]},
        'firstperson_lefthand':{'rotation':[-60,15,5],'translation':[0,2,0],'scale':[.45,.45,.45]},
        'gui':{'rotation':[0,0,-45],'translation':[0,0,0],'scale':[.3,.3,.3]},
        'ground':{'rotation':[0,0,45],'translation':[0,2,0],'scale':[.3,.3,.3]},
        'fixed':{'rotation':[0,0,-45],'translation':[0,0,0],'scale':[.3,.3,.3]}}})

path=RES/'assets/tidalterror/lang/en_us.json'
lang=json.loads(path.read_text())
for name,label in {'crusher_tooth':'Crusher Tooth','shardback_plate':'Shardback Plate','reef_spear':'Reef Spear',
    'reef_helmet':'Reef Helmet','reef_chestplate':'Reef Chestplate','reef_leggings':'Reef Leggings','reef_boots':'Reef Boots'}.items():
    lang['item.tidalterror.'+name]=label
lang['effect.tidalterror.reef_bleeding']='Reef Bleeding'
lang['tooltip.tidalterror.reef_spear']='Bleed: %s damage over %ss'
lang['enchantment.tidalterror.serration']='Serration'
lang['enchantment.tidalterror.hemorrhage']='Hemorrhage'
lang['tooltip.tidalterror.serration_book']='+%s damage per bleeding pulse'
lang['tooltip.tidalterror.hemorrhage_book']='+%s seconds of bleeding (%s seconds total)'
lang.pop('tooltip.tidalterror.spear_book',None)
lang['item.tidalterror.fang_arrow']='Fang Arrow'
lang['entity.tidalterror.fang_arrow']='Fang Arrow'
lang['tooltip.tidalterror.fang_arrow']='Hits cause 2 bleeding damage over 4 seconds, on land or in water'
lang['tooltip.tidalterror.reef_spear_refresh']='Fully charged; both in water. Refreshes bleed.'
lang['tooltip.tidalterror.reef_spear_repair']='Repair: Crusher Teeth'
lang['tooltip.tidalterror.reef_armor_repair']='Repair with Shardback Plates'
lang['tooltip.tidalterror.reef_armor']='Grounded underwater: 5% less knockback per piece (20% full set)'
write(path.relative_to(RES),lang)
print('Equipment acquisition, recipes, unlocks, language and models generated')
