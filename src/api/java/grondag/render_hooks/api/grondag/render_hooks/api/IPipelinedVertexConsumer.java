package grondag.render_hooks.api;

import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelinedVertexConsumer
{
    /**
     * For single-layer renders.<br>
     * 
     * Similar to Vanilla MC format, but always includes normals and more lighting information.<p>
     * 
     * The consumer will compute and send through ambient occlusion lightmap values based on world state.
     * The vertex producer can override the light maps by constructing blockGlowBits as follows:<p>
     * 1st (lsb) byte : 0-255 block light level - will use the block light map (including flicker)<br>
     * 2nd byte: 0-255 sky light level - will use sky lighting - generally not useful to override<br>
     * 3rd byte - bits 0-3: base layer glow level (0-15). Acts as a clamp on shading from ao and diffuse. 15 = full emissive.<br>
     * 3rd byte - bits 4-7: 2nd layer glow level (if present)<br>
     * 4th byte - bits 0-3: 3rd layer glow level (if present)<br>
     * 4th byte - bits 4-7: reserved<br>
     * <p>
     * 
     * The intention of this big, ugly method is two-fold: 1) minimize the number of calls into consumer (this
     * is definitely in a hot loop) and 2) allow flexibility in vertex/quad implementation. This means you don't
     * have to map your internal vertex format (if you even have one) to some arbitrary interface.<p>
     * 
     * @param posX
     * @param posY
     * @param posZ
     * @param normX
     * @param normY
     * @param normZ
     * @param blockGlowBits
     * @param unlitColorARGB0
     * @param u0
     * @param v0
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockGlowBits,
            int unlitColorARGB0,
            float u0,
            float v0
            );
    
    /**
     * Adds a second color and uv coordinate.<br>
     * 
     * @param posX
     * @param posY
     * @param posZ
     * @param normX
     * @param normY
     * @param normZ
     * @param blockGlowBits
     * @param unlitColorARGB0
     * @param u0
     * @param v0
     * @param unlitColorARGB1
     * @param u1
     * @param v1
     */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockGlowBits,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            );
    
    /**
    * For triple-layer renders.<br>
    */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int blockGlowBits,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1,
            int unlitColorARGB2,
            float u2,
            float v2
            );
    
    public BlockInfo getBlockInfo();

}
