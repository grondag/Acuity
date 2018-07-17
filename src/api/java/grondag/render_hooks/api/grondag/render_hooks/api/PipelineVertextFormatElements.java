package grondag.render_hooks.api;

import net.minecraft.client.renderer.vertex.VertexFormatElement;

public class PipelineVertextFormatElements
{
    public static final VertexFormatElement POSITION_3F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3);
    public static final VertexFormatElement BASE_RGBA_4UB = new VertexFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.COLOR, 4);
    public static final VertexFormatElement BASE_TEX_2F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.UV, 2);
    public static final VertexFormatElement NORMAL_AO_4UB = new VertexFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.NORMAL, 4);
    
    /**
     * First byte (r) is block light
     * Second byte (g) is sky light
     * Third byte lower four bits is glow level (0-15) for first layer, upper four bits is glow level for second layer
     * Fourth byte lower four bits is glow level for third layer. Higher bits reserved.
     */
    public static final VertexFormatElement LIGHTMAPS_4UB = new VertexFormatElement(1, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);

    public static final VertexFormatElement SECONDARY_RGBA_4UB = new VertexFormatElement(2, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);
    public static final VertexFormatElement SECONDARY_TEX_2F = new VertexFormatElement(3, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2);

    public static final VertexFormatElement TERTIARY_RGBA_4UB = new VertexFormatElement(4, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4);
    public static final VertexFormatElement TERTIARY_TEX_2F = new VertexFormatElement(5, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2);
}
