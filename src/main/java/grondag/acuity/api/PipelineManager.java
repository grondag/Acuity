package grondag.acuity.api;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL13;

import grondag.acuity.Configurator;
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
    public final RenderPipeline waterPipeline;
    public final RenderPipeline lavaPipeline;
    public final RenderPipeline defaultSinglePipeline;
    
    private float worldTime;
    private float partialTicks;
    
    @SuppressWarnings("null")
    private PipelineManager()
    {
        super();
        
        // add default pipelines
        for(TextureFormat textureFormat : TextureFormat.values())
        {
            defaultPipelines[textureFormat.ordinal()] = (RenderPipeline) this.createPipeline(
                    textureFormat, 
                    PipelineShaderManager.INSTANCE.getDefaultVertexShader(textureFormat),
                    PipelineShaderManager.INSTANCE.getDefaultFragmentShader(textureFormat)).finish();
        }
        this.waterPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/water.vert", "/assets/acuity/shader/water.frag");
        this.lavaPipeline = this.createPipeline(TextureFormat.SINGLE, "/assets/acuity/shader/lava.vert", "/assets/acuity/shader/lava.frag");
        this.defaultSinglePipeline = defaultPipelines[0];
    }
    
    public void forceReload()
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].refreshVertexFormats();
        }
    }
    
    
    @Nullable
    @Override
    public final RenderPipeline createPipeline(
            TextureFormat textureFormat, 
            String vertexShader, 
            String fragmentShader)
    {
        
        if(this.pipelineCount >= PipelineManager.MAX_PIPELINES)
            return null;
        
        return createPipeline(
                textureFormat, 
                PipelineShaderManager.INSTANCE.getOrCreateVertexShader(vertexShader, textureFormat), 
                PipelineShaderManager.INSTANCE.getOrCreateFragmentShader(fragmentShader, textureFormat));
    }
    
    @Nullable
    protected synchronized final RenderPipeline createPipeline(
            TextureFormat textureFormat, 
            PipelineVertexShader vertexShader, 
            PipelineFragmentShader fragmentShader)
    {
        
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
    public final IRenderPipeline getWaterPipeline()
    {
        return this.waterPipeline;
    }
    
    @Override
    public final IRenderPipeline getLavaPipeline()
    {
        return this.lavaPipeline;
    }

    @Override
    public IRenderPipeline getPipelineByIndex(int index)
    {
        return this.pipelines[index];
    }
    
    private void addStandardUniforms(Program program)
    {
//        program.uniform1f("u_time", UniformUpdateFrequency.PER_FRAME, u -> u.set(this.worldTime));
        
        if(containsUniformSpec(program, "sampler2D", "u_textures"))
            program.uniform1i("u_textures", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0));
        
        if(containsUniformSpec(program, "sampler2D", "u_lightmap"))
            program.uniform1i("u_lightmap", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0));
        
        if(containsUniformSpec(program, "vec3", "u_eye_position"))
            program.uniform3f("u_eye_position", UniformUpdateFrequency.PER_FRAME, u -> 
            {
                Vec3d eyePos = Minecraft.getMinecraft().player.getPositionEyes(partialTicks);
                u.set((float)eyePos.x, (float)eyePos.y, (float)eyePos.z);
            });
        
        if(containsUniformSpec(program, "vec3", "u_fogAttributes"))
            program.uniform3f("u_fogAttributes", UniformUpdateFrequency.PER_TICK, u -> 
            {
                GlStateManager.FogState fogState = GlStateManager.fogState;
                u.set(fogState.end, fogState.end - fogState.start, 
                        // zero signals shader to use linear fog
                        fogState.mode == GlStateManager.FogMode.LINEAR.capabilityId ? 0f : fogState.density);
            });
        
        if(containsUniformSpec(program, "vec3", "u_fogColor"))
            program.uniform3f("u_fogColor", UniformUpdateFrequency.PER_TICK, u -> 
            {
                EntityRenderer er = Minecraft.getMinecraft().entityRenderer;
                u.set(er.fogColorRed, er.fogColorGreen, er.fogColorBlue);
            });
    }
    
    private static boolean containsUniformSpec(Program program, String type, String name)
    {
        String regex = "(?m)^uniform\\s+" + type + "\\s+" + name + "\\s*;";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(program.vertexShader.getSource()).find() 
                || pattern.matcher(program.fragmentShader.getSource()).find(); 
    }
    
    @SuppressWarnings("null")
    public void onRenderTick(RenderTickEvent event)
    {
        for(int i = 0; i < this.pipelineCount; i++)
        {
            this.pipelines[i].onRenderTick();
        }

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        final float partialTicks = event.renderTickTime;
        this.partialTicks = partialTicks;
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
    }

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
