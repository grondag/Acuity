package grondag.render_hooks.api;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class PipelineManagerImpl extends PipelineManager
{
    final static PipelineManagerImpl INSTANCE = new PipelineManagerImpl();
    
    private RenderPipelineImpl[] pipelines = new RenderPipelineImpl[MAX_PIPELINES];
    
    private Object2ObjectOpenHashMap<Key, RenderPipelineImpl> pipelineMap = new Object2ObjectOpenHashMap<>();

    private class Key
    {
        private final @Nonnull PipelineVertexFormat format;
        private final @Nullable String vertexShaderFileName;
        private final @Nullable String fragmentShaderFileName;
        private final @Nullable IPipelineCallback callback;
        
        final int hash;
        
        private Key(
                @Nonnull PipelineVertexFormat format, 
                @Nullable String vertexShaderFileName, 
                @Nullable String fragmentShaderFileName,
                @Nullable IPipelineCallback callback)
        {
            this.format = format;
            this.vertexShaderFileName = vertexShaderFileName;
            this.fragmentShaderFileName = fragmentShaderFileName;
            this.callback = callback;
            
            int hash = format.hashCode();
            if(vertexShaderFileName != null)
                hash ^= vertexShaderFileName.hashCode();
            if(fragmentShaderFileName != null)
                hash ^= fragmentShaderFileName.hashCode();
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
                
                return Objects.equals(this.format, other.format)
                        && Objects.equals(this.vertexShaderFileName, other.vertexShaderFileName)
                        && Objects.equals(this.fragmentShaderFileName, other.fragmentShaderFileName)
                        && Objects.equals(this.callback, other.callback);
                        
            }
            return false;
        }
    }
    
    PipelineManagerImpl()
    {
        super();
        
        // add vanilla MC pipeline
        this.getOrCreatePipeline(PipelineVertexFormat.MINECRAFT, null, null, null);
    }
    
    @Nullable
    @Override
    public synchronized final RenderPipeline getOrCreatePipeline(
            @Nonnull PipelineVertexFormat format, 
            @Nullable String vertexShaderFileName, 
            @Nullable String fragmentShaderFileName,
            @Nullable IPipelineCallback callback)
    {
        Key key = new Key(format, vertexShaderFileName, fragmentShaderFileName, callback);
        
        RenderPipelineImpl result = this.pipelineMap.get(key);
        if(result == null && pipelineMap.size() < MAX_PIPELINES)
        {
            result = new RenderPipelineImpl(format, vertexShaderFileName, fragmentShaderFileName, callback);
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
    public final RenderPipeline getVanillaPipeline()
    {
        return pipelines[0];
    }
}
