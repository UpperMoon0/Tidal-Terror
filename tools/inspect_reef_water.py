"""Read saved native chunk palettes and pending ticks at diagnostic points."""
from pathlib import Path
import io, struct, zlib, nbtlib
world=Path('build/reef-native-test-v3d/reef-audit-world')
def chunk(x,z):
    cx,cz=x//16,z//16
    b=(world/f'region/r.{cx//32}.{cz//32}.mca').read_bytes()
    offset=int.from_bytes(b[4*((cx%32)+(cz%32)*32):4*((cx%32)+(cz%32)*32)+3],'big')*4096
    size=struct.unpack_from('>I',b,offset)[0]
    return nbtlib.File.parse(io.BytesIO(zlib.decompress(b[offset+5:offset+4+size])))
def palette(section,key,index):
    value=section[key];p=value['palette']
    if len(p)==1:return p[0]
    bits=max(4 if key=='block_states' else 1,(len(p)-1).bit_length());per=64//bits
    number=int(value['data'][index//per])&((1<<64)-1)
    return p[(number>>((index%per)*bits))&((1<<bits)-1)]
for x,y,z in [(42,-15,-1),(47,-13,-42),(47,-15,-37)]:
    c=chunk(x,z);s=next(s for s in c['sections'] if int(s['Y'])==y//16)
    print((x,y,z),'biome',palette(s,'biomes',((y%16)//4)*16+((z%16)//4)*4+(x%16)//4))
    for dx,dy,dz in [(0,0,0),(0,-1,0),(0,1,0),(-1,0,0),(1,0,0),(0,0,-1),(0,0,1)]:
        a,b,d=x+dx,y+dy,z+dz;n=chunk(a,d);t=next(t for t in n['sections'] if int(t['Y'])==b//16)
        print((a,b,d),palette(t,'block_states',(b%16)*256+(d%16)*16+a%16))
    print('ticks',c.get('block_ticks'),c.get('fluid_ticks'))
