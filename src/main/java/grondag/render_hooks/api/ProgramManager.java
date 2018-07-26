package grondag.render_hooks.api;

import java.util.regex.Pattern;

import org.lwjgl.opengl.GL13;

import grondag.render_hooks.RenderHooks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.model.animation.Animation;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public final class ProgramManager implements IProgramManager
{
    public final static ProgramManager INSTANCE = new ProgramManager();
    
    private float worldTime;
    private float partialTicks;
    
    final ObjectArrayList<Program> programs = new ObjectArrayList<>();

    private final IProgram[] standards = new IProgram[TextureFormat.values().length];
    
    private final IProgram waterProgram;
    private final IProgram lavaProgram;
    
    private ProgramManager()
    {
        standards[TextureFormat.SINGLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(TextureFormat.SINGLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(TextureFormat.SINGLE))
                .finish();
        
        standards[TextureFormat.DOUBLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(TextureFormat.DOUBLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(TextureFormat.DOUBLE))
                .finish();
        
        standards[TextureFormat.TRIPLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(TextureFormat.TRIPLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(TextureFormat.TRIPLE))
                .finish();
        
        this.waterProgram = createProgram(
                PipelineShaderManager.INSTANCE.getOrCreateVertexShader("/assets/render_hooks/shader/water.vert", TextureFormat.SINGLE),
                PipelineShaderManager.INSTANCE.getOrCreateFragmentShader("/assets/render_hooks/shader/water.frag", TextureFormat.SINGLE))
                .finish();
        
        this.lavaProgram = createProgram(
                PipelineShaderManager.INSTANCE.getOrCreateVertexShader("/assets/render_hooks/shader/lava.vert", TextureFormat.SINGLE),
                PipelineShaderManager.INSTANCE.getOrCreateFragmentShader("/assets/render_hooks/shader/lava.frag", TextureFormat.SINGLE))
                .finish();
    }
    
    @Override
    synchronized public IProgram createProgram(IPipelineVertexShader vertexShader, IPipelineFragmentShader fragmentShader)
    {
        TextureFormat format = ((AbstractPipelineShader)vertexShader).textureFormat;
        if(format !=  ((AbstractPipelineShader)fragmentShader).textureFormat)
        {
            RenderHooks.INSTANCE.getLog().error("Detected program with mimatched vertex formats in vertex and fragment shader.  Vertex shader format will be used but program probably won't work correctly.");
        }
        Program result = new Program(vertexShader, fragmentShader, format);
        addStandardUniforms(result);
        programs.add(result);
        return result;
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
    
    @Override
    public IProgram getDefaultProgram(TextureFormat textureFormat)
    {
        return standards[textureFormat.ordinal()];
    }
    
    void forceReload()
    {
        programs.forEach(s -> s.forceReload());
    }

    @SuppressWarnings("null")
    public void onRenderTick(RenderTickEvent event)
    {
        programs.forEach(s -> s.onRenderTick());

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        final float partialTicks = event.renderTickTime;
        this.partialTicks = partialTicks;
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
    }

    public void onGameTick(ClientTickEvent event)
    {
        programs.forEach(s -> s.onGameTick());
    }
    
    @Override
    public float worldTime()
    {
        return this.worldTime;
    }

    public IProgram getWaterProgram()
    {
        return this.waterProgram;
    }

    public IProgram getLavaProgram()
    {
        return this.lavaProgram;
    }
}
