package grondag.render_hooks.api;

import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelinedVertexConsumer
{
    /**
     * For single-layer renders.<br>
     * Will throw an error if not used with {@link PipelineVertexFormat#SINGLE}<p>
     * 
     * Similar to Vanilla MC format, but always includes normals and includes a 1-byte "glow" value (0-255)
     * for each layer.  In default shaders this is used for emmissive renders, but could be used for anything.<p>
     * 
     * The consumer will also compute and send through ambient occlusion (if enabled) and a packed lightmap.<p>
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
     * @param glowBits3UB
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
            int glowBits3UB,
            int unlitColorARGB0,
            float u0,
            float v0
            );
    
    /**
     * Adds a second color and uv coordinate.<br>
     * Will throw an error if not used with {@link PipelineVertexFormat#DOUBLE}
     * 
     * @param posX
     * @param posY
     * @param posZ
     * @param normX
     * @param normY
     * @param normZ
     * @param glowBits3UB
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
            int glowBits3UB,
            int unlitColorARGB0,
            float u0,
            float v0,
            int unlitColorARGB1,
            float u1,
            float v1
            );
    
    /**
    * For triple-layer renders.<br>
    * Will throw an error if not used with {@link PipelineVertexFormat#TRIPLE}<p>
    */
    public void acceptVertex(
            float posX,
            float posY,
            float posZ,
            float normX,
            float normY,
            float normZ,
            int glowBits3UB,
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
