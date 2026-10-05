"""Assign ImageGen materials to exported native faces and render matching item icons.

No atlas guessing: the actual baked model supplies every occupied UV rectangle.
"""
from pathlib import Path
import json
import sys
import numpy as np
from PIL import Image, ImageDraw
import ray_art

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/equipment-art'
ART=ROOT/'art/reef-equipment'
RES=ROOT/'src/main/resources/assets/tidalterror'
SIZE=256
ray_art.SIZE=SIZE


def tiles_from(path):
    source=Image.open(path).convert('RGB')
    w,h=source.size
    result={}
    for name,(x,y) in zip(('violet','cobalt','ivory','slate','hide','coral'),((0,0),(1,0),(0,1),(1,1),(0,2),(1,2))):
        tile=source.crop((int((x+.1)*w/2),int((y+.1)*h/3),int((x+.9)*w/2),int((y+.9)*h/3)))
        result[name]=np.asarray(tile.resize((16,16),Image.Resampling.NEAREST))
    result['tooth']=result['ivory']
    return result


def assemble(name, source, tiles):
    faces=ray_art.faces_from(source)
    atlas=np.zeros((SIZE,SIZE,4),np.uint8)
    occupancy=np.zeros((SIZE,SIZE),np.uint8)
    metadata=[]
    for face in faces:
        vertices=face['v']; uv=vertices[:,3:5]*SIZE
        lo=np.rint(uv.min(0)).astype(int);hi=np.rint(uv.max(0)).astype(int)
        x0,y0=lo;x1,y1=hi
        assert 0<=x0<x1<=SIZE and 0<=y0<y1<=SIZE,(face['part'],lo,hi)
        occupancy[y0:y1,x0:x1]+=1
        material=face['part'].split('/')[-1].rsplit('_',1)[0]
        assert material in tiles,material
        tile=tiles[material]
        inv=np.linalg.inv(np.column_stack((uv[1]-uv[0],uv[3]-uv[0])))
        for y in range(y0,y1):
            for x in range(x0,x1):
                a,b=inv@(np.array([x+.5,y+.5])-uv[0])
                p=(vertices[0,:3]+a*(vertices[1,:3]-vertices[0,:3])+b*(vertices[3,:3]-vertices[0,:3]))*16
                color=tile[int(np.floor(p[1]+64))%16,int(np.floor(p[0]+p[2]+64))%16]
                atlas[y,x]=[*color,255]
        metadata.append(dict(part=face['part'],material=material,rect=[int(x0),int(y0),int(x1),int(y1)]))
    assert not np.any(occupancy>1),f'{name}: overlapping native UV islands'
    mask=occupancy>0
    assert np.all(atlas[mask,3]==255) and np.all(atlas[~mask,3]==0)
    target=RES/('textures/models/armor/reef.png' if name=='armor' else 'textures/item/reef_spear_model.png')
    target.parent.mkdir(parents=True,exist_ok=True)
    Image.fromarray(atlas).save(target)
    Image.fromarray((mask*255).astype(np.uint8)).save(WORK/f'{name}-uv-mask.png')
    (WORK/f'{name}-uv-faces.json').write_text(json.dumps(metadata,indent=2)+'\n')
    return faces,atlas,dict(faces=len(faces),occupied_texels=int(mask.sum()),overlaps=0,occupied_opaque=True,unused_transparent=True)


def icon(faces, atlas, name, diagonal=False):
    faces=[dict(part=f['part'],v=f['v'].copy()) for f in faces]
    if diagonal:
        a=np.deg2rad(40)
        rotation=np.array([[np.cos(a),-np.sin(a),0],[np.sin(a),np.cos(a),0],[0,0,1]])
        for face in faces:
            face['v'][:,:3]=face['v'][:,:3]@rotation.T
            face['v'][:,5:8]=face['v'][:,5:8]@rotation.T
    pic=np.asarray(ray_art.render(faces,atlas,'front',240,240)).copy()
    alpha=np.where(np.all(pic==[231,235,237],axis=2),0,255).astype(np.uint8)
    rgba=np.dstack((pic,alpha));rgba[alpha==0,:3]=0
    image=Image.fromarray(rgba)
    bounds=image.getbbox();assert bounds,name
    image=image.crop(bounds)
    image.thumbnail((56,56),Image.Resampling.NEAREST)
    canvas=Image.new('RGBA',(64,64))
    canvas.alpha_composite(image,((64-image.width)//2,(64-image.height)//2))
    target=RES/f'textures/item/{name}.png';target.parent.mkdir(parents=True,exist_ok=True)
    canvas.save(target)
    return canvas


def main():
    ART.mkdir(parents=True,exist_ok=True)
    tiles=tiles_from(Path(sys.argv[1]))
    armor,armor_atlas,armor_check=assemble('armor',WORK/'armor-uv.txt',tiles)
    spear,spear_atlas,spear_check=assemble('spear',WORK/'spear-uv.txt',tiles)
    board=Image.new('RGB',(1600,850),(231,235,237))
    draw=ImageDraw.Draw(board)
    for i,(name,faces,atlas,view) in enumerate([
        ('SPEAR',spear,spear_atlas,'front'),('FRONT',armor,armor_atlas,'front'),
        ('SIDE',armor,armor_atlas,'side'),('BACK',armor,armor_atlas,'back')]):
        # Back view uses the same native mesh and UVs with a 180-degree turn.
        if view=='back':
            faces=[dict(part=f['part'],v=f['v'].copy()) for f in faces]
            for f in faces:
                f['v'][:,[0,2]] *= -1;f['v'][:,[5,7]] *= -1
            view='front'
        board.paste(ray_art.render(faces,atlas,view,400,770),(i*400,35))
        draw.text((i*400+20,15),name,fill=(35,30,45))
    board.save(ART/'native-orthographic.png')
    icons=[]
    icons.append(icon(spear,spear_atlas,'reef_spear',True))
    for slot,name in [('head','reef_helmet'),('chest','reef_chestplate'),('legs','reef_leggings'),('feet','reef_boots')]:
        icons.append(icon(ray_art.faces_from(WORK/f'armor-{slot}-uv.txt'),armor_atlas,name))
    icons.append(icon([f for f in spear if '/tooth' in f['part']],spear_atlas,'crusher_tooth',True))
    plate_part=next(f['part'] for f in armor if f['part'].startswith('/body/violet'))
    icons.append(icon([f for f in armor if f['part']==plate_part],armor_atlas,'shardback_plate'))
    # Status effect uses the exact exported tooth silhouette at native effect-icon size.
    effect=RES/'textures/mob_effect/reef_bleeding.png';effect.parent.mkdir(parents=True,exist_ok=True)
    icons[-2].resize((18,18),Image.Resampling.NEAREST).save(effect)
    icons_board=Image.new('RGBA',(len(icons)*128,128),(30,33,45,255))
    for i,image in enumerate(icons):icons_board.alpha_composite(image.resize((128,128),Image.Resampling.NEAREST),(i*128,0))
    icons_board.save(ART/'item-icons.png')
    checks=dict(armor=armor_check,spear=spear_check,item_icons=7,icon_size=[64,64])
    (WORK/'texture-checks.json').write_text(json.dumps(checks,indent=2)+'\n')
    print('REEF_EQUIPMENT_TEXTURE PASS',json.dumps(checks))
    # The revised vanilla-silhouette icons are independently generated assets.
    if (ROOT/'tools/reef_sprite_sources.json').exists():
        import export_reef_sprites
        export_reef_sprites.main()


if __name__=='__main__':main()
