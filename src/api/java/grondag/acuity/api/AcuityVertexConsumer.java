package grondag.acuity.api;

import java.util.Random;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ExtendedBlockView;

/**
 * Quad must call {@link IPipelinedVertexConsumer#acceptVertex(IPipelinedVertex)} with
     * its vertex information.<p>
     * 
     * For tint, quad (or the model it comes from) is responsible for retrieving and applying block tint 
     * to the vertex colors.  This is done because lighter has no way to know which colors
     * should be modified when there is more than one color/texture layer. And many models don't use it.<p>
     * 
     * You can retrieve the block color from tint with {@link IPipelinedVertexConsumer#getBlockInfo()} 
     * and then {@link BlockInfo#getColorMultiplier(int tint)}; *
 */
@Environment(EnvType.CLIENT)
public interface AcuityVertexConsumer
{
    /**
     * If your model has quads co-planar with a block face, then you should
     * exclude them if this method returns false for that face.<p>
     * 
     * This is in lieu of the getQuads(EnumFacing) pattern used in BakedQuad.
     */
    public boolean shouldOutputSide(Direction side);
    
    /**
     * Provides access to in-world block position for model customization.
     */
    public BlockPos pos();
    
    /**
     * Provides access to block world for model customization.
     */
    public ExtendedBlockView world();
    
    /**
     * Provides access to block state for model customization.<br>
     * Is what normally is passed to IBakedModel and may be an IExtendedBlockState.
     */
    public BlockState blockState();
    
    /**
     * Deterministically pseudo-random based on block position.<br>
     * Will be same as what normally is passed to BakedModel but is computed
     * lazily - will not be calculated if never retrieved.
     */
    public Random random();
    
    
    public void prepare(BlockRenderLayer layer, RenderPipeline pipeline);
    
    /**
     * For single-layer renders.<p>
     * 
     * The intention of this big, ugly method is two-fold: 1) minimize the number of calls into consumer (this
     * is definitely in a hot loop) and 2) allow flexibility in vertex/quad implementation. This means you don't
     * have to map your internal vertex format (if you even have one) to some arbitrary interface.<p>
     */
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
            );
    
    /**
     * Adds a second color and uv coordinate.<br>
     */
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
    
    /**
     * Call before calling any of the acceptVertex methods. Emissive will be false at start of each quad.<p>
     * 
     * If true, means texture in given layer should be rendered at full brightness.<p>
     * 
     * Setting true will also disable shading and ambient occlusion for the given texture.<p>
     * 
     * Use this instead of assigning custom lightmaps when you want emmissive rendering.
     * This will allow for unambiguous rendering of your quad in future 
     * lighting models that may not rely on lightmaps. <p>
     */
    public void setEmissive(int layerIndex, boolean isEmissive);
    
    /**
     * Deprecation: intended for compatibility with vanilla models that use lightmaps. 
     * 
     * Can be used to enable (the default setting) or disable (what you would usually use this for)
     * the diffuse shading component of ambient lighting. Will both be true by default at start of quad. 
     * Applies to all texture layers.<p>
     * 
     * Disabling does NOT disable lighting (light maps or future illumination models still apply).  
     * To disable lighting, (and render surface full brightness) use {@link #setEmissive(int, boolean)} <p>
     * 
     * The effect of disabling diffuse shading depends on the lighting model...<p>
     * 
     * In the vanilla lighting model, this shading is arbitrary, and does not consider light direction.
     * It simply serves to make block faces look distinct.  You will want to disable it when you
     * are providing a pre-shaded quad. <p>
     * 
     * Note that pre-shaded quads are not recommended because they will not render well in future
     * lighting models. If you must, check for lighting model when producing quads and disable your
     * shading when the lighting model is anything other than vanilla.  The API will force model rebake if the
     * lighting model is changed by the user.<p>
     * 
     * In the enhanced lighting model, this shading only affects <em>block</em> light. Sky light is
     * directional and sky illumination will not be reduced by block shading. (Which is why you shouldn't
     * bake shading into your vertex colors.)  Block light is still shadowed according to Minecraft convention.<p>
     * 
     * Future lighting models that account for light direction in both block and sky light will completely
     * ignore the enableDiffuse setting.<p>
     *  
     * Note that layers with emissive rendering enabled via {@link #setEmissive(int, boolean)} will have
     * both diffuse and AO disabled and this setting will be ignored.
     */
    @Deprecated
    public void setShading(boolean enableDiffuse);
    
    /**
     * Primarily intended for vanilla model support.  Will be true by default at start of each quad.
     */
    public void setAmbientOcclusion(boolean enableAmbientOcclusion);
    
    /**
     * blockLightRBGF is a lightmap in the form of an RGB color value.  The alpha (F) value, indicates how
     * much of this light is from torches and thus modified by torch flickering. Zero means no flicker.<p>
     * 
     * This value ONLY APPLIES IN LIGHTING MODELS THAT USE BLOCK LIGHTMAPS. USE QUAD-LEVEL GLOW FOR EMISSIVE
     * SURFACE RENDERING. The supported use for lightmaps is to tweak block lighting when simple lighting 
     * models are in effect. The application of this value depends on the lighting model in effect....<p>
     * 
     * In the vanilla lighting model, this is converted into a single 0-255 (mostly monochromatic) 
     * block lightmap value that works just like vanilla block lightmaps. (Conversion is based on luminance.)
     * The light will be torch light, and will always have 100% torch flicker. (Your flicker value is ignored.) 
     * The value is a <em>minimum</em>, so vertices that are not full brightness in your lightmap
     * can still be lit at full brightness if in sunlight or if next to a light source.  (Just like vanilla)<p>
     * 
     * In the enhanced lighting model, your lightmap will be rendered in color, and will include some amount of
     * flicker if you provide a non-zero flicker value.  If your surface is next to another block light 
     * source, then your lightmap is <em>additive</em>.  For example, if you provide a dim blue lightmap and
     * your surface is next to a torch, your surface will render with mostly torch light (with flicker) but with a
     * boosted blue component. (Clamped to full white brightness.)  In direct sunlight your lightmap probably
     * won't be noticeable.<p>
     * 
     * As noted above, future lighting models that do not rely on vertex-level lightmaps will <em>ignore</em> block 
     * lightmaps entirely. For emissive rendering, use quad-level glow.<p>
     * 
     */
    public void setBlockLightMap(int blockLightRBGF);
    
    public void setBlockLightMap(int red, int green, int blue, int flicker);
    
    public void setBlockLightMap(float red, float green, float blue, float flicker);
    
    /**
     * Deprecation: only intended for vanilla model compatibility and only honored in vanilla lighting model.<p>
     * 
     * Expected values are 0-255, not 0-15.
     */
    public void setSkyLightMap(int skyLightMap);
}
