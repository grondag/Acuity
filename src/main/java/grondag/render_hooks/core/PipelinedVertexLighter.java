package grondag.render_hooks.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import grondag.render_hooks.api.IPipelinedQuad;
import grondag.render_hooks.api.IPipelinedVertexConsumer;
import grondag.render_hooks.api.PipelineVertexFormat;
import grondag.render_hooks.api.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A heavily-modified version of Forge vertex lighter that supports multiple render paths in same quad stream.
 */
@SideOnly(Side.CLIENT)
public abstract class PipelinedVertexLighter implements IPipelinedVertexConsumer
{
    protected final RenderPipeline pipeline;
    protected final VertexFormat format;
    
    protected PipelinedVertexLighter(RenderPipeline pipeline)
    {
        this.pipeline = pipeline;
        this.format = pipeline.pipelineVertexFormat().vertexFormat;
    }
    
    @Override
    public abstract BlockInfo getBlockInfo();
    
    public abstract BufferBuilder getPipelineBuffer();
    
    protected abstract void reportOutput();
    
    public VertexFormat getVertexFormat()
    {
        return this.format;
    }

    public void acceptQuad(IPipelinedQuad quad)
    {
        this.reportOutput();
        quad.produceVertices(this);
    }
    
    @Override
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockLightMaps,
            int unlitColorARGB0,
            float u0,
            float v0
            )
    {
        if(this.pipeline.pipelineVertexFormat() != PipelineVertexFormat.SINGLE)
            throw new UnsupportedOperationException("Single-layer vertex must use single-layer pipeline format.");
        
        startVertex(posX, posY, posZ, normX, normY, normZ, blockLightMaps, unlitColorARGB0, u0, v0).endVertex();
    }
    
    @Override
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockLightMaps,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            )
    {
        if(this.pipeline.pipelineVertexFormat() != PipelineVertexFormat.DOUBLE)
            throw new UnsupportedOperationException("Double-layer vertex must use double-layer pipeline format.");
        
        BufferBuilder target = startVertex(posX, posY, posZ, normX, normY, normZ, blockLightMaps, unlitColorARGB0, u0, v0);
        final ByteBuffer bytes  = target.getByteBuffer();
        // SECONDARY_RGBA_4UB
        putColorRGBA(bytes, unlitColorARGB1);
        
        // SECONDARY_TEX_2F
        bytes.putFloat(u1);
        bytes.putFloat(v1);
        target.endVertex();
    }
    
    @Override
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockLightMaps,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1,
            int unlitColorARGB2,
            float u2,
            float v2
            )
    {
        if(this.pipeline.pipelineVertexFormat() != PipelineVertexFormat.TRIPLE)
            throw new UnsupportedOperationException("Triple-layer vertex must use triple-layer pipeline format.");
        
        BufferBuilder target = startVertex(posX, posY, posZ, normX, normY, normZ, blockLightMaps, unlitColorARGB0, u0, v0);
        final ByteBuffer bytes  = target.getByteBuffer();
        // SECONDARY_RGBA_4UB
        putColorRGBA(bytes, unlitColorARGB1);
        
        // SECONDARY_TEX_2F
        bytes.putFloat(u1);
        bytes.putFloat(v1);
        
        // TERTIARY_RGBA_4UB
        putColorRGBA(bytes, unlitColorARGB2);
        
        // TERTIARY_TEX_2F
        bytes.putFloat(u2);
        bytes.putFloat(v2);
        target.endVertex();
    }
    
    private BufferBuilder startVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockLightMaps,
            int unlitColorARGB0,
            float u0,
            float v0)
    {
            
        final BlockInfo blockInfo = getBlockInfo();
        
        // local position is vertex, + block-state-driven shift (if any);
        posX += blockInfo.getShx();
        posY += blockInfo.getShy();
        posZ += blockInfo.getShz();
        
        final float lightX = posX - .5f + normX * .5f;
        final float lightY = posY - .5f + normY * .5f;
        final float lightZ = posZ - .5f + normZ * .5f;
        
        final BufferBuilder target = getPipelineBuffer();
        final ByteBuffer bytes  = target.getByteBuffer();
        bytes.position(target.getVertexCount() * format.getNextOffset());

        // POSITION_3F
        final BlockPos pos = blockInfo.getBlockPos();
        bytes.putFloat((float) (target.xOffset + pos.getX() + posX));
        bytes.putFloat((float) (target.yOffset + pos.getY() + posY));
        bytes.putFloat((float) (target.zOffset + pos.getZ() + posZ));
        
        // BASE_RGBA_4UB
        putColorRGBA(bytes, unlitColorARGB0);
        
        // BASE_TEX_2F
        bytes.putFloat(u0);
        bytes.putFloat(v0);
        
        // NORMAL_3B
        bytes.put((byte) Math.round(normX * 127));
        bytes.put((byte) Math.round(normY * 127));
        bytes.put((byte) Math.round(normZ * 127));
        
        // AO_1B
        bytes.put((byte) Math.round(getAo(lightX, lightY, lightZ) * 255f));
        
        // LIGHTMAP_AND_GLOWS_4UB
        int blockLight, skyLight;
        if(Minecraft.isAmbientOcclusionEnabled())
        {
            blockLight = Math.round(calcLightmap(blockInfo.getBlockLight(), lightX, lightY, lightZ) * 0xFF);
            skyLight = Math.round(calcLightmap(blockInfo.getSkyLight(), lightX, lightY, lightZ) * 0xFF);
        }
        else
        {
            final int packedLight =  this.calcPackedLight(blockInfo, normX, normY, normZ, lightX, lightY, lightZ);
            blockLight = (packedLight >> 0x03) & 0xFF;
            skyLight = (packedLight >> 0x13) & 0xFF;
            
        }
        bytes.put((byte) Math.max(blockLight, (blockLightMaps & 0xFF)));
        bytes.put((byte) Math.max(blockLight, ((blockLightMaps >> 8) & 0xFF)));
        bytes.put((byte) Math.max(blockLight, ((blockLightMaps >> 16) & 0xFF)));
        bytes.put((byte) skyLight);
        
        return target;
    }
    
    private static void putColorRGBA(ByteBuffer bytes, int colorARGB)
    {
        byte alpha = (byte)(colorARGB >> 24 & 0xFF);
        byte red = (byte)(colorARGB >> 16 & 0xFF);
        byte green = (byte)(colorARGB >> 8 & 0xFF);
        byte blue = (byte)(colorARGB & 0xFF);
        
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN)
        {
            bytes.put(red);
            bytes.put(green);
            bytes.put(blue);
            bytes.put(alpha);
        }
        else
        {
            bytes.put(alpha);
            bytes.put(blue);
            bytes.put(green);
            bytes.put(red);
        }
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
