package grondag.render_hooks.core;

import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.renderer.BufferBuilder;

/**
 * Holds a thread-safe cache of buffer builders to be used for VBO uploads
 */
public class BufferStore
{
    private static final ConcurrentLinkedQueue<BufferBuilder> store = new ConcurrentLinkedQueue<BufferBuilder>();
    
    public static BufferBuilder claim()
    {
        BufferBuilder result =  store.poll();
        return result == null ? new BufferBuilder(2097152) : result;
    }
    
    public static void release(BufferBuilder buffer)
    {
        buffer.reset();
        store.offer(buffer);
    }
}
