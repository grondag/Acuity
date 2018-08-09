package grondag.acuity.api;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL13;

import grondag.acuity.Configurator;
import grondag.acuity.core.PipelineShaderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
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
    
    public static final PipelineManager INSTANCE = new PipelineManager();
    
    private final RenderPipeline[] pipelines = new RenderPipeline[PipelineManager.MAX_PIPELINES];
    
    private int pipelineCount = 0;
    
    private final RenderPipeline[] defaultPipelines = new RenderPipeline[TextureFormat.values().length];
    private final RenderPipeline waterPipeline;
    private final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;
    
    private float worldTime;
    private float partialTicks;
    
 // FAIL: unfortunately using explicit uniforms is slower
//    /**
//     * Used to retrieve project matrix from GLState. Avoids re-instantiating each frame.
//     */
//    protected final FloatBuffer projectionMatrixBuffer = BufferUtils.createFloatBuffer(16);
//    
//    /**
//     * Current projection matrix. Refreshed from GL state each frame after camera setup
//     * in {@link #beforeRenderChunks()}. Unfortunately not immutable so use caution.
//     */
//    public final Matrix4f projMatrix = new Matrix4f();
//    
//    /**
//     * See {@link #onRenderTick(RenderTickEvent)}
//     */
//    private boolean didUpdatePipelinesThisFrame = false;
    
    @SuppressWarnings("null")
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
    }
    
    
    @Nullable
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
    
    public final IRenderPipeline getPipeline(int pipelineIndex)
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
    
    private void addStandardUniforms(RenderPipeline program)
    {
        program.uniform1f("u_time", UniformUpdateFrequency.PER_FRAME, u -> u.set(this.worldTime));
        
        program.uniformSampler2d("u_textures", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0));
        
        program.uniformSampler2d("u_lightmap", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0));
        
        program.uniform3f("u_eye_position", UniformUpdateFrequency.PER_FRAME, u -> 
        {
            Vec3d eyePos = Minecraft.getMinecraft().player.getPositionEyes(partialTicks);
            u.set((float)eyePos.x, (float)eyePos.y, (float)eyePos.z);
        });
        
        program.uniform3f("u_fogAttributes", UniformUpdateFrequency.PER_TICK, u -> 
        {
            GlStateManager.FogState fogState = GlStateManager.fogState;
            u.set(fogState.end, fogState.end - fogState.start, 
                    // zero signals shader to use linear fog
                    fogState.mode == GlStateManager.FogMode.LINEAR.capabilityId ? 0f : fogState.density);
        });
        
        program.uniform3f("u_fogColor", UniformUpdateFrequency.PER_TICK, u -> 
        {
            EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
            u.set(er.fogColorRed, er.fogColorGreen, er.fogColorBlue);
        });
        
     // FAIL: unfortunately using explicit uniforms is slower
//        if(Program.containsUniformSpec(program, "mat4", "u_projection"))
//            program.uniformMatrix4f("u_projection", UniformUpdateFrequency.PER_FRAME, u -> 
//            {
//                u.set(projMatrix);
//            });
//        
//        program.setupModelViewUniforms();
    }
            
    /**
     * Called at start of each frame but does not update pipelines immediately
     * because camera has not yet been set up and we need the projection matrix.
     * So, captures state it can and sets a flag that will used to update
     * pipelines before any chunks are rendered.   Our render list will call
     * us right before it render chunks.
     */
    @SuppressWarnings("null")
    public void onRenderTick(RenderTickEvent event)
    {
     // FAIL: unfortunately using explicit uniforms is slower
//        didUpdatePipelinesThisFrame = false;

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        final float partialTicks = event.renderTickTime;
        this.partialTicks = partialTicks;
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
        
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].onRenderTick();
        }
    }
    
 // FAIL: unfortunately using explicit uniforms is slower
//    /**
//     * Called by our chunk render list before each round of chunk renders.
//     * Can be called multiple times per frame but we only update once per frame.
//     * Necessary because Forge doesn't provide a hook that happens after camera setup
//     * but before block rendering.
//     */
//    public void beforeRenderChunks()
//    {
//        if(didUpdatePipelinesThisFrame)
//            return;
//        
//        didUpdatePipelinesThisFrame = true;
//        
//        projectionMatrixBuffer.position(0);
//        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrixBuffer);
//        projMatrix.loadTranspose(projectionMatrixBuffer);
//        
//        for(int i = 0; i < this.pipelineCount; i++)
//        {
//            this.pipelines[i].onRenderTick();
//        }
//    }

    public void onGameTick(ClientTickEvent event)
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            pipelines[i].onGameTick();
        }
    }
    
    @Override
    public float worldTime()
    {
        return this.worldTime;
    }
}
