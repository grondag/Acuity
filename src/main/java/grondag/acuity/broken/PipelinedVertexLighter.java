/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.broken;

import grondag.acuity.api.model.ColorHelper;
import grondag.acuity.api.model.BlockVertexConsumer;
import grondag.acuity.api.model.TextureDepth;
import grondag.acuity.api.pipeline.RenderPipeline;
import grondag.acuity.api.pipeline.RenderPipelineImpl;
import grondag.acuity.buffer.VertexCollector;
import grondag.acuity.core.IBlockInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public abstract class PipelinedVertexLighter implements BlockVertexConsumer
{
    protected final RenderPipelineImpl pipeline;
    
    protected int glowFlags = 0;
    private int glowMask = 0;
    protected int blockLightMap = 0;
    protected int skyLightMap = 0;
    
    protected boolean enableDiffuse = true;
    protected boolean enableAmbientOcclusion = true;
    protected boolean isCurrentQuadCutout = false;
    protected boolean isCurrentQuadMipped = true;
    
    protected PipelinedVertexLighter(RenderPipeline pipeline)
    {
        this.pipeline = (RenderPipelineImpl) pipeline;
    }
    
    public abstract VertexCollector getVertexCollector();
    
    protected abstract void reportOutput();
    
    @Override
    public void setEmissive(int layerIndex, boolean isEmissive)
    {
        if(layerIndex < 0 || layerIndex > 2)
            throw new IndexOutOfBoundsException();
        
        if(isEmissive)
            this.glowFlags |= (1 << layerIndex);
        else
            this.glowFlags &= ~(1 << layerIndex);
    }   

    @Override
    public final void setBlockLightMap(int blockLightRGBF)
    {
        this.blockLightMap = blockLightRGBF;
    }
    
    @Override
    public final void setBlockLightMap(int red, int green, int blue, int flicker)
    {
        setBlockLightMap(red | (green << 8) | (blue << 16) |  (flicker << 24));
    }
    
    @Override
    public final void setBlockLightMap(float red, float green, float blue, float flicker)
    {
        setBlockLightMap(Math.round(red * 255), Math.round(green * 255), Math.round(blue * 255), Math.round(flicker * 255));
    }
    
    @Override
    public void setSkyLightMap(int skyLightMap)
    {
        this.skyLightMap  = skyLightMap & 0xFF;
    }

    @Override
    public void setShading(boolean enableDiffuse)
    {
        this.enableDiffuse = enableDiffuse;
    }
    
    @Override
    public void setAmbientOcclusion(boolean enableAmbientOcclusion)
    {
        this.enableAmbientOcclusion = enableAmbientOcclusion && MinecraftClient.isAmbientOcclusionEnabled();
    }
    
    @SuppressWarnings("null")
    protected void resetForNewQuad(IPipelinedQuad quad)
    {
        //UGLY: keep less internal state and instead query the quad reference
        this.currentQuad = quad;
        switch(quad.getRenderLayer())
        {
        case CUTOUT:
            isCurrentQuadCutout = true;
            isCurrentQuadMipped = false;
            break;
            
        case MIPPED_CUTOUT:
            isCurrentQuadCutout = true;
            isCurrentQuadMipped = true;
            break;
            
        case SOLID:
        case TRANSLUCENT:
        default:
            isCurrentQuadCutout = false;
            isCurrentQuadMipped = true;
            break;
        }
        this.glowFlags = 0;
        this.blockLightMap = 0;
        this.skyLightMap = 0;
        this.enableDiffuse = true;
        this.glowMask = (2 << quad.getPipeline().textureDepth().layerCount()) - 1;
        this.enableAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
    }
    
    protected final boolean areAllLayersEmissive()
    {
        return this.glowFlags == this.glowMask;
    }

    public void acceptQuad(IPipelinedQuad quad)
    {
        this.reportOutput();
        this.resetForNewQuad(quad);
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
            int unlitColorARGB0,
            float u0,
            float v0
            )
    {
        if(this.pipeline.textureDepth != TextureDepth.SINGLE)
            throw new UnsupportedOperationException(I18n.translateToLocal("misc.error_vertex_texture_format_mismatch_single"));
        
        startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
    }
    
    @Override
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            )
    {
        if(this.pipeline.textureDepth != TextureDepth.DOUBLE)
            throw new UnsupportedOperationException(I18n.translateToLocal("misc.error_vertex_texture_format_mismatch_double"));
        
        VertexCollector target = startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
        
        addSecondaryLayer(target, unlitColorARGB1, u1, v1);
    }
    
    protected void addSecondaryLayer(VertexCollector target, int unlitColorARGB1, float u1, float v1)
    {
        // SECONDARY_RGBA_4UB
        target.add(ColorHelper.swapRedBlue(unlitColorARGB1));
        
        // SECONDARY_TEX_2F
        target.add(u1);
        target.add(v1);
    }
    
    @Override
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
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
        if(this.pipeline.textureDepth != TextureDepth.TRIPLE)
            throw new UnsupportedOperationException(I18n.translateToLocal("misc.error_vertex_texture_format_mismatch_triple"));
        
        VertexCollector target = startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
        
        addSecondaryLayer(target, unlitColorARGB1, u1, v1);
        
        addSecondaryLayer(target, unlitColorARGB2, u2, v2);

    }
    
    protected void addTertiaryLayer(VertexCollector target, int unlitColorARGB2, float u2, float v2)
    {
        // TERTIARY_RGBA_4UB
        target.add(ColorHelper.swapRedBlue(unlitColorARGB2));
        
        // TERTIARY_TEX_2F
        target.add(u2);
        target.add(v2);
    }
    
    protected static final float LIGHTMAP_TO_255 = 34815.47f;
    protected static final float LIGHTMAP_TO_127 = LIGHTMAP_TO_255 * 127f / 255f;
    
    protected abstract VertexCollector startVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int unlitColorRGBA0,
            float u0,
            float v0);
    
    
    protected int calcPackedLight(IBlockInfo blockInfo, float normX, float normY, float normZ, float x, float y, float z)
    {
        final float e1 = 1f - 1e-2f;
        final float e2 = 0.95f;

        boolean full = blockInfo.isFullCube();
        Direction side = null;

             if((full || y < -e1) && normY < -e2) side = Direction.DOWN;
        else if((full || y >  e1) && normY >  e2) side = Direction.UP;
        else if((full || z < -e1) && normZ < -e2) side = Direction.NORTH;
        else if((full || z >  e1) && normZ >  e2) side = Direction.SOUTH;
        else if((full || x < -e1) && normX < -e2) side = Direction.WEST;
        else if((full || x >  e1) && normX >  e2) side = Direction.EAST;

        int i = side == null ? 0 : side.ordinal() + 1;
        
        return blockInfo.getPackedLightFast()[i];
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
    
    protected float getAo(IBlockInfo blockInfo, float x, float y, float z)
    {
        int sx = x < 0 ? 1 : 2;
        int sy = y < 0 ? 1 : 2;
        int sz = z < 0 ? 1 : 2;

        if(x < 0) x++;
        if(y < 0) y++;
        if(z < 0) z++;

        float a = 0;
        float[][][] ao = blockInfo.getAoFast();
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
