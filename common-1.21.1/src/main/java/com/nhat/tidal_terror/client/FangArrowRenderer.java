package com.nhat.tidal_terror.client;

import com.nhat.tidal_terror.entities.FangArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class FangArrowRenderer extends ArrowRenderer<FangArrowEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("tidalterror", "textures/entity/fang_arrow.png");
    public FangArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(FangArrowEntity arrow) { return TEXTURE; }
}
