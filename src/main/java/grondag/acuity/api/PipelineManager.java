package grondag.acuity.api;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;

import grondag.acuity.Configurator;
import grondag.acuity.core.PipelineShaderManager;
import grondag.acuity.extension.AcuityFogHelper;
import grondag.acuity.extension.AcuityGameRenderer;
import grondag.acuity.extension.AcuityMatrix4f;
import grondag.acuity.mixin.MixinFogState;
import grondag.acuity.mixin.MixinGlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

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
    
    
    // NB: initialization sequence works better if these are static (may also prevent a pointer chase)
    
    /**
     * Current projection matrix. Refreshed from GL state each frame after camera setup
     * in {@link #beforeRenderChunks()}. Unfortunately not immutable so use caution.
     */
    public static final Matrix4f projMatrix = new Matrix4f();
    
    /**
     * Used to retrieve and store project matrix from GLState. Avoids re-instantiating each frame.
     */
    public static final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    /**
     * Current mv matrix - set at program activation
     */
    public static final FloatBuffer modelViewMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    //TODO: remove if not needed
//    private static final long modelViewMatrixBufferAddress = MemoryUtil.memAddress(modelViewMatrixBuffer);
    
    /**
     * Current mvp matrix - set at program activation
     */
    public static final FloatBuffer modelViewProjectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
    
    //TODO: remove if not needed
//    private static final long modelViewProjectionMatrixBufferAddress = MemoryUtil.memAddress(modelViewProjectionMatrixBuffer);
    
    public static final PipelineManager INSTANCE = new PipelineManager();
    
    /**
     * Incremented whenever view matrix changes. Used by programs to know if they must update.
     */
    public static int viewMatrixVersionCounter = Integer.MIN_VALUE;
    
    private final RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    
    private int pipelineCount = 0;
    
    private final RenderPipeline[] defaultPipelines = new RenderPipeline[TextureFormat.values().length];
    private final RenderPipeline waterPipeline;
    private final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;
    
    /**
     * The number of seconds this world has been rendering since the last render reload,
     * including fractional seconds. <p>
     * 
     * Based on total world time, but shifted to originate from start of this game session.
     */
    private float renderSeconds;
    
    /**
     * World time ticks at last render reload..
     */
    private long baseWorldTime;
    
    /**
     * Frames are (hopefully) shorter than a client tick.  This is the fraction of a tick that
     * has elapsed since the last complete client tick.
     */
    private float fractionalFrameTicks;
    
    private PipelineManager()
    {
        super();
        
        // add default pipelines
        for(TextureFormat textureFormat : TextureFormat.values())
        {
            defaultPipelines[textureFormat.ordinal()] = (RenderPipeline) this.createPipeline(
                    textureFormat, 
                    PipelineShaderManager.INSTANCE.DEFAULT_VERTEX_SOURCE,
                    PipelineShaderManager.INSTANCE.DEFAULT_FRAGMENT_SOURCE).finish();
        }
        this.waterPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/water.vert", "/assets/acuity/shader/water.frag");
        this.lavaPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/lava.vert", "/assets/acuity/shader/lava.frag");
        this.defaultSinglePipeline = defaultPipelines[0];
    }
    
    public void forceReload()
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].forceReload();
        }
        
        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
        
        assert cameraEntity != null;
        assert cameraEntity.getEntityWorld() != null;
        
        if(cameraEntity != null && cameraEntity.getEntityWorld() != null)
        {
            this.baseWorldTime = cameraEntity.getEntityWorld().getTime();
            computeRenderSeconds(cameraEntity);
        }
    }
    
    @Override
    public final synchronized RenderPipeline createPipeline(
            TextureFormat textureFormat, 
            String vertexShader, 
            String fragmentShader)
    {
        
        if(this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        
        if(this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        RenderPipeline result = new RenderPipeline(this.pipelineCount++, vertexShader, fragmentShader, textureFormat);
        this.pipelines[result.getIndex()] = result;
        
        addStandardUniforms(result);
        
        return result;
    }
    
    public final RenderPipeline getPipeline(int pipelineIndex)
    {
        return pipelines[pipelineIndex];
    }

    @Override
    public final IRenderPipeline getDefaultPipeline(TextureFormat textureFormat)
    {
        return pipelines[textureFormat.ordinal()];
    }
    
    @Override
    public final RenderPipeline getWaterPipeline()
    {
        return Configurator.fancyFluids ? this.waterPipeline : this.defaultSinglePipeline;
    }
    
    @Override
    public final RenderPipeline getLavaPipeline()
    {
        return Configurator.fancyFluids ? this.lavaPipeline : this.defaultSinglePipeline;
    }

    @Override
    public IRenderPipeline getPipelineByIndex(int index)
    {
        return this.pipelines[index];
    }
    
    /**
     * The number of pipelines currently registered.
     */
    public final int pipelineCount()
    {
        return this.pipelineCount;
    }
    
    private void addStandardUniforms(RenderPipeline pipeline)
    {
        pipeline.uniform1f("u_time", UniformUpdateFrequency.PER_FRAME, u -> u.set(this.renderSeconds));
        
        pipeline.uniformSampler2d("u_textures", UniformUpdateFrequency.ON_LOAD, u -> u.set(GLX.GL_TEXTURE0 - GL13.GL_TEXTURE0));
        
        pipeline.uniformSampler2d("u_lightmap", UniformUpdateFrequency.ON_LOAD, u -> u.set(GLX.GL_TEXTURE1 - GL13.GL_TEXTURE0));
        
        pipeline.uniform3f("u_eye_position", UniformUpdateFrequency.PER_FRAME, u -> 
        {
            Vec3d eyePos = MinecraftClient.getInstance().player.getCameraPosVec(fractionalFrameTicks);
            u.set((float)eyePos.x, (float)eyePos.y, (float)eyePos.z);
        });
        
        pipeline.uniform3f("u_fogAttributes", UniformUpdateFrequency.PER_TICK, u -> 
        {
            MixinFogState fogState = MixinGlStateManager.FOG;
            u.set(fogState.getEnd(), fogState.getEnd() - fogState.getStart(), 
                    // zero signals shader to use linear fog
                    fogState.getMode() == GlStateManager.FogMode.LINEAR.glValue ? 0f : fogState.getDensity());
        });
        
        pipeline.uniform3f("u_fogColor", UniformUpdateFrequency.PER_TICK, u -> 
        {
            AcuityFogHelper fh = (AcuityFogHelper)((AcuityGameRenderer)MinecraftClient.getInstance().worldRenderer).fogHelper();
            u.set(fh.fogColorRed(), fh.fogColorGreen(), fh.fogColorBlue());
        });
        
        pipeline.setupModelViewUniforms();
    }
            
    /**
     * Called just before terrain setup each frame after camera, fog and projection matrix are set up,
     */
    public void prepareForFrame(Entity cameraEntity, float fractionalTicks)
    {
        this.fractionalFrameTicks = fractionalTicks;
        
        //FIXME: probably borked in some way
        projectionMatrixBuffer.position(0);
        GlStateManager.getMatrix(GL11.GL_PROJECTION_MATRIX, projectionMatrixBuffer);
        projMatrix.setFromBuffer(projectionMatrixBuffer, true); // assuming true = transpose
        
        assert cameraEntity != null;
        assert cameraEntity.getEntityWorld() != null;
        
        if(cameraEntity == null || cameraEntity.getEntityWorld() == null)
            return;

        computeRenderSeconds(cameraEntity);
    }
    
    private void computeRenderSeconds(Entity cameraEntity)
    {
        renderSeconds = (float) ((cameraEntity.getEntityWorld().getTime() 
                - baseWorldTime + fractionalFrameTicks) / 20);
    }
    
    /**
     * Called by our chunk render list before each round of chunk renders.
     * Can be called multiple times per frame but we only update once per frame.
     * Necessary because Forge doesn't provide a hook that happens after camera setup
     * but before block rendering.<p>
     * 
     * Returns true if this was first pass so caller can handle 1x actions.
     */
    public boolean beforeRenderChunks()
    {
        
        // TODO: Moved these to onRenderTick, still need a way to handle 1X detection
        
        return true;
    }

    public void onGameTick(MinecraftClient mc)
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            pipelines[i].onGameTick();
        }
    }
    
    @Override
    public float renderSeconds()
    {
        return this.renderSeconds;
    }

    private static final Matrix4f transferMatrixBase = new Matrix4f();
    private static final AcuityMatrix4f transferMatrix = (AcuityMatrix4f)(Object)transferMatrixBase;
    
    
    public static final void setModelViewMatrix(Matrix4f mvMatrix)
    {
        updateModelViewMatrix(mvMatrix);
        
        updateModelViewProjectionMatrix(mvMatrix);
        
        viewMatrixVersionCounter++;
    }
    
    private static final void updateModelViewMatrix(Matrix4f mvMatrix)
    {
        mvMatrix.putIntoBuffer(modelViewMatrixBuffer);
        // PERF - put back?
        // avoid NIO overhead
//        OpenGlHelperExt.fastMatrix4fBufferCopy(transferArray, PipelineManager.modelViewMatrixBufferAddress);
    }
    
    private static final void updateModelViewProjectionMatrix(Matrix4f mvMatrix)
    {
        //FIXME: this almost certainly is flipped in some way
        transferMatrix.setFromMatrix(mvMatrix);
        transferMatrixBase.multiply(PipelineManager.projMatrix);
        transferMatrixBase.putIntoBuffer(modelViewProjectionMatrixBuffer);
     // PERF - put back?
        // avoid NIO overhead
//        OpenGlHelperExt.fastMatrix4fBufferCopy(transferArray, PipelineManager.modelViewProjectionMatrixBufferAddress);
    }
}
