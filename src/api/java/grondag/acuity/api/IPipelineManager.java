package grondag.acuity.api;


public interface IPipelineManager
{
    /**
     * Will return null if pipeline limit would be exceeded.
     */
    IRenderPipeline createPipeline(TextureFormat textureFormat, String vertexShader, String fragmentShader);  
    
    /**
     * Use when you want standard rendering.
     */
    IRenderPipeline getDefaultPipeline(TextureFormat textureFormat);

    IRenderPipeline getWaterPipeline();

    IRenderPipeline getLavaPipeline();

    IRenderPipeline getPipelineByIndex(int index);

    /**
     * The number of seconds this world has been rendering since the last render reload,
     * including fractional seconds. Based on total world time, but shifted to 
     * originate from start of this game session. <p>
     * 
     * Use if you somehow need to know what world time is being sent to shader uniforms.
     */
    float renderSeconds();
    
}
