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

package grondag.acuity.broken;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.LoadingConfig;
import grondag.acuity.chunkrender.AbstractPipelinedRenderList;
import grondag.acuity.chunkrender.PipelinedRenderList;
import grondag.acuity.chunkrender.PipelinedRenderListDebug;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.AbstractRenderList;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.VboRenderList;

@Mixin(VboRenderList.class)
public abstract class MixinVboRenderList extends AbstractRenderList
{
    private AbstractPipelinedRenderList ext;
    
    @Inject(method = "<init>*", at = @At("RETURN"), require = 1)
    private void onConstructed(CallbackInfo ci)
    {
        ext = LoadingConfig.INSTANCE.enableRenderStats 
                ? new PipelinedRenderListDebug()
                : new PipelinedRenderList();
    }
    
    @Override
    public void add(ChunkRenderer renderChunkIn, BlockRenderLayer layer)
    {
        if(ext.isAcuityEnabled)
            ext.add(renderChunkIn, layer);
        else
            super.add(renderChunkIn, layer);
    }
    
    
    
    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn)
    {
        if(ext.isAcuityEnabled)
            ext.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
        else
            super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);
    }

    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRenderChunkLayer(BlockRenderLayer layer, CallbackInfo ci)
    {
        if(ext.isAcuityEnabled)
        {
            ext.renderChunkLayer(layer);
            ci.cancel();
        }
    }
}
