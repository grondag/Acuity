package grondag.acuity.api.model;

public enum LightSource
{
    /**
     * Standard lighting.<br>
     * Object is illuminated by sky and nearby light sources. <p>
     * 
     * Ambient occlusion and diffuse shading will be applied.
     */
    WORLD,
    
    /**
     * Surface emits light and is always rendered at full brightness.<p>
     * 
     * Ambient occlusion and diffuse shading will <em>not</em> be applied.
     */
    EMISSIVE,
    
    /**
     * Add interpolated vertex light values to world lighting.<p>
     * 
     * Ambient occlusion and diffuse shading will be applied.<p>
     *      
     * In standard model, lighting is monochromatic.  Useful for some effects
     * unless or until a global illumination model is implemented.<p>
     */
    VERTEX,
    
    /**
     * Like {@link #VERTEX} but disables ambient occlusion and diffuse shading.<p>
     * 
     * Can be used with an empty lightmap to disable AO and shading with world lighting,
     * if that is somehow useful. Probably not, but we were already using two bits, so here it is.
     */
    VERTEX_UNSHADED;
    
    
}
