"""Perspective references from exported native Minecraft faces and shipped textures.
Run the four model verification tasks with -PcoralCrusherModelTests first.
"""
from pathlib import Path
import json
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'art-source/project-banner/fresh-references'
OUT.mkdir(parents=True, exist_ok=True)

def render(name, geometry, texture, camera, roll=0):
    lines = (ROOT / geometry).read_text().splitlines()
    vertices = np.array([[float(x) for x in line.split('|')[-1].split()] for line in lines])
    faces = vertices.reshape(-1, 4, 8)
    tex = np.array(Image.open(ROOT / texture).convert('RGBA'))
    center = (vertices[:, :3].min(0) + vertices[:, :3].max(0)) / 2
    forward = np.array(camera, dtype=float)
    forward /= np.linalg.norm(forward)
    right = np.cross([0, -1, 0], forward)
    right /= np.linalg.norm(right)
    up = np.cross(forward, right)
    angle = np.deg2rad(roll)
    right, up = right*np.cos(angle)+up*np.sin(angle), -right*np.sin(angle)+up*np.cos(angle)
    distance = np.linalg.norm(np.ptp(vertices[:, :3], axis=0)) * 2
    W, H = 1200, 900
    p = vertices[:, :3] - center
    depth = distance - p @ forward
    xy = np.column_stack((p @ right / depth, -p @ up / depth))
    scale = min((W-120)/np.ptp(xy[:, 0]), (H-120)/np.ptp(xy[:, 1]))
    mid = (xy.min(0)+xy.max(0))/2
    canvas = np.full((H,W,3), [21,37,52], dtype=np.uint8)
    zbuf = np.full((H,W), np.inf)
    for face in faces:
        p = face[:, :3] - center
        if face[0, 5:8] @ forward <= 0:
            continue
        d = distance - p @ forward
        s = (np.column_stack((p @ right/d, -p @ up/d))-mid)*scale+[W/2,H/2]
        for ids in ([0,1,2],[0,2,3]):
            q = s[ids]
            lo = np.maximum(np.floor(q.min(0)).astype(int), [0,0])
            hi = np.minimum(np.ceil(q.max(0)).astype(int), [W-1,H-1])
            if np.any(hi < lo):
                continue
            xx,yy = np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
            den = (q[1,1]-q[2,1])*(q[0,0]-q[2,0])+(q[2,0]-q[1,0])*(q[0,1]-q[2,1])
            if abs(den)<1e-9:
                continue
            a = ((q[1,1]-q[2,1])*(xx-q[2,0])+(q[2,0]-q[1,0])*(yy-q[2,1]))/den
            b = ((q[2,1]-q[0,1])*(xx-q[2,0])+(q[0,0]-q[2,0])*(yy-q[2,1]))/den
            c = 1-a-b
            inv = a/d[ids[0]]+b/d[ids[1]]+c/d[ids[2]]
            z = 1/inv
            uv = (a[...,None]*face[ids[0],3:5]/d[ids[0]]+b[...,None]*face[ids[1],3:5]/d[ids[1]]+c[...,None]*face[ids[2],3:5]/d[ids[2]])/inv[...,None]
            rgba = tex[np.clip((uv[...,1]*tex.shape[0]).astype(int),0,tex.shape[0]-1),np.clip((uv[...,0]*tex.shape[1]).astype(int),0,tex.shape[1]-1)]
            region = zbuf[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
            mask = (a>=-1e-6)&(b>=-1e-6)&(c>=-1e-6)&(z<region)&(rgba[...,3]>0)
            region[mask] = z[mask]
            shade = .72+.28*max(0,float(face[0,5:8]@forward))
            canvas[lo[1]:hi[1]+1,lo[0]:hi[0]+1][mask] = (rgba[...,:3]*shade).astype(np.uint8)[mask]
    Image.fromarray(canvas).save(OUT / (name+'.png'))
    return dict(name=name, geometry=geometry, texture=texture, camera=camera, roll=roll, projection='perspective; native baked faces and native UVs')

entries=[]
for name,geo,tex,camera,roll in [
    ('cathedral-ray-underbelly','build/ray-art/native-uv-vertices.txt','src/main/resources/assets/tidalterror/textures/entity/cathedral_ray/cathedral_ray.png',[2,6,-3],-20),
    ('sandy-coral-crusher','build/texture-audit/native-uv-vertices.txt','src/main/resources/assets/tidalterror/textures/entity/coral_crusher/coral_crusher.sandy-v1.png',[-6,-3,-9],0),
    ('shardback','build/shardback-art/native-uv-vertices.txt','src/main/resources/assets/tidalterror/textures/entity/shardback/shardback.png',[-5,-4,-8],0),
    ('veilglow','build/veilglow-art/native-uv-vertices.txt','src/main/resources/assets/tidalterror/textures/entity/veilglow/veilglow.png',[-4,-2,-8],0),
]:
    entries.append(render(name,geo,tex,camera,roll))
(OUT/'render-manifest.json').write_text(json.dumps(entries,indent=2)+'\n')
print('Rendered four textured perspective model references:',OUT)
