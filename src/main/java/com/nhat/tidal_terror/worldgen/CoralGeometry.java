package com.nhat.tidal_terror.worldgen;
import java.util.*;
/** Exact voxel plans shared by generation and the separate structure viewer. */
public final class CoralGeometry {
 public record Voxel(int x,int y,int z) { Voxel offset(int dx,int dy,int dz){return new Voxel(x+dx,y+dy,z+dz);} }
 public record Plan(Map<Voxel,Integer> blocks,int height,int style) {}
 private static final int[][] N={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
 public static Plan build(long seed,int height,int style){
  Random r=new Random(seed);Set<Voxel> s=new HashSet<>();double phase=r.nextDouble()*Math.PI*2;
  if(style==0){
   tube(s,0,0,0,0,height*.52,0,3.2,2.2,phase);
   for(int root=0;root<9;root++){
    double a=phase+root*Math.PI*2/9,reach=8+r.nextDouble()*5;
    tube(s,Math.cos(a)*reach,0,Math.sin(a)*reach,Math.cos(a)*5,height*.28,Math.sin(a)*5,1.5,.8,a);
    tube(s,0,height*.15,0,Math.cos(a)*12,height*.44,Math.sin(a)*12,1.8,.7,a);
   }
   for(int arm=0;arm<7;arm++){
    double a=phase+arm*Math.PI*2/7,reach=18+r.nextDouble()*9,top=height*(.79+r.nextDouble()*.2);
    double tx=Math.cos(a)*reach,tz=Math.sin(a)*reach;
    tube(s,0,2,0,tx,top,tz,2.8,1,a);
    for(int fork=0;fork<5;fork++){
     double t=.30+fork*.125,bx=tx*t*t,bz=tz*t*t,by=2+(top-2)*t;
     double fa=a+(fork%2==0?1:-1)*(.65+r.nextDouble()*.4),length=7+r.nextDouble()*7;
     double ex=bx+Math.cos(fa)*length,ez=bz+Math.sin(fa)*length,ey=Math.min(height-1,by+12+r.nextDouble()*12);
     tube(s,bx,by,bz,ex,ey,ez,1.5,.65,fa);
     for(int twig=0;twig<3;twig++){
      double f=.45+twig*.18,px=bx+(ex-bx)*f*f,pz=bz+(ez-bz)*f*f,py=by+(ey-by)*f;
      double ta=fa+(twig%2==0?.95:-.95);
      tube(s,px,py,pz,px+Math.cos(ta)*4,Math.min(height-1,py+7),pz+Math.sin(ta)*4,.85,.55,ta);
     }
    }
   }
  }else if(style==1){
   for(int y=0;y<height;y++){
    double t=y/(double)(height-1),cx=Math.sin(t*4+phase)*t*3,cz=Math.cos(t*4+phase)*t*3,rad=3+22*Math.pow(t,1.65);
    for(int x=-32;x<=32;x++)for(int z=-32;z<=32;z++){
     double a=Math.atan2(z-cz,x-cx),fold=(1+3*t)*Math.sin(a*9+phase+t*3)+1.2*t*Math.sin(a*18-t*5);
     double rim=height-1-3*(1+Math.sin(a*9+phase));
     if(y<=rim && Math.abs(Math.hypot(x-cx,z-cz)-(rad+fold))<.78)s.add(new Voxel(x,y,z));
    }
   }
   for(int i=0;i<9;i++){double a=phase+i*Math.PI*2/9;tube(s,Math.cos(a)*10,0,Math.sin(a)*10,Math.cos(a)*6,height*.36,Math.sin(a)*6,1.8,.8,a);}
  }else{
   tube(s,0,0,0,0,height*.8,0,2.8,1.4,phase);
   for(int fan=0;fan<5;fan++){
    double angle=phase+fan*Math.PI*2/5;int start=4+fan*3;
    for(int y=start;y<height-fan*2;y++){
     double t=(y-start)/(double)(height-start),width=28*Math.pow(Math.sin(Math.PI*t),.65);
     for(int u=0;u<=Math.ceil(width);u++){
      double a=angle+.10*Math.sin(y*.09+u*.14);int x=(int)Math.round(Math.cos(a)*u),z=(int)Math.round(Math.sin(a)*u);
      boolean rib=u<3 || Math.floorMod(u+y/3,9)<2;
      double hx=Math.floorMod(u+y/9,10)-5,hy=Math.floorMod(y,13)-6;
      boolean lace=u>5 && y>15 && hx*hx+hy*hy<12 && !rib;
      if(!lace){s.add(new Voxel(x,y,z));if(rib)s.add(new Voxel(x+(int)Math.round(-Math.sin(a)),y,z+(int)Math.round(Math.cos(a))));}
     }
    }
   }
  }
  s.removeIf(v->v.y<0||v.y>=height||Math.abs(v.x)>34||Math.abs(v.z)>34);
  Map<Voxel,Integer> blocks=new HashMap<>();int base=Math.floorMod(style+(int)seed,5);
  for(Voxel v:s){boolean wet=false;for(int[] d:N)if(v.y+d[1]>=0&&!s.contains(v.offset(d[0],d[1],d[2]))){wet=true;break;}
   // Irregular small tip patches avoid a flat artificial colour stripe.
   long grain=(long)v.x*73856093L^(long)v.y*19349663L^(long)v.z*83492791L^seed;
   boolean tip=v.y>height*.82 && Math.floorMod(grain,23)<(v.y-height*.82)*.7;
   blocks.put(v,wet?(tip?(base+1)%5:base):-1);
  }
  return new Plan(Map.copyOf(blocks),height,style);
 }
 private static void tube(Set<Voxel> s,double x0,double y0,double z0,double x1,double y1,double z1,double r0,double r1,double phase){
  int steps=(int)Math.ceil(Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0)+(z1-z0)*(z1-z0))*2);
  for(int i=0;i<=steps;i++){
   double t=i/(double)Math.max(steps,1),cx=x0+(x1-x0)*t*t+Math.sin(t*Math.PI)*Math.sin(phase)*1.2;
   double cy=y0+(y1-y0)*t,cz=z0+(z1-z0)*t*t+Math.sin(t*Math.PI)*Math.cos(phase)*1.2,r=r0+(r1-r0)*t;
   for(int x=(int)Math.floor(cx-r);x<=Math.ceil(cx+r);x++)for(int y=(int)Math.floor(cy-r);y<=Math.ceil(cy+r);y++)for(int z=(int)Math.floor(cz-r);z<=Math.ceil(cz+r);z++)
    if((x-cx)*(x-cx)+(y-cy)*(y-cy)+(z-cz)*(z-cz)<=r*r+.35)s.add(new Voxel(x,y,z));
  }
 }
}
