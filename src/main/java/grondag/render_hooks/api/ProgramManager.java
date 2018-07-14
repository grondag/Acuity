package grondag.render_hooks.api;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class ProgramManager implements IProgramManager
{
    public final static ProgramManager INSTANCE = new ProgramManager();
    
    final ObjectArrayList<Program> programs = new ObjectArrayList<>();

    private final IProgram[] standards = new IProgram[PipelineVertexFormat.values().length];
    
    private ProgramManager()
    {
        standards[PipelineVertexFormat.SINGLE.ordinal()] = createProgram(
                PipelineShaderManagerImpl.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.SINGLE),
                PipelineShaderManagerImpl.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.SINGLE),
                true).finish();
        
        standards[PipelineVertexFormat.DOUBLE.ordinal()] = createProgram(
                PipelineShaderManagerImpl.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.DOUBLE),
                PipelineShaderManagerImpl.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.DOUBLE),
                true).finish();
        
        standards[PipelineVertexFormat.TRIPLE.ordinal()] = createProgram(
                PipelineShaderManagerImpl.INSTANCE.getDefaultVertexShader(PipelineVertexFormat.TRIPLE),
                PipelineShaderManagerImpl.INSTANCE.getDefaultFragmentShader(PipelineVertexFormat.TRIPLE),
                true).finish();
    }
    
    @Override
    synchronized public IProgram createProgram(IPipelineVertexShader vertexShader, IPipelineFragmentShader fragmentShader, boolean includeStandardUniforms)
    {
        Program result = new Program(vertexShader, fragmentShader);
        programs.add(result);
        return result;
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

    public void onRenderTick()
    {
        programs.forEach(s -> s.onRenderTick());
    }

    public void onGameTick()
    {
        programs.forEach(s -> s.onGameTick());
    }
}
