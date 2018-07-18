package grondag.render_hooks.core;

import static grondag.render_hooks.core.PipelineVertextFormatElements.BASE_RGBA_4UB;
import static grondag.render_hooks.core.PipelineVertextFormatElements.BASE_TEX_2F;
import static grondag.render_hooks.core.PipelineVertextFormatElements.LIGHTMAPS_4UB;
import static grondag.render_hooks.core.PipelineVertextFormatElements.POSITION_3F;
import static grondag.render_hooks.core.PipelineVertextFormatElements.NORMAL_AO_4UB;
import static grondag.render_hooks.core.PipelineVertextFormatElements.SECONDARY_RGBA_4UB;
import static grondag.render_hooks.core.PipelineVertextFormatElements.SECONDARY_TEX_2F;
import static grondag.render_hooks.core.PipelineVertextFormatElements.TERTIARY_RGBA_4UB;
import static grondag.render_hooks.core.PipelineVertextFormatElements.TERTIARY_TEX_2F;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;

public enum PipelineVertexFormat
{
    /**
     * The default MC format, for vanilla fluids (for now) and blocks that opt out.
     */
    COMPATIBLE(DefaultVertexFormats.BLOCK),
    
    /**
     * Same size as default MC format, but lightmaps a 1 byte each, giving room for 3 block lightmaps.
     */
    VANILLA_SINGLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
            .addElement(LIGHTMAPS_4UB)),
    
    /**
     * Adds one extra color and texture coordinate.
     * Use for two-layered textures.
     */
    VANILLA_DOUBLE(new VertexFormat()
            .addElement(POSITION_3F)
            .addElement(BASE_RGBA_4UB)
            .addElement(BASE_TEX_2F)
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
            .addElement(LIGHTMAPS_4UB)
            .addElement(SECONDARY_RGBA_4UB)
            .addElement(SECONDARY_TEX_2F)
            .addElement(TERTIARY_RGBA_4UB)
            .addElement(TERTIARY_TEX_2F)),
    
    /**
     * Adds normal and separate per-vertex AO for sky lighting.
     */
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
    
    private  PipelineVertexFormat(VertexFormat vertexFormat)
    {
        this.vertexFormat = vertexFormat;
        // the first three elements are always pos, color, tex - anything after that is an attribute
        this.attributeCount = vertexFormat.getElementCount() - 3;
    }
}
