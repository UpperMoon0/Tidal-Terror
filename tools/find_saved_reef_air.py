from pathlib import Path
from functools import lru_cache
import io,struct,zlib,nbtlib
world=Path('build/reef-preview-v3/saves/Coral Cathedral Preview')
@lru_cache(None)
def chunk(cx,cz):
 b=(world/f'region/r.{cx//32}.{cz//32}.mca').read_bytes();i=4*((cx%32)+(cz%32)*32)
 off=int.from_bytes(b[i:i+3],'big')*4096;size=struct.unpack_from('>I',b,off)[0]
 return nbtlib.File.parse(io.BytesIO(zlib.decompress(b[off+5:off+4+size])))
def state(x,y,z):
 c=chunk(x//16,z//16);s=next(s for s in c['sections'] if int(s['Y'])==y//16);v=s['block_states'];p=v['palette']
 if len(p)==1:return str(p[0]['Name'])
 bits=max(4,(len(p)-1).bit_length());per=64//bits;i=(y%16)*256+(z%16)*16+x%16
 number=int(v['data'][i//per])&((1<<64)-1);return str(p[(number>>((i%per)*bits))&((1<<bits)-1)]['Name'])
for x in range(41,90):
 for z in range(-44,5):
  for y in range(-46,-9):
   if state(x,y,z)=='minecraft:air' and all(state(x+a,y+b,z+d)=='minecraft:water' for a,b,d in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]):
    print('AIR',x,y,z,'pending',chunk(x//16,z//16).get('tidalterror:unfinished_reef_water'))
