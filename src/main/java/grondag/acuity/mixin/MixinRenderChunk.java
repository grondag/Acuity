package grondag.acuity.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import grondag.acuity.Acuity;
import grondag.acuity.buffering.DrawableChunk.Solid;
import grondag.acuity.buffering.DrawableChunk.Translucent;
import grondag.acuity.hooks.ChunkRebuildHelper;
import grondag.acuity.hooks.IRenderChunk;
import grondag.acuity.hooks.ISetVisibility;
import grondag.acuity.hooks.PipelineHooks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.chunk.Chunk;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IRenderChunk
{
    @Shadow public static int renderChunksUpdated;

    @Shadow public CompiledChunk compiledChunk;
    @Shadow private BlockPos.MutableBlockPos position;
    @Shadow private ChunkCache worldView;
    @Shadow abstract void preRenderBlocks(BufferBuilder bufferBuilderIn, BlockPos pos);
    @Shadow abstract void postRenderBlocks(BlockRenderLayer layer, float x, float y, float z, BufferBuilder bufferBuilderIn, CompiledChunk compiledChunkIn);
    @Shadow private ReentrantLock lockCompileTask;
    @Shadow private Set<TileEntity> setTileEntities;
    @Shadow private RenderGlobal renderGlobal;

    @Nullable Solid solidDrawable;
    @Nullable Translucent translucentDrawable;

    @Override
    public @Nullable Solid getSolidDrawable()
    {
        return solidDrawable;
    }

    @Override
    public @Nullable Translucent getTranslucentDrawable()
    {
        return translucentDrawable;
    }

    @Redirect(method = "setPosition", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;initModelviewMatrix()V"))
    private void onInitModelviewMatrix(RenderChunk renderChunk)
    {
        PipelineHooks.renderChunkInitModelViewMatrix(renderChunk);
    }

//    @Redirect(method = "rebuildChunk", require = 1,
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))
//    private void onSetVisibility(CompiledChunk compiledChunk, SetVisibility setVisibility)
//    {
//        compiledChunk.setVisibility(setVisibility);
//        PipelineHooks.mergeRenderLayers(compiledChunk);
//    }

    @Inject(method = "deleteGlResources*", at = @At("RETURN"), require = 1)
    private void onDeleteGlResources(CallbackInfo ci)
    {
        releaseDrawables();
    }

    @Inject(method = "setCompiledChunk", require = 1, 
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk"))
    private void onSetCompiledChunk(CallbackInfo ci)
    {
        ((ISetVisibility)compiledChunk.setVisibility).releaseVisibilityData();
    }

    @Inject(method = "stopCompileTask", require = 1, 
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;compiledChunk"))
    private void onStopCompiledChunk(CallbackInfo ci)
    {
        ((ISetVisibility)compiledChunk.setVisibility).releaseVisibilityData();
    }

    @Override
    public void setSolidDrawable(Solid drawable)
    {
        solidDrawable = drawable;
    }

    @Override
    public void setTranslucentDrawable(Translucent drawable)
    {
        translucentDrawable = drawable;
    }

    @Override
    public void releaseDrawables()
    {
        if(solidDrawable != null)
        {
            solidDrawable.clear();
            solidDrawable = null;
        }

        if(translucentDrawable != null)
        {
            translucentDrawable.clear();
            translucentDrawable = null;
        }
    }

    @Inject(method = "rebuildChunk", at = @At("HEAD"), cancellable = true, require = 1)
    private void onRebuildChunk(final float x, final float y, final float z, final ChunkCompileTaskGenerator generator, final CallbackInfo ci)
    {
        if(!Acuity.isModEnabled())
            return;

        
        final ChunkRebuildHelper help = ChunkRebuildHelper.get();
        help.clear();

        //PERF: reuse
        final CompiledChunk compiledChunk = new CompiledChunk();
        final MutableBlockPos minPos = this.position;

        generator.getLock().lock();

        try
        {
            if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING)
                return;

            generator.setCompiledChunk(compiledChunk);
        }
        finally
        {
            generator.getLock().unlock();
        }

        //PERF: reuse
        final VisGraph visGraph = new VisGraph();
        final HashSet<TileEntity> tileEntities = help.tileEntities;

        if (!this.worldView.isEmpty())
        {
            ++renderChunksUpdated;
            final boolean[] layerFlags = help.layerFlags;
            final BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            final MutableBlockPos searchPos = help.searchPos;
            final int xMin = minPos.getX();
            final int yMin = minPos.getY();
            final int zMin = minPos.getZ();
            final BufferBuilder[] builders = help.builders(generator.getRegionRenderCacheBuilder());
            
            for(int xPos = 0; xPos < 16; xPos++)
            {
                for(int yPos = 0; yPos < 16; yPos++)
                {
                    for(int zPos = 0; zPos < 16; zPos++)
                    {
                        searchPos.setPos(xMin + xPos, yMin + yPos, zMin + zPos);
                        final IBlockState iblockstate = this.worldView.getBlockState(searchPos);
                        final Block block = iblockstate.getBlock();

                        if (iblockstate.isOpaqueCube())
                            visGraph.setOpaqueCube(searchPos);

                        if (block.hasTileEntity(iblockstate))
                        {
                            final TileEntity tileentity = this.worldView.getTileEntity(searchPos, Chunk.EnumCreateEntityType.CHECK);

                            if (tileentity != null)
                            {
                                TileEntitySpecialRenderer<TileEntity> tileentityspecialrenderer = TileEntityRendererDispatcher.instance.<TileEntity>getRenderer(tileentity);

                                if (tileentityspecialrenderer != null)
                                {
                                    if (tileentityspecialrenderer.isGlobalRenderer(tileentity))
                                        tileEntities.add(tileentity);
                                    else 
                                        compiledChunk.addTileEntity(tileentity); // FORGE: Fix MC-112730
                                }
                            }
                        }
                        
                        for(int i = 0; i < help.layerCount; i++)
                        {
                            final BlockRenderLayer layer = help.layers[i];
                            if(!block.canRenderInLayer(iblockstate, layer)) 
                                continue;
                            
                            net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer);

                            if (block.getDefaultState().getRenderType() != EnumBlockRenderType.INVISIBLE)
                            {
                                if (!compiledChunk.isLayerStarted(layer))
                                {
                                    compiledChunk.setLayerStarted(layer);
                                    this.preRenderBlocks(builders[i], minPos);
                                }

                                layerFlags[i] |= blockrendererdispatcher.renderBlock(iblockstate, searchPos, this.worldView, builders[i]);
                            }
                        }
                        net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);
                    }
                }
            }
           

            for(int i = 0; i < help.layerCount; i++)
            {
                final BlockRenderLayer layer = help.layers[i];
                if (layerFlags[i])
                    compiledChunk.setLayerUsed(layer);

                if (compiledChunk.isLayerStarted(layer))
                    this.postRenderBlocks(layer, x, y, z, builders[i], compiledChunk);
            }
        }

        compiledChunk.setVisibility(visGraph.computeVisibility());
        
        PipelineHooks.mergeRenderLayers(compiledChunk);
        
        this.lockCompileTask.lock();

        try
        {
            help.tileEntitiesToAdd.addAll(tileEntities);
            help.tileEntitiesToRemove.addAll(this.setTileEntities);

            help.tileEntitiesToAdd.removeAll(this.setTileEntities);
            help.tileEntitiesToRemove.removeAll(tileEntities);

            this.setTileEntities.clear();
            this.setTileEntities.addAll(tileEntities);
            this.renderGlobal.updateTileEntities(help.tileEntitiesToRemove, help.tileEntitiesToAdd);
        }
        finally
        {
            this.lockCompileTask.unlock();
        }
        
        ci.cancel();
    }
}
