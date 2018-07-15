package grondag.render_hooks.api;

import org.lwjgl.opengl.GL13;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.model.animation.Animation;

public final class ProgramManager implements IProgramManager
{
    public final static ProgramManager INSTANCE = new ProgramManager();
    
    private float worldTime;
    
    final ObjectArrayList<Program> programs = new ObjectArrayList<>();

    private final IProgram[] standards = new IProgram[PipelineVertexFormat.values().length];
    
    private final IProgram waterProgram;
    private final IProgram lavaProgram;
    
    @SuppressWarnings("null")
    private ProgramManager()
    {
        standards[PipelineVertexFormat.COMPATIBLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.COMPATIBLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.COMPATIBLE),
                true).finish();
        
        standards[PipelineVertexFormat.SINGLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.SINGLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.SINGLE),
                true).finish();
        
        standards[PipelineVertexFormat.DOUBLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.DOUBLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.DOUBLE),
                true).finish();
        
        standards[PipelineVertexFormat.TRIPLE.ordinal()] = createProgram(
                PipelineShaderManager.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.TRIPLE),
                PipelineShaderManager.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.TRIPLE),
                true).finish();
        
        // TODO: create water shader
        this.waterProgram = createProgram(
                PipelineShaderManager.INSTANCE.getOrCreateVertexShader("/assets/render_hooks/shader/passthru.vert"),
                PipelineShaderManager.INSTANCE.getOrCreateFragmentShader("/assets/render_hooks/shader/passthru.frag"),
                true).finish();
        
        // TODO: create lava shader
        this.lavaProgram = createProgram(
                PipelineShaderManager.INSTANCE.getOrCreateVertexShader("/assets/render_hooks/shader/passthru.vert"),
                PipelineShaderManager.INSTANCE.getOrCreateFragmentShader("/assets/render_hooks/shader/passthru.frag"),
                true).finish();
    }
    
    @Override
    synchronized public IProgram createProgram(IPipelineVertexShader vertexShader, IPipelineFragmentShader fragmentShader, boolean includeStandardUniforms)
    {
        Program result = new Program(vertexShader, fragmentShader);
        if(includeStandardUniforms)
            addStandardUniforms(result);
        programs.add(result);
        return result;
    }

    private void addStandardUniforms(Program program)
    {
//        program.uniform1f("u_time", UniformUpdateFrequency.PER_FRAME, u -> u.set(this.worldTime));
        program.uniform1i("u_textures", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.defaultTexUnit - GL13.GL_TEXTURE0));
//        program.uniform1i("u_lightmap", UniformUpdateFrequency.ON_LOAD, u -> u.set(OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0));
    }
    
    @Override
    public IProgram getDefaultProgram(PipelineVertexFormat format)
    {
        return standards[format.ordinal()];
    }
    
    void forceReload()
    {
        programs.forEach(s -> s.forceReload());
    }

    @SuppressWarnings("null")
    public void onRenderTick()
    {
        programs.forEach(s -> s.onRenderTick());

        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        if(entity == null) return;

        float partialTicks = Animation.getPartialTickTime();
        if(entity.world != null)
            worldTime = Animation.getWorldTime(entity.world, partialTicks);
    }

    public void onGameTick()
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
