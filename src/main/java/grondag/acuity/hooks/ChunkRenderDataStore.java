package grondag.acuity.hooks;

import java.util.concurrent.ArrayBlockingQueue;

import grondag.acuity.mixin.extension.ChunkRenderDataExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkRenderData;

@Environment(EnvType.CLIENT)
public class ChunkRenderDataStore
{
    private static final ArrayBlockingQueue<ChunkRenderData> chunkDataPool = new ArrayBlockingQueue<>(4096);
 
    public static ChunkRenderData claim()
    {
        ChunkRenderData result = chunkDataPool.poll();
        if(result == null)
            result = new ChunkRenderData();
        
        return result;
    }
    
    public static void release(ChunkRenderData chunkRenderData)
    {
        ((ChunkRenderDataExt)chunkRenderData).clear();
        chunkDataPool.offer(chunkRenderData);
    }
}
