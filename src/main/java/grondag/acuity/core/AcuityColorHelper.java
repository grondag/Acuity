/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.acuity.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AcuityColorHelper
{
    /**
     * Multiplies two colors component-wise and returns result
     */
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
     * Multiplies RGB by given factor and swaps the R and B components, leaving alpha intact.<br>
     * In game code/data, colors are generally in ARGB order (left to right = high to low).<br>
     * OpenGL wants them in ABGR order (left to right = high to low).<br>
     */
    public static int shadeColorAndSwapRedBlue(int colorARGB, float shade)
    {
        int blue = colorARGB & 0xFF;
        int green = (colorARGB >> 8) & 0xFF;
        int red = (colorARGB >> 16) & 0xFF;
        
        if(shade !=  1.0f)
        {
            blue = Math.round(blue * shade);
            green = Math.round(green * shade);
            red = Math.round(red * shade);
        }
    
        return (colorARGB & 0xFF000000) | (blue << 16) | (green << 8) | red;
    }
    
    /**
     * 
     */
    public static int swapRedBlue(int color)
    {
        return (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
    }

}
