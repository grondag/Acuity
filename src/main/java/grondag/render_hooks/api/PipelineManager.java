package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class PipelineManager implements IPipelineManager
{
    final static PipelineManager INSTANCE = new PipelineManager();
    
    private final RenderPipeline[] pipelines = new RenderPipeline[MAX_PIPELINES];
    
    private final IRenderPipeline[] defaultPipelines = new RenderPipeline[PipelineVertexFormat.values().length];
    private final IRenderPipeline waterPipeline;
    private final IRenderPipeline lavaPipeline;
    
    private final Object2ObjectOpenHashMap<Key, RenderPipeline> pipelineMap = new Object2ObjectOpenHashMap<>();

    private class Key
    {
        private final @Nonnull PipelineVertexFormat format;
        private final @Nonnull IProgram program;
        private final @Nullable IPipelineCallback callback;
        
        final int hash;
        
        private Key(
                @Nonnull PipelineVertexFormat format, 
                @Nonnull IProgram program, 
                @Nullable IPipelineCallback callback)
        {
            this.format = format;
            this.program = program;
            this.callback = callback;
            
            int hash = format.hashCode();
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
                
                return this.format == other.format
                        && this.program == other.program
                        && this.callback == other.callback;
            }
            return false;
        }
    }
    
    @SuppressWarnings("null")
    PipelineManager()
    {
        super();
        
        // add default pipelines
        for(PipelineVertexFormat format : PipelineVertexFormat.values())
        {
            defaultPipelines[format.ordinal()] = this.getOrCreatePipeline(format, ProgramManager.INSTANCE.getDefaultProgram(format), null);
        }
        this.waterPipeline = this.getOrCreatePipeline(PipelineVertexFormat.COMPATIBLE, ProgramManager.INSTANCE.getWaterProgram(), null);
        this.lavaPipeline = this.getOrCreatePipeline(PipelineVertexFormat.COMPATIBLE, ProgramManager.INSTANCE.getLavaProgram(), null);
    }
    
    @Nullable
    @Override
    public synchronized final IRenderPipeline getOrCreatePipeline(
            @Nonnull PipelineVertexFormat format, 
            @Nonnull IProgram program, 
            @Nullable IPipelineCallback callback)
    {
        Key key = new Key(format, program, callback);
        
        RenderPipeline result = this.pipelineMap.get(key);
        if(result == null && pipelineMap.size() < MAX_PIPELINES)
        {
            result = new RenderPipeline(format, program, callback);
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
    public final IRenderPipeline getDefaultPipeline(PipelineVertexFormat format)
    {
        return pipelines[format.ordinal()];
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
}
