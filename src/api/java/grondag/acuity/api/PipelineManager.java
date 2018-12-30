package grondag.acuity.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface PipelineManager
{
    /**
     * Will return null if pipeline limit would be exceeded.
     */
    RenderPipeline createPipeline(TextureDepth textureFormat, String vertexShader, String fragmentShader);  
    
    /**
     * Use when you want standard rendering.
     */
    RenderPipeline getDefaultPipeline(TextureDepth textureFormat);

    RenderPipeline getWaterPipeline();

    RenderPipeline getLavaPipeline();

    RenderPipeline getPipelineByIndex(int index);

    /**
     * The number of seconds this world has been rendering since the last render reload,
     * including fractional seconds. Based on total world time, but shifted to 
     * originate from start of this game session. <p>
     * 
     * Use if you somehow need to know what world time is being sent to shader uniforms.
     */
    float renderSeconds();
    
}
