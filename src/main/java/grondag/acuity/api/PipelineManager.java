package grondag.acuity.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import grondag.acuity.Configurator;
import grondag.acuity.api.IPipelineCallback;
import grondag.acuity.api.IPipelineManager;
import grondag.acuity.api.IProgram;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.TextureFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineManager implements IPipelineManager
{
    /**
     * Will always be 1, defined to clarify intent in code.
     */
    public static final int FIRST_CUSTOM_PIPELINE_INDEX = 1;

    /**
     * Will always be 0, defined to clarify intent in code.
     */
    public static final int VANILLA_MC_PIPELINE_INDEX = 0;

    public static final int MAX_PIPELINES = Configurator.maxPipelines;
    
    public static final PipelineManager INSTANCE = new PipelineManager();
    
    private final RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    
    private final RenderPipeline[] defaultPipelines = new RenderPipeline[TextureFormat.values().length];
    public final RenderPipeline waterPipeline;
    public final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;
    
    private final Object2ObjectOpenHashMap<Key, RenderPipeline> pipelineMap = new Object2ObjectOpenHashMap<>();

    private class Key
    {
        private final TextureFormat textureFormat;
        private final IProgram program;
        private final @Nullable IPipelineCallback callback;
        
        final int hash;
        
        private Key(
                TextureFormat textureFormat, 
                IProgram program, 
                @Nullable IPipelineCallback callback)
        {
            this.textureFormat = textureFormat;
            this.program = program;
            this.callback = callback;
            
            int hash = textureFormat.hashCode();
            hash ^= program.hashCode();
            if(callback != null)
                hash ^= callback.hashCode();
            
            this.hash = hash;
        }

        @Override
        public int hashCode()
        {
            return this.hash;
        }

        @Override
        public boolean equals(@Nullable Object obj)
        {
            if(obj == this) return true;
            if(obj == null) return false;
            if(obj instanceof Key)
            {
                Key other = (Key)obj;
                
                return this.textureFormat == other.textureFormat
                        && this.program == other.program
                        && this.callback == other.callback;
            }
            return false;
        }
    }
    
    @SuppressWarnings("null")
    private PipelineManager()
    {
        super();
        
        // add default pipelines
        for(TextureFormat textureFormat : TextureFormat.values())
        {
            defaultPipelines[textureFormat.ordinal()] = this.getOrCreatePipeline(textureFormat, ProgramManager.INSTANCE.getDefaultProgram(textureFormat), null);
        }
        this.waterPipeline = this.getOrCreatePipeline(TextureFormat.SINGLE, ProgramManager.INSTANCE.getWaterProgram(), null);
        this.lavaPipeline = this.getOrCreatePipeline(TextureFormat.SINGLE, ProgramManager.INSTANCE.getLavaProgram(), null);
        this.defaultSinglePipeline = defaultPipelines[0];
    }
    
    public void forceReload()
    {
        this.pipelineMap.values().forEach(p -> p.refreshVertexFormats());
    }
    
    @Nullable
    @Override
    public synchronized final RenderPipeline getOrCreatePipeline(
            @Nonnull TextureFormat textureFormat, 
            @Nonnull IProgram program, 
            @Nullable IPipelineCallback callback)
    {
        Key key = new Key(textureFormat, program, callback);
        
        RenderPipeline result = this.pipelineMap.get(key);
        if(result == null && pipelineMap.size() < PipelineManager.MAX_PIPELINES)
        {
            result = new RenderPipeline(textureFormat, program, callback);
            this.pipelineMap.put(key, result);
            this.pipelines[result.getIndex()] = result;
        }
        
        return result;
    }
    
    public final IRenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines[pipelineIndex];
    }

    @Override
    public final IRenderPipeline getDefaultPipeline(TextureFormat textureFormat)
    {
        return pipelines[textureFormat.ordinal()];
    }
    
    @Override
    public final IRenderPipeline getWaterPipeline()
    {
        return this.waterPipeline;
    }
    
    @Override
    public final IRenderPipeline getLavaPipeline()
    {
        return this.lavaPipeline;
    }

    @Override
    public IRenderPipeline getPipelineByIndex(int index)
    {
        return this.pipelines[index];
    }
}
