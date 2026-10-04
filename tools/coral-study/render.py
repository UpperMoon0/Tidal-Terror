from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import math,csv,json,sys
folder=Path(sys.argv[1]); colors=[(50,93,212),(219,184,28),(177,52,209),(224,55,46),(222,209,47),(197,175,126)]
faces=[((1,0,0),[(1,0,0),(1,1,0),(1,1,1),(1,0,1)]),((-1,0,0),[(0,0,1),(0,1,1),(0,1,0),(0,0,0)]),((0,1,0),[(0,1,0),(0,1,1),(1,1,1),(1,1,0)]),((0,-1,0),[(0,0,1),(0,0,0),(1,0,0),(1,0,1)]),((0,0,1),[(1,0,1),(1,1,1),(0,1,1),(0,0,1)]),((0,0,-1),[(0,0,0),(0,1,0),(1,1,0),(1,0,0)])]
def render(blocks,az):
 w,h=900,1200;im=Image.new('RGB',(w,h),(13,25,34));draw=ImageDraw.Draw(im)
 el=math.radians(18);az=math.radians(az);view=(math.sin(az)*math.cos(el),math.sin(el),math.cos(az)*math.cos(el))
 def project(p):
  x,y,z=p;return(w/2+(math.cos(az)*x-math.sin(az)*z)*8,h-140+(math.sin(el)*(math.sin(az)*x+math.cos(az)*z)-math.cos(el)*y)*8)
 pending=[]
 for (x,y,z),c in blocks.items():
  for n,vs in faces:
   if sum(n[i]*view[i] for i in range(3))<=0 or (x+n[0],y+n[1],z+n[2]) in blocks:continue
   pts=[(x+a,y+b,z+d) for a,b,d in vs];dep=sum((x,y,z)[i]*view[i] for i in range(3))
   light=.68+.2*max(0,n[1])+.16*max(0,n[0])+.08*max(0,n[2]);col=tuple(int(v*light) for v in colors[c if c>=0 else 5])
   pending.append((dep,pts,col))
 for _,pts,col in sorted(pending,key=lambda v:v[0]):draw.polygon([project(p) for p in pts],fill=col)
 for y in range(0,111,10):
  a=project((-43,y,0));b=project((-41,y,0));draw.line((a,b),fill=(140,180,195),width=2);draw.text((a[0]-42,a[1]-7),str(y)+' m',fill=(155,190,205))
 return im
all_data=[];sheet=Image.new('RGB',(2700,1270),(13,25,34))
names=['Branching staghorn grove','Folded open chalice','Ribbed lace sea fans']
for style in range(3):
 rows=[tuple(map(int,row)) for row in csv.reader((folder/f'coral-{style}.csv').open())];blocks={r[:3]:r[3] for r in rows};all_data.append(rows)
 im=render(blocks,35);im.save(folder/f'coral-{style}-front.png');render(blocks,135).save(folder/f'coral-{style}-reverse.png')
 sheet.paste(im,(style*900,70));d=ImageDraw.Draw(sheet);d.text((style*900+100,25),names[style],font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',30),fill='white')
sheet.save(folder/'structure-study.png')
(folder/'voxels.json').write_text(json.dumps(all_data,separators=(',',':')))
print(folder/'structure-study.png')

template=Path(__file__).with_name('viewer.html').read_text(encoding='utf-8')
(folder.parent/'index.html').write_text(template.replace('__CORAL_VOXELS__',(folder/'voxels.json').read_text()).replace('iteration-2/',folder.name+'/'),encoding='utf-8')
