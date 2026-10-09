package net.minecraft.nbt;
import java.util.*;
import java.io.*;
/** Exact numeric tag types for handler tests; this is not Minecraft's NBT codec. */
public final class CompoundTag {
    private final Map<String,Number> values=new HashMap<>();
    public boolean contains(String key,int type){var n=values.get(key);return type==1?n instanceof Byte:type==3 && n instanceof Integer;}
    public boolean contains(String key){return values.containsKey(key);}
    public boolean getBoolean(String key){return values.getOrDefault(key,0).byteValue()!=0;}
    public int getInt(String key){return values.getOrDefault(key,0).intValue();}
    public void putBoolean(String key,boolean value){values.put(key,(byte)(value?1:0));}
    public void putInt(String key,int value){values.put(key,value);}
    public void remove(String key){values.remove(key);}
    public void write(DataOutput out)throws IOException {
        out.writeInt(values.size());
        for(var entry:values.entrySet()){out.writeUTF(entry.getKey());out.writeByte(entry.getValue() instanceof Byte?1:3);out.writeInt(entry.getValue().intValue());}
    }
    public static CompoundTag read(DataInput in)throws IOException {
        var tag=new CompoundTag();int count=in.readInt();
        for(int i=0;i<count;i++){String key=in.readUTF();int type=in.readByte(),value=in.readInt();if(type==1)tag.putBoolean(key,value!=0);else tag.putInt(key,value);}
        return tag;
    }
}
