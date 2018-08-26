package grondag.acuity.core;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.IAcuityListener;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.api.RenderPipeline;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AbstractPipelinedRenderList extends VboRenderList implements IAcuityListener
{
    protected boolean isAcuityEnabled = Acuity.isModEnabled();
    
    protected final ObjectArrayList<RenderChunk> chunks = new ObjectArrayList<RenderChunk>();
    private final ObjectArrayList<CompoundVertexBuffer> solidBuffers = new ObjectArrayList<CompoundVertexBuffer>();
    private final LongArrayList[] solidRenderLists;
    
    
    /**
     * Will hold the modelViewMatrix that was in GL context before first call to block render layer this pass.
     */
    protected final Matrix4f mvMatrix = new Matrix4f();
    protected final Matrix4f mvPos = new Matrix4f();
    protected final Matrix4f xlatMatrix = new Matrix4f();
    
    /**
     * Mimics what is in every render chunk. They all have the same matrix. 
     */
    protected final Matrix4f mvChunk = new Matrix4f();    

    protected final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    private int originX = Integer.MIN_VALUE;
    private int originY = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
  
    public AbstractPipelinedRenderList()
    {
        super();
        xlatMatrix.setIdentity();
        
        mvChunk.m00 = 1.000001f;
        mvChunk.m11 = 1.000001f;
        mvChunk.m22 = 1.000001f;
        mvChunk.m03 = 0.0000076293945f;
        mvChunk.m13 = 0.0000076293945f;
        mvChunk.m23 = 0.0000076293945f;
        mvChunk.m33 = 1.0f;
        
        solidRenderLists = new LongArrayList[PipelineManager.MAX_PIPELINES];
        for(int i = 0; i < PipelineManager.MAX_PIPELINES; i++)
        {
            solidRenderLists[i] = new LongArrayList();
        }
        
        AcuityRuntime.INSTANCE.registerListener(this);
    }

    @Override
    public void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        if(isAcuityEnabled)
        {
            this.chunks.add(renderChunkIn);
        }
        else
            super.addRenderChunk(renderChunkIn, layer);
    }
    
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(isAcuityEnabled)
        {
            if(layer == BlockRenderLayer.SOLID)
                renderChunkLayerSolid();
            else
                renderChunkLayerTranslucent();
        }
        else
            super.renderChunkLayer(layer);
    }
    
    private final void updateViewMatrix(BlockPos renderChunkOrigin)
    {
        final int ox = RenderCube.renderCubeOrigin(renderChunkOrigin.getX());
        final int oy = RenderCube.renderCubeOrigin(renderChunkOrigin.getY());
        final int oz = RenderCube.renderCubeOrigin(renderChunkOrigin.getZ());
        
        if(ox == originX && oz == originZ && oy == originY)
            return;

        originX = ox;
        originY = oy;
        originZ = oz;
        updateViewMatrixInner(ox, oy, oz);
    }
    
    private final void updateViewMatrixInner(final int ox, final int oy, final int oz)
    {
        final Matrix4f mvPos = this.mvPos;
        
        // note row-major order in the matrix library we are using
        xlatMatrix.m03 = (float)(ox -viewEntityX);
        xlatMatrix.m13 = (float)(oy -viewEntityY);
        xlatMatrix.m23 = (float)(oz - viewEntityZ);

        Matrix4f.mul(xlatMatrix, mvMatrix, mvPos);
        //TODO: confirm not needed - probably a hack to prevent seams/holes due to FP error
//        Matrix4f.mul(mvChunk, mvPos, mvPos);

        PipelineManager.setModelViewMatrix(mvPos);
    }
    
    private final void preRenderSetup()
    {
        originX = Integer.MIN_VALUE;
        originZ = Integer.MIN_VALUE;
        
        // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
        // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
        // We don't use these except for GL_VERTEX so disable them now unless we
        // are using VAOs, which will cause them to be ignored.
        // Not a problem to disable them because MC disables the when we return.
        if(!OpenGlHelperExt.isVaoEnabled())
            disableUnusedAttributes();
    }
    
    private final void disableUnusedAttributes()
    {
        GlStateManager.glDisableClientState(GL11.GL_COLOR_ARRAY);
        OpenGlHelperExt.setClientActiveTextureFast(OpenGlHelper.defaultTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelperExt.setClientActiveTextureFast(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        OpenGlHelperExt.setClientActiveTextureFast(OpenGlHelper.defaultTexUnit);
    }

    private final void downloadModelViewMatrix()
    {
        final FloatBuffer modelViewMatrixBuffer = this.modelViewMatrixBuffer;
        modelViewMatrixBuffer.position(0);
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrixBuffer);
        OpenGlHelperExt.loadTransposeQuickly(modelViewMatrixBuffer, mvMatrix);
    }
    
    final private static int SOLID_ORDINAL = BlockRenderLayer.SOLID.ordinal();
    @SuppressWarnings("null")
    protected final void renderChunkLayerSolid()
    {
        // Forge doesn't give us a hook in the render loop that comes
        // after camera transform is set up - so call out event handler
        // here as a workaround. Our event handler will only act 1x/frame.
        if(PipelineManager.INSTANCE.beforeRenderChunks())
            downloadModelViewMatrix();
        
        if (this.chunks.isEmpty()) 
            return; 
        
        preRenderSetup();
        
        final int lastPipelineIndex = populateSolidRenderLists();
        
        //TODO: precompute render cube and sort buffers by render cube also
        for(int n = 0; n <= lastPipelineIndex; n++)
        {
            final LongArrayList renderList = solidRenderLists[n];
            
            if(renderList.isEmpty())
                continue;
            
            final int bufferCount = renderList.size();
            for(int i = 0; i < bufferCount; i++)
            {
                final long val = renderList.getLong(i);
                final CompoundVertexBuffer vertexbuffer = solidBuffers.get((int) (val >> 32));
                
                updateViewMatrix(vertexbuffer.chunkPositionTransientDoNotUseExceptInSolidRenderSeriouslyIMeanIt);
                vertexbuffer.renderSolid((int)(val & 0xFFFFFFFF));
            }
            renderList.clear();
        }
        
        solidBuffers.clear();
        postRenderCleanup();
    }
    
    /**
     * Returns the highest populated pipeline index - inclusive
     */
    private int populateSolidRenderLists()
    {
        final ObjectArrayList<RenderChunk> chunks = this.chunks;
        final int chunkCount = chunks.size();
        final ObjectArrayList<CompoundVertexBuffer> solidBuffers = this.solidBuffers;
        final LongArrayList[] solidRenderLists = this.solidRenderLists;
        
        int result = -1;
        
        for (int i = 0; i < chunkCount; i++)
        {
            final RenderChunk renderchunk =  chunks.get(i);
            final CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(SOLID_ORDINAL);
            vertexbuffer.chunkPositionTransientDoNotUseExceptInSolidRenderSeriouslyIMeanIt = renderchunk.getPosition();
            vertexbuffer.prepareSolidRender();
            solidBuffers.add(vertexbuffer);
            VertexPackingList packingList = vertexbuffer.packingList();
            final int pCount = packingList.size();
            for(int j = 0; j < pCount; j++)
            {
                RenderPipeline p = packingList.getPipeline(j);
                final int pIndex = p.getIndex();
                solidRenderLists[pIndex].add(((long)i << 32) | j);
                if(pIndex > result)
                    result = pIndex;
            }
        }
        
        return result;
    }
    
    final private static int TRANSLUCENT_ORDINAL = BlockRenderLayer.TRANSLUCENT.ordinal();
    protected final void renderChunkLayerTranslucent()
    {
        final ObjectArrayList<RenderChunk> chunks = this.chunks;
        final int chunkCount = chunks.size();

        if (chunkCount == 0) 
            return;  
        
        preRenderSetup();
        
        for (int i = 0; i < chunkCount; i++)
        {
            final RenderChunk renderchunk =  chunks.get(i);
            final CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(TRANSLUCENT_ORDINAL);
            updateViewMatrix(renderchunk.getPosition());
            vertexbuffer.renderChunkTranslucent();
        }

        postRenderCleanup();
    }
    
    
    
    private final void postRenderCleanup()
    {
        if(OpenGlHelperExt.isVaoEnabled())
            OpenGlHelperExt.glBindVertexArray(0);
        
        OpenGlHelperExt.resetAttributes();
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        Program.deactivate();
        GlStateManager.resetColor();
        this.chunks.clear();
    }

    @Override
    public final void onAcuityStatusChange(boolean newEnabledStatus)
    {
        this.isAcuityEnabled = newEnabledStatus;
    }
}
