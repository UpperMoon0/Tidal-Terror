package com.nhat.tidal_terror.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.model.geom.ModelPart;

/** Same shell geometry and joints, with vanilla trim UVs instead of the packed material atlas. */
public final class ReefTrimModel {
    private static final Map<ModelPart, ReefArmorModel> CACHE = new WeakHashMap<>();
    public static ReefArmorModel get(ModelPart shell) {
        return CACHE.computeIfAbsent(shell, root -> new ReefArmorModel(copy(root)));
    }
    private static ModelPart copy(ModelPart root) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        nodes.put("", new Node(root));
        for (String name : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"))
            nodes.put("/" + name, new Node(root.getChild(name)));
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            String[] names = path.substring(1).split("/");
            String key = "";
            ModelPart part = root;
            for (String name : names) {
                key += "/" + name;
                part = part.getChild(name);
                nodes.putIfAbsent(key, new Node(part));
            }
            nodes.get(path).cubes.add(new ReefTrimCube(cube, names[0]));
        });
        List<String> paths = new ArrayList<>(nodes.keySet());
        Collections.reverse(paths);
        for (String path : paths) {
            if (path.isEmpty()) continue;
            int split = path.lastIndexOf('/');
            Node node = nodes.get(path);
            nodes.get(path.substring(0, split)).children.put(path.substring(split + 1), node.build());
        }
        return nodes.get("").build();
    }
    private static final class Node {
        final ModelPart original;
        final List<ModelPart.Cube> cubes = new ArrayList<>();
        final Map<String, ModelPart> children = new LinkedHashMap<>();
        Node(ModelPart original) { this.original = original; }
        ModelPart build() {
            ModelPart part = new ModelPart(cubes, children);
            part.setInitialPose(original.getInitialPose());
            part.loadPose(original.storePose());
            part.visible = original.visible;
            part.skipDraw = original.skipDraw;
            return part;
        }
    }
    /** Project each shell face into the corresponding vanilla 64x32 armor island. */
    public static float[] uv(String part, float x, float y, float z, float nx, float ny, float nz) {
        float u0, v0, w, h, d, px, py, pz;
        if (part.equals("head") || part.equals("hat")) {
            u0=0; v0=0; w=8; h=8; d=8;
            px=(x+5)/10; py=(y+11)/11; pz=(z+5)/10;
        } else if (part.equals("body")) {
            u0=16; v0=16; w=8; h=12; d=4;
            px=(x+5)/10; py=y/12; pz=(z+5)/10;
        } else if (part.endsWith("arm")) {
            u0=40; v0=16; w=4; h=12; d=4;
            px=(x+(part.equals("right_arm")?4:2))/6; py=(y+4)/13; pz=(z+4)/7;
        } else {
            u0=0; v0=16; w=4; h=12; d=4;
            px=(x+2.5F)/5; py=y/13; pz=(z+5)/9;
        }
        px=clamp(px); py=clamp(py); pz=clamp(pz);
        if (part.startsWith("left_")) { px=1-px; nx=-nx; }
        float u, v;
        if (ny < -.5F) { u=u0+d+px*w; v=v0+(1-pz)*d; }
        else if (ny > .5F) { u=u0+d+w+px*w; v=v0+(1-pz)*d; }
        else {
            v=v0+d+py*h;
            if (nx < -.5F) u=u0+(1-pz)*d;
            else if (nx > .5F) u=u0+d+w+pz*d;
            else if (nz < -.5F) u=u0+d+px*w;
            else u=u0+2*d+w+(1-px)*w;
        }
        return new float[]{u/64, v/32};
    }
    private static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    private ReefTrimModel() {}
}
