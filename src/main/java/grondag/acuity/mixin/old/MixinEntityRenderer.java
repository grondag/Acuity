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

package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import grondag.acuity.Acuity;
import grondag.acuity.hooks.IRenderGlobal;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.render.block.BlockRenderLayer;
import net.minecraft.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer
{
    @Redirect(method = "renderWorldPass", expect = 4,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I"))
    private int onRenderWorldPassRenderBlockLayer(RenderGlobal renderGlobal, BlockRenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn)
    {
        return PipelineHooks.renderBlockLayer(renderGlobal, blockLayerIn, partialTicks, pass, entityIn);
    }
 
    // These calls aren't needed when Acuity is active because shader directly handles mipmaps in texture sampler
    // Only first two instances are redirected - the block damage renderer needs the third 
    @Redirect(method = "renderWorldPass", expect = 1,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/texture/ITextureObject;setBlurMipmap(ZZ)V"))
    private void onSetBlurMipmapA(ITextureObject tex, boolean blurIn, boolean mipmapIn)
    {
        if(!Acuity.isModEnabled())
            tex.setBlurMipmap(blurIn, mipmapIn);
    }
    
    @Redirect(method = "renderWorldPass", expect = 1,
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/renderer/texture/ITextureObject;setBlurMipmap(ZZ)V"))
    private void onSetBlurMipmapB(ITextureObject tex, boolean blurIn, boolean mipmapIn)
    {
        if(!Acuity.isModEnabled())
            tex.setBlurMipmap(blurIn, mipmapIn);
    }
    
    @Redirect(method = "renderWorldPass", expect = 1,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/renderer/texture/ITextureObject;restoreLastBlurMipmap()V"))
    private void onRestoreLastBlurMipmapA(ITextureObject tex)
    {
        if(!Acuity.isModEnabled())
            tex.restoreLastBlurMipmap();
    }
    
    @Redirect(method = "renderWorldPass", expect = 1,
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/client/renderer/texture/ITextureObject;restoreLastBlurMipmap()V"))
    private void onRestoreLastBlurMipmapB(ITextureObject tex)
    {
        if(!Acuity.isModEnabled())
            tex.restoreLastBlurMipmap();
    }
    
    //    "net/minecraft/client/renderer/RenderGlobal", "setupTerrain", "(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V"

    @Redirect(method = "renderWorldPass",
            expect = 1,
            at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V"))
    private void onSetupTerrain(RenderGlobal renderGlobal, Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator)
    {
        if(Acuity.isModEnabled())
            ((IRenderGlobal)renderGlobal).setupTerrainFast(viewEntity, partialTicks, camera, frameCount, playerSpectator);
        else
            renderGlobal.setupTerrain(viewEntity, partialTicks, camera, frameCount, playerSpectator);
    }
}
