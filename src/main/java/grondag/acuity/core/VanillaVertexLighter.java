package grondag.acuity.core;

import grondag.acuity.api.RenderPipeline;
import grondag.acuity.api.IRenderPipeline;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VanillaVertexLighter extends CompoundVertexLighter
{
    private class ChildLighter extends PipelinedVertexLighter
    {
        private boolean areAllLayersEmissive = false;
        private float combinedShade = 1f;
        
        protected ChildLighter(IRenderPipeline pipeline)
        {
            super(pipeline);
        }
        
        @Override
        protected void resetForNewQuad()
        {
            super.resetForNewQuad();
            this.areAllLayersEmissive = false;
        }

        @Override
        public final BlockInfo getBlockInfo()
        {
            return blockInfo;
        }
        
        @Override
        public final VertexCollector getPipelineBuffer()
        {
            return target.getVertexCollector(this.pipeline);
        }
        
        @Override
        protected void reportOutput()
        {
            didOutput = true;
        }
        
        @Override
        public void setEmissive(int layerIndex, boolean isEmissive)
        {
            if(layerIndex < 0 || layerIndex > 2)
                throw new IndexOutOfBoundsException();
            
            switch (layerIndex)
            {
            case 0:
                if(isEmissive)
                    this.glowFlags |= 0b00000001;
                else
                    this.glowFlags &= 0b11111110;
                break;
            case 1:
                if(isEmissive)
                    this.glowFlags |= 0b10000000;
                else
                    this.glowFlags &= 0b01111111;
                break;
            case 2:
                if(isEmissive)
                    this.glowFlags |= 0b01111110;
                else
                    this.glowFlags &= 0b10000001;
                break;
                
            default:
                assert false : "Bad layer count.";
            }
            
            // store bits in the way we send to shader to avoid doing it later for each vertex
            final int layerCount = this.pipeline.textureFormat.layerCount();
            switch (layerCount)
            {
            case 1:
                this.areAllLayersEmissive  = this.glowFlags == 1;
                break;
            case 2:
                this.areAllLayersEmissive  = this.glowFlags == 127;
                break;
            case 3:
                this.areAllLayersEmissive  = (this.glowFlags == 255);
                break;
                
            default:
                assert false : "Bad layer count.";
            
                this.areAllLayersEmissive = false;
            }
        }

        
        @Override
        public void setBlockLightMap(int blockLightRGBF)
        {
            // convert to straight luminance value in vanilla lighting model
            // and scale to 127
            super.setBlockLightMap(Math.round(
                     (blockLightRGBF & 0xFF) * 0.2126f
                   + ((blockLightRGBF >> 8) & 0xFF) * 0.7152f
                   + ((blockLightRGBF >> 16) & 0xFF) * 0.0722f));
        }

        /**
         * Compresses alpha value to low bits of alpha component
         * and sets high bit of alpha to emissive indicator.
         * If not glowing, multiplies rgb by current shade value.
         * Swaps red and blue.
         */
        private int encodeColor(boolean glowing, int rawColor)
        {
            int blue = rawColor & 0xFF;
            int green = (rawColor >> 8) & 0xFF;
            int red = (rawColor >> 16) & 0xFF;
            int alpha = (rawColor >> 24) & 0xFF;
            alpha = Math.round(alpha / 255f * 127f);
            
            if(glowing)
                alpha |= 128;
            else
            {
                final float combinedShade = this.combinedShade;
                red = Math.round(red * combinedShade);
                green = Math.round(green * combinedShade);
                blue = Math.round(blue * combinedShade);
            }
            
            return (alpha << 24) | (blue << 16) | (green << 8) | red;
        }
        
        @Override
        protected VertexCollector startVertex(
                float posX,
                float posY,
                float posZ,
                float normX,
                float normY,
                float normZ,
                int unlitColorARGB0,
                float u0,
                float v0)
        {
                
            final BlockInfo blockInfo = getBlockInfo();
            
            // local position is vertex, + block-state-driven shift (if any);
            posX += blockInfo.getShx();
            posY += blockInfo.getShy();
            posZ += blockInfo.getShz();
            
            final VertexCollector output = getPipelineBuffer();
            final BlockPos pos = blockInfo.getBlockPos();
    
            // Compute light
            int blockLight = 0, skyLight = 0, ao = 255, shade = 255;
            
            if(this.areAllLayersEmissive)
            {
                blockLight = 255;
                skyLight = 255;
            }
            else
            {
                final float lightX = posX - .5f + normX * .5f;
                final float lightY = posY - .5f + normY * .5f;
                final float lightZ = posZ - .5f + normZ * .5f;
                
                if(this.enableDiffuse)
                    shade = Math.round(LightUtil.diffuseLight(normX, normY, normZ) * 255);
                
                if(this.enableAmbientOcclusion)
                {
                    ao = Math.round(getAo(blockInfo, lightX, lightY, lightZ) * 255);
                    if(!this.usePrecomputedLightmaps)
                    {
                        blockLight = Math.round(calcLightmap(blockInfo.getBlockLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                        skyLight = Math.round(calcLightmap(blockInfo.getSkyLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                    }
                }
                else
                {
                    if(!this.usePrecomputedLightmaps)
                    {
                        // what we get back is raw (0-15) sky << 20 | block << 4
                        // we want to output 0-255
                        final int packedLight =  this.calcPackedLight(blockInfo, normX, normY, normZ, lightX, lightY, lightZ);
                        blockLight = ((packedLight >> 4) & 0xF) * 17;
                        skyLight = ((packedLight >> 20) & 0xF) * 17;
                    }
                }
            
                blockLight = Math.max(blockLight, this.blockLightMap);
                skyLight = Math.max(skyLight, this.skyLightMap);
            }
            
            this.combinedShade = (float)shade * ao / 0xFFFF;
            
            // POSITION_3F
            output.add(target.xOffset + pos.getX() + posX);
            output.add(target.yOffset + pos.getY() + posY);
            output.add(target.zOffset + pos.getZ() + posZ);
            
            // BASE_RGBA_4UB
            output.add(encodeColor((this.glowFlags & 1) == 1, unlitColorARGB0));
            
            // BASE_TEX_2F
            output.add(u0);
            output.add(v0);
            
            // NORMAL_3UB
//            int normAo = Math.round(normX * 127 + 127);
//            normAo |= (Math.round(normY * 127 + 127) << 8);
//            normAo |= (Math.round(normZ * 127 + 127) << 16);
//            // AO 1UB
//            normAo |= (ao << 24);
//            output.add(normAo);
            
            //LIGHTMAP
            skyLight = skyLight / 17;
            blockLight = blockLight / 17;
            output.add((skyLight << 20) | (blockLight << 4));
            return output;
        }
    }

    @Override
    protected PipelinedVertexLighter createChildLighter(RenderPipeline pipeline)
    {
        return new ChildLighter(pipeline);
    }
}
