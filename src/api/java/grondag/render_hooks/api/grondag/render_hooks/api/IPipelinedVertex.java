package grondag.render_hooks.api;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface IPipelinedVertex
{
    float posX();
    float posY();
    float posZ();

    /**
     * Always required - the API does not calculate normals for you.<p>
     * 
     * Used to compute diffuse lighting if {@link #applyDiffuse(int)} is true.<p>
     * 
     * Can be sent to GPU by extending vertex format to include normals!
     */
    float normalX();
    float normalY();
    float normalZ();

    /**
     * Forces lightmap coordinates to be no less than the given value.<p>
     * 
     * Useful for simple emissive rendering.
     * If you are rendering multiple layers and want 
     * separate lighting per layer, then you'll need
     * to use a uniform and/or other special logic in your shader(s).
     */
    float minimumSkyLight();
    
    /**
     * See {@link #minimumSkyLight()}
     */
    float minimumBlockLight();

    /**
     * Bit-wise, should be ARGB
     * (blue is least significant octet, alpha is most)
     */
    int unlitColorARGB(int colorIndex);
    
    /**
     * Texture coordinate for the given texture unit.<p>
     * 
     * uvIndex refers to the ordinal position of the UV element
     * in the order it appears in the vertex format.<p>
     * 
     * The default BLOCK vertex format includes only 1 texture
     * coordinate (other than lightmap) and in that case
     * 0 is the only valid index.<p>
     * 
     * Note that lightmap, while passed as a UV element is not part 
     * of this numbering scheme. For example, if you extend the vertex format
     * to include additional texture coordinates, the first extension
     * will be index 1.
     * 
     * It is <em>strongly<em> recommended that extended coordinates 
     * should also refer to the MC texture atlas.  If you bind
     * additional or different texture units in your pipeline,
     * you must also unbind them and restore to normal setup
     * at the end of your pipeline.  This is error prone and slow.
     * Future versions may try to support more flexibility for texture binding.<p>
     */
    float u(int uvIndex);
    
    /**
     * See {@link #u(int)}
     */
    float v(int uvIndex);
    
    /**
     * If false, color for this layer will not be adjust for normal.<p>
     * 
     * Set false for emissive rendering in vanilla-like pipelines.<br>
     * False also useful for many deferred rending scenarios.<br>
     */
    default boolean applyDiffuse(int colorIndex) { return true; }
    
    /**
     * If true, block color modifier associated with quad tint (if != -1) will be applied to vertex color.
     */
    default boolean applyTint(int colorIndex) { return true; }

    /**
     * If false, color for this layer will not be adjusted for ambient occlusion
     * if AO is enabled by the user.<p>
     * 
     * Set false for emissive rendering in vanilla-like pipelines.<br>
     */
    default boolean applyAO(int colorIndex)  { return true; }
    
}
