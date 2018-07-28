package grondag.acuity.api;

import javax.annotation.Nonnull;

import grondag.acuity.api.IPipelineVertexShader;
import grondag.acuity.api.TextureFormat;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipelineVertexShader  extends AbstractPipelineShader implements IPipelineVertexShader
{
    PipelineVertexShader(@Nonnull String fileName, @Nonnull TextureFormat textureFormat)
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
