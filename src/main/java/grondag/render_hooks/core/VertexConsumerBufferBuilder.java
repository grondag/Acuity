package grondag.render_hooks.core;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.LightUtil;

/**
 * A non-wrapped version of Forge VertexBufferConsumer
 */
public abstract class VertexConsumerBufferBuilder extends BufferBuilder implements IVertexConsumer
{
    private static final float[] dummyColor = new float[]{ 1, 1, 1, 1 };
    private int[] quadData;
    private int v = 0;
    
    public VertexConsumerBufferBuilder(int bufferSizeIn)
    {
        super(bufferSizeIn);
    }
    
    abstract public BlockPos getOffset();
    
    @Override
    public void begin(int glMode, VertexFormat format)
    {
        super.begin(glMode, format);
        if(this.quadData == null || this.quadData.length != format.getNextOffset())
        {
            this.quadData = new int[format.getNextOffset()];
        }
    }

    @Override
    public void put(int e, float... data)
    {
        VertexFormat format = getVertexFormat();
        if(this.isColorDisabled() && format.getElement(e).getUsage() == EnumUsage.COLOR)
        {
            data = dummyColor;
        }
        LightUtil.pack(data, quadData, format, v, e);
        final BlockPos offset = this.getOffset();
        if(e == format.getElementCount() - 1)
        {
            v++;
            if(v == 4)
            {
                this.addVertexData(quadData);
                this.putPosition(offset.getX(), offset.getY(), offset.getZ());
                v = 0;
            }
        }
    }

    @Override
    public void setQuadTint(int tint) {}
    @Override
    public void setQuadOrientation(EnumFacing orientation) {}
    @Override
    public void setApplyDiffuseLighting(boolean diffuse) {}
    @Override
    public void setTexture(TextureAtlasSprite texture ) {}
}
