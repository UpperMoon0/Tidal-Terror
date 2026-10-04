package com.nhat.tidal_terror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;

/** Bake/export production geometry and verify joint inheritance and slot isolation. */
public final class EquipmentModelCheck {
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
    private static String export(ModelPart root) {
        StringBuilder text = new StringBuilder();
        root.visit(new PoseStack(), (pose,path,index,cube) -> {
            Capture capture = new Capture(); cube.compile(pose,capture,15728880,0,1,1,1,1);
            for(int i=0;i<capture.vertices.size();i++) {
                text.append(path).append('|').append(index).append('|').append(i/4).append('|');
                for(float v:capture.vertices.get(i)) text.append(v).append(' ');
                text.append('\n');
            }
        });
        return text.toString();
    }
    private static void verifyAtlas(String data, Path texture) throws Exception {
        int[] occupancy=new int[256*256];String[] lines=data.split("\\n");
        for(int i=0;i<lines.length;i+=4) {
            float minU=1,minV=1,maxU=0,maxV=0;
            for(int j=0;j<4;j++) {
                String[] values=lines[i+j].substring(lines[i+j].lastIndexOf('|')+1).trim().split("\\s+");
                float u=Float.parseFloat(values[3]),v=Float.parseFloat(values[4]);
                minU=Math.min(minU,u);minV=Math.min(minV,v);maxU=Math.max(maxU,u);maxV=Math.max(maxV,v);
            }
            int x0=Math.round(minU*256),y0=Math.round(minV*256),x1=Math.round(maxU*256),y1=Math.round(maxV*256);
            if(x0<0||y0<0||x1>256||y1>256||x0>=x1||y0>=y1)throw new AssertionError("Invalid UV face: "+lines[i]);
            for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++)if(++occupancy[y*256+x]>1)throw new AssertionError("Overlapping UVs in "+texture);
        }
        var image=javax.imageio.ImageIO.read(texture.toFile());
        if(image==null||image.getWidth()!=256||image.getHeight()!=256)throw new AssertionError("Wrong atlas dimensions: "+texture);
        for(int y=0;y<256;y++)for(int x=0;x<256;x++) {
            int expected=occupancy[y*256+x]>0?255:0;
            if((image.getRGB(x,y)>>>24)!=expected)throw new AssertionError("Native occupancy/alpha mismatch: "+texture+" "+x+","+y);
        }
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of("build/equipment-art");Files.createDirectories(out);
        StringBuilder all=new StringBuilder();
        for(EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            ModelPart root=ReefArmorModel.createLayer(slot).bakeRoot();
            String rest=export(root);if(rest.isBlank())throw new AssertionError("Empty armor " + slot);
            Files.writeString(out.resolve("armor-"+slot.getName()+"-uv.txt"),rest);all.append(rest);
            var model=new ReefArmorModel(root);
            if(slot==EquipmentSlot.CHEST)model.leftArm.xRot=.8F;
            else if(slot==EquipmentSlot.HEAD)model.head.yRot=.8F;
            else model.leftLeg.xRot=.8F;
            if(rest.equals(export(root)))throw new AssertionError("Armor not attached to animated joint: "+slot);
            root.getAllParts().forEach(ModelPart::resetPose);
            if(!rest.equals(export(root)))throw new AssertionError("Armor pose failed reset: "+slot);
        }
        Files.writeString(out.resolve("armor-uv.txt"),all);
        String spear=export(ReefSpearModel.createLayer().bakeRoot());
        if(spear.isBlank() || !spear.contains("tooth") || !spear.contains("hide") || !spear.contains("coral"))throw new AssertionError("Missing spear materials");
        Files.writeString(out.resolve("spear-uv.txt"),spear);
        verifyAtlas(all.toString(),Path.of("src/main/resources/assets/tidalterror/textures/models/armor/reef.png"));
        verifyAtlas(spear,Path.of("src/main/resources/assets/tidalterror/textures/item/reef_spear_model.png"));
        System.out.println("REEF_EQUIPMENT_MODEL PASS native faces exported; four slot layers; joint motion and pose reset; zero UV overlap and exact atlas alpha");
    }
}
