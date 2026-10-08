package com.nhat.tidal_terror.worldgen;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntSupplier;
/** Exact point values grouped by chunk. Expensive noise never holds the shared LRU lock. */
public final class ChunkColumnCache<K> {
 private static final int MISSING=Integer.MIN_VALUE;
 private static final class Columns {
  final AtomicIntegerArray values=new AtomicIntegerArray(256);
  Columns(){for(int i=0;i<256;i++)values.set(i,MISSING);}
 }
 private final int limit;
 private final Map<K,Columns> chunks=new LinkedHashMap<>(128,.75f,true);
 public ChunkColumnCache(int limit){if(limit<1)throw new IllegalArgumentException();this.limit=limit;}
 public int get(K chunk,int column,IntSupplier noise){
  if(column<0 || column>=256)throw new IllegalArgumentException();
  Columns values;
  synchronized(chunks){values=chunks.computeIfAbsent(chunk,k->new Columns());while(chunks.size()>limit)chunks.remove(chunks.keySet().iterator().next());}
  int value=values.values.get(column);if(value!=MISSING)return value;
  synchronized(values){value=values.values.get(column);if(value==MISSING){value=noise.getAsInt();if(value==MISSING)throw new IllegalStateException("Invalid noise height sentinel");values.values.set(column,value);}return value;}
 }
}
