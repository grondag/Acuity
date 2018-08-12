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
    
    protected final void renderChunkLayerAcuity(BlockRenderLayer layer)
    {
        // NB: Vanilla MC will have already enabled GL_VERTEX_ARRAY, GL_COLOR_ARRAY
        // and GL_TEXTURE_COORD_ARRAY for both default texture and lightmap.
        final int chunkCount = this.chunkCount;
        
        if (chunkCount == 0) 
            return;
        
        final boolean isSolidLayer = layer != BlockRenderLayer.TRANSLUCENT;
        final RenderChunk[] chunks = this.chunks;
        final Matrix4f mvPos = this.mvPos;
        final Matrix4f xlatMatrix = this.xlatMatrix;
        final Matrix4f mvChunk = this.mvChunk;
        final double viewX = -this.viewEntityX;
        final double viewY = -this.viewEntityY;
        final double viewZ = -this.viewEntityZ;
        final Matrix4f mvMatrix = this.mvMatrix;
        
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
        
        for (int i = 0; i < chunkCount; i++)
        {
            final RenderChunk renderchunk = chunks[i];
            final CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderchunk.getVertexBufferByLayer(layer.ordinal());
            
            final BlockPos blockpos = renderchunk.getPosition();
            // note row-major order in the matrix library we are using
            xlatMatrix.m03 = (float)(viewX + blockpos.getX());
            xlatMatrix.m13 = (float)(viewY + blockpos.getY());
            xlatMatrix.m23 = (float)(viewZ + blockpos.getZ());
            
            Matrix4f.mul(xlatMatrix, mvMatrix, mvPos);
            Matrix4f.mul(mvChunk, mvPos, mvPos);
            
            PipelineManager.setModelViewMatrix(mvPos);
            
            vertexbuffer.renderChunk(isSolidLayer);
        }
        
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
