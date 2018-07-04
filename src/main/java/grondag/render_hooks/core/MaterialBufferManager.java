package grondag.render_hooks.core;

import grondag.render_hooks.RenderHooks;
import grondag.render_hooks.api.IMaterialManager;
import grondag.render_hooks.api.IMaterialRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;

public class MaterialBufferManager
{
    private static BufferBuilder[] EMPTY_ARRAY = new BufferBuilder[IMaterialManager.MAX_MATERIAL_COUNT];
    
    private ObjectArrayList<BufferBuilder> buffers = new ObjectArrayList<>();
    
    private int nextBufferIndex;
    
    private BufferBuilder[] materialBuffers = new BufferBuilder[IMaterialManager.MAX_MATERIAL_COUNT];
    
    public void prepare()
    {
        System.arraycopy(EMPTY_ARRAY, 0, materialBuffers, 0, IMaterialManager.MAX_MATERIAL_COUNT);
        nextBufferIndex = 0;
    }
    
    public BufferBuilder getMaterialBuffer(int materialIndex, BlockPos chunkStartPos)
    {
        BufferBuilder result = materialBuffers[materialIndex];
        if(result == null)
            result = getInitializedBuffer(materialIndex, chunkStartPos);
        
        return result;
    }
    
    private BufferBuilder getInitializedBuffer(int materialIndex, BlockPos chunkStartPos)
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
        IMaterialRenderer material = RenderHooks.INSTANCE.runtime.materialManager.getMaterial(materialIndex);
        result.begin(material.glMode(), material.vertexFormat());
        result.setTranslation((double)(-chunkStartPos.getX()), (double)(-chunkStartPos.getY()), (double)(-chunkStartPos.getZ()));
        return result;
    }
}
