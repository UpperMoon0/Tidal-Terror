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
  System.out.println("CHUNK_COLUMN_CACHE PASS exact values, reuse, eviction, independent-chunk concurrency, failure retry");
 }
}
