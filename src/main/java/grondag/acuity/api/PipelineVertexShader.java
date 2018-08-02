package grondag.acuity.api;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineVertexShader  extends AbstractPipelineShader
{
    PipelineVertexShader(String fileName, TextureFormat textureFormat)
    {
        super(fileName, OpenGlHelper.GL_VERTEX_SHADER, textureFormat);
    }
    
    @Override
    public String getSource()
    {
        String result = super.getSource();
        result = result.replaceAll("#version\\s+120", "");
        return PipelineShaderManager.INSTANCE.vertexLibrarySource + result;
    }
}
