package grondag.render_hooks.core;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import grondag.render_hooks.api.IPipelinedQuad;
import grondag.render_hooks.api.IPipelinedQuadConsumer;
import grondag.render_hooks.api.IPipelinedVertexConsumer;
import grondag.render_hooks.api.PipelineVertexFormat;
import grondag.render_hooks.api.RenderHookRuntime;
import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.client.model.pipeline.LightUtil;

public class VanillaQuadWrapper implements IPipelinedQuad
{

    private final IRenderPipeline SIMPLE = RenderHookRuntime.INSTANCE.getPipelineManager().getDefaultPipeline(PipelineVertexFormat.SINGLE);
    private @Nullable BakedQuad wrapped;
    private @Nullable BlockRenderLayer layer;
    private float[][] positions = new float[4][3];
    
    public void prepare(BlockRenderLayer layer)
    {
        this.layer = layer;
    }
    
    @Override
    public IRenderPipeline getPipeline()
    {
        return SIMPLE;
    }

    @SuppressWarnings("null")
    @Override
    public int getTintIndex()
    {
        return wrapped.getTintIndex();
    }

    @Override
    public void produceVertices(IPipelinedVertexConsumer vertexLighter)
    {
        final int blockColor = blockColorMultiplier(vertexLighter);
        @SuppressWarnings("null")
        final int[] data =  wrapped.getVertexData();
        @SuppressWarnings("null")
        final VertexFormat format = wrapped.getFormat();
        final float[][] pos = this.positions;
        float normX = 0, normY = 1, normZ = 0;
        int normalIndex = -1;
        int uvIndex = -1;
        int lightMapIndex = -1;
        
        for(int i = 0; i < format.getElementCount(); i++)
        {
            VertexFormatElement e = format.getElement(i);
            switch(e.getUsage())
            {
            case NORMAL:
                normalIndex = i;
                break;
                
            case UV:
                if(e.getIndex() == 0)
                    uvIndex = i;
                else
                    lightMapIndex = i;
                break;
                
            default:
                break;
            }
        }
        
        for(int i = 0; i < 4; i++)
        {
            LightUtil.unpack(data, pos[i], format, i, 0);
        }
        
        if(normalIndex == -1)
        {
            Vector3f v1 = new Vector3f(pos[3]);
            Vector3f t = new Vector3f(pos[1]);
            Vector3f v2 = new Vector3f(pos[2]);
            v1.sub(t);
            t.set(pos[0]);
            v2.sub(t);
            v1.cross(v2, v1);
            v1.normalize();
            normX = v1.x;
            normY = v1.y;
            normZ = v1.z;
        }
        
        float[] unpack = new float[3];
        int lightmaps = 0;
        
        for(int i = 0; i < 4; i++)
        {
            if(normalIndex != -1)
            {
                LightUtil.unpack(data, unpack, format, i, normalIndex);
                normX = unpack[0];
                normY = unpack[1];
                normZ = unpack[2];
            }
            
            if(lightMapIndex != -1)
            {
                LightUtil.unpack(data, unpack, format, i, lightMapIndex);
                
                //FIXME: This probably isn't right
                float max = Math.max(unpack[0], unpack[1]);
                lightmaps = Math.round(max * 0xFFFF) >> 2;
            }
            
            int rawColor = data[(i * format.getNextOffset() + format.getColorOffset()) / 4];
            if(blockColor != 0xFFFFFFFF)
                rawColor = multiplyColor(rawColor, blockColor);
                    
            LightUtil.unpack(data, unpack, format, i, uvIndex);
            
            vertexLighter.acceptVertex(pos[i][0], pos[i][1], pos[i][2], normX, normY, normZ, lightmaps, rawColor, unpack[0], unpack[1]);
        }
        
    }
    
    private int multiplyColor(int color1, int color2)
    {
        int red = ((color1 >> 16) & 0xFF) * ((color2 >> 16) & 0xFF) / 0xFF;
        int green = ((color1 >> 8) & 0xFF) * ((color2 >> 8) & 0xFF) / 0xFF;
        int blue = (color1 & 0xFF) * (color2 & 0xFF) / 0xFF;
        int alpha = ((color1 >> 24) & 0xFF) * ((color2 >> 24) & 0xFF) / 0xFF;
    
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    private int blockColorMultiplier(IPipelinedVertexConsumer vertexLighter)
    {
        @SuppressWarnings("null")
        final int tint = wrapped.getTintIndex();
        return tint == -1 ? 0xFFFFFFFF : 0xFF000000 | vertexLighter.getBlockInfo().getColorMultiplier(tint); 
    }

    @SuppressWarnings("null")
    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return this.layer;
    }

    public void wrapAndLight(IPipelinedQuadConsumer lighter, BakedQuad q)
    {
        this.wrapped = q;
        lighter.accept(this); 
    }

}
