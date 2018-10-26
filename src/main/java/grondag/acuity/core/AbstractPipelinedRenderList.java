package grondag.acuity.core;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

import grondag.acuity.Acuity;
import grondag.acuity.api.AcuityRuntime;
import grondag.acuity.api.IAcuityListener;
import grondag.acuity.api.PipelineManager;
import grondag.acuity.opengl.OpenGlHelperExt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class AbstractPipelinedRenderList implements IAcuityListener
{
    protected final VboRenderList parent;
    
    public boolean isAcuityEnabled = Acuity.isModEnabled();
    
    protected final ObjectArrayList<RenderChunk> chunks = new ObjectArrayList<RenderChunk>();
    
    /**
     * Each entry is for a single 256^3 render cube.<br>
     * The entry is an array of CompoundVertexBuffers.<br>
     * The array is as long as the highest pipeline index.<br> 
     * Null values mean that pipeline isn't part of the render cube.<br>
     * Non-null values are lists of buffer in that cube with the given pipeline.<br>
     */
    private final Long2ObjectOpenHashMap<ObjectArrayList<CompoundVertexBuffer>[]> solidCubes = new Long2ObjectOpenHashMap<>();
    
    /**
     * Cache and reuse cube data stuctures.
     */
    private final ArrayDeque<ObjectArrayList<CompoundVertexBuffer>[]> cubeStore = new ArrayDeque<>();
    
    /**
     * Will hold the modelViewMatrix that was in GL context before first call to block render layer this pass.
     */
    protected final Matrix4f mvMatrix = new Matrix4f();
    protected final Matrix4f mvPos = new Matrix4f();
    protected final Matrix4f xlatMatrix = new Matrix4f();
    
    protected final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    private int originX = Integer.MIN_VALUE;
    private int originY = Integer.MIN_VALUE;
    private int originZ = Integer.MIN_VALUE;
  
    public AbstractPipelinedRenderList(VboRenderList parent)
    {
        this.parent = parent;
        xlatMatrix.setIdentity();
        AcuityRuntime.INSTANCE.registerListener(this);
    }

    public void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        if(isAcuityEnabled)
        {
            if(layer == BlockRenderLayer.TRANSLUCENT)
                this.chunks.add(renderChunkIn);
            else
                this.addSolidChunk(renderChunkIn);
        }
        else
            parent.addRenderChunk(renderChunkIn, layer);
    }
    
    @SuppressWarnings("unchecked")
    private ObjectArrayList<CompoundVertexBuffer>[] makeCube()
    {
        ObjectArrayList<CompoundVertexBuffer>[] result = cubeStore.poll();
        if(result == null)
        {
            final int size = PipelineManager.INSTANCE.pipelineCount();
            result = new ObjectArrayList[size];
            for(int i = 0; i < size; i++)
            {
                result[i] = new ObjectArrayList<CompoundVertexBuffer>();
            }
        }
        return result;
    }
    
    private void addSolidChunk(RenderChunk renderChunkIn)
    {
        final long cubeKey = RenderCube.getPackedKey(renderChunkIn.getPosition());
        
        ObjectArrayList<CompoundVertexBuffer>[] buffers = solidCubes.get(cubeKey);
        if(buffers == null)
        {
            buffers = makeCube();
            solidCubes.put(cubeKey, buffers);
        }
        addChunkToBufferArray(renderChunkIn, buffers);
    }
    
    private void addChunkToBufferArray(RenderChunk renderChunkIn, ObjectArrayList<CompoundVertexBuffer>[] buffers)
    {
        final CompoundVertexBuffer vertexbuffer = (CompoundVertexBuffer)renderChunkIn.getVertexBufferByLayer(SOLID_ORDINAL);
        vertexbuffer.prepareSolidRender();
        vertexbuffer.packingList().forEachPipeline(p -> buffers[p.getIndex()].add(vertexbuffer));
    }
    
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
            parent.renderChunkLayer(layer);
    }
    
    private final void updateViewMatrix(long packedRenderCubeKey)
    {
        updateViewMatrix(
                RenderCube.getPackedKeyOriginX(packedRenderCubeKey),
                RenderCube.getPackedKeyOriginY(packedRenderCubeKey),
                RenderCube.getPackedKeyOriginZ(packedRenderCubeKey));
    }
    
    private final void updateViewMatrix(BlockPos renderChunkOrigin)
    {
        updateViewMatrix(
                RenderCube.renderCubeOrigin(renderChunkOrigin.getX()),
                RenderCube.renderCubeOrigin(renderChunkOrigin.getY()),
                RenderCube.renderCubeOrigin(renderChunkOrigin.getZ()));
    }
    
    private final void updateViewMatrix(final int ox, final int oy, final int oz)
    {
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
        xlatMatrix.m03 = (float)(ox -parent.viewEntityX);
        xlatMatrix.m13 = (float)(oy -parent.viewEntityY);
        xlatMatrix.m23 = (float)(oz -parent.viewEntityZ);

        Matrix4f.mul(xlatMatrix, mvMatrix, mvPos);
        
        // vanilla applies a per-chunk scaling matrix, but does not seem to be essential - probably a hack to prevent seams/holes due to FP error
        // If really is necessary, would want to handle some other way.  Per-chunk matrix not initialized when Acuity enabled.
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
    protected final void renderChunkLayerSolid()
    {
        // Forge doesn't give us a hook in the render loop that comes
        // after camera transform is set up - so call out event handler
        // here as a workaround. Our event handler will only act 1x/frame.
        // Do this even if solid is empty, in case translucent layer needs it.
        if(PipelineManager.INSTANCE.beforeRenderChunks())
            downloadModelViewMatrix();
        
        if (this.solidCubes.isEmpty()) 
            return; 
        
        preRenderSetup();
        
        ObjectIterator<Entry<ObjectArrayList<CompoundVertexBuffer>[]>> it = solidCubes.long2ObjectEntrySet().fastIterator();
        while(it.hasNext())
        {
            Entry<ObjectArrayList<CompoundVertexBuffer>[]> e = it.next();
            updateViewMatrix(e.getLongKey());
            renderSolidArray(e.getValue());
        }
        
        solidCubes.clear();
        postRenderCleanup();
    }
    
    private void renderSolidArray(ObjectArrayList<CompoundVertexBuffer>[] array)
    {
        for(ObjectArrayList<CompoundVertexBuffer> list : array)
            renderSolidList(list);
        cubeStore.offer(array);
    }
    
    private void renderSolidList(ObjectArrayList<CompoundVertexBuffer> list)
    {
        if(list.isEmpty())
            return;
        list.forEach(b -> b.renderSolidNext());
        list.clear();
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

        chunks.clear();
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
    }

    @Override
    public final void onAcuityStatusChange(boolean newEnabledStatus)
    {
        this.isAcuityEnabled = newEnabledStatus;
    }
}
