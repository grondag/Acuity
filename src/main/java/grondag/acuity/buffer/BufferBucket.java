package grondag.acuity.buffer;
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

//package grondag.acuity.buffering;
//
//import javax.annotation.Nullable;
//
//import net.minecraft.util.math.MathHelper;
//
//public class BufferBucket
//{
//    /** Must be a power of 2 */
//    public static final int MAX_BUFFER_BYTES = 512 * 1024;
//    
//    /** Must be a power of 2 */
//    public static final int MIN_BUFFER_BYTES = 1024;
//
//    public static final int BUCKET_COUNT;
//    
//    public static final BufferBucket BIGGEST_BUCKET;
//    public static final BufferBucket SMALLEST_BUCKET;
//
//    private static final BufferBucket[] BUCKETS;
//    
//    private static final int LOG_OFFSET;
//    
//    static
//    {
//        final int size = Integer.numberOfTrailingZeros(MAX_BUFFER_BYTES)
//                - Integer.numberOfTrailingZeros(MIN_BUFFER_BYTES)
//                + 1;
//        
//        BUCKET_COUNT = size;
//        BUCKETS = new BufferBucket[BUCKET_COUNT];
//        
//        for(int i = 0; i < BUCKET_COUNT; i++)
//            BUCKETS[i] = new BufferBucket(i);
//        
//        SMALLEST_BUCKET = BUCKETS[0];
//        BIGGEST_BUCKET = BUCKETS[BUCKET_COUNT - 1];
//        
//        LOG_OFFSET = MathHelper.log2(MIN_BUFFER_BYTES);
//    }
//    
//    public static BufferBucket findSmalledBucketContaining(int byteCount)
//    {
//        final int index = MathHelper.clamp(MathHelper.log2DeBruijn(byteCount) - LOG_OFFSET, 0, BUCKET_COUNT - 1);
//        BufferBucket result = BUCKETS[index];
//        assert result.isMax || result.bytes >= byteCount;
//        return result;
//    }
//    
//    public static @Nullable BufferBucket findRemainderBucket(int byteCount)
//    {
//        final int index = MathHelper.log2(byteCount) - LOG_OFFSET;
//        BufferBucket result = index < 0 ? null : BUCKETS[index];
//        assert result == null || byteCount >= result.bytes;
//        return result;
//    }
//    
//    public final int ordinal;
//    public final int divisionLevel;
//    public final boolean isMax;
//    public final boolean isMin;
//    public final int bytes;
//    
//    private BufferBucket(int ordinal)
//    {
//        this.ordinal = ordinal;
//        this.divisionLevel = BUCKET_COUNT - 1 - ordinal;
//        this.bytes = MAX_BUFFER_BYTES >> divisionLevel;
//        this.isMax = ordinal == BUCKET_COUNT - 1;
//        this.isMin = ordinal == 0;
//    }
//    
//    public final @Nullable BufferBucket bigger()
//    {
//        return this.isMax ? null : BUCKETS[ordinal + 1];
//    }
//    
//    public final @Nullable BufferBucket smaller()
//    {
//        return this.isMin ? null : BUCKETS[ordinal - 1];
//    }
//}
