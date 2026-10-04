package com.nhat.tidal_terror.entities.veilglow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Exports actual baked cube faces, not a parallel hand-built mesh. */
public class VeilglowModelCheck {
    private static final class Capture implements VertexConsumer {
        final List<float[]> vertices = new ArrayList<>();
        float x,y,z,u,v,nx,ny,nz;
        public VertexConsumer vertex(double x,double y,double z){this.x=(float)x;this.y=(float)y;this.z=(float)z;return this;}
        public VertexConsumer uv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer normal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}
        public VertexConsumer uv2(int u,int v){return this;}
        public void endVertex(){vertices.add(new float[]{x,y,z,u,v,nx,ny,nz});}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
    private static String render(VeilglowModel model) {
        Capture c=new Capture(); model.renderAll(new PoseStack(),c,15728880,0,1,1,1,1);
        StringBuilder s=new StringBuilder();
        for(var v:c.vertices){for(float f:v)s.append(f).append(' ');s.append('\n');}
        return s.toString();
    }
    public static void main(String[] args) throws Exception {
        var root=VeilglowModel.createBodyLayer().bakeRoot();
        var model=new VeilglowModel(root);
        Path out=Path.of("build/veilglow-art");Files.createDirectories(out);
        StringBuilder uv=new StringBuilder();
        root.visit(new PoseStack(),(pose,path,cubeIndex,cube)->{
            Capture c=new Capture();cube.compile(pose,c,15728880,0,1,1,1,1);
            for(int i=0;i<c.vertices.size();i++){
                uv.append(path).append('|').append(cubeIndex).append('|').append(i/4).append('|');
                for(float v:c.vertices.get(i))uv.append(v).append(' ');uv.append('\n');
            }
        });
        Files.writeString(out.resolve("native-uv-vertices.txt"),uv);
        model.animatePose(0,0,true);String rest=render(model);
        model.animatePose(12,0.25F,true);
        if(rest.equals(render(model)))throw new AssertionError("Wings/tail/pitch did not move baked vertices");
        model.animatePose(0,0,true);
        if(!rest.equals(render(model)))throw new AssertionError("Shared renderer pose leaked");
        for(int frame=0;frame<48;frame++){
            model.animatePose(frame*20F/24F,0.1F,true);
            Files.writeString(out.resolve(String.format("frame-%02d.txt",frame)),render(model));
        }
        for(var state:new VeilglowEntity.Behavior[]{VeilglowEntity.Behavior.PULSE,VeilglowEntity.Behavior.FLEE}){
            model.animatePose(0,0,true);model.animateBehavior(0,state);
            if(rest.equals(render(model)))throw new AssertionError("Missing visible behavior: "+state);
            Files.writeString(out.resolve("pose-"+state.name().toLowerCase()+".txt"),render(model));
            model.animatePose(0,0,true);
            if(!rest.equals(render(model)))throw new AssertionError("Behavior pose leaked: "+state);
        }
        System.out.println("VEILGLOW_MODEL PASS native face export, animated vertices, pose reset; 48 frames");
    }
}
