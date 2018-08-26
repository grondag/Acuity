package grondag.acuity;

import it.unimi.dsi.fastutil.longs.LongArrays;

public class RunTimer
{
    public static void stats(long[] data)
    {
        final int count = data.length;
        final int bucketSize = count / 5;
        final int rem = count - bucketSize * 5;
        int[] sizes = new int[5];
        sizes[0] = bucketSize + (rem > 0 ? 1 : 0);
        sizes[1] = bucketSize + (rem > 1 ? 1 : 0);
        sizes[2] = bucketSize + (rem > 2 ? 1 : 0);
        sizes[3] = bucketSize + (rem > 3 ? 1 : 0);
        sizes[4] = bucketSize;
        
        int bucketIndex = 0;
        int nextBucket = sizes[0];
        long buckets[] = new long[5];
        
        long b = 0;
        
        LongArrays.quickSort(data);
        
        for(int i = 0; i < count; i++)
        {
            if(i == nextBucket)
            {
                buckets[bucketIndex++] = b;
                b = 0;
                nextBucket += sizes[bucketIndex];
            }
            
            b += data[i];
        }
        buckets[4] = b;
        
        long total = buckets[0] + buckets[1] + buckets[2] + buckets[3] + buckets[4];
        
        System.out.println("Acuity Enabled = " + Acuity.isModEnabled());
        System.out.println(String.format("Total: %,d  Min: %,d  Max: %,d  Mean: %,d", total, data[0], data[count - 1], total / count));
        System.out.println(String.format("Bucket Percent: %,d   %,d   %,d   %,d   %,d", 
                buckets[0] * 100 / total, buckets[1] * 100 / total, buckets[2] * 100 / total, 
                buckets[3] * 100 / total, buckets[4] * 100 / total));
        System.out.println(String.format("Bucket Averages: %,d   %,d   %,d   %,d   %,d", 
                buckets[0]/sizes[0], buckets[1]/sizes[1], buckets[2]/sizes[2], buckets[3]/sizes[3], 
                buckets[4]/sizes[4]));
        System.out.println("");
    }
    
    final long[] data;
    final int size;
    int counter;
    long start;
    
    public RunTimer(int sampleCount)
    {
        size = sampleCount;
        data = new long[sampleCount];
    }
    
    public void start()
    {
        start = System.nanoTime();
    }
    
    public void finish()
    {
        data[counter++] = (System.nanoTime() - start);
        if(counter == size)
        {
            stats(data);
            counter = 0;
        }
    }
    
    public static RunTimer TIMER_200 = new RunTimer(200);
    
    public static RunTimer TIMER_2400 = new RunTimer(2400);
    
    public static RunTimer TIMER_100000 = new RunTimer(100000);
    
}
