package grondag.acuity.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IProgramManager
{
    IProgram createProgram(IPipelineVertexShader vertexShader, IPipelineFragmentShader fragmentShader);

    IProgram getDefaultProgram(TextureFormat textureFormat);

    float worldTime();
}