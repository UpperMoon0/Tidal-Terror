package com.nhat.tidal_terror.entities.shardback;
import com.nhat.tidal_terror.TidalTerror;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public class ShardbackRenderer extends MobRenderer<ShardbackEntity,ShardbackModel> {
 private static final ResourceLocation SKIN=ResourceLocation.fromNamespaceAndPath(TidalTerror.MODID,"textures/entity/shardback/shardback.png");
 public ShardbackRenderer(EntityRendererProvider.Context context){super(context,new ShardbackModel(context.bakeLayer(ShardbackModel.LAYER)),.55F);}
 @Override public ResourceLocation getTextureLocation(ShardbackEntity crab){return SKIN;}
}
