package grondag.render_hooks.core;

import java.nio.ByteBuffer;

import grondag.render_hooks.api.IRenderPipeline;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.LightUtil;

public class VanillaVertexLighter extends CompoundVertexLighter
{
    private class ChildLighter extends PipelinedVertexLighter
    {
        private boolean areAllLayersEmissive = false;
        
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
        
        @SuppressWarnings("null")
        @Override
        public final BufferBuilder getPipelineBuffer()
        {
            return target.getPipelineBuffer(this.pipeline);
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
                this.glowFlags &= 0b11111110;
                this.glowFlags |= 0b00000001;
                break;
            case 1:
                this.glowFlags &= 0b01111111;
                this.glowFlags |= 0b10000000;
                break;
            case 2:
                this.glowFlags &= 0b10000001;
                this.glowFlags |= 0b01111110;
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

        @Override
        protected BufferBuilder startVertex(
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
            
            final BufferBuilder target = getPipelineBuffer();
            final ByteBuffer bytes  = target.getByteBuffer();
            final BlockPos pos = blockInfo.getBlockPos();
    
            // Compute light
            int blockLight, skyLight, ao = 255, shade = 255;
            
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
                    blockLight = Math.round(calcLightmap(blockInfo.getBlockLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                    skyLight = Math.round(calcLightmap(blockInfo.getSkyLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                }
                else
                {
                    // what we get back is raw (0-15) sky << 20 | block << 4
                    // we want to output 0-255
                    final int packedLight =  this.calcPackedLight(blockInfo, normX, normY, normZ, lightX, lightY, lightZ);
                    blockLight = ((packedLight >> 4) & 0xF) * 17;
                    skyLight = ((packedLight >> 20) & 0xF) * 17;
                }
            
                blockLight = Math.max(blockLight, this.blockLightMap);
                skyLight = Math.max(skyLight, this.skyLightMap);
            }
            
            bytes.position(target.getVertexCount() * target.getVertexFormat().getNextOffset());
    
            // POSITION_3F
            bytes.putFloat((float) (target.xOffset + pos.getX() + posX));
            bytes.putFloat((float) (target.yOffset + pos.getY() + posY));
            bytes.putFloat((float) (target.zOffset + pos.getZ() + posZ));
            
            // BASE_RGBA_4UB
            putColorRGBA(bytes, unlitColorARGB0);
            
            // BASE_TEX_2F
            bytes.putFloat(u0);
            bytes.putFloat(v0);
            
            //TODO: remove
            if( Math.abs((normX * normX + normY * normY + normZ * normZ) - 1) > 0.01f)
                System.out.println("bad input normal");
            
            // NORMAL_3UB
            bytes.put((byte) Math.round(normX * 127 + 127));
            bytes.put((byte) Math.round(normY * 127 + 127));
            bytes.put((byte) Math.round(normZ * 127 + 127));
            
            // AO 1UB
            bytes.put((byte) ao);
            
            //LIGHTMAP_AND_GLOWS_4UB
            bytes.put((byte) blockLight);
            bytes.put((byte) skyLight);
            bytes.put((byte) shade);
            bytes.put((byte) this.glowFlags);
            
            return target;
        }
    }

    @Override
    protected PipelinedVertexLighter createChildLighter(IRenderPipeline pipeline)
    {
        return new ChildLighter(pipeline);
    }
}
