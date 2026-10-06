package com.nhat.tidal_terror.mixin;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(MobCategory.class)
public abstract class MobCategoryAccess {
    @Shadow @Final @Mutable private static MobCategory[] $VALUES;
    @Shadow @Final @Mutable public static com.mojang.serialization.Codec<MobCategory> CODEC;
    @org.spongepowered.asm.mixin.injection.Inject(method="<clinit>",at=@org.spongepowered.asm.mixin.injection.At("TAIL"))
    private static void pools(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        extend("TIDALTERROR_CRUSHER","tidalterror:crusher",2,true,false,128);
        extend("TIDALTERROR_RAY","tidalterror:ray",8,true,false,128);
        extend("TIDALTERROR_VEILGLOW","tidalterror:veilglow",12,true,false,128);
        extend("TIDALTERROR_SHARDBACK","tidalterror:shardback",10,true,false,128);
        CODEC=net.minecraft.util.StringRepresentable.fromEnum(MobCategory::values);
    }
    @Invoker("<init>") private static MobCategory construct(String name,int ordinal,String id,int cap,boolean friendly,boolean persistent,int distance) { throw new AssertionError(); }
    private static MobCategory extend(String name,String id,int cap,boolean friendly,boolean persistent,int distance) {
        var value=construct(name,$VALUES.length,id,cap,friendly,persistent,distance);
        var next=java.util.Arrays.copyOf($VALUES,$VALUES.length+1);next[next.length-1]=value;$VALUES=next;return value;
    }
}
