package grondag.render_hooks.core;

public class Useful
{

    public static int multiplyColor(int color1, int color2)
    {
        if(color1 == 0xFFFFFFFF)
            return color2;
        else if(color2 == 0xFFFFFFFF)
            return color1;
        else
        {
            int red = Math.round((color1 & 0xFF) * (color2 & 0xFF) / 255f);
            int green = Math.round(((color1 >> 8) & 0xFF) * ((color2 >> 8) & 0xFF) / 255f);
            int blue = Math.round(((color1 >> 16) & 0xFF) * ((color2 >> 16) & 0xFF) / 255f);
            int alpha = Math.round(((color1 >> 24) & 0xFF) * ((color2 >> 24) & 0xFF) / 255f);
            return (alpha << 24) | (blue << 16) | (green << 8) | red;
        }
    }

    /**
     * Multiplies RGB by given factor and returns with alpha unmodified.
     */
    public static int shadeColor(int colorRGBA, float shade)
    {
        if(shade ==  1.0f)
            return colorRGBA;
        
        int red = Math.round((colorRGBA & 0xFF) * shade);
        int green = Math.round(((colorRGBA >> 8) & 0xFF) * shade);
        int blue = Math.round(((colorRGBA >> 16) & 0xFF) * shade);
    
        return (colorRGBA & 0xFF000000) | (blue << 16) | (green << 8) | red;
    }
    
    /**
     * Swaps the R and B components, because OpenGL seems to want it that way.
     * TODO: Find out why this hack is needed.  There is an extension for vertex attributes
     * that forces BGRA component ordering for DirectX compatibility, but not obvious if/where
     * we would be activating it.  Or byte ordering is fundamentally wrong somewhere.
     * Could also be an endianess problem, but intel is little-endian so shouldn't be an issue.
     * Also, endian problem should break alpha and alpha is fine.
     */
    public static int swizzleColor(int colorRGBA)
    {
        return (colorRGBA & 0xFF00FF00) | ((colorRGBA >> 16) & 0xFF) | ((colorRGBA & 0xFF) << 16);
    }

}
