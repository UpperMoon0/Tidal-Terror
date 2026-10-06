package com.nhat.tidal_terror.client;

import com.nhat.tidal_terror.entities.FangArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class FangArrowRenderer extends ArrowRenderer<FangArrowEntity,net.minecraft.client.renderer.entity.state.ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("tidalterror", "textures/entity/fang_arrow.png");
    public FangArrowRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState(){return new net.minecraft.client.renderer.entity.state.ArrowRenderState();}
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState arrow) { return TEXTURE; }
}
