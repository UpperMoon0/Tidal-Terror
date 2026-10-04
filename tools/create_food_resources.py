"""Create the vanilla data/model contracts for the four reef mob foods."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
FOODS = [('coral_crusher', 'coral_crusher_steak', 'Coral Crusher Steak', 2, 4),
         ('cathedral_ray', 'cathedral_ray_wing', 'Cathedral Ray Wing', 1, 3),
         ('veilglow', 'veilglow_gel', 'Veilglow Gel', 1, 2),
         ('shardback', 'shardback_claw', 'Shardback Claw', 1, 2)]

def write(relative, value):
    path = ROOT / 'src/main/resources' / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

lang_path = ROOT / 'src/main/resources/assets/tidalterror/lang/en_us.json'
lang = json.loads(lang_path.read_text(encoding='utf-8'))
for mob, food, label, minimum, maximum in FOODS:
    for state in ('raw', 'cooked'):
        item = f'{state}_{food}'
        lang[f'item.tidalterror.{item}'] = f'{state.title()} {label}'
        write(f'assets/tidalterror/models/item/{item}.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': f'tidalterror:item/{item}'}})
    write(f'data/tidalterror/loot_tables/entities/{mob}.json', {
        'type': 'minecraft:entity', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': f'tidalterror:raw_{food}', 'functions': [
                {'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': minimum, 'max': maximum}},
                {'function': 'minecraft:furnace_smelt', 'conditions': [{'condition': 'minecraft:entity_properties',
                    'entity': 'this', 'predicate': {'flags': {'is_on_fire': True}}}]},
                {'function': 'minecraft:looting_enchant', 'count': {'type': 'minecraft:uniform', 'min': 0, 'max': 1}}
            ]}]}]})
    for method, ticks in [('smelting', 200), ('smoking', 100), ('campfire_cooking', 600)]:
        recipe = f'cooked_{food}_from_{method}'
        write(f'data/tidalterror/recipes/{recipe}.json', {
            'type': f'minecraft:{method}', 'category': 'food',
            'ingredient': {'item': f'tidalterror:raw_{food}'},
            'result': f'tidalterror:cooked_{food}', 'experience': 0.35, 'cookingtime': ticks})
        write(f'data/tidalterror/advancements/recipes/food/{recipe}.json', {
            'parent': 'minecraft:recipes/root', 'criteria': {
                'has_food': {'trigger': 'minecraft:inventory_changed', 'conditions': {
                    'items': [{'items': [f'tidalterror:raw_{food}']}]}},
                'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': f'tidalterror:{recipe}'} }
            }, 'requirements': [['has_food', 'has_the_recipe']],
            'rewards': {'recipes': [f'tidalterror:{recipe}']}})
lang_path.write_text(json.dumps(lang, indent=2) + '\n', encoding='utf-8')
print('Created 8 item models, 4 loot tables, 12 cooking recipes and 12 unlock advancements.')
