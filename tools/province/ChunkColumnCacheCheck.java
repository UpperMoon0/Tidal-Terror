import com.nhat.tidal_terror.worldgen.ChunkColumnCache;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class ChunkColumnCacheCheck {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  var cache=new ChunkColumnCache<Integer>(2);var calls=new AtomicInteger();
  for(int i=0;i<256;i++){final int column=i;check(cache.get(0,i,()->{calls.incrementAndGet();return -400+column;})==-400+i);}
  for(int i=0;i<256;i++)check(cache.get(0,i,()->{throw new AssertionError("Repeated noise");})==-400+i);
  check(calls.get()==256);cache.get(1,0,()->1);cache.get(2,0,()->2);check(cache.get(0,0,()->99)==99);
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try{var pending=pool.submit(()->cache.get(3,0,()->{entered.countDown();try{check(release.await(5,TimeUnit.SECONDS));}catch(Exception e){throw new RuntimeException(e);}return 7;}));
   check(entered.await(5,TimeUnit.SECONDS));check(pool.submit(()->cache.get(4,0,()->8)).get(2,TimeUnit.SECONDS)==8);
   release.countDown();check(pending.get(2,TimeUnit.SECONDS)==7);
  }finally{release.countDown();pool.shutdownNow();}
  try{cache.get(5,0,()->{throw new IllegalStateException();});throw new AssertionError();}catch(IllegalStateException expected){}
  check(cache.get(5,0,()->3)==3);
  var batchCache=new ChunkColumnCache<Integer>(2);var batches=new AtomicInteger();
  java.util.function.Supplier<int[]> batch=()->{batches.incrementAndGet();int[] values=new int[256];for(int i=0;i<256;i++)values[i]=i-512;return values;};
  for(int i=0;i<256;i++)check(batchCache.getBatch(0,i,batch)==i-512);
  check(batches.get()==1);
  var concurrent=new ChunkColumnCache<Integer>(2);var batchEntered=new CountDownLatch(1);var batchRelease=new CountDownLatch(1);
  var batchPool=Executors.newFixedThreadPool(2);
  try {
   var first=batchPool.submit(()->concurrent.getBatch(0,0,()->{batchEntered.countDown();try{check(batchRelease.await(5,TimeUnit.SECONDS));}catch(Exception e){throw new RuntimeException(e);}return batch.get();}));
   check(batchEntered.await(5,TimeUnit.SECONDS));
   check(batchPool.submit(()->concurrent.getBatch(1,255,batch)).get(2,TimeUnit.SECONDS)==-257);
   batchRelease.countDown();check(first.get(2,TimeUnit.SECONDS)==-512);
   check(concurrent.getBatch(0,255,()->{throw new AssertionError("Repeated batch");})==-257);
  } finally {batchRelease.countDown();batchPool.shutdownNow();}
  try{batchCache.getBatch(1,0,()->new int[255]);throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{batchCache.getBatch(1,0,()->{int[] values=new int[256];values[255]=Integer.MIN_VALUE;return values;});throw new AssertionError();}catch(IllegalStateException expected){}
  check(batchCache.getBatch(1,255,batch)==-257);
  System.out.println("CHUNK_COLUMN_CACHE PASS exact values, reuse, eviction, independent-chunk concurrency, failure retry");
 }
}
