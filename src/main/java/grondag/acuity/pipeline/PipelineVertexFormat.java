package grondag.acuity.pipeline;

import static grondag.acuity.pipeline.PipelineVertextFormatElement.BASE_RGBA_4UB;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.BASE_TEX_2F;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.LIGHTMAPS_4UB;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.NORMAL_AO_4UB;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.POSITION_3F;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.SECONDARY_RGBA_4UB;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.SECONDARY_TEX_2F;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.TERTIARY_RGBA_4UB;
import static grondag.acuity.pipeline.PipelineVertextFormatElement.TERTIARY_TEX_2F;

import org.lwjgl.opengl.GL20;

import grondag.acuity.opengl.OpenGlHelperExt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormat;

@Environment(EnvType.CLIENT)
public enum PipelineVertexFormat
{
    VANILLA_SINGLE(0, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)),
    
    /**
     * Adds one extra color and texture coordinate.
     * Use for two-layered textures.
     */
    VANILLA_DOUBLE(1, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)),
    
    /**
     * Adds two extra colors and texture coordinates.
     * Use for three-layered materials.
     */
    VANILLA_TRIPLE(2, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)
            .add(TERTIARY_RGBA_4UB)
            .add(TERTIARY_TEX_2F)),
    
    ENHANCED_SINGLE(0, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)),
    
    ENHANCED_DOUBLE(1, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)),
    
    ENHANCED_TRIPLE(2, new VertexFormat()
            .add(POSITION_3F)
            .add(BASE_RGBA_4UB)
            .add(BASE_TEX_2F)
            .add(NORMAL_AO_4UB)
            .add(LIGHTMAPS_4UB)
            .add(SECONDARY_RGBA_4UB)
            .add(SECONDARY_TEX_2F)
            .add(TERTIARY_RGBA_4UB)
            .add(TERTIARY_TEX_2F));
    
    public final VertexFormat vertexFormat;
    
    /**
     * Will be a unique, 0-based ordinal within the current lighting model.
     */
    public final int layerIndex;
    
    public final int attributeCount;
    public final int stride;
    
    private final PipelineVertextFormatElement [] elements;
    
    private  PipelineVertexFormat(int layerIndex, VertexFormat vertexFormat)
    {
        this.layerIndex = layerIndex;
        this.vertexFormat = vertexFormat;
        this.stride = vertexFormat.getVertexSize();
        this.elements = vertexFormat.getElements().toArray(new PipelineVertextFormatElement[vertexFormat.getElementCount()]);
        int count = 0;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                count++;
        }
        this.attributeCount = count;
    }
    
    /**
     * Enables generic vertex attributes and binds their location.
     */
    public void enableAndBindAttributes(int bufferOffset)
    {
        OpenGlHelperExt.enableAttributes(this.attributeCount);
        bindAttributeLocations(bufferOffset);
    }
    
    /**
     * Binds attribute locations without enabling them. 
     * For use with VAOs. In other cases just call {@link #enableAndBindAttributes(int)}
     */
    public void bindAttributeLocations(int bufferOffset)
    {
        int offset = 0;
        int index = 1;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                GL20.glVertexAttribPointer(index++, e.elementCount, e.glConstant, e.isNormalized, stride, bufferOffset + offset);
            offset += e.byteSize;
        }
    }
    
    public void bindProgramAttributes(int  programID)
    {
        int index = 1;
        for(PipelineVertextFormatElement e : elements)
        {
            if(e.attributeName != null)
                GL20.glBindAttribLocation(programID, index++, e.attributeName);
        }
    }
}
