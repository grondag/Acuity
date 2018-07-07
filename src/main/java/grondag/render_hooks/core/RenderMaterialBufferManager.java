package grondag.render_hooks.core;

import javax.annotation.Nullable;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IPipelineManager;
import grondag.render_hooks.api.IRenderPipeline;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.ChunkCache;

public class RenderMaterialBufferManager
{
    private static BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
    private ObjectArrayList<BufferBuilder> buffers = new ObjectArrayList<>();
    
    private @Nullable BlockPos offset = null;
    
    private int nextBufferIndex;
    
    private BufferBuilder[] materialBuffers = new BufferBuilder[IPipelineManager.MAX_PIPELINE_COUNT];
    
    public RenderMaterialBufferManager()
    {
        System.out.println("boop");
    }
    
    public void prepare(BlockPos chunkMinPos)
    {
        System.arraycopy(EMPTY_ARRAY, 0, materialBuffers, 0, IPipelineManager.MAX_PIPELINE_COUNT);
        nextBufferIndex = 0;
        this.offset = chunkMinPos.toImmutable();
    }

    public boolean renderMaterials(IBlockState iblockstate, MutableBlockPos blockpos$mutableblockpos, ChunkCache worldView)
    {
        return false;
    }
    
    private BufferBuilder getMaterialBuffer(int materialIndex)
    {
        BufferBuilder result = materialBuffers[materialIndex];
        if(result == null)
            result = getInitializedBuffer(materialIndex);
        
        return result;
    }
    
    private BufferBuilder getInitializedBuffer(int materialIndex)
    {
        BufferBuilder result;
        
        if(nextBufferIndex < buffers.size())
        {
            result = buffers.get(nextBufferIndex++);
        }
        else
        {
            result = new BufferBuilder(1024);
            buffers.add(result);
            nextBufferIndex = buffers.size();
        }
        IRenderPipeline material = RenderHooks.INSTANCE.runtime.pipelineManager.getMaterial(materialIndex);
        result.begin(material.glMode(), material.vertexFormat());
        result.setTranslation((double)(-offset.getX()), (double)(-offset.getY()), (double)(-offset.getZ()));
        return result;
    }
}
