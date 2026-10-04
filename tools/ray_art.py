"""Assemble exact native UVs from generated materials and render baked model previews.
Usage: python tools/ray_art.py path/to/materials.png [path/to/egg.png]
Native UV export comes from verifyCathedralRayModel. Pillow/NumPy required.
"""
from pathlib import Path
import sys,json,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/ray-art'
ART=ROOT/'art/cathedral-ray'
TEX=ROOT/'src/main/resources/assets/tidalterror/textures/entity/cathedral_ray'
ART.mkdir(parents=True,exist_ok=True);TEX.mkdir(parents=True,exist_ok=True)
SIZE=512

def faces_from(path,tagged=True):
    lines=path.read_text().splitlines();result=[]
    for i in range(0,len(lines),4):
        part=lines[i].split('|')[0] if tagged else ''
        v=np.array([[float(x) for x in (s.split('|')[-1] if tagged else s).split()] for s in lines[i:i+4]])
        result.append(dict(part=part,v=v))
    return result

def dist_segment(x,z,a,b):
    p=np.array([x,z]);a=np.array(a);b=np.array(b);ab=b-a
    t=np.clip((p-a)@ab/(ab@ab),0,1)
    return np.linalg.norm(p-a-t*ab)

branches=[((9,-10),(13,10)),((13,10),(8,16)),((10,-5),(6,-8)),((11,0),(18,-5)),
          ((12,5),(19,1)),((13,9),(20,6)),((9,-10),(13,-13))]
spots=[(6,-10),(8,-1),(10,7),(17,-6),(21,-4),(6,12)]

def assemble(material):
    image=Image.open(material).convert('RGB');image.save(ART/'generated-materials.png')
    w,h=image.size
    tiles=[]
    for x,y in ((0,0),(1,0),(0,1),(1,1)):
        # Sample panel interiors, avoiding any generated boundary pixels.
        tile=image.crop((int((x+.08)*w/2),int((y+.08)*h/2),int((x+.92)*w/2),int((y+.92)*h/2)))
        tiles.append(np.array(tile.resize((16,16),Image.Resampling.NEAREST)))
    atlas=np.zeros((SIZE,SIZE,4),np.uint8);glow=np.zeros_like(atlas);count=np.zeros((SIZE,SIZE),int)
    fs=faces_from(WORK/'native-uv-vertices.txt')
    metadata=[]
    for face in fs:
        v=face['v'];uv=v[:,3:5]*SIZE;n=v[0,5:8]
        x0,y0=np.rint(uv.min(0)).astype(int);x1,y1=np.rint(uv.max(0)).astype(int)
        if x1<=x0 or y1<=y0:continue
        assert 0<=x0<x1<=SIZE and 0<=y0<y1<=SIZE
        count[y0:y1,x0:x1]+=1
        basis=np.column_stack((uv[1]-uv[0],uv[3]-uv[0]))
        inv=np.linalg.inv(basis)
        metadata.append(dict(part=face['part'],rect=[int(x0),int(y0),int(x1),int(y1)],normal=n.tolist()))
        for ty in range(y0,y1):
            for tx in range(x0,x1):
                a,b=inv@(np.array([tx+.5,ty+.5])-uv[0])
                p=(v[0,:3]+a*(v[1,:3]-v[0,:3])+b*(v[3,:3]-v[0,:3]))*16
                x,y,z=p;xabs=abs(x);part=face['part'];wing='left' in part or 'right' in part
                dorsal=n[1]<-.5;belly=n[1]>.5
                tile=2 if belly else (1 if wing and not dorsal else 0)
                lit=False
                front=round(-15+max(0,(xabs-5)/2)*.75)
                back=round(18-max(0,(xabs-5)/2)*1.2)
                if wing and (dorsal or belly) and (z<front+1 or z>back-1 or xabs>36):tile=1
                if part=='/body' and abs(n[1])<.5 and y>22:tile=2
                color=tiles[tile][int(z+50)%16,int(xabs*2+10)%16].copy()
                if dorsal and wing and 'horn' not in part:
                    if min(dist_segment(xabs,z,a,b) for a,b in branches)<.55:
                        color=tiles[3][int(z+50)%16,int(xabs+10)%16]
                    if any(abs(xabs-sx)<.65 and abs(z-sz)<.65 for sx,sz in spots):
                        color=np.array([173,255,215]);lit=True
                if part=='/body' and abs(n[0])>.5 and -15.5<z<-12.5 and 19.5<y<22.5:
                    color=np.array([10,22,39])
                    if -15<z<-13 and 20<y<22:
                        color=np.array([124,244,200]);lit=True
                if part=='/body' and belly:
                    # Paired five gill marks and a front mouth on actual ventral faces.
                    if 1<xabs<3.6 and any(abs(z-g)<.55 for g in (-7,-4,-1,2,5)):
                        color=np.array([73,91,129])
                    if xabs<2 and z<-16:color=np.array([46,46,61])
                atlas[ty,tx]=[*color,255]
                if lit:glow[ty,tx]=[*color,255]
    assert not np.any(count>1),'UV face overlap'
    mask=count>0
    assert np.all(atlas[mask,3]==255) and np.all(atlas[~mask,3]==0)
    assert np.all(glow[~mask,3]==0)
    Image.fromarray(atlas).save(TEX/'cathedral_ray.png')
    Image.fromarray(glow).save(TEX/'cathedral_ray_glow.png')
    Image.fromarray((mask*255).astype(np.uint8)).save(WORK/'uv-mask.png')
    (WORK/'uv-faces.json').write_text(json.dumps(metadata,indent=2))
    print('RAY_TEXTURE PASS',len(fs),'native faces;',int(mask.sum()),'occupied texels; zero UV overlaps; opaque used / transparent unused; glow pixels',int((glow[:,:,3]>0).sum()))
    return fs,atlas

def render(fs,texture,view,W=700,H=520):
    # Parallel camera projections; same mesh and actual native UVs for every view.
    axes={'top':([1,0,0],[0,0,1],[0,-1,0]),'belly':([-1,0,0],[0,0,1],[0,1,0]),
          'front':([1,0,0],[0,1,0],[0,0,-1]),'side':([0,0,1],[0,1,0],[1,0,0]),
          'swim':([1,0,0],[0,.86,.5],[0,-.5,.86])}
    sx,sy,sd=map(np.array,axes[view]);allv=np.concatenate([f['v'][:,:3] for f in fs])
    xx=allv@sx;yy=allv@sy;mid=np.array([(xx.min()+xx.max())/2,(yy.min()+yy.max())/2])
    scale=min((W-80)/max(np.ptp(xx),.1),(H-90)/max(np.ptp(yy),.1))
    canvas=np.zeros((H,W,3),np.uint8);canvas[:]=[231,235,237];buf=np.full((H,W),-np.inf)
    for face in fs:
        v=face['v'];p3=v[:,:3];screen=(np.column_stack((p3@sx,p3@sy))-mid)*scale+[W/2,H/2];depth=p3@sd
        if v[0,5:8]@sd<-.01:continue
        for idx in ((0,1,2),(0,2,3)):
            ids=list(idx);p=screen[ids];lo=np.maximum(np.floor(p.min(0)).astype(int),[0,0]);hi=np.minimum(np.ceil(p.max(0)).astype(int),[W-1,H-1])
            if np.any(hi<lo):continue
            xx,yy=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
            den=(p[1,1]-p[2,1])*(p[0,0]-p[2,0])+(p[2,0]-p[1,0])*(p[0,1]-p[2,1])
            if abs(den)<1e-8:continue
            a=((p[1,1]-p[2,1])*(xx-p[2,0])+(p[2,0]-p[1,0])*(yy-p[2,1]))/den
            b=((p[2,1]-p[0,1])*(xx-p[2,0])+(p[0,0]-p[2,0])*(yy-p[2,1]))/den;c=1-a-b
            z=a*depth[ids[0]]+b*depth[ids[1]]+c*depth[ids[2]]
            uv=a[...,None]*v[ids[0],3:5]+b[...,None]*v[ids[1],3:5]+c[...,None]*v[ids[2],3:5]
            rgba=texture[np.clip((uv[...,1]*SIZE).astype(int),0,SIZE-1),np.clip((uv[...,0]*SIZE).astype(int),0,SIZE-1)]
            region=buf[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
            mask=(a>=-1e-5)&(b>=-1e-5)&(c>=-1e-5)&(z>region)&(rgba[...,3]>0)
            region[mask]=z[mask]
            shade=.8+.2*max(0,float(v[0,5:8]@sd))
            canvas[lo[1]:hi[1]+1,lo[0]:hi[0]+1][mask]=(rgba[...,:3]*shade).astype(np.uint8)[mask]
    return Image.fromarray(canvas)

def previews(fs,atlas):
    board=Image.new('RGB',(1400,1040));font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',24)
    for i,view in enumerate(('top','belly','side','front')):
        pic=render(fs,atlas,view);ImageDraw.Draw(pic).text((20,15),view.upper()+' — native baked model',font=font,fill=(30,35,50))
        board.paste(pic,((i%2)*700,(i//2)*520))
    board.save(ART/'textured-model-preview.png')
    frames=[]
    for i in range(48):frames.append(render(faces_from(WORK/f'frame-{i:02}.txt',False),atlas,'swim',600,360))
    frames[0].save(ART/'cathedral-ray-swim.gif',save_all=True,append_images=frames[1:],duration=42,loop=0)

if __name__=='__main__':
    fs,atlas=assemble(sys.argv[1]);previews(fs,atlas)
    if len(sys.argv)>2:
        egg=Image.open(sys.argv[2]).convert('RGBA')
        egg.save(ART/'generated-spawn-egg.png')
        egg=egg.resize((32,32),Image.Resampling.NEAREST);a=np.array(egg);a[:,:,3]=np.where(a[:,:,3]>=128,255,0);a[a[:,:,3]==0,:3]=0
        egg=Image.fromarray(a);egg.save(ROOT/'src/main/resources/assets/tidalterror/textures/item/cathedral_ray_spawn_egg.png')
        egg.resize((512,512),Image.Resampling.NEAREST).save(ART/'spawn-egg-preview.png')
        assert set(np.unique(a[:,:,3]))<={0,255}
        print('RAY_EGG PASS 32x32, binary alpha')
