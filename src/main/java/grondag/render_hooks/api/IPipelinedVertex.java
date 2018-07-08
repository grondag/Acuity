package grondag.render_hooks.api;

public interface IPipelinedVertex
{
    float posX();
    float posY();
    float posZ();

    float normalX();
    float normalY();
    float normalZ();

    float minimumSkyLight();
    float minimumBlockLight();

    /**
     * Bit-wise, should be ARGB
     * (blue is least significant octet, alpha is most)
     */
    int unlitColorARGB(int colorIndex);
    
    float u(int uvIndex);
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
