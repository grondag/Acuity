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

package grondag.acuity.opengl;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

public class VaoStore
{
    private static final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
    private static final IntBuffer buff = MemoryUtil.memAllocInt(128);
    
    public static int[] claimVertexArrays(int howMany)
    {
        int[] result = new int[howMany];
        for(int i = 0; i < howMany; i++)
        {
            result[i] = claimVertexArray();
        }
        return result;
    }
    
    public static int claimVertexArray()
    {
        if(queue.isEmpty())
        {
            OpenGlHelperExt.glGenVertexArrays(buff);
            
            for(int i = 0; i < 128; i++)
                queue.enqueue(buff.get(i));
            
            buff.clear();
        }
        
        return queue.dequeueInt();
    }
    
    public static void releaseVertexArray(int vaoBufferId)
    {
        queue.enqueue(vaoBufferId);
    }
    
    public static void releaseVertexArrays(int[] vaoBufferId)
    {
        for(int b : vaoBufferId)
            releaseVertexArray(b);
    }
    
    // should not be needed - Gl resources are destroyed when the context is destroyed
//    public static void deleteAll()
//    {
//        while(!queue.isEmpty())
//        {
//            while(!queue.isEmpty() && buff.position() < 128)
//                buff.put(queue.dequeueInt());
//            
//            if(OpenGlHelper.arbVbo)
//                ARBVertexBufferObject.glDeleteBuffersARB(buff);
//            else
//                GL15.glDeleteBuffers(buff);
//            
//            buff.clear();
//        }
//    }
}
