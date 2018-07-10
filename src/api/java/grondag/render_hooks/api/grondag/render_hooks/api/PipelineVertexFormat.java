package grondag.render_hooks.api;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import net.minecraft.client.renderer.vertex.VertexFormat;

public enum PipelineVertexFormat
{
    /**
     * Same as Vanilla Minecraft BLOCK format.
     * Has position, color, lightmap and texture atlas uv.
     */
    BASE(BLOCK),
    
    /**
     * Everything in {@link #BASE} plus vertex normals.
     */
    NORMALS(new VertexFormat(BLOCK)
            .addElement(NORMAL_3B)
            .addElement(PADDING_1B)),
    
    /**
     * Everything in {@link #BASE} plus one extra color and texture coordinate.
     * Use for two-layered textures.
     */
    DOUBLE(new VertexFormat(BLOCK)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F)),
    
    /**
     * Everything in {@link #DOUBLE} plus vertex normals.
     */
    NORMALS_DOUBLE(new VertexFormat(BLOCK)
            .addElement(NORMAL_3B)
            .addElement(PADDING_1B)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F)),
    
    /**
     * Everything in {@link #BASE} plus two extra colors and texture coordinates.
     * Use for three-layered textures.
     */
    TRIPLE(new VertexFormat(BLOCK)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F)),
    
    /**
     * Everything in {@link #TRIPLE} plus vertex normals.
     */
    NORMALS_TRIPLE(new VertexFormat(BLOCK)
            .addElement(NORMAL_3B)
            .addElement(PADDING_1B)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F)
            .addElement(COLOR_4UB)
            .addElement(TEX_2F));
    
    public final VertexFormat vertexFormat;
    
    private  PipelineVertexFormat(VertexFormat vertexFormat)
    {
        this.vertexFormat = vertexFormat;
    }
}
