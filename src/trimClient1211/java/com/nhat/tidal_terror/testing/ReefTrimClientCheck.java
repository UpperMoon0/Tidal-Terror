package com.nhat.tidal_terror.testing;

import com.mojang.blaze3d.vertex.*;
import com.mojang.serialization.Lifecycle;
import com.nhat.tidal_terror.client.*;
import com.nhat.tidal_terror.items.ModEquipment;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.armortrim.*;

/** Runs after the real native trim atlas reload. Exercises production trim rendering. */
public final class ReefTrimClientCheck {
    private static ResourceLocation id(String name) { return ResourceLocation.withDefaultNamespace(name); }
    public static void run() {
        var materials=new MappedRegistry<TrimMaterial>(Registries.TRIM_MATERIAL,Lifecycle.stable());
        var patterns=new MappedRegistry<TrimPattern>(Registries.TRIM_PATTERN,Lifecycle.stable());
        var gold=materials.register(ResourceKey.create(Registries.TRIM_MATERIAL,id("gold")),
            TrimMaterial.create("gold",Items.GOLD_INGOT,0.6F,Component.literal("Gold"),Map.of()),RegistrationInfo.BUILT_IN);
        List<Holder<TrimPattern>> holders=new ArrayList<>();
        for(String name:List.of("sentry","dune","coast","wild","ward","eye","vex","tide","snout","rib","spire","wayfinder","shaper","silence","raiser","host","flow","bolt")) {
            holders.add(patterns.register(ResourceKey.create(Registries.TRIM_PATTERN,id(name)),
                new TrimPattern(id(name),Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE.builtInRegistryHolder(),Component.literal(name),false),RegistrationInfo.BUILT_IN));
        }
        materials.freeze(); patterns.freeze();
        RegistryAccess registry=new RegistryAccess.ImmutableRegistryAccess(List.of(materials,patterns,BuiltInRegistries.ITEM));
        int cases=0;
        for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
            Item item=switch(slot) {
                case HEAD -> ModEquipment.REEF_HELMET.get();
                case CHEST -> ModEquipment.REEF_CHESTPLATE.get();
                case LEGS -> ModEquipment.REEF_LEGGINGS.get();
                default -> ModEquipment.REEF_BOOTS.get();
            };
            var root=ReefModelCache.layer(ReefArmorModel.layer(slot));
            var base=new ReefArmorModel(root);
            Capture empty=new Capture();
            ReefArmorTrimRenderer.render(new PoseStack(),type->{empty.requests++;return empty;},15728880,new ItemStack(item),registry,slot,base);
            check(empty.requests==0 && empty.vertices.isEmpty(),"Untrimmed armor requested a trim pass");
            for(Holder<TrimPattern> pattern:holders) for(boolean young:List.of(false,true)) for(boolean foil:List.of(false,true)) {
                ItemStack stack=new ItemStack(item);
                ArmorTrim trim=new ArmorTrim(gold,pattern);
                stack.set(net.minecraft.core.component.DataComponents.TRIM,trim);
                if(foil) stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
                base.young=young;
                base.head.yRot=.35F; base.leftArm.xRot=.6F; base.rightLeg.xRot=-.4F;
                Capture shell=new Capture(), overlay=new Capture();
                PoseStack poses=new PoseStack(); poses.translate(.2,.3,.4); poses.scale(.8F,.8F,.8F);
                base.renderToBuffer(poses,shell,15728880,0);
                var trimType=net.minecraft.client.renderer.Sheets.armorTrimsSheet(trim.pattern().value().decal());
                int[] requests={0};
                Capture basePass=new Capture();
                ReefArmorTrimRenderer.renderArmor(poses,type->{
                    requests[0]++;
                    if(type==trimType) {overlay.requests++;return overlay;}
                    if(type==net.minecraft.client.renderer.RenderType.armorCutoutNoCull(ReefArmorTrimRenderer.BASE_TEXTURE)) return basePass;
                    return new Capture();
                },15728880,stack,registry,slot,base);
                check(requests[0]==(foil?3:2),"Base, trim or glint pass missing/duplicated");
                check(!basePass.vertices.isEmpty(),"Armor base texture disappeared");
                check(overlay.requests==1,"Missing or duplicate native trim pass "+slot);
                check(!overlay.vertices.isEmpty() && overlay.vertices.size()==shell.vertices.size(),"Trim geometry differs "+slot);
                List<float[]> quads=new ArrayList<>(overlay.vertices);
                java.util.Comparator<float[]> order=java.util.Comparator.comparingDouble((float[] vertex)->vertex[0]).thenComparingDouble(vertex->vertex[1]).thenComparingDouble(vertex->vertex[2]);
                shell.vertices.sort(order); overlay.vertices.sort(order);
                for(int i=0;i<shell.vertices.size();i++) {
                    float[] a=shell.vertices.get(i),b=overlay.vertices.get(i);
                    for(int j=0;j<3;j++) check(Math.abs(a[j]-b[j])<.00001F,"Trim geometry/pose moved "+slot+" young="+young);
                    check(Float.isFinite(b[3]) && Float.isFinite(b[4]) && b[3]>=0 && b[3]<=1 && b[4]>=0 && b[4]<=1,"Invalid trim atlas UV");
                }
                var armor=(ArmorItem)item;
                var texture=slot==EquipmentSlot.LEGS?trim.innerTexture(armor.getMaterial()):trim.outerTexture(armor.getMaterial());
                var sprite=Minecraft.getInstance().getModelManager().getAtlas(net.minecraft.client.renderer.Sheets.ARMOR_TRIMS_SHEET).getSprite(texture);
                check(sprite.contents().name().equals(texture),"Missing native trim sprite "+texture);
                boolean varied=false;
                for(float[] vertex:overlay.vertices) {
                    check(vertex[3]>=sprite.getU0()-.00001F && vertex[3]<=sprite.getU1()+.00001F && vertex[4]>=sprite.getV0()-.00001F && vertex[4]<=sprite.getV1()+.00001F,"Trim UV outside selected sprite "+texture+" "+vertex[3]+","+vertex[4]+" expected "+sprite.getU0()+","+sprite.getU1()+" / "+sprite.getV0()+","+sprite.getV1());
                    if(Math.abs(vertex[3]-overlay.vertices.get(0)[3])>.00001F) varied=true;
                }
                check(varied,"Flat trim UVs");
                int visiblePixels=0;
                for(int i=0;i<quads.size();i+=4) {
                    float minU=1,minV=1,maxU=0,maxV=0;
                    for(int j=0;j<4;j++) {
                        float[] vertex=quads.get(i+j);
                        float u=(vertex[3]-sprite.getU0())/(sprite.getU1()-sprite.getU0());
                        float v=(vertex[4]-sprite.getV0())/(sprite.getV1()-sprite.getV0());
                        minU=Math.min(minU,u); maxU=Math.max(maxU,u); minV=Math.min(minV,v); maxV=Math.max(maxV,v);
                    }
                    var image=sprite.contents();
                    for(int y=0;y<image.height();y++) for(int x=0;x<image.width();x++) {
                        float u=(x+.5F)/image.width(),v=(y+.5F)/image.height();
                        if(u>minU && u<maxU && v>minV && v<maxV && !image.isTransparent(0,x,y)) visiblePixels++;
                    }
                }
                check(visiblePixels>0,"Trim geometry samples only transparent pixels "+slot+" "+texture);
                cases++;
            }
        }
        org.slf4j.LoggerFactory.getLogger("ReefTrimCheck").info("TIDAL_FABRIC_TRIMS_PASS: {} native pattern/slot/age/glint cases; geometry and atlas UVs preserved",cases);
    }
    private static void check(boolean condition,String message) { if(!condition) throw new IllegalStateException(message); }
    private static final class Capture implements VertexConsumer {
        final List<float[]> vertices=new ArrayList<>();
        int requests;
        float x,y,z,u,v;
        public VertexConsumer addVertex(float x,float y,float z){this.x=x;this.y=y;this.z=z;return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){vertices.add(new float[]{this.x,this.y,this.z,u,v});return this;}
    }
}
