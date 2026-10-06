package com.nhat.tidal_terror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Native cube emission adapter; geometry, animation and trim projection are shared. */
final class ReefTrimCube extends ModelPart.Cube {
    private static final PoseStack.Pose IDENTITY = new PoseStack().last();
    private final ModelPart.Cube shell;
    private final String part;
    ReefTrimCube(ModelPart.Cube shell, String part) {
        super(0,0,0,0,0,0,0,0,0,0,0,false,64,32,java.util.Set.of());
        this.shell=shell; this.part=part;
    }
    @Override public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, float r,float g,float b,float a) {
        shell.compile(IDENTITY, new Remap(pose,buffer), light,overlay,r,g,b,a);
    }
    private final class Remap implements VertexConsumer {
        private final PoseStack.Pose pose;
        private final VertexConsumer output;
        private final Vector3f position=new Vector3f(), normal=new Vector3f();
        private float x,y,z,nx,ny,nz;
        private int r,g,b,a,lightU,lightV,overlayU,overlayV;
        Remap(PoseStack.Pose pose,VertexConsumer output) { this.pose=pose; this.output=output; }
        public VertexConsumer vertex(double x,double y,double z){this.x=(float)x;this.y=(float)y;this.z=(float)z;return this;}
        public VertexConsumer color(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;return this;}
        public VertexConsumer uv(float u,float v){return this;}
        public VertexConsumer overlayCoords(int u,int v){overlayU=u;overlayV=v;return this;}
        public VertexConsumer uv2(int u,int v){lightU=u;lightV=v;return this;}
        public VertexConsumer normal(float x,float y,float z){nx=x;ny=y;nz=z;return this;}
        public void endVertex(){
            float[] uv=ReefTrimModel.uv(part,x*16,y*16,z*16,nx,ny,nz);
            pose.pose().transformPosition(x,y,z,position);
            pose.normal().transform(nx,ny,nz,normal);
            output.vertex(position.x,position.y,position.z,r/255F,g/255F,b/255F,a/255F,uv[0],uv[1],overlayU|(overlayV<<16),lightU|(lightV<<16),normal.x,normal.y,normal.z);
        }
        public void defaultColor(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;}
        public void unsetDefaultColor(){}
    }
}
