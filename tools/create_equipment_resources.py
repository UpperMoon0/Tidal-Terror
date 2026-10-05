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
    'reef_helmet':(['PPP','ICI'], 'shardback_plate'),
    'reef_chestplate':(['P P','PCP','PIP'], 'shardback_plate'),
    'reef_leggings':(['PCP','PIP','P P'], 'shardback_plate'),
    'reef_boots':(['P P','ICI'], 'shardback_plate'),
}
ingredients={'T':{'item':'tidalterror:crusher_tooth'},'P':{'item':'tidalterror:shardback_plate'},
             'C':{'tag':'tidalterror:coral_skeletons'},'I':{'item':'minecraft:iron_ingot'},'L':{'item':'minecraft:leather'}}
for name,(pattern,unlock) in recipes.items():
    symbols=set(''.join(pattern)) - {' '}
    write(f'data/tidalterror/recipes/{name}.json',{'type':'minecraft:crafting_shaped','category':'equipment',
        'pattern':pattern,'key':{s:ingredients[s] for s in sorted(symbols)},'result':{'item':'tidalterror:'+name}})
    write(f'data/tidalterror/advancements/recipes/equipment/{name}.json',{
        'parent':'minecraft:recipes/root','criteria':{
            'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['tidalterror:'+unlock]}]}},
            'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'tidalterror:'+name}}},
        'requirements':[['has_material','has_the_recipe']], 'rewards':{'recipes':['tidalterror:'+name]}})

for name in ('crusher_tooth','shardback_plate','reef_helmet','reef_chestplate','reef_leggings','reef_boots'):
    write(f'assets/tidalterror/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'tidalterror:item/'+name}})
# Native spear model in-hand; exported native icon in inventory using Forge's separate transforms.
write('assets/tidalterror/models/item/reef_spear.json',{
    'loader':'forge:separate_transforms',
    'base':{'parent':'builtin/entity','gui_light':'front','textures':{'particle':'tidalterror:item/reef_spear'},'display':{
        'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
        'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.8,.8,.8]},
        'firstperson_righthand':{'rotation':[-60,15,5],'translation':[0,2,0],'scale':[.45,.45,.45]},
        'firstperson_lefthand':{'rotation':[-60,15,5],'translation':[0,2,0],'scale':[.45,.45,.45]},
        'ground':{'rotation':[0,0,45],'translation':[0,2,0],'scale':[.3,.3,.3]},
        'fixed':{'rotation':[0,0,45],'translation':[0,0,0],'scale':[.3,.3,.3]}}},
    'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'tidalterror:item/reef_spear'}}}})

path=RES/'assets/tidalterror/lang/en_us.json'
lang=json.loads(path.read_text())
for name,label in {'crusher_tooth':'Crusher Tooth','shardback_plate':'Shardback Plate','reef_spear':'Reef Spear',
    'reef_helmet':'Reef Helmet','reef_chestplate':'Reef Chestplate','reef_leggings':'Reef Leggings','reef_boots':'Reef Boots'}.items():
    lang['item.tidalterror.'+name]=label
lang['effect.tidalterror.reef_bleeding']='Reef Bleeding'
lang['tooltip.tidalterror.reef_spear']='Fully charged underwater hits: %s bleeding damage over %s seconds'
lang['enchantment.tidalterror.serration']='Serration'
lang['enchantment.tidalterror.hemorrhage']='Hemorrhage'
lang['tooltip.tidalterror.serration_book']='+%s damage per bleeding pulse'
lang['tooltip.tidalterror.hemorrhage_book']='+%s seconds of bleeding (%s seconds total)'
lang['tooltip.tidalterror.spear_book']='Reef Spear: fully charged underwater hits only'
lang['tooltip.tidalterror.reef_armor']='5% less knockback per piece while grounded underwater'
write(path.relative_to(RES),lang)
print('Equipment acquisition, recipes, unlocks, language and models generated')
