package grondag.render_hooks.api;

import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class PipelineVertextFormatElements
{
    public static final VertexFormatElement POSITION_3F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3);
    public static final VertexFormatElement BASE_RGBA_4UB = new VertexFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.COLOR, 4);
    public static final VertexFormatElement BASE_TEX_2F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.UV, 2);
    public static final VertexFormatElement NORMAL_3B = new VertexFormatElement(0, VertexFormatElement.EnumType.BYTE, VertexFormatElement.EnumUsage.NORMAL, 3);
    public static final VertexFormatElement AO_1B = new VertexFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 1);
    /**
     * Lightmap combined(block is high quartet and sky is low), plus glow values for primary, secondary and tertiary layers if present
     */
    public static final VertexFormatElement LIGHTMAP_AND_GLOWS_4UB = new VertexFormatElement(1, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);

    public static final VertexFormatElement SECONDARY_RGBA_4UB = new VertexFormatElement(2, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);
    public static final VertexFormatElement SECONDARY_TEX_2F = new VertexFormatElement(3, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2);

    public static final VertexFormatElement TERTIARY_RGBA_4UB = new VertexFormatElement(4, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);
    public static final VertexFormatElement TERTIARY_TEX_2F = new VertexFormatElement(5, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2);
}
