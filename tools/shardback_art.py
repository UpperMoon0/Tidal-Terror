"""Map generated material swatches onto exact baked Shardback faces."""
from pathlib import Path
import sys,json
import numpy as np
from PIL import Image,ImageDraw,ImageFont
import ray_art
ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/shardback-art';ART=ROOT/'art/shardback'
TEX=ROOT/'src/main/resources/assets/tidalterror/textures/entity/shardback'
for p in (WORK,ART,TEX):p.mkdir(parents=True,exist_ok=True)
if __name__=='__main__':
    source=Image.open(sys.argv[1]).convert('RGB');w,h=source.size
    tiles=[]
    for x,y in ((0,0),(1,0),(0,1),(1,1)):
        tiles.append(np.array(source.crop((int((x+.2)*w/2),int((y+.2)*h/2),int((x+.8)*w/2),int((y+.8)*h/2))).resize((16,16),Image.Resampling.NEAREST)))
    ray_art.SIZE=256
    faces=ray_art.faces_from(WORK/'native-uv-vertices.txt')
    atlas=np.zeros((256,256,4),np.uint8);count=np.zeros((256,256),np.uint8)
    for face in faces:
        v=face['v'];uv=v[:,3:5]*256
        x0,y0=np.rint(uv.min(0)).astype(int);x1,y1=np.rint(uv.max(0)).astype(int)
        if x1<=x0 or y1<=y0:continue
        count[y0:y1,x0:x1]+=1;part=face['part']
        violet='/shell' in part or 'palm' in part
        cobalt='/coral' in part
        slate='tip' in part
        tile=1 if cobalt else 0 if violet else 3 if slate else 2
        inv=np.linalg.inv(np.column_stack((uv[1]-uv[0],uv[3]-uv[0])))
        for ty in range(y0,y1):
            for tx in range(x0,x1):
                a,b=inv@(np.array([tx+.5,ty+.5])-uv[0]);p=(v[0,:3]+a*(v[1,:3]-v[0,:3])+b*(v[3,:3]-v[0,:3]))*16
                color=tiles[tile][int(p[1]+40)%16,int(p[0]+p[2]+40)%16].copy()
                if 'palm' in part and 'fixed' not in part and 'finger' not in part and (int(p[0]+p[1])%11)<3:
                    color=tiles[1][ty%16,tx%16]
                # Claw fingers stay ivory rather than inheriting their parent's purple.
                if 'fixed' in part or 'finger' in part:color=tiles[2][ty%16,tx%16]
                if 'leg' in part and 'lower' not in part and (tx-x0)<2:color=tiles[3][ty%16,tx%16]
                if 'pupil' in part:
                    color=np.array([25,23,31])
                    if tx==x0 and ty==y0:color=np.array([238,230,211])
                atlas[ty,tx]=[*color,255]
    assert not np.any(count>1),'Overlapping native UV islands'
    mask=count>0;assert np.all(atlas[mask,3]==255) and np.all(atlas[~mask,3]==0)
    Image.fromarray(atlas).save(TEX/'shardback.png')
    Image.fromarray((mask*255).astype(np.uint8)).save(WORK/'uv-mask.png')
    print('SHARDBACK_TEXTURE PASS',len(faces),'faces;',int(mask.sum()),'texels; zero overlap; occupied opaque')
    board=Image.new('RGB',(1400,1040));font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',22)
    for i,view in enumerate(('front','side','top','belly')):
        pic=ray_art.render(faces,atlas,view);ImageDraw.Draw(pic).text((15,15),view.upper()+' — native baked Shardback',font=font,fill=(30,35,50));board.paste(pic,((i%2)*700,(i//2)*520))
    board.save(ART/'native-shape-preview.png')
    frames=[ray_art.render(ray_art.faces_from(WORK/f'frame-{i:02}.txt',False),atlas,'front',420,520) for i in range(48)]
    frames[0].save(ART/'shardback-animation.gif',save_all=True,append_images=frames[1:],duration=42,loop=0)
    egg=Image.open(sys.argv[2]).convert('RGBA').resize((32,32),Image.Resampling.NEAREST)
    arr=np.array(egg);arr[:,:,3]=np.where(arr[:,:,3]>=128,255,0);arr[arr[:,:,3]==0,:3]=0
    egg=Image.fromarray(arr);egg.save(ROOT/'src/main/resources/assets/tidalterror/textures/item/shardback_spawn_egg.png')
    egg.resize((512,512),Image.Resampling.NEAREST).save(ART/'spawn-egg-preview.png')
    print('SHARDBACK_EGG PASS 32x32 binary alpha')
