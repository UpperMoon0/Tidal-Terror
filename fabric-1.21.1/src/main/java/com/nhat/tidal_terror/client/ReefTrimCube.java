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
    @Override public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        shell.compile(IDENTITY, new Remap(pose,buffer), light,overlay,color);
    }
    private final class Remap implements VertexConsumer {
        private final PoseStack.Pose pose;
        private final VertexConsumer output;
        private final Vector3f position=new Vector3f(), normal=new Vector3f();
        private float x,y,z,u,v,nx,ny,nz;
        private int color,light,overlay;
        Remap(PoseStack.Pose pose,VertexConsumer output) { this.pose=pose; this.output=output; }
        @Override public void addVertex(float x,float y,float z,int color,float u,float v,int overlay,int light,float nx,float ny,float nz) {
            float[] uv=ReefTrimModel.uv(part,x*16,y*16,z*16,nx,ny,nz);
            pose.pose().transformPosition(x,y,z,position);
            pose.transformNormal(nx,ny,nz,normal);
            output.addVertex(position.x,position.y,position.z,color,uv[0],uv[1],overlay,light,normal.x,normal.y,normal.z);
        }
        public VertexConsumer addVertex(float x,float y,float z){this.x=x;this.y=y;this.z=z;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){color=(a<<24)|(r<<16)|(g<<8)|b;return this;}
        public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){overlay=u|(v<<16);return this;}
        public VertexConsumer setUv2(int u,int v){light=u|(v<<16);return this;}
        public VertexConsumer setNormal(float x,float y,float z){addVertex(this.x,this.y,this.z,color,u,v,overlay,light,x,y,z);return this;}
    }
}
