/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

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
    
    final String label;
    final long[] data;
    final int size;
    int counter;
    long start;
    
    public RunTimer(String label, int sampleCount)
    {
        this.label = label;
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
            System.out.println("Run Timer Result for " + label);
            stats(data);
            counter = 0;
        }
    }
    
    public static RunTimer TIMER_200 = new RunTimer("GENERIC 200", 200);
    
    public static RunTimer TIMER_2400 = new RunTimer("GENERIC 2400", 2400);
    
    public static RunTimer TIMER_100000 = new RunTimer("GENERIC 100000", 100000);

    public static class ThreadSafeRunTimer
    {
        final String label;
        final int size;
        
        public ThreadSafeRunTimer(String label, int sampleCount)
        {
            this.label = label;
            this.size = sampleCount;
        }
        
        private ThreadLocal<RunTimer> timers = new ThreadLocal<RunTimer>()
        {
            @Override
            protected RunTimer initialValue()
            {
                return new RunTimer(ThreadSafeRunTimer.this.label, ThreadSafeRunTimer.this.size);
            }
        };
        
        public final void start()
        {
            timers.get().start();
        }
        
        public final void finish()
        {
            timers.get().finish();
        }
    }
    
    public static final ThreadSafeRunTimer THREADED_5000 = new ThreadSafeRunTimer("THREADED 5000", 5000);
    
    public static final ThreadSafeRunTimer THREADED_50K = new ThreadSafeRunTimer("THREADED 50K", 50000);
            
}
