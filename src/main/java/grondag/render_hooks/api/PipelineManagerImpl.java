package grondag.render_hooks.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class PipelineManagerImpl extends PipelineManager
{
    final static PipelineManagerImpl INSTANCE = new PipelineManagerImpl();
    
    private final RenderPipelineImpl[] pipelines = new RenderPipelineImpl[MAX_PIPELINES];
    
    private final RenderPipeline[] defaultPipelines = new RenderPipelineImpl[PipelineVertexFormat.values().length];
    
    
    private final Object2ObjectOpenHashMap<Key, RenderPipelineImpl> pipelineMap = new Object2ObjectOpenHashMap<>();

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
            if(program != null)
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
        public boolean equals(Object obj)
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
    
    PipelineManagerImpl()
    {
        super();
        
        // add default pipelines
        for(PipelineVertexFormat format : PipelineVertexFormat.values())
        {
            defaultPipelines[format.ordinal()] = this.getOrCreatePipeline(format, ProgramManager.INSTANCE.getDefaultProgram(format), null);
        }
    }
    
    @Nullable
    @Override
    public synchronized final RenderPipeline getOrCreatePipeline(
            @Nonnull PipelineVertexFormat format, 
            @Nonnull IProgram program, 
            @Nullable IPipelineCallback callback)
    {
        Key key = new Key(format, program, callback);
        
        RenderPipelineImpl result = this.pipelineMap.get(key);
        if(result == null && pipelineMap.size() < MAX_PIPELINES)
        {
            result = new RenderPipelineImpl(format, program, callback);
            this.pipelineMap.put(key, result);
            this.pipelines[result.getIndex()] = result;
        }
        
        return result;
    }
    
    public final RenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines[pipelineIndex];
    }

    @Override
    public final RenderPipeline getDefaultPipeline(PipelineVertexFormat format)
    {
        return pipelines[format.ordinal()];
    }
}
