package grondag.render_hooks.core;

import java.nio.ByteBuffer;
import java.util.List;

import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CompoundListedRenderChunk extends ListedRenderChunk
{
    /**
     * Holds all allocated gl lists, including those claimed by super 
     */
//    private IntArrayList glListIds = new IntArrayList();
//    private int nextAvailableListIndex = 0;
    
//    private int layerPopulatedFlags = 0;
//    private int currentLayerFlag = 0;
    
//    private int[] slotsInUse = new int[BlockRenderLayer.values().length];
//    private IRenderPipeline[][] pipelines = new IRenderPipeline[BlockRenderLayer.values().length][IPipelineManager.MAX_PIPELINES];
//    private int[][] pipelineListIds = new int[BlockRenderLayer.values().length][IPipelineManager.MAX_PIPELINES];
//    private int[][] pipelineCounts = new int[BlockRenderLayer.values().length][IPipelineManager.MAX_PIPELINES];
    
    public CompoundListedRenderChunk(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);
//        this.glListIds.add(this.baseDisplayList);
//        this.glListIds.add(this.baseDisplayList + 1);
//        this.glListIds.add(this.baseDisplayList + 2);
//        this.glListIds.add(this.baseDisplayList + 3);
    }

    @Override
    public void deleteGlResources()
    {
        super.deleteGlResources();
//        for(int i : this.glListIds)
//        {
//            if(i >= 0)
//                GLAllocation.deleteDisplayLists(i);
//        }
//        this.glListIds.clear();
//        
//        for(BlockRenderLayer l : BlockRenderLayer.values())
//            this.slotsInUse[l.ordinal()] = 0;
//        
//        this.nextAvailableListIndex = 0;
    }

    /**
     * Always returns glList for layer even if empty as way 
     * of passing render layer to API without changing method signatures.
     */
    @Override
    public int getDisplayList(BlockRenderLayer layer, CompiledChunk p_178600_2_)
    {
        // To avoid changing method signatures in ASM we detect the start of a new pass here.
        // Relies on the way that ChunkRenderDispatcher calls this in blocklayer order
        // and only calls from single thread.
//        if(layer == BlockRenderLayer.SOLID)
//            this.layerPopulatedFlags = 0;
//        
//        this.currentLayerFlag = 1 << layer.ordinal();
        
//        this.slotsInUse[layer.ordinal()] = 0;
//
//        // only when we do first (solid) layer, make all gl lists available to claim
//        if(layer == BlockRenderLayer.SOLID)
//            this.nextAvailableListIndex = 0;
//            
        return this.baseDisplayList + layer.ordinal();
//        return super.getDisplayList(layer, p_178600_2_);
    }
    
    public void prepareForUpload(int vanillaList)
    {
//        final int layerIndex = vanillaList - this.baseDisplayList;
//        
//        if(layerIndex == 0)
//            this.layerPopulatedFlags = 0;
//  
//        this.currentLayerFlag = 1 << layerIndex;
        
        GlStateManager.glNewList(vanillaList, 4864);
    }

    public void uploadBuffer(IRenderPipeline pipeline, BufferBuilder bufferBuilderIn)
    {
//        final int slotsInUse = this.slotsInUse[this.currentLayerIndex]++;
//        
//        pipelines[this.currentLayerIndex][slotsInUse] = pipeline;
//        
//        final int glId = getAvailableListId();
//        pipelineListIds[this.currentLayerIndex][slotsInUse] = glId;
//
//        pipelineCounts[this.currentLayerIndex][slotsInUse] = bufferBuilderIn.getVertexCount();
//        this.layerPopulatedFlags |= this.currentLayerFlag;
        
        GlStateManager.pushMatrix();
        this.multModelviewMatrix();
        pipeline.preDrawList();
        drawList(bufferBuilderIn);
        pipeline.postDrawList();
        GlStateManager.popMatrix();
    }
    
    public void completeUpload()
    {
        GlStateManager.glEndList();        
    }
    
//    private int getAvailableListId()
//    {
//        int result;
//        if(this.nextAvailableListIndex < glListIds.size())
//        {
//            return glListIds.getInt(nextAvailableListIndex++);
//        }
//        else
//        {
//            result = OpenGlHelper.glGenBuffers();
//            glListIds.add(result);
//            nextAvailableListIndex++;
//            return result;
//        }
//    }
    
    /**
     * Static adaptation of WorldVertexBufferUploader
     */
    private static void drawList(BufferBuilder bufferBuilderIn)
    {
        if (bufferBuilderIn.getVertexCount() > 0)
        {
            VertexFormat vertexformat = bufferBuilderIn.getVertexFormat();
            int i = vertexformat.getNextOffset();
            ByteBuffer bytebuffer = bufferBuilderIn.getByteBuffer();
            List<VertexFormatElement> list = vertexformat.getElements();

            for (int j = 0; j < list.size(); ++j)
            {
                VertexFormatElement vertexformatelement = list.get(j);
                bytebuffer.position(vertexformat.getOffset(j));
                vertexformatelement.getUsage().preDraw(vertexformat, j, i, bytebuffer);
            }

            GlStateManager.glDrawArrays(bufferBuilderIn.getDrawMode(), 0, bufferBuilderIn.getVertexCount());
            int i1 = 0;

            for (int j1 = list.size(); i1 < j1; ++i1)
            {
                VertexFormatElement vertexformatelement1 = list.get(i1);
                vertexformatelement1.getUsage().postDraw(vertexformat, i1, i, bytebuffer);
            }
        }

        bufferBuilderIn.reset();
    }
}
