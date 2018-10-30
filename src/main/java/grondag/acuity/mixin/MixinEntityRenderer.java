package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import grondag.acuity.Acuity;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer
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
}
