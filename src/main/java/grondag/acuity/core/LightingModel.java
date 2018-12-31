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

import static grondag.acuity.api.PipelineVertexFormat.*;

import grondag.acuity.api.PipelineVertexFormat;
import grondag.acuity.api.TextureDepth;

public enum LightingModel
{
    CLASSIC(VANILLA_SINGLE, VANILLA_DOUBLE, VANILLA_TRIPLE);
    
//    EHNANCED(ENHANCED_SINGLE, ENHANCED_DOUBLE, ENHANCED_TRIPLE)
//    {
//        @Override
//        public CompoundVertexLighter createLighter()
//        {
//            return new EnhancedVertexLighter();
//        }
//    };
    
    private LightingModel(PipelineVertexFormat... formatMap)
    {
        this.formatMap = formatMap;
    }
    
    private final PipelineVertexFormat[] formatMap;
    
    public PipelineVertexFormat vertexFormat(TextureDepth textureFormat)
    {
        return formatMap[textureFormat.ordinal()];
    }

    public CompoundVertexLighter createLighter()
    {
        return new VanillaVertexLighter();
    }
}
