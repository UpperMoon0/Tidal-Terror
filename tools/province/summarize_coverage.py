"""Summarize a native spatial survey, keeping vanilla and final-ocean denominators distinct."""
import argparse
import hashlib
import json
import math
from pathlib import Path


def summarize(cells,spacing=12288):
    names=('samples','vanilla_ocean','province_ocean','province','converted_non_ocean','cathedral','cathedral_ocean')
    total={name:sum(c[name] for c in cells) for name in names}
    total['cells']=len(cells)
    total['accepted']=sum(c['accepted'] for c in cells)
    assert total['province']==total['province_ocean']+total['converted_non_ocean']
    assert total['cathedral_ocean']<=total['province_ocean']<=total['vanilla_ocean']<=total['samples']
    final_ocean=total['vanilla_ocean']+total['converted_non_ocean']
    total['percent']={
        'vanilla_ocean_replaced':100*total['province_ocean']/total['vanilla_ocean'],
        'cathedral_of_vanilla_ocean':100*total['cathedral_ocean']/total['vanilla_ocean'],
        'province_of_final_ocean_domain':100*total['province']/final_ocean,
        'province_of_total_map':100*total['province']/total['samples'],
        'vanilla_ocean_of_total_map':100*total['vanilla_ocean']/total['samples'],
        'converted_non_ocean_of_province':100*total['converted_non_ocean']/total['province'],
        'candidate_acceptance':100*total['accepted']/total['cells'],
    }
    if 'fine_province' in cells[0]:
        fine={name:sum(c[name] for c in cells) for name in
              ('fine_strata','fine_province','fine_province_ocean','fine_cathedral','fine_cathedral_ocean')}
        coarse_fraction=total['vanilla_ocean']/total['samples']
        province_fraction=fine['fine_province']/fine['fine_strata']
        covered_fraction=fine['fine_province_ocean']/fine['fine_strata']
        converted_fraction=province_fraction-covered_fraction
        assert fine['fine_cathedral_ocean']<=fine['fine_province_ocean']<=fine['fine_province']
        total['fine_counts']=fine
        total['refined_percent']={
            'vanilla_ocean_replaced':100*covered_fraction/coarse_fraction,
            'cathedral_of_vanilla_ocean':100*fine['fine_cathedral_ocean']/fine['fine_strata']/coarse_fraction,
            'province_of_ocean_plus_province_domain':100*province_fraction/(coarse_fraction+converted_fraction),
            'province_of_total_map':100*province_fraction,
            'converted_non_ocean_of_province':100*converted_fraction/province_fraction,
            'analytical_province_of_total_map':100*math.pi*(600*1024/180)**2*(1+(.075**2+.035**2)/2)/spacing**2*total['accepted']/total['cells'],
        }
    return total


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('input',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    raw=json.loads(args.input.read_text())
    spacings={row.get('spacing',12288) for row in raw}
    assert len(spacings)==1,'Cannot pool different cell areas'
    spacing=spacings.pop()
    summary={'method':'Native Forge 1.20.1 delegate ocean tag at quart Y=8; actual ReefProvinceBiomeSource acceptance; 16x16 jittered strata per placement cell. When present, refined province intersections use independent 128x128 jittered strata in accepted cells, with equal cell-area weights.',
             'raw_sha256':hashlib.sha256(args.input.read_bytes()).hexdigest(),
             'spacing':spacing,
             'seeds':[{'seed':row['seed'],**summarize(row['cells'],spacing)} for row in raw],
             'pooled':summarize([cell for row in raw for cell in row['cells']],spacing)}
    args.output.write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2))


if __name__=='__main__':
    main()
