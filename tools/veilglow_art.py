"""Reconstruct Veilglow artwork on exact native baked faces; retain inputs."""
from pathlib import Path
import sys,json,shutil
import numpy as np
from PIL import Image,ImageDraw,ImageFont
import ray_art
ROOT=Path(__file__).resolve().parents[1];WORK=ROOT/'build/veilglow-art';ART=ROOT/'art/veilglow'
SOURCE=ROOT/'art-source/veilglow';TEX=ROOT/'src/main/resources/assets/tidalterror/textures/entity/veilglow'
for p in (ART,SOURCE,TEX):p.mkdir(parents=True,exist_ok=True)
SIZE=256
def assemble(path):
    source=Image.open(path).convert('RGB');source.save(SOURCE/'materials-v1.png');w,h=source.size
    tiles=[]
    for x,y in ((0,0),(1,0),(0,1),(1,1)):
        tiles.append(np.array(source.crop((int((x+.08)*w/2),int((y+.08)*h/2),int((x+.92)*w/2),int((y+.92)*h/2))).resize((16,16),Image.Resampling.NEAREST)))
    atlas=np.zeros((SIZE,SIZE,4),np.uint8);glow=np.zeros_like(atlas);count=np.zeros((SIZE,SIZE),int)
    faces=ray_art.faces_from(WORK/'native-uv-vertices.txt');metadata=[]
    for face in faces:
        v=face['v'];uv=v[:,3:5]*SIZE
        x0,y0=np.rint(uv.min(0)).astype(int);x1,y1=np.rint(uv.max(0)).astype(int)
        if x1<=x0 or y1<=y0:continue
        count[y0:y1,x0:x1]+=1;part=face['part'];shell='/bell' in part;core='/core' in part;rim='/rim' in part
        tile=0 if shell else 3 if core else 1 if rim else 2
        inv=np.linalg.inv(np.column_stack((uv[1]-uv[0],uv[3]-uv[0])))
        for ty in range(y0,y1):
            for tx in range(x0,x1):
                a,b=inv@(np.array([tx+.5,ty+.5])-uv[0]);p=(v[0,:3]+a*(v[1,:3]-v[0,:3])+b*(v[3,:3]-v[0,:3]))*16
                color=tiles[tile][int(p[1]+40)%16,int(p[0]+p[2]+40)%16].copy();lit=core
                if rim and (tx-x0)%4==1 and (ty-y0)%3==1:color=np.array([155,235,209]);lit=True
                if '/ribbon' in part and part.endswith('_5'):
                    color=tiles[3][int(p[1]+40)%16,int(p[0]+40)%16];lit=True
                atlas[ty,tx]=[*color,78 if shell else 255]
                if lit:glow[ty,tx]=[*np.minimum(color,215),255]
        metadata.append(dict(part=part,rect=[int(x0),int(y0),int(x1),int(y1)],translucent=shell))
    assert not np.any(count>1),'Native UV islands overlap'
    mask=count>0;assert np.all(atlas[mask,3]>0) and np.all(atlas[~mask,3]==0)
    assert np.all(glow[~mask,3]==0) and np.any(atlas[:,:,3]==78)
    Image.fromarray(atlas).save(TEX/'veilglow.png');Image.fromarray(glow).save(TEX/'veilglow_glow.png')
    Image.fromarray((mask*255).astype(np.uint8)).save(WORK/'uv-mask.png')
    (WORK/'uv-faces.json').write_text(json.dumps(metadata,indent=2))
    print('VEILGLOW_TEXTURE PASS',len(faces),'native faces;',int(mask.sum()),'used texels; no overlaps; glass alpha 78; unused transparent')
    return faces,atlas
if __name__=='__main__':
    fs,atlas=assemble(sys.argv[1]);ray_art.SIZE=256
    # Software shape reference uses an opaque approximation of glass. Actual game
    # captures establish transparency/core visibility under native render layers.
    preview=atlas.copy();preview[preview[:,:,3]>0,3]=255
    board=Image.new('RGB',(1400,1040));font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',22)
    for i,view in enumerate(('front','side','top','belly')):
        pic=ray_art.render(fs,preview,view);ImageDraw.Draw(pic).text((15,15),view.upper()+' — native geometry / glass shown opaque',font=font,fill=(30,35,50));board.paste(pic,((i%2)*700,(i//2)*520))
    board.save(ART/'native-shape-preview.png')
    frames=[ray_art.render(ray_art.faces_from(WORK/f'frame-{i:02}.txt',False),preview,'front',420,520) for i in range(48)]
    frames[0].save(ART/'veilglow-animation.gif',save_all=True,append_images=frames[1:],duration=42,loop=0)
    if len(sys.argv)>2:
        image=Image.open(sys.argv[2]).convert('RGBA');image.save(SOURCE/'spawn-egg-source-v1.png');image=image.resize((32,32),Image.Resampling.NEAREST)
        a=np.array(image);a[:,:,3]=np.where(a[:,:,3]>=128,255,0);a[a[:,:,3]==0,:3]=0
        image=Image.fromarray(a);image.save(ROOT/'src/main/resources/assets/tidalterror/textures/item/veilglow_spawn_egg.png')
        image.resize((512,512),Image.Resampling.NEAREST).save(ART/'spawn-egg-preview.png')
        print('VEILGLOW_EGG PASS 32x32 / binary alpha')
