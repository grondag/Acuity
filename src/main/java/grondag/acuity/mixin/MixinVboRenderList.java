package grondag.acuity.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.LoadingConfig;
import grondag.acuity.core.AbstractPipelinedRenderList;
import grondag.acuity.core.PipelinedRenderList;
import grondag.acuity.core.PipelinedRenderListDebug;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;

@Mixin(VboRenderList.class)
public abstract class MixinVboRenderList extends ChunkRenderContainer
{
    @SuppressWarnings("null")
    private AbstractPipelinedRenderList ext;
    
    @Inject(method = "<init>*", at = @At("RETURN"), require = 1)
    private void onConstructed(CallbackInfo ci)
    {
        ext = LoadingConfig.INSTANCE.enableRenderStats 
                ? new PipelinedRenderListDebug()
                : new PipelinedRenderList();
    }
    
    @Override
    public void addRenderChunk(RenderChunk renderChunkIn, BlockRenderLayer layer)
    {
        if(ext.isAcuityEnabled)
            ext.addRenderChunk(renderChunkIn, layer);
        else
            super.addRenderChunk(renderChunkIn, layer);
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
