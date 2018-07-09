package grondag.render_hooks.api;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IRenderPipeline
{
    /**
     * Called before VBO rendering.</br>
     * 
     * MUST set up vertex bindings. Default setup is for vanilla Block vertex format.<br>
     * Should NOT change matrix, translation or handle buffer binding.</p>
     */
    public void preDraw();
    
    /**
     * Called before display list rendering
     */
    public void preDrawList();

    /**
     * Called after VBO rendering
     */
    public void postDraw();
    
    /**
     * Called after display list rendering
     */
    public void postDrawList();

    /**
     * Target vertex buffer format to be used for this pipeline.
     * Defaults to BLOCK format.
     */
    @Nonnull
    public default VertexFormat vertexFormat()
    {
        return DefaultVertexFormats.BLOCK;
    }

    /**
     * Must return the value assigned via {@link #assignIndex(int)}.<br>
     * Note this is only unique per BlockRenderLayer.<br>
     * The first index (0) in each layer refers to the vanilla MC pipeline.<br>
     */
    public int getIndex();

    /**
     * Called 1X when pipelines are registered with manager.<br>
     * Use for no other purpose.<br>
     * See {@link #getIndex()}
     */
    public void assignIndex(int n);

    /**
     * How many (non-lightmap) UV elements?
     */
    public default int uvCount() { return 1; }
    
    /**
     * How many color elements?
     */
    public default int colorCount() { return 1; }

    /**
     * If this pipeline uses the lightmap, should be the index of
     * the UV element that accept the lightmap texture coordinates.<p>
     * 
     * In vanilla pipelines, this is 1 (the second UV element.)
     * You don't have to use that index, but you must override this
     * to the index of a UV element in your format to access the lightmap
     * in your pipeline.<p>
     * 
     * For safety, the default implementation assumes lightmap is not used 
     * and return -1 so lighter does not mistakenly output a lightmap instead
     * of your texture coordinates.
     */
    public default int lightmapIndex() { return -1; }
  
//    Not sure if this is needed yet...
//    
//    /**
//     * If true, the pipeline vertex format includes a single byte
//     * that contains bit flags (per layer) to indicate which layer(s)
//     * should use the lightmap for this block.<p>
//     * 
//     * Only useful in situations when there are multiple layers
//     * to be processed during deferred rendering and not all of them
//     * should use the lightmap. If you just want full-brightness rendering, 
//     * it is much easier to use {@link IPipelinedVertex#getMinimumBrightness()}.
//     * Or if you don't care about lightmap, simply omit it from the format and
//     * disable it in the pipeline.<p>
//
//     * If true, then {@link #lightMapFlagIndex()} must point to an
//     * unsigned byte vertex element with usage of PADDING or GENERIC.<p>
//     * 
//     * Bits are set least-first by layer. So, for example,
//     * if true for layer 0, the value will be 1.<p>
//     * 
//     * If/how a pipeline uses this value is up to the implementor.
//     * Generally, a 1 means apply the lightmap and 0 means don't.
//     * Also note this value should not be interpolated directly.<p>
//     * 
//     * If not present, a pipeline will generally have to assume that the
//     * lightmap applies to all layers (if there is only one). Intention
//     * is to allow for re-use of shaders/pipelines for layered quads.<p>
//     * 
//     * If not used, lightmap logic can also be hard-coded into the pipeline
//     * shader(s), but this is wasteful of resources unless the pipeline is very specialized.
//     */
//    public default boolean hasLightmapFlags() {return lightMapFlagIndex() >= 0;}
//    
//    /**
//     * See {@link #hasLightmapFlags()}
//     */
//    public default int lightMapFlagIndex() { return -1; };
}
