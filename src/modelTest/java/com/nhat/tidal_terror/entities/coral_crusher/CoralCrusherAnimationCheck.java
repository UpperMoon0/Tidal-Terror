package com.nhat.tidal_terror.entities.coral_crusher;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoralCrusherAnimationCheck {
    private static final class Capture implements VertexConsumer {
        final List<float[]> vertices = new ArrayList<>();
        float x, y, z, u, v, nx, ny, nz;
        public VertexConsumer vertex(double x, double y, double z) { this.x=(float)x; this.y=(float)y; this.z=(float)z; return this; }
        public VertexConsumer uv(float u, float v) { this.u=u; this.v=v; return this; }
        public VertexConsumer normal(float x, float y, float z) { nx=x; ny=y; nz=z; return this; }
        public VertexConsumer color(int r,int g,int b,int a) { return this; }
        public VertexConsumer overlayCoords(int u,int v) { return this; }
        public VertexConsumer uv2(int u,int v) { return this; }
        public void endVertex() { vertices.add(new float[]{x,y,z,u,v,nx,ny,nz}); }
        public void defaultColor(int r,int g,int b,int a) { }
        public void unsetDefaultColor() { }
    }
    private static Capture render(EntityModel<?> model) {
        Capture capture = new Capture();
        model.renderToBuffer(new PoseStack(), capture, 15728880, 0, 1, 1, 1, 1);
        return capture;
    }
    private static List<String> signature(Capture c) {
        return c.vertices.stream().map(v -> String.format(Locale.ROOT,"%.4f %.4f %.4f %.5f %.5f",v[0],v[1],v[2],v[3],v[4])).sorted().toList();
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        var root = CoralCrusherModel.createBodyLayer().bakeRoot();
        var model = new CoralCrusherModel<Entity>(root);
        Path uvDirectory = Path.of("build", "texture-audit");
        Files.createDirectories(uvDirectory);
        StringBuilder uvVertices = new StringBuilder();
        root.visit(new PoseStack(), (pose, partPath, cubeIndex, cube) -> {
            Capture capture = new Capture();
            cube.compile(pose, capture, 15728880, 0, 1, 1, 1, 1);
            for (int i = 0; i < capture.vertices.size(); i++) {
                uvVertices.append(partPath).append('|').append(cubeIndex).append('|').append(i / 4).append('|');
                for (float value : capture.vertices.get(i)) uvVertices.append(value).append(' ');
                uvVertices.append('\n');
            }
        });
        Files.writeString(uvDirectory.resolve("native-uv-vertices.txt"), uvVertices);
        var original = new OriginalCoralCrusherModel<Entity>(OriginalCoralCrusherModel.createBodyLayer().bakeRoot());
        require(signature(render(model)).equals(signature(render(original))), "Rebased geometry or texture UVs changed at rest");
        model.animatePose(true, 1, 0, 0, 0, 0);
        var first = signature(render(model));
        model.animatePose(true, 1, 8, 0, 0, 0);
        require(!first.equals(signature(render(model))), "Swim animation must move rendered vertices");
        model.animatePose(true, 0, 0, 0, 0, 0);
        var idle = signature(render(model));
        model.animatePose(true, 0, 0, 0, 0, 0.5F);
        require(!idle.equals(signature(render(model))), "Bite must move rendered jaw vertices");
        require(root.getChild("body").getChild("head").getChild("lowerjaw").xRot > 0.8F, "Lower jaw must open on swing");
        model.animatePose(true, 0, 0, 0, 0, 0);
        require(idle.equals(signature(render(model))), "Animation leaked a previous entity's pose");
        model.animatePose(false, 0, 8, 0, 0, 0);
        require(Math.abs(root.getChild("body").zRot) > 0.01F, "Land animation must move");
        Path directory = Path.of("build", "animation-preview");
        Files.createDirectories(directory);
        for (int frame = 0; frame < 48; frame++) {
            float age = frame * 20F / 24F;
            float bite = frame >= 28 && frame <= 35 ? (frame-28)/7F : 0F;
            model.animatePose(true, 0.8F, age, 0, 0, bite);
            StringBuilder text = new StringBuilder();
            for (float[] vertex : render(model).vertices) {
                for (float value : vertex) text.append(value).append(' ');
                text.append('\n');
            }
            Files.writeString(directory.resolve(String.format("frame-%02d.txt", frame)), text);
        }
        System.out.println("PASS: rest geometry/UVs preserved; native rendered swim/bite/land vertices change; shared poses reset.");
        System.out.println("Exported 48 native ModelPart frames to " + directory.toAbsolutePath());
    }
}
