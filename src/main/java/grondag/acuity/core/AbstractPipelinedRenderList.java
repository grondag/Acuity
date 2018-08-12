package grondag.acuity.core;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.IAcuityListener;
import grondag.acuity.api.PipelineManager;
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
    
    /**
     * Faster than array list - enough to make a difference.
     * The fixed size is sloppy & risky - but <em>should</em> be enough to hold all visible in any reasonable scenario.
     */
    protected final RenderChunk[] chunks = new RenderChunk[64000];
    protected int chunkCount = 0;
    
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
        
        AcuityRuntime.INSTANCE.registerListener(this);
    }

    @Override
    public void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        if(isAcuityEnabled)
            this.chunks[this.chunkCount++] = renderChunkIn;
        else
            super.addRenderChunk(renderChunkIn, layer);
    }

    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if(isAcuityEnabled)
            renderChunkLayerAcuity(layer);
        else
            super.renderChunkLayer(layer);
    }
    
    private final void updateViewMatrix(BlockPos renderChunkOrigin)
    {
        final int ox = Utility.renderCubeOrigin(renderChunkOrigin.getX());
        final int oy = Utility.renderCubeOrigin(renderChunkOrigin.getY());
        final int oz = Utility.renderCubeOrigin(renderChunkOrigin.getZ());
        
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
        
        // Forge doesn't give us a hook in the render loop that comes
        // after camera transform is set up - so call out event handler
        // here as a workaround. Our event handler will only act 1x/frame.
        if(PipelineManager.INSTANCE.beforeRenderChunks())
        {
            final FloatBuffer modelViewMatrixBuffer = this.modelViewMatrixBuffer;
            modelViewMatrixBuffer.position(0);
            GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrixBuffer);
            OpenGlHelperExt.loadTransposeQuickly(modelViewMatrixBuffer, mvMatrix);
        }
    }
    
    // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
    // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
    protected final void renderChunkLayerAcuity(BlockRenderLayer layer)
    {
        final int chunkCount = this.chunkCount;
        if (chunkCount == 0) 
            return;
        
        preRenderSetup();
        
        final int layerOrdinal = layer.ordinal();
        final RenderChunk[] chunks = this.chunks;
        
        for (int i = 0; i < chunkCount; i++)
        {
            renderTheChunk(chunks[i], layerOrdinal);
        }
        
        postRenderCleanup();
    }
    
    final private static int TRANSLUCENT_ORDINAL = BlockRenderLayer.TRANSLUCENT.ordinal();
    
    private final void renderTheChunk(final RenderChunk renderchunk, int layerOrdinal)
    {
        final CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layerOrdinal);
        updateViewMatrix(renderchunk.getPosition());
        vertexbuffer.renderChunk(layerOrdinal != TRANSLUCENT_ORDINAL);
    }
    
    private final void postRenderCleanup()
    {
        if(OpenGlHelperExt.isVaoEnabled())
            OpenGlHelperExt.glBindVertexArray(0);
        
        OpenGlHelperExt.resetAttributes();
        OpenGlHelperExt.glBindBufferFast(OpenGlHelper.GL_ARRAY_BUFFER, 0);
        OpenGlHelperExt.glUseProgramFast(0);
        GlStateManager.resetColor();
        this.chunkCount = 0;
    }

    @Override
    public final void onAcuityStatusChange(boolean newEnabledStatus)
    {
        this.isAcuityEnabled = newEnabledStatus;
    }
}
