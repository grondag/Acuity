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

package grondag.acuity.api.model;

import grondag.acuity.api.AcuityRuntimeImpl;
import grondag.acuity.api.pipeline.RenderPipeline;

abstract class AbstractVertexConsumer implements VertexConsumer
{
    protected static final int VERTEX_LIGHT_ENABLE_MASK = 0b111;
    protected static final int WORLD_DIFFUSE_FLAG =  0b0001000;
    protected static final int VERTEX_DIFFUSE_FLAG = 0b0010000;
    protected static final int WORLD_AO_FLAG =       0b0100000;
    protected static final int VERTEX_AO_FLAG =      0b1000000;
    protected static final int DIFFUSE_MASK = WORLD_DIFFUSE_FLAG | VERTEX_DIFFUSE_FLAG;
    protected static final int AO_MASK = WORLD_AO_FLAG | VERTEX_AO_FLAG;
    protected static final int DEFAULT_LIGHT_FLAGS = WORLD_DIFFUSE_FLAG | WORLD_AO_FLAG;
    
    protected int vertexOffset = 0;
    protected int vertexLight = 0;
    protected int cutoutFlags = 0b000;
    protected int mipMapFlags = 0b111;
    protected int lightFlags = DEFAULT_LIGHT_FLAGS;
    protected RenderPipeline pipeline = null;
    protected TextureDepth textureDepth = null;
    protected boolean isRawQuad = false;
    
    protected static final int POS_X = 0;
    protected static final int POS_Y =1;
    protected static final int POS_Z = 2;
    protected static final int NORM_X = 3;
    protected static final int NORM_Y = 4;
    protected static final int NORM_Z = 5;
    protected static final int COLOR_0 = 6;
    protected static final int U_0 = 7;
    protected static final int V_0 = 8;
    protected static final int COLOR_1 = 9;
    protected static final int U_1 = 10;
    protected static final int V_1 = 11;
    protected static final int COLOR_2 = 12;
    protected static final int U_2 = 13;
    protected static final int V_2 = 14;
    protected static final int LIGHTMAP = 15;
    protected static final int VERTEX_STRIDE = 16;
    protected static final int COMPLETION_INDEX = VERTEX_STRIDE * 3;
    protected static final int QUAD_STRIDE = VERTEX_STRIDE * 4;
    protected final int[] vertexData = new int[QUAD_STRIDE];
    
    protected float faceNormX;
    protected float faceNormY;
    protected float faceNormZ;
    
    // lookup block shift positions
    protected float offsetX = 0;
    protected float offsetY = 0;
    protected float offsetZ = 0;

    
    @Override
    public void clearSettings()
    {
        vertexOffset = 0;
        vertexLight = 0xFFFFFF;
        mipMapFlags = 0b111;
        cutoutFlags = 0b000;
        lightFlags = DEFAULT_LIGHT_FLAGS;
        pipeline = null;
        textureDepth = null;        
        isRawQuad = false;
    }

    abstract protected void lightAndOutputQuad();
    
    private void completeQuad()
    {
        if(!isRawQuad)
            computeFaceNormal();
        lightAndOutputQuad();
        vertexOffset = 0;
        isRawQuad = false;
        textureDepth = pipeline == null ? null : pipeline.textureDepth();
    }
    
    private void computeFaceNormal()
    {
        final float x0 = Float.intBitsToFloat(vertexData[POS_X]);
        final float y0 = Float.intBitsToFloat(vertexData[POS_Y]);
        final float z0 = Float.intBitsToFloat(vertexData[POS_Z]);
        
        final float x1 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + POS_X]);
        final float y1 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + POS_Y]);
        final float z1 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + POS_Z]);
        
        final float x2 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + VERTEX_STRIDE + POS_X]);
        final float y2 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + VERTEX_STRIDE + POS_Y]);
        final float z2 = Float.intBitsToFloat(vertexData[VERTEX_STRIDE + VERTEX_STRIDE + POS_Z]);
        
        final float dx0 = x0 - x1;
        final float dy0 = y0 - y1;
        final float dz0 = z0 - z1;
        
        final float dx1 = x2 - x0;
        final float dy1 = y2 - y0;
        final float dz1 = z2 - z0;
        
        float normX = dy0 * dz1 - dy1 * dz0;
        float normY = dx0 * dz1 - dx1 * dz0;
        float normZ = dx0 * dy1 - dx1 * dy0;
        
        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);
        
        if(l != 0)
        {
            normX /= l;
            normY /= l;
            normZ /= l;
        }
        
        faceNormX = normX;
        faceNormY = normY;
        faceNormZ = normZ;
    }
    
    @Override
    public final void setPipeline(RenderPipeline pipeline)
    {
        this.pipeline = pipeline;
    }

    @Override
    public boolean isStandardLightingModel()
    {
        return AcuityRuntimeImpl.INSTANCE.isStandardLightingModel();
    }
    
    @Override
    public final void setVertexLight(int lightRGB)
    {
        vertexLight = lightRGB;
    }

    @Override
    public final void setVertexLight(int red, int green, int blue)
    {
        setVertexLight(red | (green << 8) | (blue << 16));
    }

    @Override
    public final void setVertexLight(float red, float green, float blue)
    {
        setVertexLight(Math.round(red * 255), Math.round(green * 255), Math.round(blue * 255));
    }
    
    @Override
    public final void enableVertexLight(TextureDepth textureLayer, boolean enable)
    {
        final int mask = 1 << textureLayer.ordinal();
        if(enable)
            lightFlags |= mask;
        else
            lightFlags &= ~mask;
    }
    
    @Override
    public void enableWorldLightDiffuse(boolean enable)
    {
        if(enable)
            lightFlags |= WORLD_DIFFUSE_FLAG;
        else
            lightFlags &= ~WORLD_DIFFUSE_FLAG;        
    }

    @Override
    public void enableVertexLightDiffuse(boolean enable)
    {
        if(enable)
            lightFlags |= VERTEX_DIFFUSE_FLAG;
        else
            lightFlags &= ~VERTEX_DIFFUSE_FLAG;          
    }

    @Override
    public void enableWorldLightAmbientOcclusion(boolean enable)
    {
        if(enable)
            lightFlags |= WORLD_AO_FLAG;
        else
            lightFlags &= ~WORLD_AO_FLAG;          
    }

    @Override
    public void enableVertexLightAmbientOcclusion(boolean enable)
    {
        if(enable)
            lightFlags |= VERTEX_AO_FLAG;
        else
            lightFlags &= ~VERTEX_AO_FLAG;          
    }
    
    @Override
    public final void enableMipMap(TextureDepth textureLayer, boolean enable)
    {
        final int mask = 1 << textureLayer.ordinal();
        if(enable)
            mipMapFlags |= mask;
        else
            mipMapFlags &= ~mask;
    }

    @Override
    public final void enableCutout(TextureDepth textureLayer, boolean enable)
    {
        final int mask = 1 << textureLayer.ordinal();
        if(enable)
            cutoutFlags |= mask;
        else
            cutoutFlags &= ~mask;        
    }

    private final void startVertex(float posX, float posY, float posZ, float normX, float normY, float normZ, int unlitColorARGB0, float u0, float v0)
    {
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + POS_X] = Float.floatToRawIntBits(posX);
        vertexData[offset + POS_Y] = Float.floatToRawIntBits(posY);
        vertexData[offset + POS_Z] = Float.floatToRawIntBits(posZ);
        vertexData[offset + NORM_X] = Float.floatToRawIntBits(normX);
        vertexData[offset + NORM_Y] = Float.floatToRawIntBits(normY);
        vertexData[offset + NORM_Z] = Float.floatToRawIntBits(normZ);
        vertexData[offset + COLOR_0] = unlitColorARGB0;
        vertexData[offset + U_0] = Float.floatToRawIntBits(u0);
        vertexData[offset + V_0] = Float.floatToRawIntBits(v0);
        vertexData[offset + LIGHTMAP] = this.vertexLight;
    }
    
    private final void startRawVertex(float posX, float posY, float posZ, int rawNormal, int rawLight, int unlitColorARGB0, float u0, float v0)
    {
        if(pipeline == null)
            throw new UnsupportedOperationException("Raw vertices require a custom pipeline");
        
        isRawQuad = true;
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + POS_X] = Float.floatToRawIntBits(posX);
        vertexData[offset + POS_Y] = Float.floatToRawIntBits(posY);
        vertexData[offset + POS_Z] = Float.floatToRawIntBits(posZ);
        vertexData[offset + NORM_X] = rawNormal;
        vertexData[offset + COLOR_0] = unlitColorARGB0;
        vertexData[offset + U_0] = Float.floatToRawIntBits(u0);
        vertexData[offset + V_0] = Float.floatToRawIntBits(v0);
        vertexData[offset + LIGHTMAP] = rawLight;
    }
    
    private void finishVertex()
    {
        if(vertexOffset == COMPLETION_INDEX)
            completeQuad();
        else vertexOffset += VERTEX_STRIDE;
    }
    
    @Override
    public final void acceptVertex(float posX, float posY, float posZ, float normX, float normY, float normZ, int unlitColorARGB0, float u0, float v0)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.SINGLE;
        else if(textureDepth != TextureDepth.SINGLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        
        startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
        finishVertex();
    }

    @Override
    public final void acceptVertex(float posX, float posY, float posZ, int unlitColorARGB0, float u0, float v0)
    {
        acceptVertex(posX, posY, posZ, Float.NaN, Float.NaN, Float.NaN, unlitColorARGB0, u0, v0);
    }

    @Override
    public void acceptRawVertex(float posX, float posY, float posZ, int rawNormalData, int rawLightData, int unlitColorARGB0, float u0, float v0)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.SINGLE;
        else if(textureDepth != TextureDepth.SINGLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        startRawVertex(posX, posY, posZ, rawNormalData, rawLightData, unlitColorARGB0, u0, v0);
        finishVertex();
    }

    @Override
    public final void acceptVertex(float posX, float posY, float posZ, float normX, float normY, float normZ, int unlitColorARGB0, float u0, float v0,
            int unlitColorARGB1, float u1, float v1)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.DOUBLE;
        else if(textureDepth != TextureDepth.DOUBLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        
        startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + COLOR_1] = unlitColorARGB1;
        vertexData[offset + U_1] = Float.floatToRawIntBits(u1);
        vertexData[offset + V_1] = Float.floatToRawIntBits(v1);
        finishVertex();
    }

    @Override
    public final void acceptVertex(float posX, float posY, float posZ, int unlitColorARGB0, float u0, float v0, int unlitColorARGB1, float u1, float v1)
    {
        acceptVertex(posX, posY, posZ, Float.NaN, Float.NaN, Float.NaN, unlitColorARGB0, u0, v0, 
                unlitColorARGB1, u1, v1);
    }

    @Override
    public void acceptRawVertex(float posX, float posY, float posZ, int rawNormalData, int rawLightData, int unlitColorARGB0, float u0, float v0,
            int unlitColorARGB1, float u1, float v1)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.SINGLE;
        else if(textureDepth != TextureDepth.SINGLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        startRawVertex(posX, posY, posZ, rawNormalData, rawLightData, unlitColorARGB0, u0, v0);
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + COLOR_1] = unlitColorARGB1;
        vertexData[offset + U_1] = Float.floatToRawIntBits(u1);
        vertexData[offset + V_1] = Float.floatToRawIntBits(v1);
        finishVertex();        
    }

    @Override
    public final void acceptVertex(float posX, float posY, float posZ, float normX, float normY, float normZ, int unlitColorARGB0, float u0, float v0,
            int unlitColorARGB1, float u1, float v1, int unlitColorARGB2, float u2, float v2)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.TRIPLE;
        else if(textureDepth != TextureDepth.TRIPLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        
        startVertex(posX, posY, posZ, normX, normY, normZ, unlitColorARGB0, u0, v0);
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + COLOR_1] = unlitColorARGB1;
        vertexData[offset + U_1] = Float.floatToRawIntBits(u1);
        vertexData[offset + V_1] = Float.floatToRawIntBits(v1);
        vertexData[offset + COLOR_2] = unlitColorARGB2;
        vertexData[offset + U_2] = Float.floatToRawIntBits(u2);
        vertexData[offset + V_2] = Float.floatToRawIntBits(v2);
        finishVertex();        
    }

    @Override
    public final void acceptVertex(float posX, float posY, float posZ, int unlitColorARGB0, float u0, float v0, int unlitColorARGB1, float u1, float v1,
            int unlitColorARGB2, float u2, float v2)
    {
        acceptVertex(posX, posY, posZ, Float.NaN, Float.NaN, Float.NaN, unlitColorARGB0, u0, v0, 
                unlitColorARGB1, u1, v1, unlitColorARGB2, u2, v2);
    }
    
    @Override
    public void acceptRawVertex(float posX, float posY, float posZ, int rawNormalData, int rawLightData, int unlitColorARGB0, float u0, float v0,
            int unlitColorARGB1, float u1, float v1, int unlitColorARGB2, float u2, float v2)
    {
        if(textureDepth == null)
            textureDepth = TextureDepth.SINGLE;
        else if(textureDepth != TextureDepth.SINGLE)
            throw new UnsupportedOperationException("Vertex texture depth mismatch within quad.");
        startRawVertex(posX, posY, posZ, rawNormalData, rawLightData, unlitColorARGB0, u0, v0);
        final int offset = this.vertexOffset;
        final int[] vertexData = this.vertexData;
        vertexData[offset + COLOR_1] = unlitColorARGB1;
        vertexData[offset + U_1] = Float.floatToRawIntBits(u1);
        vertexData[offset + V_1] = Float.floatToRawIntBits(v1);
        vertexData[offset + COLOR_2] = unlitColorARGB2;
        vertexData[offset + U_2] = Float.floatToRawIntBits(u2);
        vertexData[offset + V_2] = Float.floatToRawIntBits(v2);
        finishVertex();              
    }
}
