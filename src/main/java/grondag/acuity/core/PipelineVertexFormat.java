package grondag.acuity.core;

import static grondag.acuity.core.PipelineVertextFormatElement.BASE_RGBA_4UB;
import static grondag.acuity.core.PipelineVertextFormatElement.BASE_TEX_2F;
import static grondag.acuity.core.PipelineVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.acuity.core.PipelineVertextFormatElement.NORMAL_AO_4UB;
import static grondag.acuity.core.PipelineVertextFormatElement.POSITION_3F;
import static grondag.acuity.core.PipelineVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.acuity.core.PipelineVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.acuity.core.PipelineVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.acuity.core.PipelineVertextFormatElement.TERTIARY_TEX_2F;

import org.lwjgl.opengl.GL20;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum PipelineVertexFormat
{
    VANILLA_SINGLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)),
    
    /**
     * Adds one extra color and texture coordinate.
     * Use for two-layered textures.
     */
    VANILLA_DOUBLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)
            .addElement(SECONDARY_RGBA_4UB)
            .addElement(SECONDARY_TEX_2F)),
    
    /**
     * Adds two extra colors and texture coordinates.
     * Use for three-layered materials.
     */
    VANILLA_TRIPLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)
            .addElement(SECONDARY_RGBA_4UB)
            .addElement(SECONDARY_TEX_2F)
            .addElement(TERTIARY_RGBA_4UB)
            .addElement(TERTIARY_TEX_2F)),
    
    ENHANCED_SINGLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)),
    
    ENHANCED_DOUBLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)
            .addElement(SECONDARY_RGBA_4UB)
            .addElement(SECONDARY_TEX_2F)),
    
    ENHANCED_TRIPLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(NORMAL_AO_4UB)
            .addElement(LIGHTMAPS_4UB)
            .addElement(SECONDARY_RGBA_4UB)
            .addElement(SECONDARY_TEX_2F)
            .addElement(TERTIARY_RGBA_4UB)
            .addElement(TERTIARY_TEX_2F));
    
    public final VertexFormat vertexFormat;
    
    public final int attributeCount;
    protected final int stride;
    
    private final PipelineVertextFormatElement [] elements;
    
    private  PipelineVertexFormat(VertexFormat vertexFormat)
    {
        this.vertexFormat = vertexFormat;
        this.stride = vertexFormat.getNextOffset();
        this.elements = vertexFormat.getElements().toArray(new PipelineVertextFormatElement[vertexFormat.getElementCount()]);
        int count = 0;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                count++;
        }
        this.attributeCount = count;
    }
    
    public void setupAttributes(int bufferOffset)
    {
        OpenGlHelperExt.enableAttributes(this.attributeCount);
        int offset = 0;
        int index = 1;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                OpenGlHelperExt.glVertexAttribPointerFast(index++, e.elementCount, e.glConstant, e.isNormalized, stride, bufferOffset + offset);
            offset += e.byteSize;
        }
    }
    
    public void bindAttributes(int  programID)
    {
        int index = 1;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                GL20.glBindAttribLocation(programID, index++, e.attributeName);
        }
    }
}
