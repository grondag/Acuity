package grondag.acuity.core;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PipelineVertextFormatElement extends VertexFormatElement
{
    // openGL implementation on my dev laptop *really* wants to get vertex positions via standard (GL 2.1) binding
    // slows to a crawl otherwise
    public static final PipelineVertextFormatElement POSITION_3F = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3, null);
    public static final PipelineVertextFormatElement BASE_RGBA_4UB = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_color_0");
    public static final PipelineVertextFormatElement BASE_TEX_2F = new PipelineVertextFormatElement(1, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_uv_0");
//    public static final PipelineVertextFormatElement LIGHTMAPS_2S = new PipelineVertextFormatElement(2, VertexFormatElement.EnumType.SHORT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_lightmap", false);

    /**
     * Format varies by model.  <p>
     * 
     * In vanilla lighting model, Bytes 1-2 are sky and block lightmap coordinates<br>
     * 3rd byte is has control flags.  bits 0-2 are emissive flags, bit 3 controls mimmaping (1=off), bit 4 is cutout<br>
     * bits 5-7 and the last byte are reserved
     * 
     * In enhanced lighting model, bytes 1-3 are rgb light color/glow  flag, and the last byte is amount of torch flicker.
     * The most significant bit of the rgb color bytes indicates if layers are emissive. 
     * The color values are thus scale to 0-127 and need to be normalized in the shader after stripping the glow bit.<p>
     */
    public static final PipelineVertextFormatElement LIGHTMAPS_4UB = new PipelineVertextFormatElement(2, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_lightmap", false);

    //UGLY: index in for generic attributes misaligned now due to mismatched usage in vanilla vs extended formats 
    // (doesn't currently matter)
    public static final PipelineVertextFormatElement NORMAL_AO_4UB = new PipelineVertextFormatElement(0, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_normal_ao", false);

    public static final PipelineVertextFormatElement SECONDARY_RGBA_4UB = new PipelineVertextFormatElement(3, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_color_1");
    public static final PipelineVertextFormatElement SECONDARY_TEX_2F = new PipelineVertextFormatElement(4, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_uv_1");

    public static final PipelineVertextFormatElement TERTIARY_RGBA_4UB = new PipelineVertextFormatElement(5, VertexFormatElement.EnumType.UBYTE, VertexFormatElement.EnumUsage.GENERIC, 4, "in_color_2");
    public static final PipelineVertextFormatElement TERTIARY_TEX_2F = new PipelineVertextFormatElement(6, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.GENERIC, 2, "in_uv_2");

    public final @Nullable String attributeName;
    public final int elementCount;
    public final int glConstant;
    public final boolean isNormalized;
    public final int byteSize;
    
    private PipelineVertextFormatElement(int indexIn, EnumType typeIn, EnumUsage usageIn, int count, @Nullable String attributeName)
    {
        this(indexIn, typeIn, usageIn, count, attributeName, true);
    }
    
    private PipelineVertextFormatElement(int indexIn, EnumType typeIn, EnumUsage usageIn, int count, @Nullable String attributeName, boolean isNormalized)
    {
        super(indexIn, typeIn, usageIn, count);
        this.attributeName = attributeName;
        this.elementCount = this.getElementCount();
        this.glConstant = this.getType().getGlConstant();
        this.byteSize = this.getSize();
        this.isNormalized = isNormalized;
    }
}
