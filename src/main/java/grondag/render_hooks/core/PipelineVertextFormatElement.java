package grondag.render_hooks.core;

import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class PipelineVertextFormatElement extends VertexFormatElement
{
    public static final PipelineVertextFormatElement POSITION_3F = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3, "in_position");
    public static final PipelineVertextFormatElement BASE_RGBA_4UB = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.COLOR, 4, "in_color_0");
    public static final PipelineVertextFormatElement BASE_TEX_2F = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.UV, 2, "in_uv_0");
    public static final PipelineVertextFormatElement NORMAL_AO_4UB = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.NORMAL, 4, "in_normal_ao");
    
    /**
     * First byte (r) is block light
     * Second byte (g) is sky light
     * Third byte lower four bits is glow level (0-15) for first layer, upper four bits is glow level for second layer
     * Fourth byte lower four bits is glow level for third layer. Higher bits reserved.
     */
    public static final PipelineVertextFormatElement LIGHTMAPS_4UB = new PipelineVertextFormatElement(1, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_lightmaps", false);

    public static final PipelineVertextFormatElement SECONDARY_RGBA_4UB = new PipelineVertextFormatElement(2, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_color_1");
    public static final PipelineVertextFormatElement SECONDARY_TEX_2F = new PipelineVertextFormatElement(3, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_uv_1");

    public static final PipelineVertextFormatElement TERTIARY_RGBA_4UB = new PipelineVertextFormatElement(4, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_color_2");
    public static final PipelineVertextFormatElement TERTIARY_TEX_2F = new PipelineVertextFormatElement(5, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_uv_2");

    public final String attributeName;
    public final int elementCount;
    public final int glConstant;
    public final boolean isNormalized;
    public final int byteSize;
    
    private PipelineVertextFormatElement(int indexIn, EnumType typeIn, EnumUsage usageIn, int count, String attributeName)
    {
        this(indexIn, typeIn, usageIn, count, attributeName, true);
    }
    
    private PipelineVertextFormatElement(int indexIn, EnumType typeIn, EnumUsage usageIn, int count, String attributeName, boolean isNormalized)
    {
        super(indexIn, typeIn, usageIn, count);
        this.attributeName = attributeName;
        this.elementCount = this.getElementCount();
        this.glConstant = this.getType().getGlConstant();
        this.byteSize = this.getSize();
        this.isNormalized = isNormalized;
    }
}
