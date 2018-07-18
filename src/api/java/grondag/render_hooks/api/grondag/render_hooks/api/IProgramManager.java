package grondag.render_hooks.api;

public interface IProgramManager
{

    IProgram createProgram(IPipelineVertexShader vertexShader, IPipelineFragmentShader fragmentShader, boolean includeStandardUniforms);

    IProgram getDefaultProgram(TextureFormat textureFormat);

    float worldTime();
}