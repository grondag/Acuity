package grondag.acuity.core;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import grondag.acuity.Acuity;
import grondag.acuity.api.RenderPipeline;
import grondag.acuity.buffering.IDrawableChunk;
import grondag.acuity.buffering.IUploadableChunk;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundBufferBuilder extends BufferBuilder
{
    /**
     * Holds vertex data and packing data for next upload if we have it.
     * Buffer is obtained from BufferStore and will be released back to store by upload.
     */
    private AtomicReference<IUploadableChunk>  uploadState  = new AtomicReference<>();
    
    /**
     * During drawing collects vertex info. Should be null other times.
     */
    @Nullable VertexCollectorList collectors;
    
    /**
     * Tells us which block layer we are buffering.
     * Can be used with {@link #owner} to find peer buffers for other layers.
     * Could also be handy for other purposes.
     */
    private @Nullable BlockRenderLayer layer;

    private @Nullable CompoundBufferBuilder proxy;
    
    private class CompoundState extends State
    {
        private final VertexCollectorList stateCollectors;
        
        public CompoundState(int[] buffer, VertexFormat format, VertexCollectorList collectors)
        {
            super(buffer, format);
            this.stateCollectors = collectors;
        }
    }
    
    public CompoundBufferBuilder(int bufferSizeIn)
    {
        super(limitBufferSize(bufferSizeIn));
    }
    
    /**
     * Called at end of RegionRenderCacheBuilder init via ASM.
     */
    public void setupLinks(RegionRenderCacheBuilder owner, BlockRenderLayer layer)
    {
        this.layer = layer;
        
        if(this.layer == BlockRenderLayer.CUTOUT || this.layer == BlockRenderLayer.CUTOUT_MIPPED)
        {
            this.proxy = (CompoundBufferBuilder) owner.getWorldRendererByLayer(BlockRenderLayer.SOLID);
        }
    }
    
    /**
     * The RegionRenderCacheBuilder instantiates this with significant sizes
     * but in most cases the super instance won't be used when the mod is enabled
     * so don't honor these when mod is enabled to reduce memory footprint.
     */
    static final int limitBufferSize(int bufferSizeIn)
    {
        if(Acuity.isModEnabled())
        {
            if(bufferSizeIn == 2097152 || bufferSizeIn == 131072 || bufferSizeIn == 262144)
            {
                return 2048;
            }
        }
        return bufferSizeIn;
    }
    
    /**
     * Used to retrieve and save collector state for later resorting of translucency.<p>
     * 
     * Temporarily means we have a reference to state in two places but reference in 
     * this instance will be removed during {@link #finishDrawingIfNotAlreadyFinished()}.
     */
    @Override
    public State getVertexState()
    {
        if(Acuity.isModEnabled())
        {
            assert this.collectors != null : "getVertexState called more than one for same collector state.";
            assert this.proxy == null;
            assert this.layer == BlockRenderLayer.TRANSLUCENT;
            
            State inner = super.getVertexState();
            @SuppressWarnings("null")
            CompoundState result = new CompoundState(inner.getRawBuffer(), inner.getVertexFormat(), this.collectors);
            return result;
        }
        else
            return super.getVertexState();
    }

    @SuppressWarnings("null")
    @Override
    public void setVertexState(State state)
    {
        super.setVertexState(state);
        if(Acuity.isModEnabled())
        {
            assert this.proxy == null;
            assert this.layer == BlockRenderLayer.TRANSLUCENT;
            assert this.collectors.isEmpty() : "Non-empty collector state when restoring vertex state.";
            
            VertexCollectorList.release(this.collectors);
            this.collectors = ((CompoundState)state).stateCollectors;
            
            assert this.collectors != null;
        }
    }

    @Override
    public void reset()
    {
        super.reset();
        if(Acuity.isModEnabled())
        {
            assert this.collectors == null : "CompoundBufferBuilder reset before vertex collector list consumed";
            this.collectors = VertexCollectorList.claim();
        }
    }
    
    @SuppressWarnings("null")
    public VertexCollector getVertexCollector(RenderPipeline pipeline)
    {
        if(Acuity.isModEnabled() && this.proxy != null)
            return this.proxy.getVertexCollector(pipeline);
        
        return this.collectors.getOrCreate(pipeline);
    }
    
    public void beginIfNotAlreadyDrawing(int glMode, VertexFormat format)
    {
        if(!this.isDrawing)
        {
            assert this.layer == BlockRenderLayer.SOLID || this.layer == BlockRenderLayer.TRANSLUCENT || !Acuity.isModEnabled();
            
            // NB: this calls reset which initializes collector list
            super.begin(glMode, format);
        }
    }
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        if(Acuity.isModEnabled() && proxy != null)
            proxy.beginIfNotAlreadyDrawing(glMode, format);
        else
            beginIfNotAlreadyDrawing(glMode, format);
    }
    
    @SuppressWarnings("null")
    public void finishDrawingIfNotAlreadyFinished()
    {
        if(this.isDrawing)
        {
            super.finishDrawing();
            
            if(Acuity.isModEnabled())
            {
                switch(this.layer)
                {
                    case SOLID:
                        this.uploadState.getAndSet(collectors.packUploadSolid());
//                        Pair<ExpandableByteBuffer, VertexPackingList> pair = collectors.packUpload();
//                        if(this.uploadState.getAndSet(pair) != null)
//                            System.out.println(Integer.toHexString(CompoundBufferBuilder.this.hashCode()) + " Discarding & replacing upload state (Solid) in Compound Vertex Buffer - probably because rebuild overtook upload queue");
                        
                        VertexCollectorList.release(collectors);
                        collectors = null;
                        return;
                    
                    case TRANSLUCENT:
                        this.uploadState.getAndSet(collectors.packUploadTranslucent());
//                        Pair<ExpandableByteBuffer, VertexPackingList> pair = collectors.packUploadSorted();
//                        if(this.uploadState.getAndSet(pair) != null)
//                            System.out.println(Integer.toHexString(CompoundBufferBuilder.this.hashCode()) + " Discarding & replacing upload state (Translucent) in Compound Vertex Buffer - probably because rebuild overtook upload queue");
                        
                        // can't release collector list because retained in vertex state
                        // but remove reference to prevent mishap
                        collectors = null;
                        return;
                    
                    case CUTOUT:
                    case CUTOUT_MIPPED:
                    default:
                        assert false : "Bad render layer in compound buffer builder finish";
                        break;
                
                }
            }
        }
    }
    
    @Override
    public void finishDrawing()
    {
        if(Acuity.isModEnabled() && proxy != null)
        {
            proxy.finishDrawingIfNotAlreadyFinished();
            return;
        }
        else
            this.finishDrawingIfNotAlreadyFinished();
       
    }

    public void uploadTo(IDrawableChunk target)
    {   
        assert this.layer == BlockRenderLayer.SOLID || this.layer == BlockRenderLayer.TRANSLUCENT;
        
        IUploadableChunk uploadBuffer = this.uploadState.getAndSet(null);
        if(uploadBuffer == null)
        {
//            System.out.println(Integer.toHexString(CompoundBufferBuilder.this.hashCode()) + " Ignoring upload request due to missing upload state in Compound Vertex Buffer (" + layer.toString() + ") - must have been loaded earlier");
            return;
        }

        target.upload(uploadBuffer);
    }
    
//    public static final ConcurrentHashMap<BlockPos, Long> SORTS = new ConcurrentHashMap<>();
//    private @Nullable BlockPos chunkOriginPos;
    
//    @Override
//    public void setTranslation(double x, double y, double z)
//    {
//        super.setTranslation(x, y, z);
//        chunkOriginPos = new BlockPos((MathHelper.fastFloor(-x) >> 4) << 4, (MathHelper.fastFloor(-y) >> 4) << 4, (MathHelper.fastFloor(-z) >> 4) << 4);
//    }
    
    @SuppressWarnings("null")
    @Override
    public void sortVertexData(float x, float y, float z)
    {
//        SORTS.put(chunkOriginPos, System.nanoTime());
        
        if(Acuity.isModEnabled())
            // save sort perspective coordinate for use during packing.  Actual sort occurs then.
            collectors.setViewCoordinates(x, y, z);
        else
            super.sortVertexData(x, y, z);
    }
}
