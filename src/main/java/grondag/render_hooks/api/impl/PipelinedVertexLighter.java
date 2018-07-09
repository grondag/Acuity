package grondag.render_hooks.api.impl;

import grondag.render_hooks.api.IPipelinedQuad;
import grondag.render_hooks.api.IPipelinedVertex;
import grondag.render_hooks.api.IPipelinedVertexConsumer;
import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A heavily-modified version of Forge vertex lighter that supports multiple render paths in same quad stream.
 */
@SideOnly(Side.CLIENT)
public abstract class PipelinedVertexLighter implements IPipelinedVertexConsumer
{
    protected final IRenderPipeline pipeline;
    protected final VertexFormat format;
    
    protected float blockColorR = -1f;
    protected float blockColorG = -1f;
    protected float blockColorB = -1f;
    
    protected PipelinedVertexLighter(IRenderPipeline pipeline)
    {
        this.pipeline = pipeline;
        VertexFormat format = pipeline.vertexFormat();
        this.format = format;
    }
    
    public abstract BlockInfo getBlockInfo();
    
    public abstract BufferBuilder getPipelineBuffer();
    
    public VertexFormat getVertexFormat()
    {
        return this.format;
    }

    public void acceptQuad(IPipelinedQuad quad)
    {
        if(quad.getTintIndex() == -1)
        {
            this.blockColorR = -1f;
        }
        else
        {
            final int multiplier = getBlockInfo().getColorMultiplier(quad.getTintIndex());
            this.blockColorR = (float)(multiplier >> 16 & 0xFF) / 0xFF;
            this.blockColorG = (float)(multiplier >> 8 & 0xFF) / 0xFF;
            this.blockColorB = (float)(multiplier & 0xFF) / 0xFF;
        }
        quad.produceVertices(this);
    }
    
    @Override
    public void acceptVertex(IPipelinedVertex vertex)
    {
        final BlockInfo blockInfo = getBlockInfo();
        
        // local position is vertex, + block-state-driven shift (if any);
        final float posX = vertex.posX() + blockInfo.getShx();
        final float posY = vertex.posY() + blockInfo.getShy();
        final float posZ = vertex.posZ() + blockInfo.getShz();
        
        final float normX = vertex.normalX();
        final float normY = vertex.normalY();
        final float normZ = vertex.normalZ();
        
        final float lightX = posX - .5f + normX * .5f;
        final float lightY = posY - .5f + normY * .5f;
        final float lightZ = posZ - .5f + normZ * .5f;
        
        final boolean aoEnabled = Minecraft.isAmbientOcclusionEnabled();
        
        final boolean haveTint = this.blockColorR != -1;
        float diffuse = -1;
        float ao = -1;
        
        int colorIndex = 0;
        int uvIndex = 0;
        
        final int elementCount = format.getElementCount();
        final BufferBuilder target = getPipelineBuffer();
        for(int i = 0; i <  elementCount; i++)
        {
            switch(format.getElement(i).getUsage())
            {
                case POSITION:
                {
                    final BlockPos pos = blockInfo.getBlockPos();
                    target.pos((double)pos.getX() + posX, (double)pos.getY() + posY, (double)pos.getZ() + posZ);
                    break;
                }
                
                case NORMAL:
                    target.normal(normX, normY, normZ);
                    break;
                    
                case COLOR:
                {
                    int c = vertex.unlitColorARGB(colorIndex);
                    float a = (float)(c >> 24 & 0xFF);
                    float r = (float)(c >> 16 & 0xFF);
                    float g = (float)(c >> 8 & 0xFF);
                    float b = (float)(c & 0xFF);
                            
                    if(vertex.applyDiffuse(colorIndex))
                    {
                        if(diffuse == -1)
                            diffuse = LightUtil.diffuseLight(normX, normY, normZ);
                        
                        r *= diffuse;
                        g *= diffuse;
                        b *= diffuse;
                    }
                    
                    if(aoEnabled && vertex.applyAO(colorIndex))
                    {
                        if(ao == -1)
                            ao = getAo(lightX, lightY, lightZ);
                        
                        r *= ao;
                        g *= ao;
                        b *= ao;
                    }
                    
                    if(haveTint && vertex.applyTint(colorIndex))
                    {
                        r *= this.blockColorR;
                        g *= this.blockColorG;
                        b *= this.blockColorB;
                    }
                    target.color(Math.round(r), Math.round(g), Math.round(b), Math.round(a));
                    colorIndex++;
                    break;
                }
                    
                case UV: 
                    if(i == pipeline.lightmapIndex())
                    {
                         if(aoEnabled)
                         {
                             final float blockLight = Math.max(vertex.minimumBlockLight(), calcLightmap(blockInfo.getBlockLight(), lightX, lightY, lightZ));
                             final float skyLight = Math.max(vertex.minimumSkyLight(), calcLightmap(blockInfo.getSkyLight(), lightX, lightY, lightZ));
                             target.tex(blockLight, skyLight);
                         }
                         else
                         {
                             final int packedLight =  this.calcPackedLight(blockInfo, normX, normY, normZ, lightX, lightY, lightZ);
                             final float blockLight = Math.max(vertex.minimumBlockLight(), ((float)((packedLight >> 0x04) & 0xF) * 0x20) / 0xFFFF);
                             final float skyLight = Math.max(vertex.minimumSkyLight(), ((float)((packedLight >> 0x14) & 0xF) * 0x20) / 0xFFFF);
                             target.tex(blockLight, skyLight);
                         }
                    }
                    else
                    {
                        target.tex(vertex.u(uvIndex), vertex.v(uvIndex++));
                    }
                    break;
                
                case PADDING:
                    // NOOP
                    break;
                    
                default:
                    throw new UnsupportedOperationException("Unsupported vertex element in pipelined render.");
            }
        }
        target.endVertex();
    }
    
    protected int calcPackedLight(BlockInfo blockInfo, float normX, float normY, float normZ, float x, float y, float z)
    {
        final float e1 = 1f - 1e-2f;
        final float e2 = 0.95f;

        boolean full = blockInfo.isFullCube();
        EnumFacing side = null;

             if((full || y < -e1) && normY < -e2) side = EnumFacing.DOWN;
        else if((full || y >  e1) && normY >  e2) side = EnumFacing.UP;
        else if((full || z < -e1) && normZ < -e2) side = EnumFacing.NORTH;
        else if((full || z >  e1) && normZ >  e2) side = EnumFacing.SOUTH;
        else if((full || x < -e1) && normX < -e2) side = EnumFacing.WEST;
        else if((full || x >  e1) && normX >  e2) side = EnumFacing.EAST;

        int i = side == null ? 0 : side.ordinal() + 1;
        return blockInfo.getPackedLight()[i];
    }
    
    protected float calcLightmap(float[][][][] light, float x, float y, float z)
    {
        x *= 2;
        y *= 2;
        z *= 2;
        float l2 = x * x + y * y + z * z;
        if(l2 > 6 - 2e-2f)
        {
            float s = (float)Math.sqrt((6 - 2e-2f) / l2);
            x *= s;
            y *= s;
            z *= s;
        }
        float ax = x > 0 ? x : -x;
        float ay = y > 0 ? y : -y;
        float az = z > 0 ? z : -z;
        float e1 = 1 + 1e-4f;
        if(ax > 2 - 1e-4f && ay <= e1 && az <= e1)
        {
            x = x < 0 ? -2 + 1e-4f : 2 - 1e-4f;
        }
        else if(ay > 2 - 1e-4f && az <= e1 && ax <= e1)
        {
            y = y < 0 ? -2 + 1e-4f : 2 - 1e-4f;
        }
        else if(az > 2 - 1e-4f && ax <= e1 && ay <= e1)
        {
            z = z < 0 ? -2 + 1e-4f : 2 - 1e-4f;
        }
        ax = x > 0 ? x : -x;
        ay = y > 0 ? y : -y;
        az = z > 0 ? z : -z;
        if(ax <= e1 && ay + az > 3f - 1e-4f)
        {
            float s = (3f - 1e-4f) / (ay + az);
            y *= s;
            z *= s;
        }
        else if(ay <= e1 && az + ax > 3f - 1e-4f)
        {
            float s = (3f - 1e-4f) / (az + ax);
            z *= s;
            x *= s;
        }
        else if(az <= e1 && ax + ay > 3f - 1e-4f)
        {
            float s = (3f - 1e-4f) / (ax + ay);
            x *= s;
            y *= s;
        }
        else if(ax + ay + az > 4 - 1e-4f)
        {
            float s = (4 - 1e-4f) / (ax + ay + az);
            x *= s;
            y *= s;
            z *= s;
        }

        float l = 0;
        float s = 0;

        for(int ix = 0; ix <= 1; ix++)
        {
            for(int iy = 0; iy <= 1; iy++)
            {
                for(int iz = 0; iz <= 1; iz++)
                {
                    float vx = x * (1 - ix * 2);
                    float vy = y * (1 - iy * 2);
                    float vz = z * (1 - iz * 2);

                    float s3 = vx + vy + vz + 4;
                    float sx = vy + vz + 3;
                    float sy = vz + vx + 3;
                    float sz = vx + vy + 3;

                    float bx = (2 * vx + vy + vz + 6) / (s3 * sy * sz * (vx + 2));
                    s += bx;
                    l += bx * light[0][ix][iy][iz];

                    float by = (2 * vy + vz + vx + 6) / (s3 * sz * sx * (vy + 2));
                    s += by;
                    l += by * light[1][ix][iy][iz];

                    float bz = (2 * vz + vx + vy + 6) / (s3 * sx * sy * (vz + 2));
                    s += bz;
                    l += bz * light[2][ix][iy][iz];
                }
            }
        }

        l /= s;

        if(l > 15f * 0x20 / 0xFFFF) l = 15f * 0x20 / 0xFFFF;
        if(l < 0) l = 0;

        return l;
    }
    
    protected float getAo(float x, float y, float z)
    {
        int sx = x < 0 ? 1 : 2;
        int sy = y < 0 ? 1 : 2;
        int sz = z < 0 ? 1 : 2;

        if(x < 0) x++;
        if(y < 0) y++;
        if(z < 0) z++;

        float a = 0;
        float[][][] ao = this.getBlockInfo().getAo();
        a += ao[sx - 1][sy - 1][sz - 1] * (1 - x) * (1 - y) * (1 - z);
        a += ao[sx - 1][sy - 1][sz - 0] * (1 - x) * (1 - y) * (0 + z);
        a += ao[sx - 1][sy - 0][sz - 1] * (1 - x) * (0 + y) * (1 - z);
        a += ao[sx - 1][sy - 0][sz - 0] * (1 - x) * (0 + y) * (0 + z);
        a += ao[sx - 0][sy - 1][sz - 1] * (0 + x) * (1 - y) * (1 - z);
        a += ao[sx - 0][sy - 1][sz - 0] * (0 + x) * (1 - y) * (0 + z);
        a += ao[sx - 0][sy - 0][sz - 1] * (0 + x) * (0 + y) * (1 - z);
        a += ao[sx - 0][sy - 0][sz - 0] * (0 + x) * (0 + y) * (0 + z);

        a = MathHelper.clamp(a, 0, 1);
        return a;
    }
}
