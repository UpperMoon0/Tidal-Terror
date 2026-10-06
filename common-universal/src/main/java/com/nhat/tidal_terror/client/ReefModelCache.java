package com.nhat.tidal_terror.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

/** Reuse baked geometry until a resource reload replaces the native model set. */
public final class ReefModelCache {
    private static EntityModelSet generation;
    private static final java.util.Map<ModelLayerLocation, ModelPart> PARTS = new java.util.HashMap<>();
    public static ModelPart layer(ModelLayerLocation layer) {
        var current = Minecraft.getInstance().getEntityModels();
        if (generation != current) { generation = current; PARTS.clear(); }
        return PARTS.computeIfAbsent(layer, current::bakeLayer);
    }
    private ReefModelCache() {}
}
