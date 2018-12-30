package grondag.acuity.mixin.old;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.LoadingConfig;
import grondag.acuity.core.AbstractPipelinedRenderList;
import grondag.acuity.core.PipelinedRenderList;
import grondag.acuity.core.PipelinedRenderListDebug;
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
