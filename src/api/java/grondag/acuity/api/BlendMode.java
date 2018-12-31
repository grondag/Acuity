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

package grondag.acuity.api;

public enum BlendMode
{
    SOLID,
    CUTOUT,
    
    /**
     * Render quads in the translucent render layer.<p>
     * 
     * Translucent rendering comes with a significant performance penalty and should
     * only be used for glass, force fields or similar surfaces that require texture
     * blending with the scene background.<p>
     * 
     * Rendered alpha value will be texture alpha multiplied by (interpolated) vertex color alpha.<br>
     * Both of those values should be greater than zero (or nothing will render) and at least
     * one should be less than 1.0 or there is no point to a translucent render.<p>
     * 
     * The same guidance applies to multi-texture quads.  Texture layers will be blended with each
     * other first and then blended with the scene background.  Secondary and tertiary layers will
     * be blended on "top" of lower texture layers. <p>
     * 
     */
    TRANSLUCENT
}
